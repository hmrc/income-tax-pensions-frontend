/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.pensions.paymentsIntoPension

import common.AnnualAllowanceTaxPaidValues
import config.{AppConfig, ErrorHandler}
import controllers.predicates.AuthorisedAction
import models.mongo.PensionsCYAModel
import models.pension.AllPensionsData
import models.pension.charges.PensionAnnualAllowancesViewModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.PaymentsIntoPensionsCYAView

import javax.inject.Inject
import scala.concurrent.Future

class PaymentsIntoPensionsCYAController @Inject()(implicit val cc: MessagesControllerComponents,
                                                  authAction: AuthorisedAction,
                                                  appConfig: AppConfig,
                                                  view: PaymentsIntoPensionsCYAView,
                                                  pensionSessionService: PensionSessionService,
                                                  errorHandler: ErrorHandler,
                                                  clock: Clock
                                                 ) extends FrontendController(cc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getAndHandle(taxYear, request.user) { (cya, prior) =>
      (cya.map(_.pensions), prior) match {
        case (Some(cyaData), _) =>
          if(cyaData.paymentsIntoPension.isFinished) {
            Future.successful(Ok(view(taxYear, cyaData.paymentsIntoPension)))
          } else {
            //TODO - redirect to first unanswered question
            Future.successful(Redirect(controllers.pensions.paymentsIntoPension.routes.ReliefAtSourcePensionsController.show(taxYear)))
          }
        case (None, Some(priorData)) =>
          val cyaModel = generateCyaFromPrior(priorData)
          pensionSessionService.createOrUpdateSessionData(request.user,
            cyaModel, taxYear, isPriorSubmission = false)(
            errorHandler.internalServerError())(
            Ok(view(taxYear, cyaModel.paymentsIntoPension))
          )
        case _ => Future.successful(Redirect(controllers.pensions.paymentsIntoPension.routes.ReliefAtSourcePensionsController.show(taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getAndHandle(taxYear, request.user) { (cya, prior) =>
      cya.fold(
        Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      ) { model =>

        if(comparePriorData(model.pensions, prior)){
          //        TODO - build submission model from cya data and submit to DES if cya data doesn't match prior data
          //        val submissionModel = AllPensionsData(None, None, None)
          Future.successful(Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear)))
        } else {
          Future.successful(Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear)))
        }
      }
    }
  }

  private def generateCyaFromPrior(prior: AllPensionsData): PensionsCYAModel = {

    PensionsCYAModel(
      PaymentsIntoPensionViewModel(
        prior.pensionReliefs.map(a => a.pensionReliefs.regularPensionContributions.isDefined),
        prior.pensionReliefs.flatMap(a => a.pensionReliefs.regularPensionContributions),
        prior.pensionReliefs.map(a => a.pensionReliefs.oneOffPensionContributionsPaid.isDefined),
        prior.pensionReliefs.flatMap(a => a.pensionReliefs.oneOffPensionContributionsPaid),
        prior.pensionReliefs.map(a =>
          a.pensionReliefs.retirementAnnuityPayments.isDefined || a.pensionReliefs.paymentToEmployersSchemeNoTaxRelief.isDefined
        ),
        prior.pensionReliefs.map(a => a.pensionReliefs.retirementAnnuityPayments.isDefined),
        prior.pensionReliefs.flatMap(a => a.pensionReliefs.retirementAnnuityPayments),
        prior.pensionReliefs.map(a => a.pensionReliefs.paymentToEmployersSchemeNoTaxRelief.isDefined),
        prior.pensionReliefs.flatMap(a => a.pensionReliefs.paymentToEmployersSchemeNoTaxRelief)
      ),

      //TODO: validate and amend when building the annual allowance CYA page
      PensionAnnualAllowancesViewModel(

        prior.pensionCharges.flatMap(a => a.pensionSavingsTaxCharges).map(_.isAnnualAllowanceReduced),
        prior.pensionCharges.flatMap(a => a.pensionSavingsTaxCharges).flatMap(_.moneyPurchasedAllowance),
        prior.pensionCharges.flatMap(a => a.pensionSavingsTaxCharges).flatMap(_.taperedAnnualAllowance),
        prior.pensionCharges.map(a => a.pensionContributions.isDefined),
        prior.pensionCharges.flatMap(a => a.pensionContributions).map(_.inExcessOfTheAnnualAllowance),
        // yes, no, and intend to pay are the values for this but from prior are always either yes or no
        //TODO: For the following two, if you answered 'no' and 'yes' in any order you still have to populate the amount for both
        // i.e. if one is supplied the other is mandatory. The only way to resolve that might be to populate it with 0.00 for the 'No' question
        // We may need to decide in that case that the question is set to false if we check the value as zero. Not done here so far.
        if(prior.pensionCharges.map(a => a.pensionContributions).isDefined) Some(AnnualAllowanceTaxPaidValues.yes) else Some(AnnualAllowanceTaxPaidValues.no),
        prior.pensionCharges.flatMap(a => a.pensionContributions).map(_.annualAllowanceTaxPaid),
        prior.pensionCharges.flatMap(a => a.pensionContributions).map(_.pensionSchemeTaxReference)
      )
    )
  }

  private def comparePriorData(cyaData: PensionsCYAModel, priorData: Option[AllPensionsData]): Boolean = {
    priorData match {
      case None => true
      case Some(prior) => !cyaData.equals(generateCyaFromPrior(prior))
    }
  }

}
