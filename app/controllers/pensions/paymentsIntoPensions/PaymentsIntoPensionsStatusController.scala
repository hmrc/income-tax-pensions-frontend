/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.pensions.paymentsIntoPensions

import config.{AppConfig, ErrorHandler}
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import models.{AuthorisationRequest, User}
import models.mongo.PensionsCYAModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import play.api.i18n.I18nSupport
import utils.Clock
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.paymentsIntoPensions.PaymentsIntoPensionsStatusView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentsIntoPensionsStatusController @Inject()(authAction: AuthorisedAction,
                                                     formProvider: PaymentsIntoPensionFormProvider,
                                                     view: PaymentsIntoPensionsStatusView,
                                                     pensionSessionService: PensionSessionService,
                                                     errorHandler: ErrorHandler,
                                                     clock: Clock)
                                                (implicit cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async {
    implicit request =>
      Future.successful(
        Ok(view(taxYear, formProvider.paymentsIntoPensionsStatusForm(request.user.isAgent)))
      )
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)).async
  { implicit request =>

      formProvider.paymentsIntoPensionsStatusForm(request.user.isAgent).bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(taxYear, formWithErrors))),
        yesNoAnswer =>
          pensionSessionService.getAndHandle(taxYear, request.user) {
            case (sessionData, prior) =>
              val pensionsCya: PensionsCYAModel = {
                if (!yesNoAnswer) {
                  PensionsCYAModel.emptyModels.copy(PaymentsIntoPensionViewModel().copy(gateway = Some(yesNoAnswer)))
                } else {
                  sessionData.fold{PensionsCYAModel.emptyModels.copy(PaymentsIntoPensionViewModel().copy(gateway = Some(yesNoAnswer)))}
                  {pensionsUserData => pensionsUserData.pensions.copy(pensionsUserData.pensions.paymentsIntoPension.copy(gateway = Some(yesNoAnswer)))}
                }
              }

              pensionSessionService.createOrUpdateSessionData[Either[Status, Result]](request.user, pensionsCya, taxYear, sessionData.fold(false)
              (sd => sd.isPriorSubmission)) {
                Left(BadRequest)
              } {
                if (pensionsCya.paymentsIntoPension.isFinished) {
                  if (sessionData.isEmpty) {
                    Right(Redirect(controllers.pensions.paymentsIntoPensions.routes.PaymentsIntoPensionsCYAController.show(taxYear)))
                  } else {

                    if (!yesNoAnswer && prior.isDefined) {
                      updateSessionToZero(taxYear)(request.user, request)
                      Right(Redirect(controllers.pensions.paymentsIntoPensions.routes.PaymentsIntoPensionsCYAController.show(taxYear)))
                    } else if(!yesNoAnswer && prior.isEmpty){
                      Right(Redirect(controllers.pensions.paymentsIntoPensions.routes.PaymentsIntoPensionsCYAController.show(taxYear)))
                    } else if(yesNoAnswer && prior.isDefined) {
                      Right(Redirect(controllers.pensions.paymentsIntoPensions.routes.PaymentsIntoPensionsCYAController.show(taxYear)))
                    }
                    else {
                      Right(Redirect(controllers.pensions.paymentsIntoPensions.routes.ReliefAtSourcePensionsController.show(taxYear)))
                    }
                  }
                } else {
                  Right(Redirect(controllers.pensions.paymentsIntoPensions.routes.ReliefAtSourcePensionsController.show(taxYear)))
                }
              }(clock).flatMap{
                case Left(error) => Future.successful(errorHandler.handleError(BAD_REQUEST))
                case Right(redirect) => Future.successful(redirect)
              }(ec)
          }
      )
  }

  private def updateModelToZero(cyaData: PaymentsIntoPensionViewModel): PaymentsIntoPensionViewModel = {
    cyaData.copy(
      gateway = Some(false),
      totalRASPaymentsAndTaxRelief = None,
      totalOneOffRasPaymentPlusTaxRelief =  None,
      totalRetirementAnnuityContractPayments = None,
      totalWorkplacePensionPayments = None,
      rasPensionPaymentQuestion = if (cyaData.rasPensionPaymentQuestion.isDefined) Some(false) else None,
      oneOffRasPaymentPlusTaxReliefQuestion = if (cyaData.oneOffRasPaymentPlusTaxReliefQuestion.isDefined) Some(false) else None,
      retirementAnnuityContractPaymentsQuestion = if (cyaData.retirementAnnuityContractPaymentsQuestion.isDefined) Some(false) else None,
      workplacePensionPaymentsQuestion = if (cyaData.workplacePensionPaymentsQuestion.isDefined) Some(false) else None
    )

  }

  def updateSessionToZero(taxYear: Int)(implicit user: User, request: AuthorisationRequest[AnyContent]): Future[Result] = {
    pensionSessionService.getAndHandle(taxYear, user) { (cya, prior) =>
      (cya, prior) match {
        case (Some(cyaData), _) =>
          val newSessionData = cyaData.pensions.copy( paymentsIntoPension = updateModelToZero(cyaData.pensions.paymentsIntoPension))
          pensionSessionService.createOrUpdateSessionData(user, newSessionData, taxYear, cyaData.isPriorSubmission)(errorHandler.internalServerError()) {
            Redirect(controllers.pensions.paymentsIntoPensions.routes.PaymentsIntoPensionsCYAController.show(taxYear))
          }(clock)
        case _ => Future.successful(Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear)))
      }
    }
  }
}
