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
import controllers.predicates.ActionsProvider
import forms.FormsProvider
import models.mongo.PensionsUserData
import models.pension.charges.TransferPensionScheme
import models.pension.pages.OverseasTransferChargePaidPage
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import services.OverseasTransferChargesService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.transferIntoOverseasPensions.OverseasTransferChargesPaidView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasTransferChargePaidController @Inject()(actionsProvider: ActionsProvider,
                                                     formsProvider: FormsProvider,
                                                     pageView: OverseasTransferChargesPaidView,
                                                     errorHandler: ErrorHandler,
                                                     overseasTransferChargesService: OverseasTransferChargesService
                                                    )(implicit mcc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {


  val outOfBoundsRedirect: Int => Result = (taxYear: Int) => Redirect(controllers.pensions.routes.PensionsSummaryController.show(taxYear))

  def show(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) { implicit sessionUserData =>

    validateScheme(pensionSchemeIndex, sessionUserData.pensionsUserData.pensions.transfersIntoOverseasPensions.transferPensionScheme) match {
      case Left(_) => outOfBoundsRedirect(taxYear)
      case Right(_) => Ok(
        pageView(OverseasTransferChargePaidPage(taxYear, pensionSchemeIndex, sessionUserData.pensionsUserData.pensions.transfersIntoOverseasPensions, formsProvider.overseasTransferChargePaidForm)))
    }
  }

  def submit(taxYear: Int, pensionSchemeIndex: Option[Int]): Action[AnyContent] = {
    actionsProvider.userSessionDataFor(taxYear).async { implicit sessionUserData =>

      validateScheme(pensionSchemeIndex, sessionUserData.pensionsUserData.pensions.transfersIntoOverseasPensions.transferPensionScheme) match {
        case Left(_) => Future.successful(outOfBoundsRedirect(taxYear))
        case Right(_) => formsProvider.overseasTransferChargePaidForm.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(
              BadRequest(pageView(OverseasTransferChargePaidPage(taxYear, pensionSchemeIndex, sessionUserData.pensionsUserData.pensions.transfersIntoOverseasPensions, formWithErrors)))),
          yesNoValue => {
            overseasTransferChargesService.updateOverseasTransferChargeQuestion(sessionUserData.pensionsUserData, yesNoValue, pensionSchemeIndex).map {
              case Left(_) => errorHandler.internalServerError()
              case Right(userData) => pensionSchemeIndex.getOrElse(userData.pensions.transfersIntoOverseasPensions.transferPensionScheme.size)
                Redirect(getRedirectCall(
                  taxYear,
                  Some(pensionSchemeIndex.getOrElse(userData.pensions.transfersIntoOverseasPensions.transferPensionScheme.size-1)),
                  yesNoValue,
                  userData))
            }
          }
        )
      }
    }
  }

  def validateScheme(index: Option[Int], pensionSchemeLists: Seq[TransferPensionScheme]): Either[Unit, Option[TransferPensionScheme]] = {
    index match {
      case Some(value) if pensionSchemeLists.indices contains value => Right(Some(pensionSchemeLists(value)))
      case None => Right(None)
      case _ => Left(())
    }
  }

  private def getRedirectCall(taxYear: Int,
                              pensionSchemeIndex: Option[Int],
                              yesNoValue: Boolean,
                              userData: PensionsUserData): Call = {
    if (yesNoValue) {
      controllers.pensions.transferIntoOverseasPensions.routes.OverseasTransferChargePaidController.show(taxYear, pensionSchemeIndex)
    } else {
      controllers.pensions.transferIntoOverseasPensions.routes.OverseasTransferChargePaidController.show(taxYear, pensionSchemeIndex)
    }
  }
}
