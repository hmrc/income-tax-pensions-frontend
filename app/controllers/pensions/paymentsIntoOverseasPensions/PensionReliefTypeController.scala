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

package controllers.pensions.paymentsIntoOverseasPensions

import config.{AppConfig, ErrorHandler}
import controllers.predicates.ActionsProvider
import forms.FormsProvider
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{Clock, SessionHelper}
import views.html.pensions.paymentsIntoOverseasPensions.PensionReliefTypeView
import controllers.pensions.paymentsIntoOverseasPensions.routes._
import controllers.validateIndex
import models.pension.charges.{Relief, TaxReliefQuestion}
import models.requests.UserSessionDataRequest

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PensionReliefTypeController@Inject()(actionsProvider: ActionsProvider,
                                           pensionSessionService: PensionSessionService,
                                           view: PensionReliefTypeView,
                                           formsProvider: FormsProvider,
                                           errorHandler: ErrorHandler,
                                           ec: ExecutionContext)
                                          (implicit val mcc: MessagesControllerComponents, appConfig: AppConfig, clock: Clock)
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def show(taxYear: Int, reliefIndex: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionData =>
      validateIndex(reliefIndex, sessionData.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs.size) match {
        case Some(idx) =>
          sessionData.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs(idx).reliefType.fold {
            Future.successful(Ok(view(formsProvider.overseasPensionsReliefTypeForm, taxYear, reliefIndex)))
          } {
            reliefType =>
              Future.successful(Ok(view(formsProvider.overseasPensionsReliefTypeForm.fill(reliefType), taxYear, reliefIndex)))
          }
        case _ =>
          Future.successful(Redirect(PensionsCustomerReferenceNumberController.show(taxYear)))
      }
  }

  def submit(taxYear: Int, reliefIndex: Option[Int]): Action[AnyContent] = actionsProvider.userSessionDataFor(taxYear) async {
    implicit sessionData =>
      formsProvider.overseasPensionsReliefTypeForm.bindFromRequest().fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, reliefIndex))),
        newTaxReliefQuestion => updateViewModel(newTaxReliefQuestion, reliefIndex, taxYear)(request = sessionData)
      )
  }

  private def updateViewModel(
                               taxReliefQuestion: String,
                               indexOpt: Option[Int],
                               taxYear: Int)(implicit request: UserSessionDataRequest[_]): Future[Result] = {
    validateIndex(indexOpt, request.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs.size) match {
      case Some(idx) =>
        if (!request.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs(idx).reliefType.contains(taxReliefQuestion)) {
          val updatedReliefs: Relief = request.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs(idx).copy(
            reliefType = Some(taxReliefQuestion),
            doubleTaxationCountryCode = None,
            doubleTaxationCountryArticle = None,
            doubleTaxationCountryTreaty = None,
            doubleTaxationReliefAmount = None,
            sf74Reference = None,
            qualifyingOverseasPensionSchemeReferenceNumber = None
          )
          pensionSessionService.createOrUpdateSessionData(
            request.user,
            request.pensionsUserData.pensions.copy(
              paymentsIntoOverseasPensions = request.pensionsUserData.pensions.paymentsIntoOverseasPensions.copy(
                reliefs = request.pensionsUserData.pensions.paymentsIntoOverseasPensions.reliefs.updated(idx, updatedReliefs))),
            taxYear,
            request.pensionsUserData.isPriorSubmission
          )(errorHandler.internalServerError()) {
            redirectBaseOnTaxReliefQuestion(taxReliefQuestion, taxYear, indexOpt)(mcc, request)
          }
        } else {
          Future.successful(redirectBaseOnTaxReliefQuestion(taxReliefQuestion, taxYear, indexOpt)(mcc, request))
        }
      case _ =>
        Future.successful(Redirect(PensionsCustomerReferenceNumberController.show(taxYear)))
    }
  }

  private def redirectBaseOnTaxReliefQuestion(taxReliefQuestion: String,
                                              taxYear: Int,
                                              reliefIndex: Option[Int]
                                             )(implicit mcc: MessagesControllerComponents, request: UserSessionDataRequest[_]): Result =
    taxReliefQuestion match {
    case TaxReliefQuestion.DoubleTaxationRelief =>
      Ok(view(formsProvider.overseasPensionsReliefTypeForm, taxYear, reliefIndex)) //todo redirect to "Double taxation agreement details" Page when built
    case TaxReliefQuestion.MigrantMemberRelief =>
     Redirect(QOPSReferenceController.show(taxYear))
    case TaxReliefQuestion.TransitionalCorrespondingRelief =>
      Redirect(SF74ReferenceController.show(taxYear))
    case TaxReliefQuestion.NoTaxRelief =>
      Redirect(ReliefsSchemeDetailsController.show(taxYear, reliefIndex))
    case _ =>
      BadRequest(view(formsProvider.overseasPensionsReliefTypeForm, taxYear, reliefIndex))
  }
}
