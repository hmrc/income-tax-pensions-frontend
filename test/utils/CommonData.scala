package utils

import common.TaxYear
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import java.time.LocalDate
import scala.concurrent.ExecutionContext

object CommonData {
  private val dateNow           = LocalDate.now()
  private val taxYearCutoffDate = LocalDate.parse(s"${dateNow.getYear}-04-05")

  private val taxYear: Int = if (dateNow.isAfter(taxYearCutoffDate)) LocalDate.now().getYear + 1 else LocalDate.now().getYear

  val nino: String      = "AA123456A"
  val mtditid: String   = "1234567890"
  val sessionId: String = "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe"

  val currTaxYear: TaxYear = TaxYear(taxYear)

  implicit val headerCarrierWithSession: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
  implicit val ec: ExecutionContext                    = ExecutionContext.Implicits.global
  implicit val testClock: Clock                        = UnitTestClock

}
