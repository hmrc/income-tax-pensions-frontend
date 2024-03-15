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

package controllers.pensions.incomeFromOverseasPensions

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.IncomeFromOverseasPensionsViewModelBuilder.anIncomeFromOverseasPensionsViewModel
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionUserDataWithIncomeOverseasPension}
import builders.UserBuilder.aUser
import controllers.pensions.routes.{OverseasPensionsSummaryController, PensionsSummaryController}
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.PensionScheme
import models.pension.reliefs.PaymentsIntoPensionsViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.Logging
import play.api.http.HeaderNames
import play.api.http.Status.SEE_OTHER
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromOverseasPensionsPages.{checkIncomeFromOverseasPensionsCyaUrl, incomeFromOverseasPensionsStatus}
import utils.PageUrls.fullUrl
import utils.{CommonUtils, IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class IncomeFromOverseasPensionsCYAControllerISpec
    extends IntegrationTest
    with ViewHelpers
    with BeforeAndAfterEach
    with PensionsDatabaseHelper
    with CommonUtils
    with Logging { // scalastyle:off magic.number

  val cyaDataIncomplete: PaymentsIntoPensionsViewModel = PaymentsIntoPensionsViewModel(rasPensionPaymentQuestion = Some(true))

  object ChangeLinksIncomeFromOverseasPensions {
    val paymentsFromOverseasPensions: String = controllers.pensions.incomeFromOverseasPensions.routes.PensionOverseasIncomeStatus.show(taxYearEOY).url
    val countrySummaryListController: String =
      controllers.pensions.incomeFromOverseasPensions.routes.CountrySummaryListController.show(taxYearEOY).url
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    lazy val expectedH1: String = expectedTitle
    val yes: String
  }

  trait CommonExpectedResults {
    def expectedCaption(taxYear: Int): String

    val yes: String
    val no: String
    val paymentsFromOverseasPensions: String
    val overseasPensionsScheme: String
    val saveAndContinue: String
    val error: String
    val paymentsFromOverseasPensionsHidden: String
    val overseasPensionsSchemeHidden: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"

    val yes                                = "Yes"
    val no                                 = "No"
    val paymentsFromOverseasPensions       = "Payments from overseas pensions"
    val overseasPensionsScheme             = "Overseas pension schemes"
    val saveAndContinue                    = "Save and continue"
    val error                              = "Sorry, there is a problem with the service"
    val paymentsFromOverseasPensionsHidden = "Change Payments from overseas pensions"
    val overseasPensionsSchemeHidden       = "Change overseas pension schemes"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Incwm o bensiynau tramor ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"

    val yes                                = "Iawn"
    val no                                 = "Na"
    val paymentsFromOverseasPensions       = "Taliadau o bensiynau tramor"
    val overseasPensionsScheme             = "Cynllun pensiwn tramor"
    val saveAndContinue                    = "Cadw ac yn eich blaen"
    val error                              = "Sorry, there is a problem with the service"
    val paymentsFromOverseasPensionsHidden = "Newid taliadau o bensiynau tramor"
    val overseasPensionsSchemeHidden       = "Change overseas pension schemes"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Check income from overseas pensions"
    val yes           = "Yes"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Check income from overseas pensions"
    val yes           = "Yes"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Gwirio incwm o bensiynau tramor"
    val yes           = "Iawn"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Gwirio incwm o bensiynau tramor"
    val yes           = "Iawn"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  def pensionsUsersData(isPrior: Boolean = false, pensionsCyaModel: PensionsCYAModel): PensionsUserData =
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = isPrior, pensions = pensionsCyaModel)

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        import user.commonExpectedResults._

        "render the Check Income From Overseas Pensions page" when {

          "there is CYA data" which {

            lazy val result: WSResponse = {
              dropPensionsDB()
              authoriseAgentOrIndividual(user.isAgent)
              userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
              insertCyaData(pensionUserDataWithIncomeOverseasPension(anIncomeFromOverseasPensionsViewModel))
              urlGet(
                fullUrl(checkIncomeFromOverseasPensionsCyaUrl(taxYearEOY)),
                welsh = user.isWelsh,
                headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
              )
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
            captionCheck(user.commonExpectedResults.expectedCaption(taxYearEOY))
            cyaRowCheck(
              paymentsFromOverseasPensions,
              user.specificExpectedResults.get.yes,
              ChangeLinksIncomeFromOverseasPensions.paymentsFromOverseasPensions,
              paymentsFromOverseasPensionsHidden,
              1
            )
            cyaRowCheck(
              overseasPensionsScheme,
              s"FRANCE, GERMANY",
              ChangeLinksIncomeFromOverseasPensions.countrySummaryListController,
              overseasPensionsSchemeHidden,
              2)
            buttonCheck(saveAndContinue)
            welshToggleCheck(user.isWelsh)

          }
        }

      }
    }

    "redirect to the first page in the journey if cya data is incomplete" in {
      val incompleteViewModel = anIncomeFromOverseasPensionsViewModel.copy(
        overseasIncomePensionSchemes = Seq(
          PensionScheme(
            alphaThreeCode = None,
            alphaTwoCode = Some("FR"),
            pensionPaymentAmount = Some(1999.99),
            pensionPaymentTaxPaid = Some(1999.99),
            specialWithholdingTaxQuestion = Some(true),
            specialWithholdingTaxAmount = None,
            foreignTaxCreditReliefQuestion = Some(true),
            taxableAmount = Some(1999.99)
          )
        )
      )
      implicit lazy val result: WSResponse = {
        authoriseAgentOrIndividual(aUser.isAgent)
        dropPensionsDB()
        userDataStub(anIncomeTaxUserData, nino, taxYearEOY)
        insertCyaData(pensionUserDataWithIncomeOverseasPension(incompleteViewModel))

        urlGet(
          fullUrl(checkIncomeFromOverseasPensionsCyaUrl(taxYearEOY)),
          aUser.isAgent,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(incomeFromOverseasPensionsStatus(taxYearEOY))
    }
  }

  ".submit" should {
    "redirect to the summary page" when {

      "the CYA data is persisted" should {

        val form = Map[String, String]()

        lazy val result: WSResponse = {
          dropPensionsDB()
          userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
          pensionIncomeSessionStub("", nino, taxYear)
          insertCyaData(aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(paymentsIntoPension = cyaDataIncomplete), taxYear = taxYear))
          authoriseAgentOrIndividual()
          urlPost(
            fullUrl(checkIncomeFromOverseasPensionsCyaUrl(taxYear)),
            form,
            follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList))
          )
        }

        "the status is SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the summary page" in {
          result.headers("Location").head shouldBe OverseasPensionsSummaryController.show(taxYear).url
        }
      }
    }
  }
}
