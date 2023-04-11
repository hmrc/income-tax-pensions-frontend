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
import controllers._
import controllers.pensions.paymentsIntoOverseasPensions.routes.PensionsCustomerReferenceNumberController
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.ActionsProvider
import forms.PensionCustomerReferenceNumberForm
import models.User
import models.mongo.PensionsCYAModel
import models.pension.charges.Relief
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import views.html.pensions.paymentsIntoOverseasPensions.PensionsCustomerReferenceNumberView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PensionsCustomerReferenceNumberController @Inject()(actionsProvider: ActionsProvider,
                                                          pensionsCustomerReferenceNumberView: PensionsCustomerReferenceNumberView,
                                                          pensionSessionService: PensionSessionService,
                                                          errorHandler: ErrorHandler
                                                         )(implicit val mcc: MessagesControllerComponents,
                                                           appConfig: AppConfig,
                                                           clock: Clock,
                                                           ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  def referenceForm(user: User): Form[String] = PensionCustomerReferenceNumberForm.pensionCustomerReferenceNumberForm(
    incorrectFormatMsg = s"overseasPension.pensionsCustomerReferenceNumber.error.noEntry.${if (user.isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionDataRequest => {
      index match {
        case Some(_) => validateIndex(index, sessionDataRequest.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs.size) match {
          case Some(idx) => sessionDataRequest.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs(idx).customerReferenceNumberQuestion match {
            case None => Future.successful(Ok(pensionsCustomerReferenceNumberView(referenceForm(sessionDataRequest.user), taxYear, index)))
            case Some(customerReferenceNumber) =>
              Future.successful(Ok(pensionsCustomerReferenceNumberView(referenceForm(sessionDataRequest.user)
                .fill(customerReferenceNumber),
                taxYear, index)))
          }
          case None => Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
        }
        case None => Future.successful(Ok(pensionsCustomerReferenceNumberView(referenceForm(sessionDataRequest.user), taxYear, index)))
      }
    }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionDataRequest =>

      def createOrUpdateSessionData(updatedCyaModel: PensionsCYAModel, newIndex: Some[Int]): Future[Result] = pensionSessionService.createOrUpdateSessionData(
        sessionDataRequest.user,
        updatedCyaModel,
        taxYear,
        sessionDataRequest.pensionsUserData.isPriorSubmission
      )(errorHandler.internalServerError()) {
        Redirect(PensionsCustomerReferenceNumberController.show(taxYear, newIndex)) //TODO - redirect to untaxed-employer-payments SASS-3099
      }

      index match {
        case None => referenceForm(sessionDataRequest.user).bindFromRequest().fold(
          formWithErrors =>
            Future.successful(BadRequest(pensionsCustomerReferenceNumberView(formWithErrors, taxYear, index))),
          pensionCustomerReferenceNumber => {
            val updatedCyaModel: PensionsCYAModel = sessionDataRequest.pensionsUserData.pensions.copy(
              paymentsIntoOverseasPensions = sessionDataRequest.pensionsUserData.pensions.paymentsIntoOverseasPensions.copy(
                reliefs = sessionDataRequest.pensionsUserData.pensions.paymentsIntoOverseasPensions
                  .reliefs :+ Relief(customerReferenceNumberQuestion = Some(pensionCustomerReferenceNumber))
              )
            )
            createOrUpdateSessionData(updatedCyaModel, Some(updatedCyaModel.paymentsIntoOverseasPensions.reliefs.size))
          }
        )
        case Some(_) =>
          validateIndex(index, sessionDataRequest.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs.size) match {
            case Some(validIndex: Int) =>
              referenceForm(sessionDataRequest.user).bindFromRequest().fold(
                formWithErrors =>
                  Future.successful(BadRequest(pensionsCustomerReferenceNumberView(formWithErrors, taxYear, Some(validIndex)))),
                pensionCustomerReferenceNumber => {
                  val updatedCyaModel: PensionsCYAModel = sessionDataRequest.pensionsUserData.pensions.copy(
                    paymentsIntoOverseasPensions = sessionDataRequest.pensionsUserData.pensions.paymentsIntoOverseasPensions.copy(
                      reliefs = sessionDataRequest.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs.updated(
                        validIndex,
                        sessionDataRequest.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs(validIndex).copy(customerReferenceNumberQuestion = Some(pensionCustomerReferenceNumber))
                      )
                    )
                  )
                  val currentIndex = index.fold(
                    Some(updatedCyaModel.paymentsIntoOverseasPensions.reliefs.size - 1))(index => Some(index))
                  createOrUpdateSessionData(updatedCyaModel, currentIndex)
                }
              )
            case _ =>
              Future.successful(Redirect(PensionsSummaryController.show(taxYear)))
          }
      }
  }
}
