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

import builders.PensionsCYAModelBuilder.{aPensionsCYAEmptyModel, aPensionsCYAModel}
import controllers.ControllerSpec.PreferredLanguages.{English, Welsh}
import controllers.ControllerSpec.UserTypes.{Agent, Individual}
import controllers.ControllerSpec._
import controllers.YesNoAmountControllerSpec
import models.mongo.PensionsUserData
import models.pension.charges.PensionScheme
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.ws.{WSClient, WSResponse}

class SpecialWithholdingTaxControllerISpec extends YesNoAmountControllerSpec("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-swt") {
  val selectorForSummaryText = "#main-content > div > div > details > summary"
  val selectorForSummaryPara1 = "#main-content > div > div > details > div > p:nth-child(1)"
  val selectorForSummaryBullet1 = "#main-content > div > div > details > div > ul > li:nth-child(1)"
  val selectorForSummaryBullet2 = "#main-content > div > div > details > div > ul > li:nth-child(2)"
  val selectorForSummaryBullet3 = "#main-content > div > div > details > div > ul > li:nth-child(3)"
  val selectorForSummaryBullet4 = "#main-content > div > div > details > div > ul > li:nth-child(4)"
  val selectorForSummaryBullet5 = "#main-content > div > div > details > div > ul > li:nth-child(5)"
  val selectorForSummaryBullet6 = "#main-content > div > div > details > div > ul > li:nth-child(6)"
  val selectorForSummaryBullet7 = "#main-content > div > div > details > div > ul > li:nth-child(7)"
  val selectorForSummaryBullet8 = "#main-content > div > div > details > div > ul > li:nth-child(8)"
  val selectorForSummaryBullet9 = "#main-content > div > div > details > div > ul > li:nth-child(9)"
  val selectorForSummaryBullet10 = "#main-content > div > div > details > div > ul > li:nth-child(10)"
  val selectorForSummaryBullet11 = "#main-content > div > div > details > div > ul > li:nth-child(11)"
  val selectorForSummaryPara2 = "#main-content > div > div > details > div > #para2"
  val selectorForSummaryPara3 = "#main-content > div > div > details > div > #para3"

  val expectedContentEN = ExpectedYesNoAmountPageContents(
    title = "Did you have Special Withholding Tax (SWT) deducted from your pension?",
    header = "Did you have Special Withholding Tax (SWT) deducted from your pension?",
    caption = s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
    radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
    radioButtonForNo = uncheckedExpectedRadioButton("No"),
    buttonForContinue = ExpectedButton("Continue", ""),
    amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "", Some("For example, £193.54")),
    links = Set(
      ExpectedLink(
        "special-withholding-tax-link",
        "Special Withholding Tax at Gov.uk (opens in new tab)",
        "https://www.gov.uk/government/publications/remittance-basis-hs264-self-assessment-helpsheet/remittance-basis-2022-hs264")
    ),
    text = Set(
      ExpectedText(selectorForSummaryText, "Understanding Special Withholding Tax (SWT)"),
      ExpectedText(selectorForSummaryPara1, "Special Withholding Tax (SWT) is an amount of tax taken off certain foreign payments to UK residents. " +
        "SWT will be paid along with any foreign tax deducted by the country where the payment came from. The countries that may deduct SWT are:"),
      ExpectedText(selectorForSummaryBullet1, "Andorra"),
      ExpectedText(selectorForSummaryBullet2, "Austria"),
      ExpectedText(selectorForSummaryBullet3, "Curaçao"),
      ExpectedText(selectorForSummaryBullet4, "Gibraltar"),
      ExpectedText(selectorForSummaryBullet5, "Jersey"),
      ExpectedText(selectorForSummaryBullet6, "Liechtenstein"),
      ExpectedText(selectorForSummaryBullet7, "Luxembourg"),
      ExpectedText(selectorForSummaryBullet8, "Monaco"),
      ExpectedText(selectorForSummaryBullet9, "San Marino"),
      ExpectedText(selectorForSummaryBullet10, "Saint Maarten"),
      ExpectedText(selectorForSummaryBullet11, "Switzerland"),
      ExpectedText(selectorForSummaryPara2,
        "Where SWT has been deducted you’re treated as having paid the same amount of income tax in the UK in the same year. " +
          "This can be set against your UK tax liability of that year, or repaid to you if the amount is more than the tax you must pay."),
      ExpectedText(selectorForSummaryPara3, "Read more about Special Withholding Tax at Gov.uk (opens in new tab).")
    ),
    formUrl = formUrl()
  )

  val expectedContentCY = ExpectedYesNoAmountPageContents(
    title = "Did you have Special Withholding Tax (SWT) deducted from your pension?",
    header = "Did you have Special Withholding Tax (SWT) deducted from your pension?",
    caption = s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
    radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
    radioButtonForNo = uncheckedExpectedRadioButton("No"),
    buttonForContinue = ExpectedButton("Continue", ""),
    amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "", Some("For example, £193.54")),
    links = Set(
      ExpectedLink(
        "special-withholding-tax-link",
        "Special Withholding Tax at Gov.uk (opens in new tab)",
        "https://www.gov.uk/government/publications/remittance-basis-hs264-self-assessment-helpsheet/remittance-basis-2022-hs264")
    ),
    text = Set(
      ExpectedText(selectorForSummaryText, "Understanding Special Withholding Tax (SWT)"),
      ExpectedText(selectorForSummaryPara1, "Special Withholding Tax (SWT) is an amount of tax taken off certain foreign payments to UK residents. " +
        "SWT will be paid along with any foreign tax deducted by the country where the payment came from. The countries that may deduct SWT are:"),
      ExpectedText(selectorForSummaryBullet1, "Andorra"),
      ExpectedText(selectorForSummaryBullet2, "Austria"),
      ExpectedText(selectorForSummaryBullet3, "Curaçao"),
      ExpectedText(selectorForSummaryBullet4, "Gibraltar"),
      ExpectedText(selectorForSummaryBullet5, "Jersey"),
      ExpectedText(selectorForSummaryBullet6, "Liechtenstein"),
      ExpectedText(selectorForSummaryBullet7, "Luxembourg"),
      ExpectedText(selectorForSummaryBullet8, "Monaco"),
      ExpectedText(selectorForSummaryBullet9, "San Marino"),
      ExpectedText(selectorForSummaryBullet10, "Saint Maarten"),
      ExpectedText(selectorForSummaryBullet11, "Switzerland"),
      ExpectedText(selectorForSummaryPara2,
        "Where SWT has been deducted you’re treated as having paid the same amount of income tax in the UK in the same year. " +
          "This can be set against your UK tax liability of that year, or repaid to you if the amount is more than the tax you must pay."),
      ExpectedText(selectorForSummaryPara3, "Read more about Special Withholding Tax at Gov.uk (opens in new tab).")
    ),
    formUrl = formUrl()
  )

  implicit val isWelsh: Boolean = false //set to true in welsh sections

  "show" should { //scalastyle:off magic.number
    "redirect to the summary page the user has no stored session data" in {
      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
      implicit val response: WSResponse = getPageWithIndex()

      assertRedirectionAsExpected(PageRelativeURLs.overseasSummaryPage)
    }
    "redirect to the the first page of the IFOP scheme loop when the user had not previously specified the amount" in {
      val sessionData: PensionsUserData = pensionsUserData(aPensionsCYAEmptyModel)
      implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
      implicit val response: WSResponse = getPageWithIndex()

      assertRedirectionAsExpected(PageRelativeURLs.incomeFromOverseasPensionsCountry)
    }
    "redirect to the the IFOP scheme summary page" when {
      "an incorrect index is provided" in {
        val sessionData = pensionsUserData(aPensionsCYAModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse = getPageWithIndex(7)

        assertRedirectionAsExpected(PageRelativeURLs.incomeFromOverseasPensionsCountrySummary)
      }
      "the user provides a negative index number" in {
        val sessionData = pensionsUserData(aPensionsCYAModel)
        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse = getPageWithIndex(-1)

        assertRedirectionAsExpected(PageRelativeURLs.incomeFromOverseasPensionsCountrySummary)
      }
    }
    "appear as expected" when {

      val expectedContentIndividualEN = expectedContentEN
      val expectedContentIndividualCY = expectedContentCY

      val expectedContentAgentEN = expectedContentEN.copy(
        title = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
        header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
      )
      val expectedContentAgentCY = expectedContentCY.copy(
        title = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
        header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
      )

      "the user has no session data relevant to this page and" when {

        val updatedPensionScheme = aPensionsCYAModel.incomeFromOverseasPensions.overseasIncomePensionSchemes.updated(
          0, PensionScheme(
            alphaTwoCode = Some("FR"),
            alphaThreeCode = Some("FRA"),
            pensionPaymentAmount = Some(1999.99),
            pensionPaymentTaxPaid = Some(1999.99),
            specialWithholdingTaxQuestion = None,
            specialWithholdingTaxAmount = None,
            foreignTaxCreditReliefQuestion = Some(false),
            taxableAmount = Some(1999.99)
          ))

        val sessionData: PensionsUserData =
          pensionsUserData(aPensionsCYAModel.copy(
            incomeFromOverseasPensions = aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes =
              updatedPensionScheme)
          ))

        scenarioNameForIndividualAndEnglish in {

          implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()

          assertSWTPageAsExpected(OK, expectedContentIndividualEN)
        }

        scenarioNameForIndividualAndWelsh ignore {

          implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()
          assertSWTPageAsExpected(OK, expectedContentIndividualCY, isWelsh = true)
        }

        scenarioNameForAgentAndEnglish in {

          implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()
          assertSWTPageAsExpected(OK, expectedContentAgentEN)
        }
        scenarioNameForAgentAndWelsh ignore {

          implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()
          assertSWTPageAsExpected(OK, expectedContentAgentCY, isWelsh = true)
        }
      }
      "the user had previously answered 'Yes' with a valid amount, and" when {

        val sessionData: PensionsUserData =
          pensionsUserData(aPensionsCYAModel.copy(
            incomeFromOverseasPensions = aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes =
              aPensionsCYAModel.incomeFromOverseasPensions.overseasIncomePensionSchemes.updated(
                0, PensionScheme(
                  alphaTwoCode = Some("FR"),
                  alphaThreeCode = Some("FRA"),
                  pensionPaymentAmount = Some(1999.99),
                  pensionPaymentTaxPaid = Some(1999.99),
                  specialWithholdingTaxQuestion = Some(true),
                  specialWithholdingTaxAmount = Some(1999.99),
                  foreignTaxCreditReliefQuestion = Some(false),
                  taxableAmount = Some(1999.99)
                )))
          ))

        val expContentIndividualEN = expectedContentEN.copy(
          radioButtonForYes = checkedExpectedRadioButton("Yes"),
          amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "1,999.99", Some("For example, £193.54"))
        )
        val expContentAgentEN = expectedContentAgentEN.copy(
          radioButtonForYes = checkedExpectedRadioButton("Yes"),
          amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "1,999.99", Some("For example, £193.54"))
        )

        val expContentIndividualCY = expectedContentCY.copy(
          radioButtonForYes = checkedExpectedRadioButton("Yes"),
          amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "1,999.99", Some("For example, £193.54"))
        )
        val expContentAgentCY = expectedContentAgentCY.copy(
          radioButtonForYes = checkedExpectedRadioButton("Yes"),
          amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "1,999.99", Some("For example, £193.54"))
        )

        scenarioNameForIndividualAndEnglish in {

          implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()
          assertPageAsExpected(OK, expContentIndividualEN)
        }

        scenarioNameForIndividualAndWelsh ignore {

          implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()
          assertSWTPageAsExpected(OK, expContentIndividualCY, isWelsh = true)
        }

        scenarioNameForAgentAndEnglish in {

          implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()
          assertSWTPageAsExpected(OK, expContentAgentEN)
        }

        scenarioNameForAgentAndWelsh ignore {

          implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()
          assertSWTPageAsExpected(OK, expContentAgentCY, isWelsh = true)
        }

      }
      "the user has multiple pensionSchemes, and" when {

        val newSequence = PensionScheme(
          alphaTwoCode = Some("GB"),
          alphaThreeCode = Some("GBR"),
          pensionPaymentAmount = Some(1999.99),
          pensionPaymentTaxPaid = Some(1999.99),
          specialWithholdingTaxQuestion = Some(true),
          specialWithholdingTaxAmount = Some(1999.99),
          foreignTaxCreditReliefQuestion = Some(false),
          taxableAmount = Some(1999.99)
        ) +: aPensionsCYAModel.incomeFromOverseasPensions.overseasIncomePensionSchemes

        val sessionData: PensionsUserData =
          pensionsUserData(aPensionsCYAModel.copy(
            incomeFromOverseasPensions = aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes = newSequence)
          ))

        val expContentIndividualEN = expectedContentEN.copy(
          radioButtonForYes = checkedExpectedRadioButton("Yes"),
          amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "1,999.99", Some("For example, £193.54")),
          formUrl = formUrl(1)
        )
        val expContentAgentEN = expectedContentAgentEN.copy(
          radioButtonForYes = checkedExpectedRadioButton("Yes"),
          amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "1,999.99", Some("For example, £193.54")),
          formUrl = formUrl(1)
        )

        val expContentIndividualCY = expectedContentCY.copy(
          radioButtonForYes = checkedExpectedRadioButton("Yes"),
          amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "1,999.99", Some("For example, £193.54")),
          formUrl = formUrl(1)
        )

        val expContentAgentCY = expectedContentAgentCY.copy(
          radioButtonForYes = checkedExpectedRadioButton("Yes"),
          amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "1,999.99", Some("For example, £193.54")),
          formUrl = formUrl(1)
        )

        scenarioNameForIndividualAndEnglish in {

          implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex(1)
          assertSWTPageAsExpected(OK, expContentIndividualEN)
        }

        scenarioNameForIndividualAndWelsh ignore {

          implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex(1)
          assertSWTPageAsExpected(OK, expContentIndividualCY, isWelsh = true)
        }

        scenarioNameForAgentAndEnglish in {

          implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex(1)
          assertSWTPageAsExpected(OK, expContentAgentEN)
        }
        scenarioNameForAgentAndWelsh ignore {

          implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex(1)
          assertSWTPageAsExpected(OK, expContentAgentCY, isWelsh = true)
        }

      }
      "the user had previously answered 'No' without an amount, and" when {


        val updatedPensionScheme = aPensionsCYAModel.incomeFromOverseasPensions.overseasIncomePensionSchemes.updated(
          0, PensionScheme(
            alphaTwoCode = Some("FR"),
            alphaThreeCode = Some("FRA"),
            pensionPaymentAmount = Some(1999.99),
            pensionPaymentTaxPaid = Some(1999.99),
            specialWithholdingTaxQuestion = Some(false),
            specialWithholdingTaxAmount = None,
            foreignTaxCreditReliefQuestion = Some(false),
            taxableAmount = Some(1999.99)
          ))

        val sessionData: PensionsUserData =
          pensionsUserData(aPensionsCYAModel.copy(
            incomeFromOverseasPensions = aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes =
              updatedPensionScheme)
          ))

        val expContentIndividualEN = expectedContentEN.copy(
          radioButtonForNo = checkedExpectedRadioButton("No"),
          amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "", Some("For example, £193.54")),
        )
        val expContentAgentEN = expectedContentAgentEN.copy(
          radioButtonForNo = checkedExpectedRadioButton("No"),
          amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "", Some("For example, £193.54")),
        )

        val expContentIndividualCY = expectedContentCY.copy(
          radioButtonForNo = checkedExpectedRadioButton("No"),
          amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "", Some("For example, £193.54")),
        )
        val expContentAgentCY = expectedContentAgentCY.copy(
          radioButtonForNo = checkedExpectedRadioButton("No"),
          amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "", Some("For example, £193.54")),
        )
        scenarioNameForIndividualAndEnglish in {

          implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()
          assertSWTPageAsExpected(OK, expContentIndividualEN)
        }

        scenarioNameForIndividualAndWelsh ignore {

          implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()
          assertSWTPageAsExpected(OK, expContentIndividualCY, isWelsh = true)
        }

        scenarioNameForAgentAndEnglish in {

          implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()
          assertSWTPageAsExpected(OK, expContentAgentEN)
        }

        scenarioNameForAgentAndWelsh ignore {

          implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
          implicit val response: WSResponse = getPageWithIndex()
          assertSWTPageAsExpected(OK, expContentAgentCY)
        }

      }
    }
  }

  "submit" should {
    "redirect to the expected page" when {
      "the index is invalid" in {
        val sessionData: PensionsUserData = pensionsUserData(aPensionsCYAModel)

        val expectedViewModel = sessionData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.head

        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
        implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(false), None))
        implicit val index: Int = 7

        assertRedirectionAsExpected(PageRelativeURLs.incomeFromOverseasPensionsCountrySummary)

      }
      "the user has no stored session data at all" in {

        implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
        implicit val response: WSResponse = submitForm(SubmittedFormDataForYesNoAmountPage(Some(false), None))
        implicit val index: Int = 0

        assertRedirectionAsExpected(PageRelativeURLs.overseasSummaryPage)
        getViewModel mustBe None

      }
    }
    "succeed" when {
      "the user has relevant session data and" when {

        val sessionData = pensionsUserData(aPensionsCYAModel)

        "the user has selected 'No' with a blank amount" in {

          val updatedModel: PensionsUserData =
            pensionsUserData(sessionData.pensions.copy(
              incomeFromOverseasPensions = sessionData.pensions.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes =
                sessionData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.updated(
                  0, PensionScheme(
                    alphaTwoCode = Some("FR"),
                    alphaThreeCode = Some("FRA"),
                    pensionPaymentAmount = Some(1999.99),
                    pensionPaymentTaxPaid = Some(1999.99),
                    specialWithholdingTaxQuestion = Some(false),
                    specialWithholdingTaxAmount = None,
                    foreignTaxCreditReliefQuestion = Some(true),
                    taxableAmount = Some(1999.99)
                  )))
            ))

          val expectedViewModel = updatedModel.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.head


          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(false), Some("")))
          implicit val index: Int = 0

          assertRedirectionAsExpected(relativeUrl("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-ftcr?index=0"))
          getViewModel mustBe Some(expectedViewModel)

        }
        "the user has selected 'Yes' as well as a valid amount (unformatted), and" in {

          val updatedPensionScheme = aPensionsCYAModel.incomeFromOverseasPensions.overseasIncomePensionSchemes.updated(
            0, PensionScheme(
              alphaTwoCode = Some("FR"),
              alphaThreeCode = Some("FRA"),
              pensionPaymentAmount = Some(1999.99),
              pensionPaymentTaxPaid = Some(1999.99),
              specialWithholdingTaxQuestion = Some(true),
              specialWithholdingTaxAmount = Some(BigDecimal(42.64)),
              foreignTaxCreditReliefQuestion = Some(true),
              taxableAmount = Some(1999.99)
            ))

          val updatedModel: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(
              incomeFromOverseasPensions = aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes =
                updatedPensionScheme)
            ))

          val expectedViewModel = updatedModel.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.head


          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("42.64")))
          implicit val index: Int = 0

          assertRedirectionAsExpected(relativeUrl("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-ftcr?index=0"))
          getViewModel mustBe Some(expectedViewModel)

        }
        "the user has selected 'Yes' as well as a valid amount (formatted), and" in {

          val updatedPensionScheme = aPensionsCYAModel.incomeFromOverseasPensions.overseasIncomePensionSchemes.updated(
            0, PensionScheme(
              alphaTwoCode = Some("FR"),
              alphaThreeCode = Some("FRA"),
              pensionPaymentAmount = Some(1999.99),
              pensionPaymentTaxPaid = Some(1999.99),
              specialWithholdingTaxQuestion = Some(true),
              specialWithholdingTaxAmount = Some(BigDecimal(1042.64)),
              foreignTaxCreditReliefQuestion = Some(true),
              taxableAmount = Some(1999.99)
            ))

          val updatedModel: PensionsUserData =
            pensionsUserData(aPensionsCYAModel.copy(
              incomeFromOverseasPensions = aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes =
                updatedPensionScheme)
            ))

          val expectedViewModel = updatedModel.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.head

          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("£1,042.64")))
          implicit val index: Int = 0

          assertRedirectionAsExpected(relativeUrl("/overseas-pensions/income-from-overseas-pensions/pension-overseas-income-ftcr?index=0"))
          getViewModel mustBe Some(expectedViewModel)

        }

      }
    }
    "fail" when {

      val expectedContentsIndividualEN = ExpectedYesNoAmountPageContents(
        title = "Error: Did you have Special Withholding Tax (SWT) deducted from your pension?",
        header = "Did you have Special Withholding Tax (SWT) deducted from your pension?",
        caption = s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
        radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
        radioButtonForNo = uncheckedExpectedRadioButton("No"),
        buttonForContinue = ExpectedButton("Continue", ""),
        amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "", Some("For example, £193.54")),
        errorSummarySectionOpt = Some(
          ErrorSummarySection(
            title = "There is a problem",
            body = "Select yes or no if you had Special Withholding Tax deducted from your pension.",
            link = "#value")
        ),
        errorAboveElementCheckSectionOpt = Some(
          ErrorAboveElementCheckSection(
            title = "Error: Select yes or no if you had Special Withholding Tax deducted from your pension.",
            idOpt = Some("value")
          )
        ),
        formUrl = formUrl()
      )
      val expectedContentsAgentEN = expectedContentsIndividualEN.copy(
        title = "Error: Did your client have Special Withholding Tax (SWT) deducted from their pension?",
        header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
        errorSummarySectionOpt = Some(ErrorSummarySection(
          title = "There is a problem", body = "Select yes or no if your client had Special Withholding Tax deducted from their pension.", link = "#value")
        ),
        errorAboveElementCheckSectionOpt = Some(ErrorAboveElementCheckSection(
          title = "Error: Select yes or no if your client had Special Withholding Tax deducted from their pension.", idOpt = Some("value"))
        )
      )

      val expectedContentsIndividualCY = ExpectedYesNoAmountPageContents(
        title = "Error: Did you have Special Withholding Tax (SWT) deducted from your pension?",
        header = "Did you have Special Withholding Tax (SWT) deducted from your pension?",
        caption = s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear",
        radioButtonForYes = uncheckedExpectedRadioButton("Yes"),
        radioButtonForNo = uncheckedExpectedRadioButton("No"),
        buttonForContinue = ExpectedButton("Continue", ""),
        amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "", Some("For example, £193.54")),
        errorSummarySectionOpt = Some(
          ErrorSummarySection(
            title = "There is a problem",
            body = "Select yes or no if you had Special Withholding Tax deducted from your pension.",
            link = "#value")
        ),
        errorAboveElementCheckSectionOpt = Some(
          ErrorAboveElementCheckSection(
            title = "Error: Select yes or no if you had Special Withholding Tax deducted from your pension.",
            idOpt = Some("value")
          )
        ),
        formUrl = formUrl()
      )
      val expectedContentsAgentCY = expectedContentsIndividualEN.copy(
        title = "Error: Did your client have Special Withholding Tax (SWT) deducted from their pension?",
        header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
        errorSummarySectionOpt = Some(ErrorSummarySection(
          title = "There is a problem", body = "Select yes or no if your client had Special Withholding Tax deducted from their pension.", link = "#value")
        ),
        errorAboveElementCheckSectionOpt = Some(ErrorAboveElementCheckSection(
          title = "Error: Select yes or no if your client had Special Withholding Tax deducted from their pension.", idOpt = Some("value"))
        )
      )

      def welshTitle(epc: ExpectedYesNoAmountPageContents): Option[ErrorSummarySection] =
        epc.errorSummarySectionOpt.map(ess => ess.copy(title = "Mae problem wedi codi"))


      "the user has no session data relevant to this page and" when {

        val updatedPensionScheme = aPensionsCYAModel.incomeFromOverseasPensions.overseasIncomePensionSchemes.updated(
          0, PensionScheme(
            alphaTwoCode = Some("FR"),
            alphaThreeCode = Some("FRA"),
            pensionPaymentAmount = Some(1999.99),
            pensionPaymentTaxPaid = Some(1999.99),
            specialWithholdingTaxQuestion = None,
            specialWithholdingTaxAmount = None,
            foreignTaxCreditReliefQuestion = Some(false),
            taxableAmount = Some(1999.99)
          ))

        val sessionData: PensionsUserData =
          pensionsUserData(aPensionsCYAModel.copy(
            incomeFromOverseasPensions = aPensionsCYAModel.incomeFromOverseasPensions.copy(overseasIncomePensionSchemes =
              updatedPensionScheme)
          ))

        val expectedViewModel = sessionData.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes.head
        implicit val index: Int = 0


        "the user has selected neither 'Yes' nor 'No' and" when {
          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(None, None))

            assertSWTPageAsExpected(BAD_REQUEST, expectedContentsIndividualEN)
            getViewModel mustBe Some(expectedViewModel)

          }
          scenarioNameForIndividualAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(None, None))

            assertSWTPageAsExpected(BAD_REQUEST, expectedContentsIndividualCY.copy(errorSummarySectionOpt = welshTitle(expectedContentsIndividualCY)))
            getViewModel mustBe Some(expectedViewModel)

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(None, None))

            assertSWTPageAsExpected(BAD_REQUEST, expectedContentsAgentEN)
            getViewModel mustBe Some(expectedViewModel)

          }
          scenarioNameForAgentAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(None, None))

            assertSWTPageAsExpected(BAD_REQUEST, expectedContentsAgentCY.copy(errorSummarySectionOpt = welshTitle(expectedContentsAgentCY)))
            getViewModel mustBe Some(expectedViewModel)
          }
        }

        "the user has selected 'Yes' but has provided an empty amount, and" when {

          val expContentsIndividualEN = expectedContentsIndividualEN.copy(
            radioButtonForYes = checkedExpectedRadioButton("Yes"),
            errorSummarySectionOpt = Some(ErrorSummarySection(
              title = "There is a problem", body = "Enter an amount in pounds for the amount of Special Withholding Tax deducted", link = "#amount-2")
            ),
            errorAboveElementCheckSectionOpt = Some(ErrorAboveElementCheckSection(
              title = "Error: Enter an amount in pounds for the amount of Special Withholding Tax deducted", idOpt = Some("amount-2")))
          )
          val expContentsAgentEN = expContentsIndividualEN.copy(
            title = "Error: Did your client have Special Withholding Tax (SWT) deducted from their pension?",
            header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
          )

          val expContentsIndividualCY = expectedContentsIndividualEN.copy(
            radioButtonForYes = checkedExpectedRadioButton("Yes"),
            errorSummarySectionOpt = Some(ErrorSummarySection(
              title = "There is a problem", body = "Enter an amount in pounds for the amount of Special Withholding Tax deducted", link = "#amount-2")
            ),
            errorAboveElementCheckSectionOpt = Some(ErrorAboveElementCheckSection(
              title = "Error: Enter an amount in pounds for the amount of Special Withholding Tax deducted", idOpt = Some("amount-2")))
          )
          val expContentsAgentCY = expContentsIndividualEN.copy(
            title = "Error: Did your client have Special Withholding Tax (SWT) deducted from their pension?",
            header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
          )

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("")))
            assertSWTPageAsExpected(BAD_REQUEST, expContentsIndividualEN)
            getViewModel mustBe Some(expectedViewModel)
          }

          scenarioNameForIndividualAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), None))

            assertSWTPageAsExpected(BAD_REQUEST, expContentsIndividualCY.copy(errorSummarySectionOpt = welshTitle(expContentsIndividualCY)))
            getViewModel mustBe Some(expectedViewModel)
          }

          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), None))

            assertSWTPageAsExpected(BAD_REQUEST, expContentsAgentEN)
            getViewModel mustBe Some(expectedViewModel)
          }

          scenarioNameForAgentAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), None))

            assertSWTPageAsExpected(BAD_REQUEST, expContentsAgentCY.copy(errorSummarySectionOpt = welshTitle(expContentsAgentCY)), isWelsh = true)
            getViewModel mustBe Some(expectedViewModel)
          }

        }
        "the user has selected 'Yes' but has provided an amount of an invalid format, and" when {

          val expContentsIndividualEN = expectedContentsIndividualEN.copy(
            radioButtonForYes = checkedExpectedRadioButton("Yes"),
            amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "x2.64", Some("For example, £193.54")),
            errorSummarySectionOpt = Some(ErrorSummarySection(
              title = "There is a problem", body = "Enter the amount of Special Withholding Tax deducted in the correct format", link = "#amount-2")
            ),
            errorAboveElementCheckSectionOpt = Some(ErrorAboveElementCheckSection(
              title = "Error: Enter the amount of Special Withholding Tax deducted in the correct format", idOpt = Some("amount-2")))
          )
          val expContentsAgentEN = expContentsIndividualEN.copy(
            title = "Error: Did your client have Special Withholding Tax (SWT) deducted from their pension?",
            header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
          )

          val expContentsIndividualCY = expectedContentsIndividualCY.copy(
            radioButtonForYes = checkedExpectedRadioButton("Yes"),
            amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "x2.64", Some("For example, £193.54")),
            errorSummarySectionOpt = Some(ErrorSummarySection(
              title = "There is a problem", body = "Enter the amount of Special Withholding Tax deducted in the correct format", link = "#amount-2")
            ),
            errorAboveElementCheckSectionOpt = Some(ErrorAboveElementCheckSection(
              title = "Error: Enter the amount of Special Withholding Tax deducted in the correct format", idOpt = Some("amount-2")))
          )
          val expContentsAgentCY = expContentsIndividualCY.copy(
            title = "Error: Did your client have Special Withholding Tax (SWT) deducted from their pension?",
            header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
          )

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

            assertSWTPageAsExpected(BAD_REQUEST, expContentsIndividualEN)
            getViewModel mustBe Some(expectedViewModel)
          }

          scenarioNameForIndividualAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

            assertSWTPageAsExpected(BAD_REQUEST, expContentsIndividualCY.copy(errorSummarySectionOpt = welshTitle(expContentsIndividualCY)))
            getViewModel mustBe Some(expectedViewModel)
          }

          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

            assertSWTPageAsExpected(BAD_REQUEST, expContentsAgentEN)
            getViewModel mustBe Some(expectedViewModel)

          }
          scenarioNameForAgentAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("x2.64")))

            assertSWTPageAsExpected(BAD_REQUEST, expContentsAgentCY.copy(errorSummarySectionOpt = welshTitle(expContentsAgentEN)), isWelsh = true)
            getViewModel mustBe Some(expectedViewModel)

          }
        }
        "the user has selected 'Yes' but has provided an amount of a negative format, and" when {

          val expContentsIndividualEN = expectedContentsIndividualEN.copy(
            radioButtonForYes = checkedExpectedRadioButton("Yes"),
            amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "-42.64", Some("For example, £193.54")),
            errorSummarySectionOpt = Some(ErrorSummarySection(
              title = "There is a problem", body = "Enter the amount of Special Withholding Tax deducted in the correct format", link = "#amount-2")
            ),
            errorAboveElementCheckSectionOpt = Some(ErrorAboveElementCheckSection(
              title = "Error: Enter the amount of Special Withholding Tax deducted in the correct format", idOpt = Some("amount-2")))
          )
          val expContentsAgentEN = expContentsIndividualEN.copy(
            title = "Error: Did your client have Special Withholding Tax (SWT) deducted from their pension?",
            header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
          )

          val expContentsIndividualCY = expectedContentsIndividualCY.copy(
            radioButtonForYes = checkedExpectedRadioButton("Yes"),
            amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "-42.64", Some("For example, £193.54")),
            errorSummarySectionOpt = Some(ErrorSummarySection(
              title = "There is a problem", body = "Enter the amount of Special Withholding Tax deducted in the correct format", link = "#amount-2")
            ),
            errorAboveElementCheckSectionOpt = Some(ErrorAboveElementCheckSection(
              title = "Error: Enter the amount of Special Withholding Tax deducted in the correct format", idOpt = Some("amount-2")))
          )
          val expContentsAgentCY = expContentsIndividualCY.copy(
            title = "Error: Did your client have Special Withholding Tax (SWT) deducted from their pension?",
            header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
          )

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("-42.64")))

            assertSWTPageAsExpected(BAD_REQUEST, expContentsIndividualEN)
            getViewModel mustBe Some(expectedViewModel)
          }

          scenarioNameForIndividualAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("-42.64")))

            assertSWTPageAsExpected(BAD_REQUEST, expContentsIndividualCY.copy(errorSummarySectionOpt = welshTitle(expContentsIndividualCY)))
            getViewModel mustBe Some(expectedViewModel)

          }
          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("-42.64")))

            assertSWTPageAsExpected(BAD_REQUEST, expContentsAgentEN)
            getViewModel mustBe Some(expectedViewModel)
          }

          scenarioNameForAgentAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("-42.64")))

            assertSWTPageAsExpected(BAD_REQUEST, expContentsAgentCY.copy(errorSummarySectionOpt = welshTitle(expContentsAgentCY)), isWelsh = true)
            getViewModel mustBe Some(expectedViewModel)
          }

        }
        "the user has selected 'Yes' but has provided an excessive amount, and" when {

          val expContentsIndividualEN = expectedContentsIndividualEN.copy(
            radioButtonForYes = checkedExpectedRadioButton("Yes"),
            amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "100000000002", Some("For example, £193.54")),
            errorSummarySectionOpt = Some(ErrorSummarySection(
              title = "There is a problem", body = "Amount of Special Withholding Tax deducted must be less than £100,000,000,000", link = "#amount-2")
            ),
            errorAboveElementCheckSectionOpt = Some(ErrorAboveElementCheckSection(
              title = "Error: Amount of Special Withholding Tax deducted must be less than £100,000,000,000", idOpt = Some("amount-2")))
          )
          val expContentsAgentEN = expContentsIndividualEN.copy(
            title = "Error: Did your client have Special Withholding Tax (SWT) deducted from their pension?",
            header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
          )

          val expContentsIndividualCY = expectedContentsIndividualCY.copy(
            radioButtonForYes = checkedExpectedRadioButton("Yes"),
            amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "100000000002", Some("For example, £193.54")),
            errorSummarySectionOpt = Some(ErrorSummarySection(
              title = "There is a problem", body = "Amount of Special Withholding Tax deducted must be less than £100,000,000,000", link = "#amount-2")
            ),
            errorAboveElementCheckSectionOpt = Some(ErrorAboveElementCheckSection(
              title = "Error: Amount of Special Withholding Tax deducted must be less than £100,000,000,000", idOpt = Some("amount-2")))
          )
          val expContentsAgentCY = expContentsIndividualCY.copy(
            title = "Error: Did your client have Special Withholding Tax (SWT) deducted from their pension?",
            header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
          )

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

            assertSWTPageAsExpected(BAD_REQUEST, expContentsIndividualEN)
            getViewModel mustBe Some(expectedViewModel)
          }

          scenarioNameForIndividualAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

            assertSWTPageAsExpected(BAD_REQUEST, expContentsIndividualCY.copy(errorSummarySectionOpt = welshTitle(expContentsIndividualCY)))
            getViewModel mustBe Some(expectedViewModel)
          }

          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

            assertSWTPageAsExpected(BAD_REQUEST, expContentsAgentEN)
            getViewModel mustBe Some(expectedViewModel)
          }

          scenarioNameForAgentAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("100000000002")))

            assertSWTPageAsExpected(BAD_REQUEST,
              expContentsAgentCY.copy(errorSummarySectionOpt = welshTitle(expContentsAgentCY)), isWelsh = true)
            getViewModel mustBe Some(expectedViewModel)
          }
        }

        "the user has selected 'Yes' but has provided a zero for amount, and" when {

          val expContentsIndividualEN = expectedContentsIndividualEN.copy(
            radioButtonForYes = checkedExpectedRadioButton("Yes"),
            amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "0", Some("For example, £193.54")),
            errorSummarySectionOpt = Some(ErrorSummarySection(
              title = "There is a problem", body = "Enter an amount greater than zero", link = "#amount-2")
            ),
            errorAboveElementCheckSectionOpt = Some(ErrorAboveElementCheckSection(
              title = "Error: Enter an amount greater than zero", idOpt = Some("amount-2")))
          )
          val expContentsAgentEN = expContentsIndividualEN.copy(
            title = "Error: Did your client have Special Withholding Tax (SWT) deducted from their pension?",
            header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
          )

          val expContentsIndividualCY = expectedContentsIndividualCY.copy(
            radioButtonForYes = checkedExpectedRadioButton("Yes"),
            amountSection = ExpectedAmountSection("Amount of SWT, in pounds", "0", Some("For example, £193.54")),
            errorSummarySectionOpt = Some(ErrorSummarySection(
              title = "There is a problem", body = "Enter an amount greater than zero", link = "#amount-2")
            ),
            errorAboveElementCheckSectionOpt = Some(ErrorAboveElementCheckSection(
              title = "Error: Enter an amount greater than zero", idOpt = Some("amount-2")))
          )
          val expContentsAgentCY = expContentsIndividualCY.copy(
            title = "Error: Did your client have Special Withholding Tax (SWT) deducted from their pension?",
            header = "Did your client have Special Withholding Tax (SWT) deducted from their pension?",
          )

          scenarioNameForIndividualAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Individual, English, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("0")))

            assertSWTPageAsExpected(BAD_REQUEST, expContentsIndividualEN)
            getViewModel mustBe Some(expectedViewModel)
          }

          scenarioNameForIndividualAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Individual, Welsh, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("0")))

            assertSWTPageAsExpected(BAD_REQUEST, expContentsIndividualCY
              .copy(errorSummarySectionOpt = welshTitle(expContentsIndividualCY)), isWelsh = true)
            getViewModel mustBe Some(expectedViewModel)
          }

          scenarioNameForAgentAndEnglish in {

            implicit val userConfig: UserConfig = UserConfig(Agent, English, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("0")))

            assertSWTPageAsExpected(BAD_REQUEST, expContentsAgentEN)
            getViewModel mustBe Some(expectedViewModel)
          }

          scenarioNameForAgentAndWelsh ignore {

            implicit val userConfig: UserConfig = UserConfig(Agent, Welsh, Some(sessionData))
            implicit val response: WSResponse = submitFormWithIndex(SubmittedFormDataForYesNoAmountPage(Some(true), Some("0")))

            assertSWTPageAsExpected(BAD_REQUEST,
              expContentsAgentCY.copy(errorSummarySectionOpt = welshTitle(expContentsAgentCY)), isWelsh = true)
            getViewModel mustBe Some(expectedViewModel)
          }
        }
      }
    }
  }

  private def getViewModel(implicit userConfig: UserConfig, index: Int): Option[PensionScheme] =
    loadPensionUserData.map(_.pensions.incomeFromOverseasPensions.overseasIncomePensionSchemes(index))

  private def formUrl(index: Int = 0): Option[String] =
    Some(relativeUrlForThisPage + "?index=" + index)

  private def submitFormWithIndex(submittedFormData: SubmittedFormData, index: Int = 0)(implicit userConfig: UserConfig, wsClient: WSClient): WSResponse = {
    submitForm(submittedFormData, getMap(index))
  }

  private def getMap(index: Int): Map[String, String] = {
    Map("index" -> index.toString)
  }

  private def assertSWTPageAsExpected(expectedStatusCode: Int, expectedPageContents: ExpectedYesNoAmountPageContents, isWelsh: Boolean = false)
                                     (implicit userConfig: UserConfig, response: WSResponse): Unit = {
    assertPageAsExpected(expectedStatusCode, expectedPageContents)(userConfig, response, isWelsh)
  }

}

