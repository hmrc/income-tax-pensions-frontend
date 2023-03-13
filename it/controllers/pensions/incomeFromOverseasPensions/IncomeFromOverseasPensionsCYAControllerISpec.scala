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
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.aPensionsUserData
import builders.UserBuilder.aUserRequest
import controllers.pensions.routes.OverseasPensionsSummaryController
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.reliefs.PaymentsIntoPensionViewModel
import org.scalatest.BeforeAndAfterEach
import play.api.Logging
import play.api.http.HeaderNames
import play.api.http.Status.SEE_OTHER
import play.api.libs.ws.WSResponse
import utils.PageUrls.IncomeFromOverseasPensionsPages.checkIncomeFromOverseasPensionsCyaUrl
import utils.PageUrls.fullUrl
import utils.{CommonUtils, IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class IncomeFromOverseasPensionsCYAControllerISpec extends
  IntegrationTest with
  ViewHelpers with
  BeforeAndAfterEach with
  PensionsDatabaseHelper with
  CommonUtils with Logging {   //scalastyle:off magic.number
  
  val cyaDataIncomplete: PaymentsIntoPensionViewModel = PaymentsIntoPensionViewModel( rasPensionPaymentQuestion = Some(true))

  object ChangeLinksIncomeFromOverseasPensions {
    val paymentsFromOverseasPensions: String = controllers.pensions.incomeFromOverseasPensions.routes.PensionOverseasIncomeStatus.show(taxYear).url
    val countrySummaryListController : String = controllers.pensions.incomeFromOverseasPensions.routes.CountrySummaryListController.show(taxYear).url
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
  }

  trait CommonExpectedResults {
    def expectedCaption(taxYear: Int): String

    val yes: String
    val no: String

    val paymentsFromOverseasPensions: String
    val overseasPensionsScheme : String

    val saveAndContinue: String
    val error: String

    val paymentsFromOverseasPensionsHidden: String
    val overseasPensionsSchemeHidden : String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"

    val yes = "Yes"
    val no = "No"

    val paymentsFromOverseasPensions = "Payments from overseas pensions"
    val overseasPensionsScheme = "Overseas pension schemes"


    val saveAndContinue = "Save and continue"
    val error = "Sorry, there is a problem with the service"

    val paymentsFromOverseasPensionsHidden = "Change Payments from overseas pensions"
    val overseasPensionsSchemeHidden = "Change Overseas pension schemes"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"

    val yes = "Yes"
    val no = "No"

    val paymentsFromOverseasPensions = "Payments from overseas pensions"
    val overseasPensionsScheme = "Overseas pension schemes"

    val saveAndContinue = "Save and continue"
    val error = "Sorry, there is a problem with the service"

    val paymentsFromOverseasPensionsHidden = "Change Payments from overseas pensions"
    val overseasPensionsSchemeHidden = "Change Overseas pension schemes"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1 = "Check income from overseas pensions"
    val expectedTitle = "Check income from overseas pensions"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1 = "Check income from overseas pensions"
    val expectedTitle = "Check income from overseas pensions"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1 = "Check income from overseas pensions"
    val expectedTitle = "Check income from overseas pensions"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "Check income from overseas pensions"
    val expectedTitle = "Check income from overseas pensions"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  def pensionsUsersData(isPrior: Boolean = false, pensionsCyaModel: PensionsCYAModel): PensionsUserData = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = isPrior, pensions = pensionsCyaModel)
  }

  def stringToBoolean(yesNo: Boolean): String = if (yesNo) "Yes" else "No"

//  ".show" should {
//    userScenarios.foreach { user =>
//      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
//        import user.commonExpectedResults._
//
//        "there is no CYA data and a CYA model is generated and  " which {
//
//          val anUpdatedAllPensionsData = anAllPensionsData.copy(
//            pensionIncome = Some(anAllPensionsData.pensionIncome.get.copy(
//              foreignPension = anAllPensionsData.pensionIncome.get.foreignPension.updated(0, ForeignPension (
//                countryCode = "FRA",
//                taxableAmount = BigDecimal(100),
//                amountBeforeTax = Some(BigDecimal(100)),
//                taxTakenOff = Some(BigDecimal(100)),
//                specialWithholdingTax = Some(BigDecimal(100)),
//                foreignTaxCreditRelief = Some(true)
//              ))
//            ))
//          )
//
//          implicit lazy val result: WSResponse = {
//            authoriseAgentOrIndividual(user.isAgent)
//            dropPensionsDB()
//            insertCyaData(pensionUserDataWithIncomeOverseasPension(anIncomeFromOverseasPensionsViewModel, isPriorSubmission = false), aUserRequest)
//            userDataStub(anIncomeTaxUserData.copy(pensions = Some(anUpdatedAllPensionsData)), nino, taxYear)
//            urlGet(fullUrl(checkIncomeFromOverseasPensionsCyaUrl(taxYear)), welsh = user.isWelsh,
//              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
//          }
//
//          "has an OK status" in {
//            result.status shouldBe OK
//          }
//
//          implicit def document: () => Document = () => Jsoup.parse(result.body)
//
//          titleCheck(user.specificExpectedResults.get.expectedTitle)
//          cyaRowCheck(paymentsFromOverseasPensions, stringToBoolean(anUpdatedAllPensionsData.pensionIncome.map(_.foreignPension).isDefined),
//            ChangeLinksIncomeFromOverseasPensions.paymentsFromOverseasPensions, paymentsFromOverseasPensionsHidden, 1)
//          cyaRowCheck(overseasPensionsScheme, s"FRANCE",
//            ChangeLinksIncomeFromOverseasPensions.countrySummaryListController, overseasPensionsSchemeHidden, 2)
//          buttonCheck(saveAndContinue)
//          welshToggleCheck(user.isWelsh)
//        }
//
//        "there is no CYA data and a CYA model is generated and there is no foreign pensions " which {
//
//          val anUpdatedAllPensionsData = anAllPensionsData.copy(
//            pensionIncome = Some(anAllPensionsData.pensionIncome.get.copy(
//              foreignPension = Seq[ForeignPension]()
//            ))
//          )
//          implicit lazy val result: WSResponse = {
//            authoriseAgentOrIndividual(user.isAgent)
//            dropPensionsDB()
//            insertCyaData(pensionUserDataWithIncomeOverseasPension(anIncomeFromOverseasPensionsViewModel, isPriorSubmission = false), aUserRequest)
//            userDataStub(anIncomeTaxUserData.copy(pensions = Some(anUpdatedAllPensionsData)), nino, taxYear)
//            urlGet(fullUrl(checkIncomeFromOverseasPensionsCyaUrl(taxYear)), welsh = user.isWelsh,
//              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
//          }
//
//          "has an OK status" in {
//            result.status shouldBe OK
//          }
//
//          implicit def document: () => Document = () => Jsoup.parse(result.body)
//
//          titleCheck(user.specificExpectedResults.get.expectedTitle)
//          cyaRowCheck(paymentsFromOverseasPensions, "No",
//            ChangeLinksIncomeFromOverseasPensions.paymentsFromOverseasPensions, paymentsFromOverseasPensionsHidden, 1)
//
//          buttonCheck(saveAndContinue)
//          welshToggleCheck(user.isWelsh)
//        }
//      }
//
//    }
//  }

  ".submit" should {
      "redirect to the overview page" when {
        "there is no CYA data available" should {

          val form = Map[String, String]()
          val userData = anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData))


          lazy val result: WSResponse = {
            dropPensionsDB()
            authoriseAgentOrIndividual(isAgent = false)
            userDataStub(userData, nino, taxYear)
            pensionIncomeSessionStub("", nino, taxYear)
            urlPost(fullUrl(checkIncomeFromOverseasPensionsCyaUrl(taxYear)), form, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
          }

          "have the status SEE OTHER" in {
            result.status shouldBe SEE_OTHER
          }

          "redirects to the overview page" in {
            result.headers("Location").head shouldBe OverseasPensionsSummaryController.show(taxYear).url
          }
        }
      }

      "redirect to the summary page" when {

        "the CYA data is persisted" should {

          val form = Map[String, String]()

          lazy val result: WSResponse = {
            dropPensionsDB()
            userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
            pensionIncomeSessionStub("", nino, taxYear)
            insertCyaData(aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(paymentsIntoPension = cyaDataIncomplete), taxYear = taxYear), aUserRequest)
            authoriseAgentOrIndividual(isAgent = false)
            urlPost(fullUrl(checkIncomeFromOverseasPensionsCyaUrl(taxYear)), form, follow = false,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
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
