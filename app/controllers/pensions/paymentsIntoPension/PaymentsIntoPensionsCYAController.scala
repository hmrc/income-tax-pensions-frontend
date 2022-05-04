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

import config.{AppConfig, ErrorHandler}
import controllers.predicates.AuthorisedAction
import forms.{No, Yes}
import models.mongo.PensionsCYAModel
import models.pension.AllPensionsData
import models.pension.charges.{PensionAnnualAllowancesViewModel, PensionLifetimeAllowancesViewModel}
import models.pension.reliefs.PaymentsIntoPensionViewModel
import models.pension.statebenefits.{IncomeFromPensionsViewModel, StateBenefit, StateBenefitViewModel, UkPensionIncomeViewModel}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.paymentsIntoPensions.PaymentsIntoPensionsCYAView

import javax.inject.Inject
import models.redirects.ConditionalRedirect
import services.RedirectService.{PaymentsIntoPensionsRedirects, redirectBasedOnCurrentAnswers}
import utils.PaymentsIntoPensionPages.CheckYourAnswersPage

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

      (cya, prior) match {
        case (Some(_), _) =>
          redirectBasedOnCurrentAnswers(taxYear, cya)(redirects(_, taxYear)) { data =>
            Future.successful(Ok(view(taxYear, data.pensions.paymentsIntoPension)))
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

        if (comparePriorData(model.pensions, prior)) {
          //        TODO - build submission model from cya data and submit to DES if cya data doesn't match prior data
          //        val submissionModel = AllPensionsData(None, None, None)
          Future.successful(Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear)))
        } else {
          Future.successful(Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear)))
        }
      }
    }
  }

  private def getAboveLifetimeAllowanceQuestion(prior: AllPensionsData): Option[Boolean] = {
    if (prior.pensionCharges.flatMap(a => a.pensionSavingsTaxCharges).map(
      _.benefitInExcessOfLifetimeAllowance).isDefined || prior.pensionCharges.flatMap(
      a => a.pensionSavingsTaxCharges).map(_.lumpSumBenefitTakenInExcessOfLifetimeAllowance).isDefined) {
      Some(true)
    } else {
      None
    }
  }

  // scalastyle:off method.length
  private def generateCyaFromPrior(prior: AllPensionsData): PensionsCYAModel = {

    val statePension: Option[StateBenefit] = prior.stateBenefits.flatMap(_.stateBenefits.flatMap(_.statePension))
    val statePensionLumpSum: Option[StateBenefit] = prior.stateBenefits.flatMap(_.stateBenefits.flatMap(_.statePensionLumpSum))

    //TODO: get the list of UK income from the prior data as the combined list from
    // the hmrcEmploymentData and customerEmploymentData when it's been added to the backend data
    val getUkPensionIncome: Seq[UkPensionIncomeViewModel] = {

      Seq(UkPensionIncomeViewModel(
        employmentId = Some("00000000-0000-1000-8000-000000000001"),
        pensionId = Some("Some Customer ref 1"),
        startDate = Some("2019-10-23"),
        endDate = Some("2020-10-24"),
        pensionSchemeName = Some("pension name 1"),
        pensionSchemeRef = Some("666/66666"),
        amount = Some(211.33),
        taxPaid = Some(14.77),
        isCustomerEmploymentData = Some(true)
      ),
        UkPensionIncomeViewModel(
          employmentId = Some("00000000-0000-1000-8000-000000000002"),
          pensionId = Some("Some Customer ref 2"),
          startDate = Some("2019-11-23"),
          endDate = Some("2020-11-24"),
          pensionSchemeName = Some("pension name 2"),
          pensionSchemeRef = Some("777/77777"),
          amount = Some(311.44),
          taxPaid = Some(34.88),
          isCustomerEmploymentData = Some(true)
        ),
        UkPensionIncomeViewModel(
          employmentId = Some("00000000-0000-1000-8000-000000000003"),
          pensionId = Some("Some hmrc ref 3"),
          startDate = Some("2019-07-23"),
          endDate = Some("2020-07-24"),
          pensionSchemeName = Some("pension name 3"),
          pensionSchemeRef = Some("888/88888"),
          amount = Some(411.55),
          taxPaid = Some(44.89),
          isCustomerEmploymentData = Some(false)
        ),
        UkPensionIncomeViewModel(
          employmentId = Some("00000000-0000-1000-8000-000000000004"),
          pensionId = Some("Some hmrc ref 4"),
          startDate = Some("2019-09-23"),
          endDate = Some("2020-09-24"),
          pensionSchemeName = Some("pension name 4"),
          pensionSchemeRef = Some("999/99999"),
          amount = Some(514.56),
          taxPaid = Some(55.12),
          isCustomerEmploymentData = Some(false)
        )
      )

    }

    PensionsCYAModel(
      PaymentsIntoPensionViewModel(
        prior.pensionReliefs.map(a => a.pensionReliefs.regularPensionContributions.isDefined),
        prior.pensionReliefs.flatMap(a => a.pensionReliefs.regularPensionContributions),
        prior.pensionReliefs.map(a => a.pensionReliefs.oneOffPensionContributionsPaid.isDefined),
        prior.pensionReliefs.flatMap(a => a.pensionReliefs.oneOffPensionContributionsPaid),
        Some(true),
        prior.pensionReliefs.map(a =>
          a.pensionReliefs.retirementAnnuityPayments.isDefined || a.pensionReliefs.paymentToEmployersSchemeNoTaxRelief.isDefined
        ),
        prior.pensionReliefs.map(a => a.pensionReliefs.retirementAnnuityPayments.isDefined),
        prior.pensionReliefs.flatMap(a => a.pensionReliefs.retirementAnnuityPayments),
        prior.pensionReliefs.map(a => a.pensionReliefs.paymentToEmployersSchemeNoTaxRelief.isDefined),
        prior.pensionReliefs.flatMap(a => a.pensionReliefs.paymentToEmployersSchemeNoTaxRelief)
      ),

      //TODO: validate and amend if necessary when building the annual allowance CYA page
      PensionAnnualAllowancesViewModel(
        prior.pensionCharges.flatMap(a => a.pensionSavingsTaxCharges).map(_.isAnnualAllowanceReduced),
        prior.pensionCharges.flatMap(a => a.pensionSavingsTaxCharges).flatMap(_.moneyPurchasedAllowance),
        prior.pensionCharges.flatMap(a => a.pensionSavingsTaxCharges).flatMap(_.taperedAnnualAllowance),
        prior.pensionCharges.map(a => a.pensionContributions.isDefined),
        prior.pensionCharges.flatMap(a => a.pensionContributions).map(_.inExcessOfTheAnnualAllowance),
        prior.pensionCharges.flatMap(a => a.pensionContributions.map(x => x.annualAllowanceTaxPaid)) match {
          case Some(taxVal) if taxVal > 0 => Some(Yes.toString)
          case _ => Some(No.toString)
        },
        prior.pensionCharges.flatMap(a => a.pensionContributions).map(_.annualAllowanceTaxPaid),
        prior.pensionCharges.flatMap(a => a.pensionContributions).map(_.pensionSchemeTaxReference)
      ),

      //TODO: validate and amend if necessary when building the lifetime allowance CYA page
      pensionLifetimeAllowances = PensionLifetimeAllowancesViewModel(
        aboveLifetimeAllowanceQuestion = getAboveLifetimeAllowanceQuestion(prior),
        pensionAsLumpSumQuestion = prior.pensionCharges.flatMap(
          a => a.pensionSavingsTaxCharges).map(_.lumpSumBenefitTakenInExcessOfLifetimeAllowance.isDefined),
        pensionAsLumpSum = prior.pensionCharges.flatMap(
          a => a.pensionSavingsTaxCharges).flatMap(_.lumpSumBenefitTakenInExcessOfLifetimeAllowance),
        pensionPaidAnotherWayQuestion = prior.pensionCharges.flatMap(
          a => a.pensionSavingsTaxCharges).map(_.benefitInExcessOfLifetimeAllowance.isDefined),
        pensionPaidAnotherWay = prior.pensionCharges.flatMap(
          a => a.pensionSavingsTaxCharges).flatMap(_.benefitInExcessOfLifetimeAllowance)

      ),

      //TODO: validate as necessary on building CYA page
      incomeFromPensions = IncomeFromPensionsViewModel(
        statePension = getStatePensionModel(statePension),
        statePensionLumpSum = getStatePensionModel(statePensionLumpSum),
        //TODO: set the question below based on the list from backend
        uKPensionIncomesQuestion = Some(getUkPensionIncome.nonEmpty),
        uKPensionIncomes = getUkPensionIncome
      )
    )
  }
  // scalastyle:on method.length

  private def getStatePensionModel(statePension: Option[StateBenefit]): Option[StateBenefitViewModel] = {
    statePension match {
      case Some(benefit) => Some(StateBenefitViewModel(
        benefitId = Some(benefit.benefitId),
        startDateQuestion = Some(true),
        startDate = Some(benefit.startDate),
        endDateQuestion = Some(benefit.endDate.isDefined),
        endDate = benefit.endDate,
        submittedOnQuestion = Some(benefit.submittedOn.isDefined),
        submittedOn = benefit.submittedOn,
        dateIgnoredQuestion = Some(benefit.dateIgnored.isDefined),
        dateIgnored = benefit.dateIgnored,
        amountPaidQuestion = Some(benefit.amount.isDefined),
        amount = benefit.amount,
        taxPaidQuestion = Some(benefit.taxPaid.isDefined),
        taxPaid = benefit.taxPaid)
      )
      case _ => None
    }
  }

  private def comparePriorData(cyaData: PensionsCYAModel, priorData: Option[AllPensionsData]): Boolean = {
    priorData match {
      case None => true
      case Some(prior) => !cyaData.equals(generateCyaFromPrior(prior))
    }
  }

  private def redirects(cya: PensionsCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    PaymentsIntoPensionsRedirects.journeyCheck(CheckYourAnswersPage, cya, taxYear)
  }

}
