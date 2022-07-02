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

import builders.PaymentsIntoPensionVewModelBuilder.aPaymentsIntoPensionsEmptyViewModel
import builders.PensionsUserDataBuilder.pensionsUserDataWithPaymentsIntoPensions
import builders.UserBuilder.aUser
import common.SessionValues
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Results.{BadRequest, InternalServerError, Redirect}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, contentType, status}
import support.mocks.{MockAuthorisedAction, MockErrorHandler, MockPensionSessionService}
import support.{ControllerUnitTest, ViewHelper}
import views.html.pensions.paymentsIntoPensions.ReliefAtSourcePensionsView
import controllers.errors.routes.UnauthorisedUserErrorController


class ReliefAtSourcePensionsControllerSpec extends ControllerUnitTest with ViewHelper
  with MockAuthorisedAction
  with MockPensionSessionService
  with MockErrorHandler {

  private val yesRadioButtonCssSelector = ".govuk-radios__item > input#value"
  private val noRadioButtonCssSelector = ".govuk-radios__item > input#value-no"

  private val fakeIndividualRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
    .withHeaders("X-Session-ID" -> aUser.sessionId)
    .withSession(
      SessionValues.TAX_YEAR -> taxYearEOY.toString,
      SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(","))

  private val fakeAgentRequest: FakeRequest[AnyContentAsEmpty.type] = fakeIndividualRequest
    .withHeaders("X-Session-ID" -> aUser.sessionId)
    .withSession(
      SessionValues.CLIENT_MTDITID -> "1234567890",
      SessionValues.CLIENT_NINO -> "AA123456A",
      SessionValues.TAX_YEAR -> taxYearEOY.toString,
      SessionValues.VALID_TAX_YEARS -> validTaxYearList.mkString(",")
    )

  private val pageView = inject[ReliefAtSourcePensionsView]
  private val formsProvider = new PaymentsIntoPensionFormProvider()

  private lazy val underTest = new ReliefAtSourcePensionsController(
    mockAuthorisedAction,
    pageView,
    mockPensionSessionService,
    mockErrorHandler,
    formsProvider)

  ".show" should {
    "redirect to UnauthorisedUserErrorController when authentication fails" in {
      mockFailToAuthenticate()
      await(underTest.show(taxYear = taxYearEOY)(fakeIndividualRequest)) shouldBe
        Redirect(UnauthorisedUserErrorController.show)
    }


    "return internal server error when find employment returns Left" in {
      mockAuthAsIndividual(Some("AA123456A"))
      mockGetPensionSessionData(taxYearEOY, Left())
      mockHandleError(INTERNAL_SERVER_ERROR, InternalServerError)
      await(underTest.show(taxYearEOY).apply(fakeIndividualRequest)) shouldBe InternalServerError
    }

    "return result for individual" which {
      "render page when empty form when payment into pension model is empty" in {
        val paymentsIntoPensionModel = aPaymentsIntoPensionsEmptyViewModel
        val pensionUserData = pensionsUserDataWithPaymentsIntoPensions(paymentsIntoPensionModel)
        mockAuthAsIndividual(Some("AA123456A"))
        mockGetPensionSessionData(taxYearEOY, Right(Some(pensionUserData)))

        val result = underTest.show(taxYearEOY).apply(fakeIndividualRequest)
        status(result) shouldBe OK
        contentType(result) shouldBe Some("text/html")

        implicit val document: Document = Jsoup.parse(contentAsString(result))
        document.select(yesRadioButtonCssSelector).hasAttr("checked") shouldBe false
        document.select(noRadioButtonCssSelector).hasAttr("checked") shouldBe false
      }

      "render page when empty form when rasPensionPaymentQuestion is true" in {
        val paymentsIntoPensionModel = aPaymentsIntoPensionsEmptyViewModel.copy(rasPensionPaymentQuestion = Some(true))
        val pensionUserData = pensionsUserDataWithPaymentsIntoPensions(paymentsIntoPensionModel)
        mockAuthAsIndividual(Some("AA123456A"))
        mockGetPensionSessionData(taxYearEOY, Right(Some(pensionUserData)))

        val result = underTest.show(taxYearEOY).apply(fakeIndividualRequest)
        status(result) shouldBe OK
        contentType(result) shouldBe Some("text/html")

        implicit val document: Document = Jsoup.parse(contentAsString(result))
        document.select(yesRadioButtonCssSelector).hasAttr("checked") shouldBe true
        document.select(noRadioButtonCssSelector).hasAttr("checked") shouldBe false
      }

      "render page when empty form when rasPensionPaymentQuestion is false" in {
        val paymentsIntoPensionModel = aPaymentsIntoPensionsEmptyViewModel.copy(rasPensionPaymentQuestion = Some(false))
        val pensionUserData = pensionsUserDataWithPaymentsIntoPensions(paymentsIntoPensionModel)
        mockAuthAsIndividual(Some("AA123456A"))
        mockGetPensionSessionData(taxYearEOY, Right(Some(pensionUserData)))

        val result = underTest.show(taxYearEOY).apply(fakeIndividualRequest)
        status(result) shouldBe OK
        contentType(result) shouldBe Some("text/html")

        implicit val document: Document = Jsoup.parse(contentAsString(result))
        document.select(yesRadioButtonCssSelector).hasAttr("checked") shouldBe false
        document.select(noRadioButtonCssSelector).hasAttr("checked") shouldBe true
      }
    }
    "return result for agent" which {
      "render page when empty form when payment into pension model is empty" in {
        val paymentsIntoPensionModel = aPaymentsIntoPensionsEmptyViewModel
        val pensionUserData = pensionsUserDataWithPaymentsIntoPensions(paymentsIntoPensionModel)
        mockAuthAsIndividual(Some("AA123456A"))
        mockGetPensionSessionData(taxYearEOY, Right(Some(pensionUserData)))

        val result = underTest.show(taxYearEOY).apply(fakeAgentRequest)
        status(result) shouldBe OK
        contentType(result) shouldBe Some("text/html")

        implicit val document: Document = Jsoup.parse(contentAsString(result))
        document.select(yesRadioButtonCssSelector).hasAttr("checked") shouldBe false
        document.select(noRadioButtonCssSelector).hasAttr("checked") shouldBe false
      }

      "render page when empty form when rasPensionPaymentQuestion is true" in {
        val paymentsIntoPensionModel = aPaymentsIntoPensionsEmptyViewModel.copy(rasPensionPaymentQuestion = Some(true))
        val pensionUserData = pensionsUserDataWithPaymentsIntoPensions(paymentsIntoPensionModel)
        mockAuthAsIndividual(Some("AA123456A"))
        mockGetPensionSessionData(taxYearEOY, Right(Some(pensionUserData)))

        val result = underTest.show(taxYearEOY).apply(fakeAgentRequest)
        status(result) shouldBe OK
        contentType(result) shouldBe Some("text/html")

        implicit val document: Document = Jsoup.parse(contentAsString(result))
        document.select(yesRadioButtonCssSelector).hasAttr("checked") shouldBe true
        document.select(noRadioButtonCssSelector).hasAttr("checked") shouldBe false
      }

      "render page when empty form when rasPensionPaymentQuestion is false" in {
        val paymentsIntoPensionModel = aPaymentsIntoPensionsEmptyViewModel.copy(rasPensionPaymentQuestion = Some(false))
        val pensionUserData = pensionsUserDataWithPaymentsIntoPensions(paymentsIntoPensionModel)
        mockAuthAsIndividual(Some("AA123456A"))
        mockGetPensionSessionData(taxYearEOY, Right(Some(pensionUserData)))

        val result = underTest.show(taxYearEOY).apply(fakeAgentRequest)
        status(result) shouldBe OK
        contentType(result) shouldBe Some("text/html")

        implicit val document: Document = Jsoup.parse(contentAsString(result))
        document.select(yesRadioButtonCssSelector).hasAttr("checked") shouldBe false
        document.select(noRadioButtonCssSelector).hasAttr("checked") shouldBe true
      }
    }
  }

    ".submit" should {
      "redirect to UnauthorisedUserErrorController when authentication fails" in {
        mockFailToAuthenticate()
        await(underTest.submit(taxYear = taxYearEOY)(fakeIndividualRequest.withFormUrlEncodedBody("value" -> "true"))) shouldBe
          Redirect(UnauthorisedUserErrorController.show)
      }


      "return BadRequest when getSessionData returns BadRequest" in {
        val request = fakeIndividualRequest.withSession(SessionValues.TAX_YEAR -> taxYearEOY.toString).withFormUrlEncodedBody("value" -> "true")

        mockAuthAsIndividual(Some("AA123456A"))
        mockGetPensionsSessionDataResult(taxYearEOY, BadRequest)

        await(underTest.submit(taxYearEOY).apply(request)) shouldBe BadRequest
      }
    }
}

