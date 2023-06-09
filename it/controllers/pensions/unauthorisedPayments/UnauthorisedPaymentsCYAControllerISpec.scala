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

package controllers.pensions.unauthorisedPayments

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionChargesBuilder.anPensionCharges
import builders.PensionSchemeUnauthorisedPaymentsBuilder.anPensionSchemeUnauthorisedPayments
import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.{aPensionsUserData, pensionsUserDataWithUnauthorisedPayments}
import builders.UnauthorisedPaymentsViewModelBuilder.anUnauthorisedPaymentsViewModel
import controllers.pensions.unauthorisedPayments.routes._
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.reliefs.PaymentsIntoPensionViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.BeforeAndAfterEach
import play.api.Logging
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.fullUrl
import utils.PageUrls.unauthorisedPaymentsPages.checkUnauthorisedPaymentsCyaUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}


class UnauthorisedPaymentsCYAControllerISpec extends
  IntegrationTest with
  ViewHelpers with
  BeforeAndAfterEach with
  PensionsDatabaseHelper with Logging { //scalastyle:off magic.number

  val cyaDataIncomplete: PaymentsIntoPensionViewModel = PaymentsIntoPensionViewModel(
    rasPensionPaymentQuestion = Some(true)
  )


  object ChangeLinksUnauthorisedPayments {
    val unauthorisedPayments: String = UnauthorisedPaymentsController.show(taxYear).url
    val amountSurcharged: String = SurchargeAmountController.show(taxYear).url
    val nonUKTaxOnAmountResultedInSurcharge: String = NonUKTaxOnAmountResultedInSurchargeController.show(taxYear).url
    val amountNotSurcharged: String = NoSurchargeAmountController.show(taxYear).url
    val nonUKTaxOnAmountNotResultedInSurcharge: String = NonUKTaxOnAmountNotResultedInSurchargeController.show(taxYear).url
    val ukPensionSchemes: String = WhereAnyOfTheUnauthorisedPaymentsController.show(taxYear).url
    val pensionSchemeTaxReferences: String = UnauthorisedPensionSchemeTaxReferenceController.show(taxYear, None).url
    val pensionSchemeTaxDetails: String = UkPensionSchemeDetailsController.show(taxYear).url
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
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
    val expectedCaption: Int => String = (taxYear: Int) => s"Unauthorised payments from pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
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
    val expectedCaption: Int => String = (taxYear: Int) => s"Payments into pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yes = "Iawn"
    val no = "Na"
    val unauthorisedPayments = "Taliadau heb awdurdod"
    val amountSurcharged = "Y swm y codwyd gordal arno"
    val nonUkTaxAmountSurcharged = "Treth y tu allan i’r DU ar y swm y codwyd gordal arno"
    val amountNotSurcharged = "Y swm na chodwyd gordal arno"
    val nonUkTaxAmountNotSurcharged = "Treth y tu allan i’r DU ar y swm na chodwyd gordal arno"
    val ukPensionSchemes = "Cynlluniau pensiwn y DU"
    val pensionSchemeTaxReferences = "Cyfeirnodau Treth y Cynlluniau Pensiwn"

    val saveAndContinue = "Cadw ac yn eich blaen"
    val error = "Sorry, there is a problem with the service"

    val unauthorisedPaymentsHidden = "Newid taliadau heb awdurdod"
    val amountSurchargedHidden = "Newid y swm y codwyd gordal arno"
    val nonUkTaxAmountSurchargedHidden = "Newid swm y dreth y tu allan i’r DU ar y swm y codwyd gordal arno"
    val amountNotSurchargedHidden = "Newid y swm na chodwyd gordal arno"
    val nonUkTaxAmountNotSurchargedHidden = "Newid swm y dreth y tu allan i’r DU ar y swm na chodwyd gordal arno"
    val ukPensionSchemesHidden = "Newid cynlluniau pensiwn y DU"
    val pensionSchemeTaxReferencesHidden = "Newid Cyfeirnodau Treth y Cynlluniau Pensiwn"
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
    val expectedH1 = "Gwirio’ch taliadau heb awdurdod"
    val expectedTitle = "Gwirio’ch taliadau heb awdurdod"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "Gwirio taliadau’ch cleient a oedd heb awdurdod"
    val expectedTitle = "Gwirio taliadau’ch cleient a oedd heb awdurdod"
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
 

  ".show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        import user.commonExpectedResults._

        val stringToBoolean: Boolean => String = (yesNo: Boolean) => if (yesNo) user.commonExpectedResults.yes else  user.commonExpectedResults.no

        "there is no CYA data and a CYA model is generated and both surchargeQuestion and noSurchargeQuestion is set to true " which {
          implicit lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            dropPensionsDB()
            insertCyaData(pensionsUserDataWithUnauthorisedPayments(anUnauthorisedPaymentsViewModel, isPriorSubmission = false))
            userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYear)
            urlGet(fullUrl(checkUnauthorisedPaymentsCyaUrl(taxYear)), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          val unauthorisedPaymentsFromIncomeTaxSubmission = anAllPensionsData.pensionCharges.get.pensionSchemeUnauthorisedPayments

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          cyaRowCheck(unauthorisedPayments, stringToBoolean(unauthorisedPaymentsFromIncomeTaxSubmission.get.surcharge.isDefined),
            ChangeLinksUnauthorisedPayments.unauthorisedPayments, unauthorisedPaymentsHidden, 1)
          
          cyaRowCheck(amountSurcharged, s"${moneyContent(unauthorisedPaymentsFromIncomeTaxSubmission.get.surcharge.map(_.amount).get)}",
            ChangeLinksUnauthorisedPayments.amountSurcharged, amountSurchargedHidden, 2)
          
          cyaRowCheck(nonUkTaxAmountSurcharged, s"${moneyContent(unauthorisedPaymentsFromIncomeTaxSubmission.get.surcharge.map(_.foreignTaxPaid).get)}",
            ChangeLinksUnauthorisedPayments.nonUKTaxOnAmountResultedInSurcharge, nonUkTaxAmountSurchargedHidden, 3)
          
          cyaRowCheck(amountNotSurcharged, s"${moneyContent(unauthorisedPaymentsFromIncomeTaxSubmission.get.noSurcharge.map(_.amount).get)}",
            ChangeLinksUnauthorisedPayments.amountNotSurcharged, amountNotSurchargedHidden, 4)
          
          cyaRowCheck(nonUkTaxAmountNotSurcharged, s"${moneyContent(unauthorisedPaymentsFromIncomeTaxSubmission.get.noSurcharge.map(_.foreignTaxPaid).get)}",
            ChangeLinksUnauthorisedPayments.nonUKTaxOnAmountNotResultedInSurcharge, nonUkTaxAmountNotSurchargedHidden, 5)
          
          cyaRowCheck(ukPensionSchemes, stringToBoolean(unauthorisedPaymentsFromIncomeTaxSubmission.map(_.pensionSchemeTaxReference).isDefined),
            ChangeLinksUnauthorisedPayments.ukPensionSchemes, ukPensionSchemesHidden, 6)
          
          cyaRowCheck(pensionSchemeTaxReferences, s"${unauthorisedPaymentsFromIncomeTaxSubmission.get.pensionSchemeTaxReference.get.mkString(", ")}",
            ChangeLinksUnauthorisedPayments.pensionSchemeTaxDetails, pensionSchemeTaxReferencesHidden, 7)

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
            insertCyaData(pensionsUserDataWithUnauthorisedPayments(updatedAnUnauthorisedPaymentsViewModel, isPriorSubmission = false))
            userDataStub(anIncomeTaxUserData.copy(pensions = Some(updatedanAllPensionsData)), nino, taxYear)
            urlGet(fullUrl(checkUnauthorisedPaymentsCyaUrl(taxYear)), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          val unauthorisedPaymentsFromIncomeTaxSubmission = updatedanAllPensionsData.pensionCharges.get.pensionSchemeUnauthorisedPayments

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          cyaRowCheck(unauthorisedPayments, yes, ChangeLinksUnauthorisedPayments.unauthorisedPayments, unauthorisedPaymentsHidden, 1)
          
          cyaRowCheck(amountNotSurcharged, s"${moneyContent(unauthorisedPaymentsFromIncomeTaxSubmission.get.noSurcharge.map(_.amount).get)}",
            ChangeLinksUnauthorisedPayments.amountNotSurcharged, amountNotSurchargedHidden, 2)
          
          cyaRowCheck(nonUkTaxAmountNotSurcharged, s"${moneyContent(unauthorisedPaymentsFromIncomeTaxSubmission.get.noSurcharge.map(_.foreignTaxPaid).get)}",
            ChangeLinksUnauthorisedPayments.nonUKTaxOnAmountNotResultedInSurcharge, nonUkTaxAmountNotSurchargedHidden, 3)
          
          cyaRowCheck(ukPensionSchemes, stringToBoolean(unauthorisedPaymentsFromIncomeTaxSubmission.map(_.pensionSchemeTaxReference).isDefined),
            ChangeLinksUnauthorisedPayments.ukPensionSchemes, ukPensionSchemesHidden, 4)
          
          cyaRowCheck(pensionSchemeTaxReferences, s"${unauthorisedPaymentsFromIncomeTaxSubmission.get.pensionSchemeTaxReference.get.mkString(", ")}",
            ChangeLinksUnauthorisedPayments.pensionSchemeTaxDetails, pensionSchemeTaxReferencesHidden, 5)

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
            insertCyaData(pensionsUserDataWithUnauthorisedPayments(updatedAnUnauthorisedPaymentsViewModel, isPriorSubmission = false))
            userDataStub(anIncomeTaxUserData.copy(pensions = Some(updatedanAllPensionsData)), nino, taxYear)
            urlGet(fullUrl(checkUnauthorisedPaymentsCyaUrl(taxYear)), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          val unauthorisedPaymentsFromIncomeTaxSubmission = updatedanAllPensionsData.pensionCharges.get.pensionSchemeUnauthorisedPayments

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          cyaRowCheck(unauthorisedPayments, stringToBoolean(unauthorisedPaymentsFromIncomeTaxSubmission.get.surcharge.isDefined),
            ChangeLinksUnauthorisedPayments.unauthorisedPayments, unauthorisedPaymentsHidden, 1)
          
          cyaRowCheck(amountSurcharged, s"${moneyContent(unauthorisedPaymentsFromIncomeTaxSubmission.get.surcharge.map(_.amount).get)}",
            ChangeLinksUnauthorisedPayments.amountSurcharged, amountSurchargedHidden, 2)
          
          cyaRowCheck(nonUkTaxAmountSurcharged, s"${moneyContent(unauthorisedPaymentsFromIncomeTaxSubmission.get.surcharge.map(_.foreignTaxPaid).get)}",
            ChangeLinksUnauthorisedPayments.nonUKTaxOnAmountResultedInSurcharge, nonUkTaxAmountSurchargedHidden, 3)
          
          cyaRowCheck(ukPensionSchemes, stringToBoolean(unauthorisedPaymentsFromIncomeTaxSubmission.map(_.pensionSchemeTaxReference).isDefined),
            ChangeLinksUnauthorisedPayments.ukPensionSchemes, ukPensionSchemesHidden, 4)
          
          cyaRowCheck(pensionSchemeTaxReferences, s"${unauthorisedPaymentsFromIncomeTaxSubmission.get.pensionSchemeTaxReference.get.mkString(", ")}",
            ChangeLinksUnauthorisedPayments.pensionSchemeTaxDetails, pensionSchemeTaxReferencesHidden, 5)

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
            insertCyaData(pensionsUserDataWithUnauthorisedPayments(updatedAnUnauthorisedPaymentsViewModel, isPriorSubmission = false))
            userDataStub(anIncomeTaxUserData.copy(pensions = Some(updatedanAllPensionsData)), nino, taxYear)
            urlGet(fullUrl(checkUnauthorisedPaymentsCyaUrl(taxYear)), welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
          }

          "has an OK status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          val unauthorisedPaymentsSurcharge = updatedanAllPensionsData.pensionCharges
            .flatMap(_.pensionSchemeUnauthorisedPayments).flatMap(_.surcharge).map(charge => stringToBoolean(charge.amount > 0))

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          cyaRowCheck(unauthorisedPayments, unauthorisedPaymentsSurcharge.getOrElse(""),
            ChangeLinksUnauthorisedPayments.unauthorisedPayments, unauthorisedPaymentsHidden, 1)

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
          authoriseAgentOrIndividual()
          pensionChargesSessionStub("", nino, taxYear)
          urlPost(fullUrl(checkUnauthorisedPaymentsCyaUrl(taxYear)), form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        "have the status SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the overview page" in {
          result.headers("Location").head shouldBe controllers.pensions.routes.PensionsSummaryController.show(taxYear).url
        }
      }
    }

    "redirect to the summary page" when {

      "the cya data is persisted to pensions backend" should {

        val form = Map[String, String]()
        val userData = anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData))

        lazy val result: WSResponse = {
          dropPensionsDB()
          userDataStub(userData, nino, taxYear)
          pensionChargesSessionStub("", nino, taxYear)
          insertCyaData(aPensionsUserData.copy(pensions = aPensionsCYAModel.copy(paymentsIntoPension = cyaDataIncomplete), taxYear = taxYear))
          authoriseAgentOrIndividual()
          urlPost(fullUrl(checkUnauthorisedPaymentsCyaUrl(taxYear)), form, follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear, validTaxYearList)))
        }

        "the status is SEE OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the summary page" in {
          result.headers("Location").head shouldBe controllers.pensions.routes.PensionsSummaryController.show(taxYear).url
        }
      }

    }
  }
}

