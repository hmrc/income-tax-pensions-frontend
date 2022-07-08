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
import controllers.predicates.TaxYearAction.taxYearAction
import models.mongo.PensionsCYAModel
import models.pension.AllPensionsData
import models.redirects.ConditionalRedirect
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.RedirectService.{PaymentsIntoPensionsRedirects, redirectBasedOnCurrentAnswers}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import utils.PaymentsIntoPensionPages.CheckYourAnswersPage
import views.html.pensions.paymentsIntoPensions.PaymentsIntoPensionsCYAView

import javax.inject.Inject
import scala.concurrent.Future

class PaymentsIntoPensionsCYAController @Inject()(authAction: AuthorisedAction,
                                                  view: PaymentsIntoPensionsCYAView,
                                                  pensionSessionService: PensionSessionService,
                                                  errorHandler: ErrorHandler)
                                                 (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport {

    def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async { implicit request =>
      pensionSessionService.getAndHandle(taxYear, request.user) { (cya, prior) =>

        (cya, prior) match {
          case (Some(_), _) =>
            redirectBasedOnCurrentAnswers(taxYear, cya)(redirects(_, taxYear)) { data =>
              Future.successful(Ok(view(taxYear, data.pensions.paymentsIntoPension)))
            }
          case (None, Some(priorData)) =>
            val cyaModel = pensionSessionService.generateCyaFromPrior(priorData)
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


    private def comparePriorData(cyaData: PensionsCYAModel, priorData: Option[AllPensionsData]): Boolean = {
      priorData match {
        case None => true
        case Some(prior) => !cyaData.equals(pensionSessionService.generateCyaFromPrior(prior))
      }
    }

    private def redirects(cya: PensionsCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
      PaymentsIntoPensionsRedirects.journeyCheck(CheckYourAnswersPage, cya, taxYear)
    }

  }
