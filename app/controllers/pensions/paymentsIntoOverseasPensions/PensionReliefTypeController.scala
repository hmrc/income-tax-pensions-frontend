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
import controllers.pensions.paymentsIntoOverseasPensions.routes._
import controllers.predicates.actions.ActionsProvider
import controllers.validatedIndex
import forms.FormsProvider
import models.User
import models.pension.charges.{Relief, TaxReliefQuestion}
import models.requests.UserSessionDataRequest
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.PensionSessionService
import services.redirects.PaymentsIntoOverseasPensionsPages.PensionReliefTypePage
import services.redirects.PaymentsIntoOverseasPensionsRedirects.{indexCheckThenJourneyCheck, redirectForSchemeLoop}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.paymentsIntoOverseasPensions.PensionReliefTypeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class PensionReliefTypeController @Inject() (actionsProvider: ActionsProvider,
                                             pensionSessionService: PensionSessionService,
                                             view: PensionReliefTypeView,
                                             formsProvider: FormsProvider,
                                             errorHandler: ErrorHandler,
                                             mcc: MessagesControllerComponents)(implicit appConfig: AppConfig)
    extends FrontendController(mcc)
    with I18nSupport
    with SessionHelper {

  def show(taxYear: Int, reliefIndex: Option[Int]): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    indexCheckThenJourneyCheck(request.sessionData, reliefIndex, PensionReliefTypePage, taxYear) { relief: Relief =>
      relief.reliefType.fold {
        Future.successful(Ok(view(formsProvider.overseasPensionsReliefTypeForm(request.user), taxYear, reliefIndex)))
      } { reliefType =>
        Future.successful(Ok(view(formsProvider.overseasPensionsReliefTypeForm(request.user).fill(reliefType), taxYear, reliefIndex)))
      }
    }
  }

  def submit(taxYear: Int, reliefIndex: Option[Int]): Action[AnyContent] = actionsProvider.authoriseWithSession(taxYear) async { implicit request =>
    indexCheckThenJourneyCheck(request.sessionData, reliefIndex, PensionReliefTypePage, taxYear) { _: Relief =>
      formsProvider
        .overseasPensionsReliefTypeForm(request.user)
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear, reliefIndex))),
          newTaxReliefQuestion => updateViewModel(newTaxReliefQuestion, reliefIndex, taxYear)(request = request)
        )
    }
  }

  private def updateViewModel(taxReliefQuestion: String, indexOpt: Option[Int], taxYear: Int)(implicit
      request: UserSessionDataRequest[_]): Future[Result] = {

    val piopReliefs = request.sessionData.pensions.paymentsIntoOverseasPensions.reliefs
    validatedIndex(indexOpt, piopReliefs.size) match {
      case Some(idx) =>
        if (!piopReliefs(idx).reliefType.contains(taxReliefQuestion)) {
          val updatedReliefs: Relief = piopReliefs(idx).copy(
            reliefType = Some(taxReliefQuestion),
            alphaTwoCountryCode = None,
            alphaThreeCountryCode = None,
            doubleTaxationArticle = None,
            doubleTaxationTreaty = None,
            doubleTaxationReliefAmount = None,
            sf74Reference = None,
            qopsReference = None
          )
          pensionSessionService.createOrUpdateSessionData(
            request.user,
            request.sessionData.pensions.copy(paymentsIntoOverseasPensions =
              request.sessionData.pensions.paymentsIntoOverseasPensions.copy(reliefs = piopReliefs.updated(idx, updatedReliefs))),
            taxYear,
            request.sessionData.isPriorSubmission
          )(errorHandler.internalServerError()) {
            redirectBaseOnTaxReliefQuestion(taxReliefQuestion, taxYear, request.user, indexOpt)(request)
          }
        } else {
          Future.successful(redirectBaseOnTaxReliefQuestion(taxReliefQuestion, taxYear, request.user, indexOpt)(request))
        }
      case _ =>
        Future.successful(Redirect(redirectForSchemeLoop(piopReliefs, taxYear)))
    }
  }

  private def redirectBaseOnTaxReliefQuestion(taxReliefQuestion: String, taxYear: Int, user: User, reliefIndex: Option[Int])(implicit
      request: UserSessionDataRequest[_]): Result =
    taxReliefQuestion match {
      case TaxReliefQuestion.DoubleTaxationRelief =>
        Redirect(DoubleTaxationAgreementController.show(taxYear, reliefIndex))
      case TaxReliefQuestion.MigrantMemberRelief =>
        Redirect(QOPSReferenceController.show(taxYear, reliefIndex))
      case TaxReliefQuestion.TransitionalCorrespondingRelief =>
        Redirect(SF74ReferenceController.show(taxYear, reliefIndex))
      case TaxReliefQuestion.NoTaxRelief =>
        Redirect(ReliefsSchemeDetailsController.show(taxYear, reliefIndex))
      case _ =>
        BadRequest(view(formsProvider.overseasPensionsReliefTypeForm(user), taxYear, reliefIndex))
    }
}
