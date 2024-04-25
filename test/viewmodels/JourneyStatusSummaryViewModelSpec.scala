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

package viewmodels

import builders.AllPensionsDataBuilder.{anAllPensionDataEmpty, anAllPensionsData}
import builders.PensionsCYAModelBuilder.{aPensionsCYAModel, emptyPensionsData}
import models.mongo.JourneyStatus
import models.mongo.JourneyStatus.{Completed, InProgress, NotStarted}
import models.pension.Journey.{IncomeFromPensionsSummary, PensionsSummary, StatePension, UkPensionIncome}
import models.pension.JourneyNameAndStatus
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.Injecting
import utils.UnitTest
import viewmodels.JourneyStatusSummaryViewModel.buildSummaryList

class JourneyStatusSummaryViewModelSpec extends UnitTest with Injecting with TableDrivenPropertyChecks {

  implicit val messages: Messages = inject[MessagesApi].preferred(fakeRequest.withHeaders())

  private val completed      = "Completed"
  private val inProgress     = "In Progress"
  private val notStarted     = "Not Started"
  private val startHref      = "/update-and-submit-income-tax-return/pensions/2025/pension-income/uk-pension-income"
  private val cyaHref        = "/update-and-submit-income-tax-return/pensions/2025/pension-income/pensions-income-summary"
  private val ukpiNotStarted = JourneyNameAndStatus(UkPensionIncome, NotStarted)
  private val ukpiInProgress = JourneyNameAndStatus(UkPensionIncome, InProgress)
  private val ukpiCompleted  = JourneyNameAndStatus(UkPensionIncome, Completed)
  private val spNotStarted   = JourneyNameAndStatus(StatePension, NotStarted)
  private val spInProgress   = JourneyNameAndStatus(StatePension, InProgress)
  private val spCompleted    = JourneyNameAndStatus(StatePension, Completed)

  private val cases = Table(
    ("journeyStatuses", "priorData", "sessionData", "expectedStatus", "expectedHref"),
    // Not Started if no status
    (Seq.empty, None, None, notStarted, startHref),
    // Match status data
    (Seq(ukpiNotStarted), None, None, notStarted, startHref),
    (Seq(ukpiInProgress), None, None, inProgress, startHref),
    (Seq(ukpiCompleted), None, None, completed, startHref),
    // Override Not Started status to In Progress if journey has session data
    (Seq(ukpiNotStarted), None, Some(emptyPensionsData), notStarted, startHref),
    (Seq(ukpiNotStarted), None, Some(aPensionsCYAModel), inProgress, startHref),
    (Seq(ukpiInProgress), None, Some(aPensionsCYAModel), inProgress, startHref),
    (Seq(ukpiCompleted), None, Some(aPensionsCYAModel), completed, startHref),
    // href to CYA page if full journey answers saved in backend database
    (Seq(ukpiInProgress), Some(anAllPensionsData), None, inProgress, cyaHref),
    (Seq(ukpiCompleted), Some(anAllPensionsData), None, completed, cyaHref),
    (Seq(ukpiCompleted), Some(anAllPensionDataEmpty), None, completed, startHref),
    // Override Not Started status to In Progress if full journey answers saved in backend database
    (Seq(ukpiNotStarted), Some(anAllPensionsData), None, inProgress, cyaHref),
    (Seq(ukpiNotStarted), Some(anAllPensionDataEmpty), None, notStarted, cyaHref)
  )

  private def incomeSummary(status: JourneyStatus) =
    if (status == Completed) """<div class="right-float" id="journey-income-from-pensions-summary-status"> Completed </div>"""
    else
      s"""<div class="right-float" id="journey-income-from-pensions-summary-status"> <strong class="govuk-tag govuk-tag--blue">${if (status == NotStarted)
          "Not Started"
        else "In Progress"}</strong> </div>"""

  private val summaryCases = Table(
    ("journeyStatuses", "priorData", "sessionData", "expectedIncomeStatus"),
    // All are NotStarted => NotStarted summary status
    (Seq.empty, None, None, incomeSummary(NotStarted)),
    (Seq(ukpiNotStarted, spNotStarted), None, None, incomeSummary(NotStarted)),
    // All are Completed => Completed
    (Seq(ukpiCompleted, spCompleted), None, None, incomeSummary(Completed)),
    // Any other combination => InProgress
    (Seq(ukpiNotStarted, spInProgress), None, None, incomeSummary(InProgress)),
    (Seq(ukpiNotStarted, spCompleted), None, None, incomeSummary(InProgress)),
    (Seq(ukpiInProgress, spNotStarted), None, None, incomeSummary(InProgress)),
    (Seq(ukpiInProgress, spInProgress), None, None, incomeSummary(InProgress)),
    (Seq(ukpiInProgress, spCompleted), None, None, incomeSummary(InProgress)),
    (Seq(ukpiCompleted, spNotStarted), None, None, incomeSummary(InProgress)),
    (Seq(ukpiCompleted, spInProgress), None, None, incomeSummary(InProgress)),
    // All are NotStarted but has journey session data and/or completed pior journey answers => InProgress
    (Seq(ukpiNotStarted, spNotStarted), Some(anAllPensionsData), None, incomeSummary(InProgress)),
    (Seq(ukpiNotStarted, spNotStarted), None, Some(aPensionsCYAModel), incomeSummary(InProgress)),
    (Seq(ukpiNotStarted, spNotStarted), Some(anAllPensionsData), Some(aPensionsCYAModel), incomeSummary(InProgress))
  )

  "JourneyStatusSummaryViewModel.buildSummaryList" should {
    forAll(cases) { case (journeyStatuses, priorData, sessionData, expectedStatus, expectedHref) =>
      s"return a summary list of relevant journeys (with data: $journeyStatuses, $priorData, $sessionData, $expectedStatus, $expectedHref)" which {
        val result = buildSummaryList(IncomeFromPensionsSummary, journeyStatuses, priorData, sessionData, taxYear).toString
        "has the correct status" in result.contains(expectedStatus)
        "has the correct href" in result.contains(expectedHref)
      }
    }
    forAll(summaryCases) { case (journeyStatuses, priorData, sessionData, expectedIncomeStatus) =>
      s"contain summary links which have the correct statuses, dependent on the journeys they contain (data: $journeyStatuses, $priorData, $sessionData, $expectedIncomeStatus)" in {
        val result = buildSummaryList(PensionsSummary, journeyStatuses, priorData, sessionData, taxYear).toString
        result.contains(expectedIncomeStatus)
      }
    }
  }
}
