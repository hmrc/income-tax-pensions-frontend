package connectors

import builders.PensionsUserDataBuilder.currentTaxYear
import cats.implicits.{catsSyntaxEitherId, catsSyntaxOptionId}
import models.pension.charges.CreateUpdatePensionChargesRequestModel
import models.{APIErrorBodyModel, APIErrorModel}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT}
import play.api.libs.json.JsObject
import uk.gov.hmrc.http.HeaderCarrier
import utils.IntegrationTest
import utils.ModelHelpers.{emptyChargesDownstreamRequestModel, emptyPensionContributions}

class PensionChargesConnectorISpec extends IntegrationTest {

  val hc: HeaderCarrier = headerCarrier.withExtraHeaders("X-Session-ID" -> sessionId)

  val requestModel: CreateUpdatePensionChargesRequestModel =
    emptyChargesDownstreamRequestModel
      .copy(pensionContributions = emptyPensionContributions
        .copy(isAnnualAllowanceReduced = false.some)
        .some)

  val downstreamUrl = s"/income-tax-pensions/pension-charges/session-data/nino/$nino/taxYear/$taxYear"

  val connector = new PensionChargesConnector(httpClient, appConfig)

  "saving journey answers" when {
    "downstream returns a successful result" should {
      "return Unit" in {
        stubPutWithBodyAndHeaders(
          url = downstreamUrl,
          requestBody = requestModel,
          expectedStatus = NO_CONTENT,
          responseBody = JsObject.empty,
          sessionHeader = "X-Session-ID" -> sessionId,
          mtdidHeader = "mtditid"        -> mtditid
        )

        val result = connector.saveAnswers(requestModel, currentTaxYear, nino)(hc).futureValue

        result shouldBe ().asRight
      }
    }
    "downstream call is unsuccessful" should {
      "return an API error" in {
        stubPutWithBodyAndHeaders(
          url = downstreamUrl,
          requestBody = requestModel,
          expectedStatus = INTERNAL_SERVER_ERROR,
          responseBody = JsObject.empty,
          sessionHeader = "X-Session-ID" -> sessionId,
          mtdidHeader = "mtditid"        -> mtditid
        )

        val result = connector.saveAnswers(requestModel, currentTaxYear, nino)(hc).futureValue

        val expectedApiError =
          APIErrorModel(
            status = INTERNAL_SERVER_ERROR,
            body = APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")
          )

        result shouldBe expectedApiError.asLeft
      }
    }
  }

}
