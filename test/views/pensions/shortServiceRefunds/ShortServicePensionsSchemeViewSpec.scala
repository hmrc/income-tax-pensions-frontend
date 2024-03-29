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

package views.pensions.shortServiceRefunds

import forms.overseas.PensionSchemeForm.{OverseasOnlyPensionSchemeFormModel, toOverseasPensionSchemeForm}
import models.requests.UserSessionDataRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages
import play.api.mvc.AnyContent
import support.ViewUnitTest
import utils.createLongString
import views.html.pensions.shortServiceRefunds.ShortServicePensionsSchemeView

class ShortServicePensionsSchemeViewSpec extends ViewUnitTest {

  object Selectors {
    val captionSelector: String        = "#main-content > div > div > header > p"
    val continueButtonSelector: String = "#continue"
    val formSelector: String           = "#main-content > div > div > form"
    val qopsPrefixSelector             = ".govuk-input__prefix"
    val refHintSelector                = "#schemeReference-hint"
    val countryIdHintSelector          = "#countryId-hint"
    val providerNameInputSelector      = "#providerName"
    val refInputSelector               = "#schemeReference"
    val providerAddressInputSelector   = "#providerAddress"
    val countryIdOptionSelector        = "#countryId"
    val providerNameErrorHref          = "#providerName"
    val refErrorHref                   = "#schemeReference"
    val providerAddressErrorHref       = "#providerAddress"
    val countryIdErrorHref             = "#countryId"

    def labelSelector(index: Int, divs: String = ""): String = s"form > div:nth-of-type($index) $divs > label"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedHeading: String
    val expectedErrorTitle: String
    val expectedCaption: Int => String
    val expectedIfYouGetParagraph: String
    val buttonText: String
    val providerNameLabel: String
    val providerAddressLabel: String
    val providerNameEmptyErrorText: String
    val refEmptyErrorText: String
    val qopsEmptyErrorText: String
    val providerAddressEmptyErrorText: String
    val providerNameInvalidFormatErrorText: String
    val providerNameOverCharLimitErrorText: String
    val providerAddressOverCharLimitErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle: String                 = "Pension scheme paying tax on the short service refund"
    val expectedHeading: String               = "Pension scheme paying tax on the short service refund"
    val expectedErrorTitle: String            = s"Error: $expectedTitle"
    val expectedCaption: Int => String        = (taxYear: Int) => s"Short service refunds for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedIfYouGetParagraph: String     = "If more than one pension scheme paid a transfer charge, you can add them later."
    val buttonText: String                    = "Continue"
    val providerNameLabel: String             = "Name of pension scheme"
    val providerAddressLabel: String          = "Pension provider address"
    val providerNameEmptyErrorText: String    = "Enter the name of the pension scheme"
    val refEmptyErrorText: String             = "Enter the Pension Scheme Tax Reference"
    val qopsEmptyErrorText: String            = "Enter the Qualifying Overseas Pension Scheme Reference"
    val providerAddressEmptyErrorText: String = "Enter the pension provider’s address"
    val providerNameInvalidFormatErrorText: String = "The pension scheme name must only include numbers 0-9, " +
      "letters a to z, hyphens, spaces, apostrophes, commas, full stops, round brackets and the special characters, &, /, @, £, *."
    val providerNameOverCharLimitErrorText: String    = "Name of pension scheme must be 105 characters or less"
    val providerAddressOverCharLimitErrorText: String = "Pension provider’s address must be 250 characters or less"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle: String                 = "Cynllun pensiwn yn talu’r dreth ar yr ad-daliad am wasanaeth byr"
    val expectedHeading: String               = "Cynllun pensiwn yn talu’r dreth ar yr ad-daliad am wasanaeth byr"
    val expectedErrorTitle: String            = s"Gwall: $expectedTitle"
    val expectedCaption: Int => String        = (taxYear: Int) => s"Ad-daliadau am wasanaeth byr ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val expectedIfYouGetParagraph: String     = "Os gwnaeth mwy nag un cynllun pensiwn dalu tâl trosglwyddiadau, gallwch eu hychwanegu nes ymlaen."
    val buttonText: String                    = "Yn eich blaen"
    val providerNameLabel: String             = "Enw’r cynllun pensiwn"
    val providerAddressLabel: String          = "Cyfeiriad y darparwr pensiwn"
    val providerNameEmptyErrorText: String    = "Nodwch enw’r cynllun pensiwn"
    val refEmptyErrorText: String             = "Nodwch Gyfeirnod Treth y Cynllun Pensiwn"
    val qopsEmptyErrorText: String            = "Enter the Qualifying Overseas Pension Scheme Reference"
    val providerAddressEmptyErrorText: String = "Nodwch gyfeiriad y darparwr pensiwn"
    val providerNameInvalidFormatErrorText: String = "Mae’n rhaid i enw’r cynllun pensiwn gynnwys y rhifau 0-9, " +
      "llythrennau a-z, cysylltnodau, bylchau, collnodau, comas, atalnodau llawn, cromfachau crwn a’r cymeriadau arbennig &, /, @, £,* yn unig."
    val providerNameOverCharLimitErrorText: String    = "Mae’n rhaid i enw’r cynllun pensiwn fod yn 105 o gymeriadau neu lai"
    val providerAddressOverCharLimitErrorText: String = "Mae’n rhaid i gyfeiriad y darparwr pensiwn fod yn 250 o gymeriadau neu lai"
  }

  trait SpecificExpectedResults {
    val countryIdLabel: String
    val countryIdEmptyErrorText: String
    val ukReferenceLabel: String
    val nonUkReferenceLabel: String
    val uKRefHintText: String
    val nonUKRefHintText: String
    val ukRefInvalidFormatErrorText: String
    val nonUKRefInvalidFormatErrorText: String
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val countryIdLabel: String          = "Country"
    val countryIdEmptyErrorText: String = "Enter the country"
    val ukReferenceLabel: String        = "Pension Scheme Tax Reference"
    val nonUkReferenceLabel: String     = "Qualifying Overseas Pension Scheme reference"
    val uKRefHintText: String           = "For example, ‘12345678RA’"
    val nonUKRefHintText: String        = "For example, QOPS123456"
    val ukRefInvalidFormatErrorText: String =
      "Enter a reference with 8 numbers and 2 letters, such as ‘12345678AB’"
    val nonUKRefInvalidFormatErrorText: String = "Enter a six digit number"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val countryIdLabel: String          = "Gwlad"
    val countryIdEmptyErrorText: String = "Nodwch y wlad"
    val ukReferenceLabel: String        = "Cyfeirnod Treth y Cynllun Pensiwn"
    val nonUkReferenceLabel: String     = "Cyfeirnod Cynllun Pensiwn Tramor Cymwys"
    val uKRefHintText: String           = "Er enghraifft, ‘12345678RA’"
    val nonUKRefHintText: String        = "Er enghraifft, QOPS123456"
    val ukRefInvalidFormatErrorText: String =
      "Nodwch gyfeirnod sydd ag 8 rhif a 2 lythyren, megis ‘12345678AB’"
    val nonUKRefInvalidFormatErrorText: String = "Nodwch rif chwe digid"
  }

  val ExpectedAgentEN: ExpectedIndividualEN.type = ExpectedIndividualEN
  val ExpectedAgentCY: ExpectedIndividualCY.type = ExpectedIndividualCY

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
  )

  private lazy val underTest = inject[ShortServicePensionsSchemeView]

  userScenarios.foreach { userScenario =>
    import Selectors._
    import userScenario.commonExpectedResults._

    val agentOrIndividual = agentTest(userScenario.isAgent)

    val providerNameIF    = "providerName"
    val schemeRefIF       = "schemeReference"
    val providerAddressIF = "providerAddress"
    val countryIF         = "countryId"

    implicit val userSessionDataRequest: UserSessionDataRequest[AnyContent] = getUserSession(userScenario.isAgent)
    implicit val messages: Messages                                         = getMessages(userScenario.isWelsh)

    "on show" should {

      s"Overseas Pensions Scheme, language is ${welshTest(userScenario.isWelsh)} and request is from an $agentOrIndividual" should {

        "render Pension scheme page with no prefilling" which {
          val htmlFormat                  = underTest(toOverseasPensionSchemeForm(agentOrIndividual), taxYearEOY, 0)
          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedTitle, userScenario.isWelsh)
          checkCommonElements
          checkInputElements("", "", "", "")
        }

        "render Pension scheme page with prefilling" which {
          val (country, ref) = (Some("FR"), "654321")
          val formWithData =
            toOverseasPensionSchemeForm(agentOrIndividual)
              .fill(
                OverseasOnlyPensionSchemeFormModel(
                  providerName = "Scheme Name",
                  schemeReference = ref,
                  providerAddress = "Scheme Address",
                  countryId = country))
          val htmlFormat                  = underTest(formWithData, taxYearEOY, 0)
          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedTitle, userScenario.isWelsh)
          checkCommonElements
          checkInputElements("Scheme Name", ref, "Scheme Address", country.getOrElse(""))
        }
      }
    }

    "on submit" should {

      s"Overseas Pensions Scheme, language is ${welshTest(userScenario.isWelsh)} and request is from an $agentOrIndividual" should {
        import userScenario.{specificExpectedResults => SER}

        "form is correctly submitted with correct fields values" which {
          val schemeRef                   = "654321"
          val formMap                     = setFormData("Scheme Name", schemeRef, "Scheme Address", "FR")
          val htmlFormat                  = underTest(toOverseasPensionSchemeForm(agentOrIndividual).bind(formMap), taxYearEOY, 0)
          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedTitle, userScenario.isWelsh)
          checkCommonElements
          checkInputElements("Scheme Name", schemeRef, "Scheme Address", "FR")
        }

        "page is returned with errors when form is submitted with all fields empty" which {
          val formMap                     = setFormData("", "", "", "")
          val htmlFormat                  = underTest(toOverseasPensionSchemeForm(agentOrIndividual).bind(formMap), taxYearEOY, 0)
          implicit val document: Document = Jsoup.parse(htmlFormat.body)
          val referenceError              = qopsEmptyErrorText

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          checkCommonElements
          checkInputElements("", "", "", "")

          val multiErrorList = List(
            (providerNameEmptyErrorText, providerNameErrorHref),
            (referenceError, refErrorHref),
            (providerAddressEmptyErrorText, providerAddressErrorHref)) ++
            List((SER.get.countryIdEmptyErrorText, countryIdErrorHref))

          multipleErrorCheck(multiErrorList, userScenario.isWelsh)

          errorAboveElementCheck(providerNameEmptyErrorText, Some(providerNameIF))
          errorAboveElementCheck(referenceError, Some(schemeRefIF))
          errorAboveElementCheck(providerAddressEmptyErrorText, Some(providerAddressIF))
          errorAboveElementCheck(SER.get.countryIdEmptyErrorText, Some(countryIF))
        }

        "page is returned with errors when form is submitted with fields with incorrect format" which {
          val formMap                     = setFormData("d#", "#d", "some-address", "FR")
          val htmlFormat                  = underTest(toOverseasPensionSchemeForm(agentOrIndividual).bind(formMap), taxYearEOY, 0)
          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          checkCommonElements
          checkInputElements("d#", "#d", "some-address", "FR")

          val multiErrorList = List(
            (providerNameInvalidFormatErrorText, providerNameErrorHref),
            (SER.get.nonUKRefInvalidFormatErrorText, refErrorHref)
          )

          multipleErrorCheck(multiErrorList, userScenario.isWelsh)
          errorAboveElementCheck(providerNameInvalidFormatErrorText, Some(providerNameIF))
          errorAboveElementCheck(SER.get.nonUKRefInvalidFormatErrorText, Some(schemeRefIF))
        }

        "page is returned with errors when form is submitted with fields that are too long" which {
          val (pName, pAddress) = (createLongString(106), createLongString(251))
          val schemeRef         = "654321"
          val formMap           = setFormData(pName, schemeRef, pAddress, "FR")

          val htmlFormat                  = underTest(toOverseasPensionSchemeForm(agentOrIndividual).bind(formMap), taxYearEOY, 0)
          implicit val document: Document = Jsoup.parse(htmlFormat.body)

          titleCheck(expectedErrorTitle, userScenario.isWelsh)
          checkCommonElements
          checkInputElements(pName, schemeRef, pAddress, "FR")

          val multiErrorList =
            List((providerNameOverCharLimitErrorText, providerNameErrorHref), (providerAddressOverCharLimitErrorText, providerAddressErrorHref))

          multipleErrorCheck(multiErrorList, userScenario.isWelsh)
          errorAboveElementCheck(providerNameOverCharLimitErrorText, Some(providerNameIF))
          errorAboveElementCheck(providerAddressOverCharLimitErrorText, Some(providerAddressIF))
        }
      }
    }

    def checkCommonElements(implicit document: Document): Unit = {
      import userScenario.{specificExpectedResults => SER}

      val referenceLabel = SER.get.nonUkReferenceLabel
      val refHintText    = SER.get.nonUKRefHintText

      h1Check(expectedTitle)
      captionCheck(expectedCaption(taxYearEOY), captionSelector)
      textOnPageCheck(expectedIfYouGetParagraph, paragraphSelector(1))
      textOnPageCheck(providerNameLabel, labelSelector(1))
      textOnPageCheck(referenceLabel, labelSelector(2))
      textOnPageCheck(providerAddressLabel, labelSelector(3))
      textOnPageCheck(SER.get.countryIdLabel, labelSelector(4, "> div"))
      textOnPageCheck(refHintText, refHintSelector)
      buttonCheck(buttonText, continueButtonSelector)
      welshToggleCheck(userScenario.isWelsh)
    }

    def checkInputElements(pName: String, tRef: String, pAddress: String, cc: String)(implicit document: Document): Unit = {
      inputFieldValueCheck(providerNameIF, providerNameInputSelector, pName)
      inputFieldValueCheck(schemeRefIF, refInputSelector, tRef)
      textareaFieldValueCheck(providerAddressIF, providerAddressInputSelector, pAddress)
      selectFieldValueCheck(countryIF, countryIdOptionSelector, cc)
    }

    def setFormData(pName: String, tRef: String, pAddress: String, country: String): Map[String, String] =
      Map(providerNameIF -> pName, schemeRefIF -> tRef, providerAddressIF -> pAddress) ++
        Map(countryIF -> country)
  }

}
