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
import models.pension.{AllPensionsData, Journey, JourneyNameAndStatus}
import models.redirects.AppLocations.{INCOME_FROM_PENSIONS_HOME, OVERSEAS_HOME}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{Key, SummaryList}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Actions, SummaryListRow}
import utils.StatusHelper._

object JourneyStatusSummaryViewModel {

  def buildSummaryList(summaryPage: Journey,
                       journeyStatuses: Seq[JourneyNameAndStatus],
                       priorData: Option[AllPensionsData],
                       cya: Option[PensionsCYAModel],
                       taxYear: Int)(implicit messages: Messages): SummaryList = {
    implicit val impTaxYear: TaxYear                           = TaxYear(taxYear)
    implicit val impJourneyStatuses: Seq[JourneyNameAndStatus] = journeyStatuses

    summaryPage match {
      case PensionsSummary =>
        val paymentsIntoPensionsRow = buildRow(PaymentsIntoPensions, paymentIntoPensionHasPriorData(priorData), paymentsIntoPensionsIsUpdated(cya))

        val incomeFromPensionsHasPriorData = ukPensionsSchemeHasPriorData(priorData) | statePensionsHasPriorData(priorData)
        val incomeFromPensionsIsUpdated    = ukPensionsSchemeIsUpdated(cya) | statePensionIsUpdated(cya)
        val incomeFromPensionsRow          = buildRow(IncomeFromPensionsSummary, incomeFromPensionsHasPriorData, incomeFromPensionsIsUpdated)

        val annualAllowancesRow = buildRow(AnnualAllowances, annualAllowanceHasPriorData(priorData), annualAllowanceIsUpdated(cya))

        val unauthorisedPaymentsRow =
          buildRow(UnauthorisedPayments, unauthorisedPaymentsHasPriorData(priorData), unauthorisedPaymentsFromPensionsIsUpdated(cya))

        val overseasPensionsHasPriorData = paymentsIntoOverseasPensionsHasPriorData(priorData) | incomeFromOverseasPensionsHasPriorData(
          priorData) | transferIntoOverseasPensionHasPriorData(priorData) | shortServiceRefundsHasPriorData(priorData)
        val overseasPensionsIsUpdated =
          paymentsIntoOverseasPensionsIsUpdated(cya) | incomeFromOverseasPensionsIsUpdated(cya) | overseasPensionsTransferChargesIsUpdated(
            cya) | shortServiceRefundsIsUpdated(cya)
        val overseasPensionsRow = buildRow(OverseasPensionsSummary, overseasPensionsHasPriorData, overseasPensionsIsUpdated)

        SummaryList(rows = Seq(
          paymentsIntoPensionsRow,
          incomeFromPensionsRow,
          annualAllowancesRow,
          unauthorisedPaymentsRow,
          overseasPensionsRow
        ))

      case IncomeFromPensionsSummary =>
        val ukPensionsIncomeRow = buildRow(UkPensionIncome, ukPensionsSchemeHasPriorData(priorData), ukPensionsSchemeIsUpdated(cya))
        val statePensionRow     = buildRow(StatePension, statePensionsHasPriorData(priorData), statePensionIsUpdated(cya))
        SummaryList(rows = Seq(ukPensionsIncomeRow, statePensionRow))

      case OverseasPensionsSummary =>
        val paymentsIntoOverseasPensionsRow =
          buildRow(PaymentsIntoOverseasPensions, paymentsIntoOverseasPensionsHasPriorData(priorData), paymentsIntoOverseasPensionsIsUpdated(cya))
        val incomeFromOverseasPensionsRow =
          buildRow(IncomeFromOverseasPensions, incomeFromOverseasPensionsHasPriorData(priorData), incomeFromOverseasPensionsIsUpdated(cya))
        val transferIntoOverseasPensionsRow =
          buildRow(TransferIntoOverseasPensions, transferIntoOverseasPensionHasPriorData(priorData), overseasPensionsTransferChargesIsUpdated(cya))
        val shortServiceRefundsRow = buildRow(ShortServiceRefunds, shortServiceRefundsHasPriorData(priorData), shortServiceRefundsIsUpdated(cya))
        SummaryList(rows =
          Seq(paymentsIntoOverseasPensionsRow, incomeFromOverseasPensionsRow, transferIntoOverseasPensionsRow, shortServiceRefundsRow))
    }
  }

  private def buildRow(journey: Journey, hasPriorData: Boolean, journeyIsStarted: Boolean)(implicit
      messages: Messages,
      taxYear: TaxYear,
      journeyStatuses: Seq[JourneyNameAndStatus]): SummaryListRow = {
    val status    = getJourneyStatus(journey, journeyStatuses, hasPriorData, journeyIsStarted)
    val keyString = messages(s"journey.$journey")
    val href      = getUrl(journey, status, taxYear)

    buildSummaryListRow(href, keyString, status)
  }

  private def buildSummaryListRow(href: String, keyString: String, status: JourneyStatus)(implicit messages: Messages): SummaryListRow =
    SummaryListRow(
      key = Key(content = HtmlContent(s"<span class='app-task-list__task-name govuk-!-font-weight-regular'> <a href=$href> $keyString </a> </span>")),
      actions = Some(
        Actions(
          items = Seq(
            ActionItem(
              content = HtmlContent(messages(s"common.status.$status")),
              href = href,
              classes = s"govuk-tag app-task-list__tag ${tagStyle(status)}"
            )))),
      classes = "app-task-list__item no-wrap no-after-content"
    )

  private def tagStyle(status: JourneyStatus): String = status match {
    case Completed  => ""
    case InProgress => "govuk-tag--blue"
    case NotStarted => "govuk-tag--grey"
  }

  private def getJourneyStatus(journey: Journey,
                               journeyStatuses: Seq[JourneyNameAndStatus],
                               hasPriorData: Boolean,
                               journeyIsStarted: Boolean): JourneyStatus =
    journeyStatuses.find(_.name == journey).map(_.journeyStatus).getOrElse(NotStarted) match {
      case NotStarted if hasPriorData | journeyIsStarted => InProgress
      case NotStarted                                    => NotStarted
      case status                                        => status
    }

  private def getUrl(journey: Journey, journeyStatus: JourneyStatus, taxYear: TaxYear): String = {
    implicit val status: JourneyStatus = journeyStatus
    journey match {
      case IncomeFromPensionsSummary => INCOME_FROM_PENSIONS_HOME(taxYear.endYear).url
      case OverseasPensionsSummary   => OVERSEAS_HOME(taxYear.endYear).url
      case PaymentsIntoPensions =>
        determineJourneyStartOrCyaUrl(
          pipRoutes.ReliefAtSourcePensionsController.show(taxYear.endYear).url,
          pipRoutes.PaymentsIntoPensionsCYAController.show(taxYear.endYear).url
        )
      case AnnualAllowances =>
        determineJourneyStartOrCyaUrl(
          aaRoutes.ReducedAnnualAllowanceController.show(taxYear.endYear).url,
          aaRoutes.AnnualAllowanceCYAController.show(taxYear.endYear).url
        )
      case UnauthorisedPayments =>
        determineJourneyStartOrCyaUrl(
          upRoutes.UnauthorisedPaymentsController.show(taxYear.endYear).url,
          upRoutes.UnauthorisedPaymentsCYAController.show(taxYear.endYear).url
        )
      case UkPensionIncome =>
        determineJourneyStartOrCyaUrl(
          ifpRoutes.UkPensionSchemePaymentsController.show(taxYear.endYear).url,
          ifpRoutes.UkPensionIncomeCYAController.show(taxYear.endYear).url
        )
      case StatePension =>
        determineJourneyStartOrCyaUrl(
          ifpRoutes.StatePensionController.show(taxYear.endYear).url,
          ifpRoutes.StatePensionCYAController.show(taxYear.endYear).url
        )
      case PaymentsIntoOverseasPensions =>
        determineJourneyStartOrCyaUrl(
          piopRoutes.PaymentIntoPensionSchemeController.show(taxYear.endYear).url,
          piopRoutes.PaymentsIntoOverseasPensionsCYAController.show(taxYear.endYear).url
        )
      case IncomeFromOverseasPensions =>
        determineJourneyStartOrCyaUrl(
          ifopRoutes.PensionOverseasIncomeStatus.show(taxYear.endYear).url,
          ifopRoutes.IncomeFromOverseasPensionsCYAController.show(taxYear.endYear).url
        )
      case TransferIntoOverseasPensions =>
        determineJourneyStartOrCyaUrl(
          tiopRoutes.TransferPensionSavingsController.show(taxYear.endYear).url,
          tiopRoutes.TransferIntoOverseasPensionsCYAController.show(taxYear.endYear).url
        )
      case ShortServiceRefunds =>
        determineJourneyStartOrCyaUrl(
          ssrRoutes.TaxableRefundAmountController.show(taxYear.endYear).url,
          ssrRoutes.ShortServiceRefundsCYAController.show(taxYear.endYear).url
        )
      case _ => "#"
    }
  }

  private def determineJourneyStartOrCyaUrl(startUrl: String, cyaUrl: String)(implicit status: JourneyStatus): String =
    status match {
      case Completed | InProgress => cyaUrl
      case NotStarted             => startUrl
    }

// TODO if url is dependent upon completion instead of status, use this =>

//  private def getUrl(journey: Journey, taxYear: TaxYear, journeyIsComplete: Boolean): String = {
//    implicit val isComplete: Boolean = journeyIsComplete
//    journey match {
//      case PaymentsIntoPensions =>
//        determineJourneyStartOrCyaUrl(
//          pipRoutes.ReliefAtSourcePensionsController.show(taxYear.endYear).url,
//          pipRoutes.PaymentsIntoPensionsCYAController.show(taxYear.endYear).url
//        )
//      case _ => "#"
//    }
//  }
//  private def determineJourneyStartOrCyaUrl(startUrl: String, cyaUrl: String)(implicit journeyIsComplete: Boolean): String =
//    if (journeyIsComplete) cyaUrl else startUrl
}
