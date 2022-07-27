package controllers.pensions.lifetimeAllowance

import forms.YesNoForm
import org.scalatest.BeforeAndAfterEach
import play.api.{Application, Environment, Mode}
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{route, writeableOf_AnyContentAsFormUrlEncoded}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

import scala.concurrent.Future

class AnnualLifetimeAllowanceControlllerISpec extends IntegrationTest
  with ViewHelpers
  with BeforeAndAfterEach
  with PensionsDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_, _]] = Seq.empty

  val csrfContent: (String, String) = "Csrf-Token" -> "nocheck"

  protected val configWithTailoringDisabled: Map[String, String] = config ++ Map("feature-switch.tailoringEnabled" -> "false")

  lazy val appWithTailoringDisabled: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(configWithTailoringDisabled)
    .build()

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    dropPensionsDB()
  }

  private def url(taxYear: Int): String = {
    s"/update-and-submit-income-tax-return/pensions/$taxYear/annual-lifetime-allowance/annual-lifetime-allowance-status"
  }

  ".show" should {
    "redirect to income tax submission overview when tailoring is disabled" in {
      val request = FakeRequest("GET", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        route(appWithTailoringDisabled, request, "{}").get
      }
      status(result) shouldBe SEE_OTHER
      await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
    }

    "return OK when tailoring is enabled" in {
      val request = FakeRequest("GET", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        route(app, request, "{}").get
      }

      status(result) shouldBe OK
    }
  }

  ".submit" should {
    "redirect to income tax submission overview when tailoring is disabled" in {
      val request = FakeRequest("POST", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList), csrfContent)

      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        route(appWithTailoringDisabled, request, "{}").get
      }

      status(result) shouldBe SEE_OTHER
      await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
    }

    "redirect to pensions summary when tailoring is enabled and 'Yes' is selected" in {
      val request = FakeRequest("POST", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList), csrfContent)
        .withFormUrlEncodedBody(YesNoForm.yesNo -> "true")

      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        route(app, request).get
      }

      status(result) shouldBe SEE_OTHER
      await(result).header.headers("Location") shouldBe controllers.pensions.routes.PensionsSummaryController.show(taxYearEOY).url
    }

    "redirect to income tax submission overview when tailoring is enabled and 'No' is selected" in {
      val request = FakeRequest("POST", url(taxYearEOY)).withHeaders(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList), csrfContent)
        .withFormUrlEncodedBody(YesNoForm.yesNo -> "false")

      lazy val result: Future[Result] = {
        authoriseIndividual(true)
        route(app, request).get
      }

      status(result) shouldBe SEE_OTHER
      await(result).header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYearEOY)
    }
  }
}