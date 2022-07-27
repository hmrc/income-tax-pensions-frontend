package controllers.pensions.lifetimeAllowance


import config.{AppConfig, ErrorHandler}
import controllers.pensions.routes.PensionsSummaryController
import controllers.predicates.TailoringEnabledFilterAction.tailoringEnabledFilterAction
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import forms.YesNoForm
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.PensionSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.pensions.lifetimeAllowance.AnnualLifetimeAllowanceGatewayView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AnnualLifetimeAllowanceGatewayController @Inject()(authAction: AuthorisedAction,
                                                         view: AnnualLifetimeAllowanceGatewayView,
                                                         pensionSessionService: PensionSessionService,
                                                         errorHandler: ErrorHandler)
                                                        (implicit cc: MessagesControllerComponents, appConfig: AppConfig, ec: ExecutionContext)
  extends FrontendController(cc) with I18nSupport with SessionHelper {

  def unauthorisedPaymentForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    missingInputError = s"AnnualAndLifetimeAllowance.gateway.error.${if (isAgent) "agent" else "individual"}"
  )

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen tailoringEnabledFilterAction(taxYear) andThen taxYearAction(taxYear)).async {
    implicit request =>
      Future.successful(
        Ok(view(taxYear, unauthorisedPaymentForm(request.user.isAgent)))
      )
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen tailoringEnabledFilterAction(taxYear) andThen taxYearAction(taxYear)).async { implicit request =>
    unauthorisedPaymentForm(request.user.isAgent).bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(view(taxYear, formWithErrors))),
      yesNoAnswer =>
        if (yesNoAnswer) {
          Future(Redirect(PensionsSummaryController.show(taxYear)))
        } else {
          Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        }
    )
  }
}