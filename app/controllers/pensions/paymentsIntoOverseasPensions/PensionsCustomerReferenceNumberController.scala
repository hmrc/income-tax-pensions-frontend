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

/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *     http:www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.pensions.paymentsIntoOverseasPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.paymentsIntoOverseasPensions.routes.PensionsCustomerReferenceNumberController
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.AuthorisedAction
import forms.PensionCustomerReferenceNumberForm
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.{AuthorisationRequest, User}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.{data, inject}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.paymentsIntoOverseasPensions.PensionsCustomerReferenceNumberView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PensionsCustomerReferenceNumberController @Inject()(authAction: AuthorisedAction,
                                                          pensionsCustomerReferenceNumberView: PensionsCustomerReferenceNumberView,
                                                          pensionSessionService: PensionSessionService,
                                                          errorHandler: ErrorHandler
                                                         )(implicit val mcc: MessagesControllerComponents,
                                                           appConfig: AppConfig,
                                                           clock: Clock,
                                                           ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  def referenceForm(): Form[String] = PensionCustomerReferenceNumberForm.pensionCustomerReferenceNumberForm(
    incorrectFormatMsg = "overseasPension.pensionsCustomerReferenceNumber.error.noEntry"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionSessionData(taxYear, request.user).map {
      case Left(_) => errorHandler.handleError(INTERNAL_SERVER_ERROR)
      case Right(optPensionUserData) =>
        optPensionUserData
          .map(pensionUserData => Ok(pensionsCustomerReferenceNumberView(referenceForm(pensionUserData), taxYear)))
          .getOrElse(Redirect(PensionsSummaryController.show(taxYear)))
    }
  }


  private def referenceForm(pensionUserData: PensionsUserData)(implicit request: AuthorisationRequest[AnyContent]): Form[String] =
    pensionUserData.pensions.paymentsIntoOverseasPensions.customerReferenceNumberQuestion
      .map(value => referenceForm().fill(value))
      .getOrElse(referenceForm())

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    referenceForm().bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(pensionsCustomerReferenceNumberView(formWithErrors, taxYear))),
      pensionCustomerReferenceNumber => {
        pensionSessionService.getPensionSessionData(taxYear, request.user).flatMap {
          case Right(Some(data)) =>
            val updatedCyaModel: PensionsCYAModel = data.pensions.copy(
              paymentsIntoOverseasPensions = data.pensions.paymentsIntoOverseasPensions.copy(
                customerReferenceNumberQuestion = Some(pensionCustomerReferenceNumber)))
            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
              Redirect(PensionsCustomerReferenceNumberController.show(taxYear)) //TODO - redirect to untaxed-employer-payments
            }
          case _ =>
            Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
        }
      }
    )
  }
}
