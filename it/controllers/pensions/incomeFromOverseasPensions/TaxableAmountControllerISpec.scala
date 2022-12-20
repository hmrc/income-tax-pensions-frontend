package controllers.pensions.incomeFromOverseasPensions

import builders.PensionsUserDataBuilder.anPensionsUserDataEmptyCya
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse
import utils.CommonUtils
import utils.PageUrls.IncomeFromOverseasPensionsPages

class TaxableAmountControllerISpec extends CommonUtils with BeforeAndAfterEach {
  object Selectors {
    val captionSelector: String = "#main-content > div > div > header > p"
    val formSelector: String = "#main-content > div > div > form"
    val continueButtonSelector: String = "#continue"

    def labelSelector(index: Int): String = s"form > div:nth-of-type($index) > label"

    def paragraphSelector(index: Int): String = s"#main-content > div > div > p:nth-of-type($index)"
  }

  trait CommonExpectedResults {
    val expectedCaption: Int => String
    val expectedTitle: String
    val expectedHeading: String
    val expectedSubHeading: String
    val expectedParagraph: String
    val expectedButtonText: String
  }
  trait SpecificExpectedResults {
    val expectedError: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle: String = "Your taxable amount"
    val expectedHeading: String = "Your taxable amount"
    val expectedButtonText: String = "Continue"
    val expectedParagraph: String = "You can add pension schemes from other countries later."
    val expectedSubHeading: String = "Country"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: Int => String = (taxYear: Int) => s"Income from overseas pensions for 6 April ${taxYear - 1} to 5 April $taxYear"
    val expectedTitle: String = "Your taxable amount"
    val expectedHeading: String = "Your taxable amount"
    val expectedButtonText: String = "Continue"
    val expectedParagraph: String = "You can add pension schemes from other countries later."
    val expectedSubHeading: String = "Country"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY)
  )

  "show" should {
    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {
        import Selectors._
        import user.commonExpectedResults._

        "render the page with correct content and no prefilling" which {
          implicit val overseasIncomeCountryUrl: Int => String = IncomeFromOverseasPensionsPages.taxableAmountUrl(0)
          implicit lazy val result: WSResponse = showPage(user, anPensionsUserDataEmptyCya)


          "has an ok status" in {
            result.status shouldBe OK
          }

        }
    }
  }
}
