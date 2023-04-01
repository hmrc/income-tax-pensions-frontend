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

import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsEmptyViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.UserBuilder.{aUser, aUserRequest}
import forms.RadioButtonForm
import models.mongo.PensionsCYAModel
import models.pension.charges.TaxReliefQuestion
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoOverseasPensions.{pensionCustomerReferenceNumberUrl, pensionReliefSchemeDetailsUrl, pensionReliefTypeUrl, qopsReferenceUrl, sf74ReferenceUrl, doubleTaxationAgreementUrl}
import utils.PageUrls.{fullUrl, overviewUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class PensionsReliefControllerISpec extends IntegrationTest with ViewHelpers
  with PensionsDatabaseHelper {

  private def pensionsUsersData(isPrior: Boolean, pensionsCyaModel: PensionsCYAModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = isPrior, pensions = pensionsCyaModel)
  }

  override val userScenarios: Seq[UserScenario[_, _]] = Nil

  ".show" should {
    "redirect to Overview Page when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel), aUserRequest)
        urlGet(fullUrl(pensionReliefTypeUrl(taxYear, 1)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
      }
      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe overviewUrl(taxYear)
    }

    "show page when EOY" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel), aUserRequest)
        urlGet(fullUrl(pensionReliefTypeUrl(taxYearEOY, 1)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe OK
    }

    "redirect to start of sequence when index doesn't match" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel), aUserRequest)
        urlGet(fullUrl(pensionReliefTypeUrl(taxYearEOY, 100)), !aUser.isAgent, follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }
      result.status shouldBe SEE_OTHER
      result.headers("Location").head shouldBe pensionCustomerReferenceNumberUrl(taxYearEOY)
    }
    }

  ".submit" should {
    "redirect to overview when in year" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val formData = Map(RadioButtonForm.value -> TaxReliefQuestion.TransitionalCorrespondingRelief)

        insertCyaData(pensionsUsersData(
          isPrior = false,
          aPensionsCYAModel.copy(paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsEmptyViewModel)),
          aUserRequest)

        urlPost(
          fullUrl(pensionReliefTypeUrl(taxYear, 0)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)),
          follow = false,
          body = formData)
      }

      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe overviewUrl(taxYear)
    }

    "persist data and redirect to SF74 reference page when TCR is selected" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val formData = Map(RadioButtonForm.value -> TaxReliefQuestion.TransitionalCorrespondingRelief)
        insertCyaData(pensionsUsersData(isPrior = false, aPensionsCYAModel), aUserRequest)
        urlPost(
          fullUrl(pensionReliefTypeUrl(taxYearEOY, 0)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = formData)
      }

      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe sf74ReferenceUrl(taxYearEOY)
    }

    "persist amount and redirect to QOPS Reference Page when MMR is selected" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val formData = Map(RadioButtonForm.value -> TaxReliefQuestion.MigrantMemberRelief)
        insertCyaData(pensionsUsersData(isPrior = true, aPensionsCYAModel), aUserRequest)
        urlPost(
          fullUrl(pensionReliefTypeUrl(taxYearEOY, 0)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = formData)
      }

      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe qopsReferenceUrl(taxYearEOY)
    }

    "persist amount and redirect to DTR when DTR is selected" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val formData = Map(RadioButtonForm.value -> TaxReliefQuestion.DoubleTaxationRelief)
        insertCyaData(pensionsUsersData(isPrior = true, aPensionsCYAModel), aUserRequest)
        urlPost(
          fullUrl(pensionReliefTypeUrl(taxYearEOY, 0)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = formData)
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe doubleTaxationAgreementUrl(0)(taxYearEOY)
    }

    "persist amount and redirect to CYA when None of the above is selected" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val formData = Map(RadioButtonForm.value -> TaxReliefQuestion.NoTaxRelief)
        insertCyaData(pensionsUsersData(isPrior = true, aPensionsCYAModel), aUserRequest)
        urlPost(
          fullUrl(pensionReliefTypeUrl(taxYearEOY, 0)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = formData)
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe pensionReliefSchemeDetailsUrl(taxYearEOY, 0)
    }

    "return an error when form is submitted with no entry" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val formData = Map(RadioButtonForm.value -> "")
        insertCyaData(pensionsUsersData(isPrior = true, aPensionsCYAModel), aUserRequest)
        urlPost(
          fullUrl(pensionReliefTypeUrl(taxYearEOY, 1)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = formData)
      }
      result.status shouldBe BAD_REQUEST
    }

    "redirect to start of sequence when index doesn't match" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val formData = Map(RadioButtonForm.value -> TaxReliefQuestion.TransitionalCorrespondingRelief)
        insertCyaData(pensionsUsersData(isPrior = true, aPensionsCYAModel), aUserRequest)
        urlPost(
          fullUrl(pensionReliefTypeUrl(taxYearEOY, 100)),
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)),
          follow = false,
          body = formData)
      }
      result.status shouldBe SEE_OTHER
      result.headers("location").head shouldBe pensionCustomerReferenceNumberUrl(taxYearEOY)
    }
  }
}
