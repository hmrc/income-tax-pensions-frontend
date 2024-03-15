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

package controllers.pensions.annualAllowances

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionAnnualAllowanceViewModelBuilder.aPensionAnnualAllowanceViewModel
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.pensionsUserDataWithAnnualAllowances
import builders.UserBuilder.{aUser, aUserRequest}
import forms.{RadioButtonAmountForm, YesNoForm}
import models.mongo.PensionsCYAModel
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PensionAnnualAllowancePages._
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class PensionProviderPaidTaxControllerISpec extends IntegrationTest with ViewHelpers with PensionsDatabaseHelper {
  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel) =
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = false, pensions = pensionsCyaModel)

  override val userScenarios: Seq[UserScenario[_, _]] = Nil

  ".show" should {
    "show page when EOY" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)
        urlGet(
          fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)),
          !aUser.isAgent,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe OK
    }

    "redirect to reduced annual allowance page" when {
      "previous questions have not been answered" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowance = None)
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))

          urlGet(
            fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)),
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has a SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(reducedAnnualAllowanceUrl(taxYearEOY)) shouldBe true
        }

      }
      "page is invalid in journey" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(aboveAnnualAllowanceQuestion = Some(false))
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))

          urlGet(
            fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)),
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
        }

        "has a SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location").contains(reducedAnnualAllowanceUrl(taxYearEOY)) shouldBe true
        }
      }
    }
  }

  ".submit" should {

    "redirect to reduced annual allowance page" when {
      lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "14.55")

      "previous questions have not been answered" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(reducedAnnualAllowanceQuestion = None)
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))

          urlPost(
            fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)),
            body = form,
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
          )
        }

        "has a SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(reducedAnnualAllowanceUrl(taxYearEOY))
        }

      }
      "page is invalid in journey" which {
        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual()
          val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(reducedAnnualAllowanceQuestion = Some(false))
          insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(pensionsAnnualAllowances = pensionsViewModel)))

          urlPost(
            fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)),
            body = form,
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
          )
        }

        "has a SEE_OTHER status" in {
          result.status shouldBe SEE_OTHER
          result.header("location") shouldBe Some(reducedAnnualAllowanceUrl(taxYearEOY))
        }
      }
    }

    "redirect to PensionSchemeTaxReference page when user selects 'yes' with an amount and is not a prior submission" which {
      lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "14.55")
      val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
        pensionProvidePaidAnnualAllowanceQuestion = None,
        taxPaidByPensionProvider = None,
        pensionSchemeTaxReferences = None)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
        urlPost(
          fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(pensionSchemeTaxReferenceUrl(taxYearEOY))
      }

      "updates reducedAnnualAllowanceQuestion to Some(true)" in {
        val expectedViewModel = aPensionAnnualAllowanceViewModel.copy(pensionSchemeTaxReferences = None)
        lazy val cyaModel     = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances shouldBe expectedViewModel
      }
    }

    "redirect to CYA page when user selects 'yes' with an amount, updating existing CYA data which is now complete" which {
      lazy val form: Map[String, String] = Map(RadioButtonAmountForm.yesNo -> "true", RadioButtonAmountForm.amount2 -> "300")
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        insertCyaData(pensionsUserDataWithAnnualAllowances(aPensionAnnualAllowanceViewModel))
        urlPost(
          fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(annualAllowancesCYAUrl(taxYearEOY))
      }

      "updates reducedAnnualAllowanceQuestion to Some(true)" in {
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances shouldBe aPensionAnnualAllowanceViewModel.copy(taxPaidByPensionProvider = Some(300))
      }
    }

    "redirect to the CYA page when user selects 'no' without prior data" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      val pensionsViewModel = aPensionAnnualAllowanceViewModel.copy(
        pensionProvidePaidAnnualAllowanceQuestion = None,
        taxPaidByPensionProvider = None,
        pensionSchemeTaxReferences = None)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        insertCyaData(pensionsUserDataWithAnnualAllowances(pensionsViewModel))
        urlPost(
          fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(annualAllowancesCYAUrl(taxYearEOY))
      }

      "updates reducedAnnualAllowanceQuestion to Some(false)" in {
        val expectedViewModel = pensionsViewModel.copy(pensionProvidePaidAnnualAllowanceQuestion = Some(false))
        lazy val cyaModel     = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances shouldBe expectedViewModel
      }
    }

    "redirect to the CYA page updating question to 'no' and clearing other prior data" which {
      lazy val form: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
      lazy val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual()
        insertCyaData(pensionsUserDataWithAnnualAllowances(aPensionAnnualAllowanceViewModel))
        urlPost(
          fullUrl(pensionProviderPaidTaxUrl(taxYearEOY)),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header("location") shouldBe Some(annualAllowancesCYAUrl(taxYearEOY))
      }

      "updates reducedAnnualAllowanceQuestion to Some(false)" in {
        val expectedViewModel = aPensionAnnualAllowanceViewModel.copy(
          pensionProvidePaidAnnualAllowanceQuestion = Some(false),
          taxPaidByPensionProvider = None,
          pensionSchemeTaxReferences = None)
        lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
        cyaModel.pensions.pensionsAnnualAllowances shouldBe expectedViewModel
      }
    }
  }
}
