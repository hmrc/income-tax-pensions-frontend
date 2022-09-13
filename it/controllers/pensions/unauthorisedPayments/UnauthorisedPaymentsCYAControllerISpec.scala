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

package controllers.pensions.unauthorisedPayments

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PensionChargesBuilder.anPensionCharges
import builders.PensionContributionsBuilder.anPensionContributions
import builders.PensionLifetimeAllowanceViewModelBuilder.aPensionLifetimeAllowanceViewModel
import builders.PensionSavingTaxChargesBuilder.anPensionSavngTaxCharges
import builders.PensionSchemeUnauthorisedPaymentsBuilder.anPensionSchemeUnauthorisedPayments
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.{aPensionsUserData, anPensionsUserDataEmptyCya, pensionsUserDataWithUnauthorisedPayments}
import builders.ReliefsBuilder.anReliefs
import builders.UnauthorisedPaymentsViewModelBuilder.{anUnauthorisedPaymentsEmptyViewModel, anUnauthorisedPaymentsViewModel}
import builders.UserBuilder.aUserRequest
import models.mongo.PensionsCYAModel
import models.pension.charges.PensionAnnualAllowancesViewModel
import models.pension.reliefs.PaymentsIntoPensionViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.Logging
import play.api.http.HeaderNames
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.{fullUrl, pensionSummaryUrl}
import utils.PageUrls.unauthorisedPaymentsPages.checkUnauthorisedPaymentsCyaUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}


class UnauthorisedPaymentsCYAControllerISpec extends
  IntegrationTest with
  ViewHelpers with
  BeforeAndAfterEach with
  PensionsDatabaseHelper with Logging {

  val cyaDataIncomplete: PaymentsIntoPensionViewModel = PaymentsIntoPensionViewModel(
    rasPensionPaymentQuestion = Some(true)
  )


  object ChangeLinksUnauthorisedPayments {
    val unauthorisedPayments: String = controllers.pensions.unauthorisedPayments.routes.UnAuthorisedPaymentsController.show(taxYear).url
    val amountSurcharged: String = controllers.pensions.unauthorisedPayments.routes.SurchargeAmountController.show(taxYear).url
    val nonUkTaxAmountSurcharged: String = controllers.pensions.unauthorisedPayments.routes.DidYouPayNonUkTaxController.show(taxYear).url
    val amountNotSurcharged: String = controllers.pensions.unauthorisedPayments.routes.NoSurchargeAmountController.show(taxYear).url
    val nonUkTaxAmountNotSurcharged: String = controllers.pensions.unauthorisedPayments.routes.NonUkTaxOnAmountNotSurchargeController.show(taxYear).url
    val ukPensionSchemes: String = controllers.pensions.unauthorisedPayments.routes.WhereAnyOfTheUnauthorisedPaymentsController.show(taxYear).url
    val pensionSchemeTaxReferences: String = controllers.pensions.unauthorisedPayments.routes.UnauthorisedPensionSchemeTaxReferenceController.show(taxYear).url
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
  }

  trait CommonExpectedResults {
    def expectedCaption(taxYear: Int): String

    val yes: String
    val no: String

    val unauthorisedPayments: String
    val amountSurcharged: String
    val nonUkTaxAmountSurcharged: String
    val amountNotSurcharged: String
    val nonUkTaxAmountNotSurcharged: String
    val ukPensionSchemes: String
    val pensionSchemeTaxReferences: String

    val saveAndContinue: String
    val error: String

    val unauthorisedPaymentsHidden: String
    val amountSurchargedHidden: String
    val nonUkTaxAmountSurchargedHidden: String
    val amountNotSurchargedHidden: String
    val nonUkTaxAmountNotSurchargedHidden: String
    val ukPensionSchemesHidden: String
    val pensionSchemeTaxReferencesHidden: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"

    val yes = "Yes"
    val no = "No"

    val unauthorisedPayments = "Unauthorised payments"
    val amountSurcharged = "Amount surcharged"
    val nonUkTaxAmountSurcharged = "Non UK-tax on amount surcharged"
    val amountNotSurcharged = "Amount not surcharged"
    val nonUkTaxAmountNotSurcharged = "Non UK-tax on amount not surcharged"
    val ukPensionSchemes = "UK pension schemes"
    val pensionSchemeTaxReferences = "Pension Scheme Tax References"


    val saveAndContinue = "Save and continue"
    val error = "Sorry, there is a problem with the service"

    val unauthorisedPaymentsHidden = "Change unauthorised payments"
    val amountSurchargedHidden = "Change amount surcharged"
    val nonUkTaxAmountSurchargedHidden = "Change non UK-tax on amount surcharged"
    val amountNotSurchargedHidden = "Change amount not surcharged"
    val nonUkTaxAmountNotSurchargedHidden = "Change non UK-tax on amount not surcharged"
    val ukPensionSchemesHidden = "Change UK pension schemes"
    val pensionSchemeTaxReferencesHidden = "Change Pension Scheme Tax References"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    def expectedCaption(taxYear: Int): String = s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"

    val yes = "Yes"
    val no = "No"

    val unauthorisedPayments = "Unauthorised payments"
    val amountSurcharged = "Amount surcharged"
    val nonUkTaxAmountSurcharged = "Non UK-tax on amount surcharged"
    val amountNotSurcharged = "Amount not surcharged"
    val nonUkTaxAmountNotSurcharged = "Non UK-tax on amount not surcharged"
    val ukPensionSchemes = "UK pension schemes"
    val pensionSchemeTaxReferences = "Pension Scheme Tax References"

    val saveAndContinue = "Save and continue"
    val error = "Sorry, there is a problem with the service"

    val unauthorisedPaymentsHidden = "Change unauthorised payments"
    val amountSurchargedHidden = "Change amount surcharged"
    val nonUkTaxAmountSurchargedHidden = "Change non UK-tax on amount surcharged"
    val amountNotSurchargedHidden = "Change amount not surcharged"
    val nonUkTaxAmountNotSurchargedHidden = "Change non UK-tax on amount not surcharged"
    val ukPensionSchemesHidden = "Change UK pension schemes"
    val pensionSchemeTaxReferencesHidden = "Change Pension Scheme Tax References"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1 = "Check your unauthorised payments"
    val expectedTitle = "Check your unauthorised payments"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s unauthorised payments"
    val expectedTitle = "Check your client’s unauthorised payments"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1 = "Check your unauthorised payments"
    val expectedTitle = "Check your unauthorised payments"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "Check your client’s unauthorised payments"
    val expectedTitle = "Check your client’s unauthorised payments"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  def pensionsUsersData(isPrior: Boolean = false, pensionsCyaModel: PensionsCYAModel) = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = isPrior, pensions = pensionsCyaModel)
  }

  def stringToBoolean(yesNo: Boolean) = if (yesNo) "Yes" else "No"

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        import user.commonExpectedResults._

        "there is no CYA data and a CYA model is generated and both surchargeQuestion and noSurchargeQuestion is set to true " which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            insertCyaData(pensionsUserDataWithUnauthorisedPayments(anUnauthorisedPaymentsViewModel, isPriorSubmission = false), aUserRequest)
            userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
            urlGet(fullUrl(checkUnauthorisedPaymentsCyaUrl(taxYear)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          val unauthorisedPaymentsFromIncomeTaxSubmission = anAllPensionsData.pensionCharges.get.pensionSchemeUnauthorisedPayments

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          cyaRowCheck(unauthorisedPayments, stringToBoolean(unauthorisedPaymentsFromIncomeTaxSubmission.get.surcharge.isDefined), ChangeLinksUnauthorisedPayments.unauthorisedPayments, unauthorisedPaymentsHidden, 1)
          cyaRowCheck(amountSurcharged, s"${moneyContent(unauthorisedPaymentsFromIncomeTaxSubmission.get.surcharge.map(_.amount).get)}",
            ChangeLinksUnauthorisedPayments.amountSurcharged, amountSurchargedHidden, 2)
          cyaRowCheck(nonUkTaxAmountSurcharged, s"${moneyContent(unauthorisedPaymentsFromIncomeTaxSubmission.get.surcharge.map(_.foreignTaxPaid).get)}",
            ChangeLinksUnauthorisedPayments.nonUkTaxAmountSurcharged, nonUkTaxAmountSurchargedHidden, 3)
          cyaRowCheck(amountNotSurcharged, s"${moneyContent(unauthorisedPaymentsFromIncomeTaxSubmission.get.noSurcharge.map(_.amount).get)}",
            ChangeLinksUnauthorisedPayments.amountNotSurcharged, amountNotSurchargedHidden, 4)
          cyaRowCheck(nonUkTaxAmountNotSurcharged, s"${moneyContent(unauthorisedPaymentsFromIncomeTaxSubmission.get.noSurcharge.map(_.foreignTaxPaid).get)}",
            ChangeLinksUnauthorisedPayments.nonUkTaxAmountNotSurcharged, nonUkTaxAmountNotSurchargedHidden, 5)
          cyaRowCheck(ukPensionSchemes, stringToBoolean(unauthorisedPaymentsFromIncomeTaxSubmission.map(_.pensionSchemeTaxReference).isDefined), ChangeLinksUnauthorisedPayments.ukPensionSchemes, ukPensionSchemesHidden, 6)
          cyaRowCheck(pensionSchemeTaxReferences, s"${unauthorisedPaymentsFromIncomeTaxSubmission.get.pensionSchemeTaxReference.mkString(", ")}", ChangeLinksUnauthorisedPayments.pensionSchemeTaxReferences, pensionSchemeTaxReferencesHidden, 7)

          buttonCheck(saveAndContinue)
          welshToggleCheck(user.isWelsh)
        }

        "there is no CYA data and a CYA model is generated and surchargeQuestion is set to false " which {
          val updatedanAllPensionsData = anAllPensionsData.copy(
            pensionCharges = Some(anPensionCharges.copy(
              pensionSchemeUnauthorisedPayments = Some(anPensionSchemeUnauthorisedPayments.copy(
                surcharge = None
              ))
            )))
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val updatedAnUnauthorisedPaymentsViewModel = anUnauthorisedPaymentsViewModel.copy(
              surchargeQuestion = Some(false)
            )
            insertCyaData(pensionsUserDataWithUnauthorisedPayments(updatedAnUnauthorisedPaymentsViewModel, isPriorSubmission = false), aUserRequest)
            userDataStub(anIncomeTaxUserData.copy(pensions = Some(updatedanAllPensionsData)), nino, taxYear)
            urlGet(fullUrl(checkUnauthorisedPaymentsCyaUrl(taxYear)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          val unauthorisedPaymentsFromIncomeTaxSubmission = updatedanAllPensionsData.pensionCharges.get.pensionSchemeUnauthorisedPayments

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          cyaRowCheck(unauthorisedPayments, yes, ChangeLinksUnauthorisedPayments.unauthorisedPayments, unauthorisedPaymentsHidden, 1)
          cyaRowCheck(amountNotSurcharged, s"${moneyContent(unauthorisedPaymentsFromIncomeTaxSubmission.get.noSurcharge.map(_.amount).get)}",
                      ChangeLinksUnauthorisedPayments.amountNotSurcharged, amountNotSurchargedHidden, 2)
          cyaRowCheck(nonUkTaxAmountNotSurcharged, s"${moneyContent(unauthorisedPaymentsFromIncomeTaxSubmission.get.noSurcharge.map(_.foreignTaxPaid).get)}",
                      ChangeLinksUnauthorisedPayments.nonUkTaxAmountNotSurcharged, nonUkTaxAmountNotSurchargedHidden, 3)
          cyaRowCheck(ukPensionSchemes, stringToBoolean(unauthorisedPaymentsFromIncomeTaxSubmission.map(_.pensionSchemeTaxReference).isDefined), ChangeLinksUnauthorisedPayments.ukPensionSchemes, ukPensionSchemesHidden, 4)
          cyaRowCheck(pensionSchemeTaxReferences, s"${unauthorisedPaymentsFromIncomeTaxSubmission.get.pensionSchemeTaxReference.mkString(", ")}", ChangeLinksUnauthorisedPayments.pensionSchemeTaxReferences, pensionSchemeTaxReferencesHidden, 5)

          buttonCheck(saveAndContinue)
          welshToggleCheck(user.isWelsh)
        }

        "there is no CYA data and a CYA model is generated and noSurchargeQuestion is set to true " which {
          val updatedanAllPensionsData = anAllPensionsData.copy(
            pensionCharges = Some(anPensionCharges.copy(
              pensionSchemeUnauthorisedPayments = Some(anPensionSchemeUnauthorisedPayments.copy(
                noSurcharge = None
              ))
            )))
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val updatedAnUnauthorisedPaymentsViewModel = anUnauthorisedPaymentsViewModel.copy(
              noSurchargeQuestion = Some(false)
            )
            insertCyaData(pensionsUserDataWithUnauthorisedPayments(updatedAnUnauthorisedPaymentsViewModel, isPriorSubmission = false), aUserRequest)
            userDataStub(anIncomeTaxUserData.copy(pensions = Some(updatedanAllPensionsData)), nino, taxYear)
            urlGet(fullUrl(checkUnauthorisedPaymentsCyaUrl(taxYear)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          val unauthorisedPaymentsFromIncomeTaxSubmission = updatedanAllPensionsData.pensionCharges.get.pensionSchemeUnauthorisedPayments

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          cyaRowCheck(unauthorisedPayments, stringToBoolean(unauthorisedPaymentsFromIncomeTaxSubmission.get.surcharge.isDefined), ChangeLinksUnauthorisedPayments.unauthorisedPayments, unauthorisedPaymentsHidden, 1)
          cyaRowCheck(amountSurcharged, s"${moneyContent(unauthorisedPaymentsFromIncomeTaxSubmission.get.surcharge.map(_.amount).get)}",
            ChangeLinksUnauthorisedPayments.amountSurcharged, amountSurchargedHidden, 2)
          cyaRowCheck(nonUkTaxAmountSurcharged, s"${moneyContent(unauthorisedPaymentsFromIncomeTaxSubmission.get.surcharge.map(_.foreignTaxPaid).get)}",
            ChangeLinksUnauthorisedPayments.nonUkTaxAmountSurcharged, nonUkTaxAmountSurchargedHidden, 3)
          cyaRowCheck(ukPensionSchemes, stringToBoolean(unauthorisedPaymentsFromIncomeTaxSubmission.map(_.pensionSchemeTaxReference).isDefined), ChangeLinksUnauthorisedPayments.ukPensionSchemes, ukPensionSchemesHidden, 4)
          cyaRowCheck(pensionSchemeTaxReferences, s"${unauthorisedPaymentsFromIncomeTaxSubmission.get.pensionSchemeTaxReference.mkString(", ")}", ChangeLinksUnauthorisedPayments.pensionSchemeTaxReferences, pensionSchemeTaxReferencesHidden, 5)

          buttonCheck(saveAndContinue)
          welshToggleCheck(user.isWelsh)
        }

        "there is no CYA data and a CYA model is generated and both surchargeQuestion and noSurchargeQuestion are set to false " which {
          val updatedanAllPensionsData = anAllPensionsData.copy(
            pensionCharges = Some(anPensionCharges.copy(
              pensionSchemeUnauthorisedPayments = Some(anPensionSchemeUnauthorisedPayments.copy(
                surcharge = None,
                noSurcharge = None
              ))
            )))

          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            val updatedAnUnauthorisedPaymentsViewModel = anUnauthorisedPaymentsViewModel.copy(
              surchargeQuestion = Some(false),
              noSurchargeQuestion = Some(false)
            )
            insertCyaData(pensionsUserDataWithUnauthorisedPayments(updatedAnUnauthorisedPaymentsViewModel, isPriorSubmission = false), aUserRequest)
            userDataStub(anIncomeTaxUserData.copy(pensions = Some(updatedanAllPensionsData)), nino, taxYear)
            urlGet(fullUrl(checkUnauthorisedPaymentsCyaUrl(taxYear)), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          val unauthorisedPaymentsFromIncomeTaxSubmission = updatedanAllPensionsData.pensionCharges.get.pensionSchemeUnauthorisedPayments

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          cyaRowCheck(unauthorisedPayments, stringToBoolean(unauthorisedPaymentsFromIncomeTaxSubmission.get.surcharge.isDefined), ChangeLinksUnauthorisedPayments.unauthorisedPayments, unauthorisedPaymentsHidden, 1)

          buttonCheck(saveAndContinue)
          welshToggleCheck(user.isWelsh)
        }

      }

    }
  }

  ".submit" should {
    "redirect to the overview page" when {

      "there is no CYA data available" should {

        val form = Map[String, String]()

        lazy val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(checkUnauthorisedPaymentsCyaUrl(taxYear)), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        "have the status SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the overview page" in {
          result.headers("Location").head shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
        }
      }
    }

    "redirect to the summary page" when {

      "the CYA data differs from the prior data" should {

        val form = Map[String, String]()

        lazy val result: WSResponse = {
          dropPensionsDB()
          userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
          insertCyaData(aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(paymentsIntoPension = cyaDataIncomplete), taxYear = taxYear), aUserRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(checkUnauthorisedPaymentsCyaUrl(taxYear)), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        "the status is SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the summary page" in {
          result.headers("Location").head shouldBe controllers.pensions.unauthorisedPayments.routes.UnauthorisedPaymentsCYAController.show(taxYear).url
        }
      }

      "the user makes no changes and no submission to DES is made" should {

        val unchangedModel =
          PaymentsIntoPensionViewModel(
            None, Some(true), anReliefs.regularPensionContributions,
            Some(true), anReliefs.oneOffPensionContributionsPaid, Some(true), Some(true), Some(true),
            anReliefs.retirementAnnuityPayments, Some(true), anReliefs.paymentToEmployersSchemeNoTaxRelief)

        val unchangedAllowances = PensionAnnualAllowancesViewModel(
          Some(anPensionSavngTaxCharges.isAnnualAllowanceReduced),
          anPensionSavngTaxCharges.moneyPurchasedAllowance, anPensionSavngTaxCharges.taperedAnnualAllowance,
          Some(true), Some(anPensionContributions.inExcessOfTheAnnualAllowance), Some(true),
          Some(anPensionContributions.annualAllowanceTaxPaid),
          Some(anPensionContributions.pensionSchemeTaxReference))

        val form = Map[String, String]()

        lazy val result: WSResponse = {
          dropPensionsDB()
          userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
          insertCyaData(aPensionsUserData.copy(pensions = aPensionsCYAModel.copy
          (paymentsIntoPension = unchangedModel, pensionsAnnualAllowances = unchangedAllowances,
            pensionLifetimeAllowances = aPensionLifetimeAllowanceViewModel,
            incomeFromPensions = anIncomeFromPensionsViewModel,
            unauthorisedPayments = anUnauthorisedPaymentsViewModel,
            paymentsIntoOverseasPensions = aPaymentsIntoOverseasPensionsViewModel
          ), taxYear = taxYear), aUserRequest)
          authoriseAgentOrIndividual(isAgent = false)
          urlPost(fullUrl(checkUnauthorisedPaymentsCyaUrl(taxYear)), form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        "the status is SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the summary page" in {
          result.headers("Location").head shouldBe controllers.pensions.unauthorisedPayments.routes.UnauthorisedPaymentsCYAController.show(taxYear).url
        }
      }
    }
  }
}

