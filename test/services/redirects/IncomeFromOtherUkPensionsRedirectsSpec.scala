
package services.redirects

import play.api.mvc.Call
import controllers.pensions.incomeFromPensions.routes._
import services.redirects.IncomeFromOtherUkPensionsRedirects.cyaPageCall
import utils.UnitTest

class IncomeFromOtherUkPensionsRedirectsSpec extends UnitTest {

  private val checkYourAnswersCall: Call = UkPensionIncomeCYAController.show(taxYear)

    ".cyaPageCall" should {
    "return a redirect call to the cya page" in {
      cyaPageCall(taxYear) shouldBe checkYourAnswersCall
    }
  }
}
