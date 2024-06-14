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

import builders.IncomeFromPensionsViewModelBuilder.anIncomeFromPensionsViewModel
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

  private val partialJourneyData =
    emptyPensionsData.copy(incomeFromPensions = anIncomeFromPensionsViewModel.copy(uKPensionIncomes = Some(List.empty)))

  private val cases = Table(
    ("journeyStatuses", "sessionData", "expectedStatus", "expectedHref"),
    // Not Started if no status
    (Seq.empty, None, notStarted, startHref),
    // Match status data
    (Seq(ukpiNotStarted), None, notStarted, startHref),
    (Seq(ukpiInProgress), None, inProgress, startHref),
    (Seq(ukpiCompleted), None, completed, startHref),
    // Override Not Started status to In Progress if journey has session data
    (Seq(ukpiNotStarted), Some(emptyPensionsData), notStarted, startHref),
    (Seq(ukpiNotStarted), Some(partialJourneyData), inProgress, startHref),
    (Seq(ukpiInProgress), Some(partialJourneyData), inProgress, startHref),
    (Seq(ukpiCompleted), Some(partialJourneyData), completed, startHref),
    // href to CYA page if full journey answers are in session data
    (Seq(ukpiInProgress), Some(aPensionsCYAModel), inProgress, cyaHref),
    (Seq(ukpiCompleted), Some(aPensionsCYAModel), completed, cyaHref)
  )

  private def incomeSummary(status: JourneyStatus) =
    if (status == Completed) """<div class="right-float" id="journey-income-from-pensions-summary-status"> Completed </div>"""
    else
      s"""<div class="right-float" id="journey-income-from-pensions-summary-status"> <strong class="govuk-tag govuk-tag--blue">${if (status == NotStarted)
          "Not Started"
        else "In Progress"}</strong> </div>"""

  private val summaryCases = Table(
    ("journeyStatuses", "sessionData", "expectedIncomeStatus"),
    // All are NotStarted => NotStarted summary status
    (Seq.empty, None, incomeSummary(NotStarted)),
    (Seq(ukpiNotStarted, spNotStarted), None, incomeSummary(NotStarted)),
    // All are Completed => Completed
    (Seq(ukpiCompleted, spCompleted), None, incomeSummary(Completed)),
    // Any other combination => InProgress
    (Seq(ukpiNotStarted, spInProgress), None, incomeSummary(InProgress)),
    (Seq(ukpiNotStarted, spCompleted), None, incomeSummary(InProgress)),
    (Seq(ukpiInProgress, spNotStarted), None, incomeSummary(InProgress)),
    (Seq(ukpiInProgress, spInProgress), None, incomeSummary(InProgress)),
    (Seq(ukpiInProgress, spCompleted), None, incomeSummary(InProgress)),
    (Seq(ukpiCompleted, spNotStarted), None, incomeSummary(InProgress)),
    (Seq(ukpiCompleted, spInProgress), None, incomeSummary(InProgress)),
    // All are NotStarted but have journey session data => InProgress
    (Seq(ukpiNotStarted, spNotStarted), Some(aPensionsCYAModel), incomeSummary(InProgress))
  )

  "JourneyStatusSummaryViewModel.buildSummaryList" should {
    forAll(cases) { case (journeyStatuses, sessionData, expectedStatus, expectedHref) =>
      s"return a summary list of relevant journeys (with data: $journeyStatuses, $sessionData, $expectedStatus, $expectedHref)" which {
        val result = buildSummaryList(IncomeFromPensionsSummary, journeyStatuses, sessionData, taxYear).toString
        "has the correct status" in result.contains(expectedStatus)
        "has the correct href" in result.contains(expectedHref)
      }
    }
    forAll(summaryCases) { case (journeyStatuses, sessionData, expectedIncomeStatus) =>
      s"contain summary links which have the correct statuses, dependent on the journeys they contain (data: $journeyStatuses, $sessionData, $expectedIncomeStatus)" in {
        val result = buildSummaryList(PensionsSummary, journeyStatuses, sessionData, taxYear).toString
        result.contains(expectedIncomeStatus)
      }
    }
  }
}
