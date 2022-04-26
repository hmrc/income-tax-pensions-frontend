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

import java.text.NumberFormat
import java.util.Locale

import config.{AppConfig, ErrorHandler}
import controllers.predicates.AuthorisedAction
import forms.YesNoForm
import javax.inject.{Inject, Singleton}
import models.User
import models.mongo.PensionsCYAModel
import models.redirects.ConditionalRedirect
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.RedirectService.{isFinishedCheck, PaymentsIntoPensionsRedirects, redirectBasedOnCurrentAnswers}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.Clock
import utils.PaymentsIntoPensionPages.TotalRasPage
import views.html.pensions.paymentsIntoPensions.TotalPaymentsIntoRASView

import scala.concurrent.Future

@Singleton
class TotalPaymentsIntoRASController @Inject()(implicit val mcc: MessagesControllerComponents,
                                               appConfig: AppConfig,
                                               authAction: AuthorisedAction,
                                               pensionSessionService: PensionSessionService,
                                               errorHandler: ErrorHandler,
                                               view: TotalPaymentsIntoRASView,
                                               clock: Clock
                                              ) extends FrontendController(mcc) with I18nSupport {

  private def yesNoForm(user: User): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = "paymentsIntoPensions.totalRASPayments.error"
  )

  def show(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
      redirectBasedOnCurrentAnswers(taxYear, optData)(redirects(_, taxYear)) { data =>

        val model = data.pensions.paymentsIntoPension
        model.totalRASPaymentsAndTaxRelief match {
          case Some(totalRAS) =>
            val viewValues = calculateViewValues(totalRAS, model.totalOneOffRasPaymentPlusTaxRelief)
            val form = model.totalPaymentsIntoRASQuestion.fold(yesNoForm(request.user))(yesNoForm(request.user).fill(_))
            Future.successful(Ok(view(form, taxYear, viewValues._1, viewValues._2, viewValues._3, viewValues._4)))
          case _ =>
            Future.successful(Redirect(controllers.pensions.paymentsIntoPension.routes.ReliefAtSourcePaymentsAndTaxReliefAmountController.show(taxYear)))
        }
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit request =>
    pensionSessionService.getPensionsSessionDataResult(taxYear, request.user) { optData =>
      redirectBasedOnCurrentAnswers(taxYear, optData)(redirects(_, taxYear)) { data =>

        val cya = data.pensions
        val model = cya.paymentsIntoPension
        yesNoForm(request.user).bindFromRequest().fold(
          formWithErrors => {
            model.totalRASPaymentsAndTaxRelief match {
              case Some(totalRAS) => {
                val viewValues = calculateViewValues(totalRAS, model.totalOneOffRasPaymentPlusTaxRelief)
                Future.successful(BadRequest(view(formWithErrors, taxYear, viewValues._1, viewValues._2, viewValues._3, viewValues._4)))
              }
              case _ =>
                Future.successful(Redirect(controllers.pensions.paymentsIntoPension.routes.ReliefAtSourcePaymentsAndTaxReliefAmountController.show(taxYear)))
            }
          },
          yesNo => {
            val updatedCyaModel: PensionsCYAModel =
              cya.copy(paymentsIntoPension = model.copy(totalPaymentsIntoRASQuestion = Some(yesNo)))
            val redirectLocation = if (yesNo) {
              controllers.pensions.paymentsIntoPension.routes.PensionsTaxReliefNotClaimedController.show(taxYear)
            } else {
              controllers.pensions.paymentsIntoPension.routes.ReliefAtSourcePaymentsAndTaxReliefAmountController.show(taxYear)
            }

            pensionSessionService.createOrUpdateSessionData(request.user,
              updatedCyaModel, taxYear, data.isPriorSubmission)(errorHandler.internalServerError()) {
              isFinishedCheck(updatedCyaModel, taxYear, redirectLocation)
            }
          }
        )
      }
    }
  }

  private def calculateViewValues(totalRAS: BigDecimal, oneOff: Option[BigDecimal]): (String, Option[String], String, String) = {
    val total = totalRAS + oneOff.getOrElse(BigDecimal(0))

    def formatNoZeros(amount: BigDecimal): String = {
      NumberFormat.getCurrencyInstance(Locale.UK).format(amount)
        .replaceAll("\\.00", "")
    }

    (
      formatNoZeros(total),
      oneOff.map(amount => formatNoZeros(amount)),
      formatNoZeros(total.*(0.8)),
      formatNoZeros(total.*(0.2))
    )
  }

  private def redirects(cya: PensionsCYAModel, taxYear: Int): Seq[ConditionalRedirect] = {
    PaymentsIntoPensionsRedirects.journeyCheck(TotalRasPage, cya, taxYear)
  }

}
