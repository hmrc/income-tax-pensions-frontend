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

package controllers.pensions.shortServiceRefunds

import config.{AppConfig, ErrorHandler}
import controllers.predicates.ActionsProvider
import controllers.validateScheme
import forms.FormsProvider
import javax.inject.{Inject, Singleton}
import models.pension.pages.shortServiceRefunds.TaxOnShortServiceRefundPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.ShortServiceRefundsService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.shortServiceRefunds.TaxPaidOnShortServiceRefundView
import routes._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxOnShortServiceRefundController @Inject()(actionsProvider: ActionsProvider,
                                                  view: TaxPaidOnShortServiceRefundView,
                                                  formsProvider: FormsProvider,
                                                  errorHandler: ErrorHandler,
                                                  shortServiceRefundsService: ShortServiceRefundsService)
                                                 (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  val outOfBoundsRedirect: Int => Result = (taxYear: Int) => Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear))

  def show(taxYear: Int, refundPensionSchemeIndex: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) { implicit sessionUserData =>
    validateScheme(refundPensionSchemeIndex, sessionUserData.pensionsUserData.pensions.shortServiceRefunds.refundPensionScheme) match {
      case Left(_) => outOfBoundsRedirect(taxYear)
      case Right(_) => Ok(
        view(TaxOnShortServiceRefundPage(taxYear, refundPensionSchemeIndex, sessionUserData.pensionsUserData.pensions.shortServiceRefunds, formsProvider.shortServiceTaxOnShortServiceRefundForm)))
    }
  }

  def submit(taxYear: Int, refundPensionSchemeIndex: Option[Int]): Action[AnyContent] = {
    actionsProvider.userSessionDataFor(taxYear).async { implicit sessionUserData =>

      validateScheme(refundPensionSchemeIndex, sessionUserData.pensionsUserData.pensions.shortServiceRefunds.refundPensionScheme) match {
        case Left(_) => Future.successful(outOfBoundsRedirect(taxYear))
        case Right(_) => formsProvider.shortServiceTaxOnShortServiceRefundForm.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(
              BadRequest(view(
                TaxOnShortServiceRefundPage(taxYear, refundPensionSchemeIndex, sessionUserData.pensionsUserData.pensions.shortServiceRefunds, formWithErrors)
              ))),
          yesNoValue => {
            shortServiceRefundsService.createOrUpdateShortServiceRefundQuestion(sessionUserData.pensionsUserData, yesNoValue, refundPensionSchemeIndex).map {
              case Left(_) => errorHandler.internalServerError()
              case Right(userData) =>
                //The collection will always have a value
                val index = Some(refundPensionSchemeIndex.getOrElse(userData.pensions.shortServiceRefunds.refundPensionScheme.size-1))
                Redirect(TaxOnShortServiceRefundController.show(taxYear, index))
            }
          }
        )
      }
    }
  }


}