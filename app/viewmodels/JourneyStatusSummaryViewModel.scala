/*
 * Copyright 2024 HM Revenue & Customs
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

import common.TaxYear
import controllers.pensions.annualAllowances.{routes => aaRoutes}
import controllers.pensions.incomeFromOverseasPensions.{routes => ifopRoutes}
import controllers.pensions.incomeFromPensions.{routes => ifpRoutes}
import controllers.pensions.paymentsIntoOverseasPensions.{routes => piopRoutes}
import controllers.pensions.paymentsIntoPensions.{routes => pipRoutes}
import controllers.pensions.shortServiceRefunds.{routes => ssrRoutes}
import controllers.pensions.transferIntoOverseasPensions.{routes => tiopRoutes}
import controllers.pensions.unauthorisedPayments.{routes => upRoutes}
import models.mongo.JourneyStatus.{Completed, InProgress, NotStarted}
import models.mongo.{JourneyStatus, PensionsCYAModel}
import models.pension.Journey._
import models.pension.{Journey, JourneyNameAndStatus}
import models.redirects.AppLocations.{INCOME_FROM_PENSIONS_HOME, OVERSEAS_HOME}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import utils.StatusHelper._

object JourneyStatusSummaryViewModel {

  def buildSummaryList(summaryPage: Journey, journeyStatuses: Seq[JourneyNameAndStatus], cya: Option[PensionsCYAModel], taxYear: Int)(implicit
      messages: Messages): HtmlContent = {
    implicit val impTaxYear: TaxYear                           = TaxYear(taxYear)
    implicit val impJourneyStatuses: Seq[JourneyNameAndStatus] = journeyStatuses

    def paymentsIntoPensionsRow = buildRow(
      PaymentsIntoPensions,
      paymentsIntoPensionsIsUpdated(cya),
      cya.exists(_.paymentsIntoPension.isFinished)
    )

    def incomeFromPensionsIsUpdated = ukPensionsSchemeIsUpdated(cya) | statePensionIsUpdated(cya)
    def incomeFromPensionsRow =
      buildRow(IncomeFromPensionsSummary, incomeFromPensionsIsUpdated, cya.exists(_.incomeFromPensions.isFinished))

    def annualAllowancesRow = buildRow(AnnualAllowances, annualAllowanceIsUpdated(cya), cya.exists(_.pensionsAnnualAllowances.isFinished))

    def unauthorisedPaymentsRow =
      buildRow(
        UnauthorisedPayments,
        unauthorisedPaymentsFromPensionsIsUpdated(cya),
        cya.exists(_.unauthorisedPayments.isFinished)
      )

    def overseasPensionsIsUpdated =
      paymentsIntoOverseasPensionsIsUpdated(cya) | incomeFromOverseasPensionsIsUpdated(cya) | overseasPensionsTransferChargesIsUpdated(
        cya) | shortServiceRefundsIsUpdated(cya)
    def overseasPensionsRow = buildRow(OverseasPensionsSummary, overseasPensionsIsUpdated, journeyAnswersAreComplete = false)

    def ukPensionsIncomeRow =
      buildRow(UkPensionIncome, ukPensionsSchemeIsUpdated(cya), cya.exists(_.incomeFromPensions.isUkPensionFinished))
    def statePensionRow = buildRow(
      StatePension,
      statePensionIsUpdated(cya),
      cya.exists(_.incomeFromPensions.isStatePensionFinished)
    )

    def paymentsIntoOverseasPensionsRow =
      buildRow(
        PaymentsIntoOverseasPensions,
        paymentsIntoOverseasPensionsIsUpdated(cya),
        cya.exists(_.paymentsIntoOverseasPensions.isFinished)
      )
    def incomeFromOverseasPensionsRow =
      buildRow(
        IncomeFromOverseasPensions,
        incomeFromOverseasPensionsIsUpdated(cya),
        cya.exists(_.incomeFromOverseasPensions.isFinished)
      )
    def transferIntoOverseasPensionsRow =
      buildRow(
        TransferIntoOverseasPensions,
        overseasPensionsTransferChargesIsUpdated(cya),
        cya.exists(_.transfersIntoOverseasPensions.isFinished)
      )
    def shortServiceRefundsRow = buildRow(ShortServiceRefunds, shortServiceRefundsIsUpdated(cya), cya.exists(_.shortServiceRefunds.isFinished))

    HtmlContent(summaryPage match {
      case PensionsSummary =>
        buildTaskListSummary(rows = Seq(
          paymentsIntoPensionsRow,
          incomeFromPensionsRow,
          annualAllowancesRow,
          unauthorisedPaymentsRow,
          overseasPensionsRow
        ))
      case IncomeFromPensionsSummary =>
        buildTaskListSummary(rows = Seq(ukPensionsIncomeRow, statePensionRow))
      case OverseasPensionsSummary =>
        buildTaskListSummary(rows =
          Seq(paymentsIntoOverseasPensionsRow, incomeFromOverseasPensionsRow, transferIntoOverseasPensionsRow, shortServiceRefundsRow))
      case _ => buildTaskListSummary(Seq.empty)
    })
  }

  private def buildTaskListSummary(rows: Seq[String]): String =
    s"""
       |  <div class="app-task-list">
       |    <ul class="app-task-list__items govuk-!-padding-left-0">
       |      ${rows.map(row => s"<li class='app-task-list__item'>$row</li>").mkString("\n")}
       |    </ul
       |  </div>
       |""".stripMargin

  private def buildRow(journey: Journey, journeyIsStarted: Boolean, journeyAnswersAreComplete: Boolean)(implicit
      messages: Messages,
      taxYear: TaxYear,
      journeyStatuses: Seq[JourneyNameAndStatus]): String = {
    // URL is determined by journey completion in session data, not by status
    val status   = getJourneyStatus(journey, journeyStatuses, journeyIsStarted)
    val href     = getUrl(journey, taxYear, journeyAnswersAreComplete)
    val statusId = s"journey-${journey.toString}-status"
    val statusTag =
      if (status == Completed) messages(s"common.status.$status")
      else s"""<strong class="govuk-tag govuk-tag--blue">${messages(s"common.status.$status")}</strong>"""

    val journeyId = s"${journey.toString}-link"
    s"""
       |  <span class="app-task-list__task-name">
       |    <a id="$journeyId" class="govuk-link govuk-task-list__link"
       |       href=$href aria-describedby=$statusId> ${messages(s"journey.${journey.toString}")}</a>
       |  </span>
       |  <div class="right-float" id=$statusId> $statusTag </div>
       |""".stripMargin
  }

  private def getJourneyStatus(journey: Journey, journeyStatuses: Seq[JourneyNameAndStatus], journeyIsStarted: Boolean): JourneyStatus = {
    def getStatus(journey: Journey) =
      journeyStatuses.find(_.name == journey).map(_.journeyStatus).getOrElse(NotStarted)
    def summaryStatus(dependentJourneys: Seq[Journey]): JourneyStatus = {
      val statusesAreAll = (status: JourneyStatus) => dependentJourneys.map(getStatus).forall(_ == status)
      if (statusesAreAll(Completed)) Completed else if (statusesAreAll(NotStarted)) NotStarted else InProgress
    }
    val status = journey match {
      case IncomeFromPensionsSummary => summaryStatus(Seq(UkPensionIncome, StatePension))
      case OverseasPensionsSummary =>
        summaryStatus(Seq(PaymentsIntoOverseasPensions, IncomeFromOverseasPensions, TransferIntoOverseasPensions, ShortServiceRefunds))
      case _ => getStatus(journey)
    }
    status match {
      case NotStarted if journeyIsStarted => InProgress
      case status                         => status
    }
  }

  private def getUrl(journey: Journey, taxYear: TaxYear, journeyIsComplete: Boolean): String = {
    def determineJourneyStartOrCyaUrl(startUrl: String, cyaUrl: String): String = if (journeyIsComplete) cyaUrl else startUrl

    journey match {
      case IncomeFromPensionsSummary => INCOME_FROM_PENSIONS_HOME(taxYear.endYear).url
      case OverseasPensionsSummary   => OVERSEAS_HOME(taxYear.endYear).url
      case PaymentsIntoPensions =>
        determineJourneyStartOrCyaUrl(
          pipRoutes.ReliefAtSourcePensionsController.show(taxYear.endYear).url,
          pipRoutes.PaymentsIntoPensionsCYAController.show(taxYear).url
        )
      case AnnualAllowances =>
        determineJourneyStartOrCyaUrl(
          aaRoutes.ReducedAnnualAllowanceController.show(taxYear.endYear).url,
          aaRoutes.AnnualAllowanceCYAController.show(taxYear).url
        )
      case UnauthorisedPayments =>
        determineJourneyStartOrCyaUrl(
          upRoutes.UnauthorisedPaymentsController.show(taxYear.endYear).url,
          upRoutes.UnauthorisedPaymentsCYAController.show(taxYear).url
        )
      case UkPensionIncome =>
        determineJourneyStartOrCyaUrl(
          ifpRoutes.UkPensionSchemePaymentsController.show(taxYear.endYear).url,
          ifpRoutes.UkPensionIncomeCYAController.show(taxYear).url
        )
      case StatePension =>
        determineJourneyStartOrCyaUrl(
          ifpRoutes.StatePensionController.show(taxYear.endYear).url,
          ifpRoutes.StatePensionCYAController.show(taxYear).url
        )
      case PaymentsIntoOverseasPensions =>
        determineJourneyStartOrCyaUrl(
          piopRoutes.PaymentIntoPensionSchemeController.show(taxYear.endYear).url,
          piopRoutes.PaymentsIntoOverseasPensionsCYAController.show(taxYear).url
        )
      case IncomeFromOverseasPensions =>
        determineJourneyStartOrCyaUrl(
          ifopRoutes.PensionOverseasIncomeStatus.show(taxYear.endYear).url,
          ifopRoutes.IncomeFromOverseasPensionsCYAController.show(taxYear).url
        )
      case TransferIntoOverseasPensions =>
        determineJourneyStartOrCyaUrl(
          tiopRoutes.TransferPensionSavingsController.show(taxYear.endYear).url,
          tiopRoutes.TransferIntoOverseasPensionsCYAController.show(taxYear).url
        )
      case ShortServiceRefunds =>
        determineJourneyStartOrCyaUrl(
          ssrRoutes.TaxableRefundAmountController.show(taxYear.endYear).url,
          ssrRoutes.ShortServiceRefundsCYAController.show(taxYear).url
        )
      case _ => "#"
    }
  }
}
