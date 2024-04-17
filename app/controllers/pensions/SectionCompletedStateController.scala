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

package controllers.pensions

import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import views.html.pensions.SectionCompletedStateView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SectionCompletedStateController @Inject()(override val messagesApi: MessagesApi,
                                                service: SelfEmploymentService,
                                                identify: IdentifierAction,
                                                getData: DataRetrievalAction,
                                                formProvider: BooleanFormProvider,
                                                val controllerComponents: MessagesControllerComponents,
                                                view: SectionCompletedStateView)(implicit val ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val page = SectionCompletedStatePage

  def show(taxYear: String, businessId: String, journey: String, mode: String): Action[AnyContent] = (identify andThen getData) async {
    implicit request =>
      val form: Form[Boolean] = formProvider(page, request.userType, userSpecificRequiredError = false)
//      val preparedForm = service
//        .getJourneyStatus(JourneyAnswersContext(taxYear, businessId, request.mtditid, Journey.withName(journey)))
//        .value
//        .map(_.fold(_ => form, fill(form, _)))
//
//      preparedForm map { form =>
        Ok(view(form, taxYear, businessId, journey, mode))
//      }
  }

//  private def fill(form: Form[Boolean], status: JourneyStatus) =
//    status match {
//      case Completed                                     => form.fill(true)
//      case InProgress                                    => form.fill(false)
//      case NotStarted | CheckOurRecords | CannotStartYet => form
//    }

//  def onSubmit(taxYear: TaxYear, businessId: BusinessId, journey: String, mode: Mode): Action[AnyContent] = (identify andThen getData) async {
//    implicit request =>
//      formProvider(page, request.userType, userSpecificRequiredError = false)
//        .bindFromRequest()
//        .fold(
//          formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, businessId, Journey.withName(journey), mode))),
//          answer =>
//            handleResultT(
//              saveAndRedirect(JourneyAnswersContext(taxYear, businessId, request.mtditid, Journey.withName(journey)), answer)
//            )
//        )
//  }

//  private def saveAndRedirect(ctx: JourneyAnswersContext, answer: Boolean)(implicit hc: HeaderCarrier) = {
//    val status = if (answer) Completed else InProgress
//    service.setJourneyStatus(ctx, status) map { _ =>
//      Redirect(page.nextPage(ctx.taxYear))
//    }
//  }
}
