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

package controllers.pensions.transferIntoOverseasPensions

import config.{AppConfig, ErrorHandler}
import controllers.pensions.transferIntoOverseasPensions.routes._
import controllers.predicates.actions.ActionsProvider
import controllers.validatedIndex
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.TransferPensionScheme
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import services.redirects.SimpleRedirectService.redirectBasedOnCurrentAnswers
import services.redirects.TransfersIntoOverseasPensionsPages.RemoveSchemePage
import services.redirects.TransfersIntoOverseasPensionsRedirects.{cyaPageCall, journeyCheck}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.transferIntoOverseasPensions.RemoveTransferChargeSchemeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RemoveTransferChargeSchemeController @Inject() (actionsProvider: ActionsProvider,
                                                      pensionSessionService: PensionSessionService,
                                                      view: RemoveTransferChargeSchemeView,
                                                      errorHandler: ErrorHandler,
                                                      mcc: MessagesControllerComponents)(implicit val appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val checkRedirect = journeyCheck(RemoveSchemePage, _: PensionsCYAModel, taxYear)
    redirectBasedOnCurrentAnswers(taxYear, Some(request.sessionData), cyaPageCall(taxYear))(checkRedirect) { data =>
      val transferChargeScheme = data.pensions.transfersIntoOverseasPensions.transferPensionScheme
      validatedIndex(index, transferChargeScheme.size).fold(Future.successful(Redirect(TransferChargeSummaryController.show(taxYear)))) { i =>
        transferChargeScheme(i).name.fold(Future.successful(Redirect(TransferChargeSummaryController.show(taxYear)))) { name =>
          Future.successful(Ok(view(taxYear, name, index)))
        }
      }
    }
  }

  def submit(taxYear: Int, index: Option[Int]): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    val transferChargeScheme = request.sessionData.pensions.transfersIntoOverseasPensions.transferPensionScheme
    validatedIndex(index, transferChargeScheme.size)
      .fold(Future.successful(Redirect(TransferChargeSummaryController.show(taxYear)))) { i =>
        val updatedTransferScheme = transferChargeScheme.patch(i, Nil, 1)
        val checkRedirect         = journeyCheck(RemoveSchemePage, _: PensionsCYAModel, taxYear)
        redirectBasedOnCurrentAnswers(taxYear, Some(request.sessionData), cyaPageCall(taxYear))(checkRedirect) { _ =>
          updateSessionData(request.sessionData, updatedTransferScheme, taxYear)
        }
      }
  }

  private def updateSessionData[T](pensionUserData: PensionsUserData, transferChargeScheme: Seq[TransferPensionScheme], taxYear: Int)(implicit
      request: UserSessionDataRequest[T]) = {
    val updatedCyaModel: PensionsCYAModel = pensionUserData.pensions.copy(
      transfersIntoOverseasPensions = pensionUserData.pensions.transfersIntoOverseasPensions.copy(transferPensionScheme = transferChargeScheme))

    pensionSessionService.createOrUpdateSessionData(request.user, updatedCyaModel, taxYear, pensionUserData.isPriorSubmission)(
      errorHandler.internalServerError()) {
      Redirect(TransferChargeSummaryController.show(taxYear))
    }
  }

}
