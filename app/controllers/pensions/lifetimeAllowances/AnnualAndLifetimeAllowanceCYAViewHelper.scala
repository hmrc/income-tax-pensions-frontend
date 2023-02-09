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

package controllers.pensions.lifetimeAllowances

import controllers.pensions.annualAllowances.{routes => annualRoutes}
import models.pension.charges.{PensionAnnualAllowancesViewModel, PensionLifetimeAllowancesViewModel}
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import utils.CYABaseHelper


object AnnualAndLifetimeAllowanceCYAViewHelper extends CYABaseHelper {

  def summaryListRows(annualAllowancesViewModel: PensionAnnualAllowancesViewModel, lifetimeAllowancesViewModel: PensionLifetimeAllowancesViewModel, taxYear: Int)
                     (implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      summaryRowForAboveLifetimeOrAnnualAllowanceQuestion(lifetimeAllowancesViewModel, taxYear),
      summaryRowForReducedAnnualAllowanceQuestion(lifetimeAllowancesViewModel, annualAllowancesViewModel, taxYear),
      summaryRowTypeOfReducedAnnualAllowanceQuestion(lifetimeAllowancesViewModel, annualAllowancesViewModel, taxYear),
      summaryRowForAboveAnnualAllowance(lifetimeAllowancesViewModel, annualAllowancesViewModel, taxYear),
      summaryRowForAnnualAllowanceTax(lifetimeAllowancesViewModel, annualAllowancesViewModel, taxYear),
      summaryRowForAnnualAllowanceSchemeTaxReferences(lifetimeAllowancesViewModel, annualAllowancesViewModel, taxYear),
      summaryRowForAboveLifetimeAllowanceQuestion(lifetimeAllowancesViewModel, taxYear),
      summaryRowForLumpSumAmounts(lifetimeAllowancesViewModel, taxYear),
      summaryRowForOtherPayments(lifetimeAllowancesViewModel, taxYear),
      summaryRowForLifetimeAllowanceSchemeTaxReferences(lifetimeAllowancesViewModel, taxYear)
    ).flatten

  private def summaryRowForAboveLifetimeOrAnnualAllowanceQuestion(lifetimeAllowancesViewModel: PensionLifetimeAllowancesViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    Some(
      summaryListRowWithBooleanValue(
        "lifetimeAllowance.cya.aboveAnnualOrLifetimeAllowance",
        lifetimeAllowancesViewModel.aboveLifetimeAllowanceQuestion,
        routes.AboveAnnualLifeTimeAllowanceController.show(taxYear))(messages)
    )
  }

  private def summaryRowForReducedAnnualAllowanceQuestion(lifetimeAllowancesViewModel: PensionLifetimeAllowancesViewModel, annualAllowancesViewModel: PensionAnnualAllowancesViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    lifetimeAllowancesViewModel.aboveLifetimeAllowanceQuestion.filter(_ == true)
      .map(_ =>
        summaryListRowWithBooleanValue(
          "lifetimeAllowance.cya.reducedAnnualAllowance",
          annualAllowancesViewModel.reducedAnnualAllowanceQuestion,
          annualRoutes.ReducedAnnualAllowanceController.show(taxYear))(messages)
      )
  }


  private def summaryRowTypeOfReducedAnnualAllowanceQuestion(lifetimeAllowancesViewModel: PensionLifetimeAllowancesViewModel, annualAllowancesViewModel: PensionAnnualAllowancesViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    lifetimeAllowancesViewModel.aboveLifetimeAllowanceQuestion.filter(_ == true).flatMap { _ =>
      annualAllowancesViewModel.reducedAnnualAllowanceQuestion.filter(_ == true)
        .map { _ =>
          //TODO: When page is updated update CYA to ensure only one value can be correct
          val value =
            annualAllowancesViewModel.moneyPurchaseAnnualAllowance.collect { case true => "Money purchase" case false => "Tapered" }.map(Seq(_))
          summaryListRowWithString(
            "lifetimeAllowance.cya.typeOfReducedAnnualAllowance",
            value,
            annualRoutes.ReducedAnnualAllowanceTypeController.show(taxYear))(messages)
        }
    }
  }


  private def summaryRowForAboveAnnualAllowance(lifetimeAllowancesViewModel: PensionLifetimeAllowancesViewModel, annualAllowancesViewModel: PensionAnnualAllowancesViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    lifetimeAllowancesViewModel.aboveLifetimeAllowanceQuestion.filter(_ == true).flatMap { _ =>
      annualAllowancesViewModel.reducedAnnualAllowanceQuestion.filter(_ == true)
        //TODO needs to be updated to match the prototype answer should either be the amount or no
        .map(_ =>
          annualAllowancesViewModel.aboveAnnualAllowanceQuestion match {
            case Some(true) if annualAllowancesViewModel.aboveAnnualAllowance.isDefined =>
              summaryListRowWithOptionalAmountValue(
                "lifetimeAllowance.cya.aboveAnnualAllowance",
                annualAllowancesViewModel.aboveAnnualAllowance,
                annualRoutes.AboveReducedAnnualAllowanceController.show(taxYear))(messages)
            case _ =>
              summaryListRowWithBooleanValue(
                "lifetimeAllowance.cya.aboveAnnualAllowance",
                annualAllowancesViewModel.aboveAnnualAllowanceQuestion,
                annualRoutes.AboveReducedAnnualAllowanceController.show(taxYear))(messages)
          }
        )
    }
  }

  private def summaryRowForAnnualAllowanceTax(lifetimeAllowancesViewModel: PensionLifetimeAllowancesViewModel, annualAllowancesViewModel: PensionAnnualAllowancesViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    lifetimeAllowancesViewModel.aboveLifetimeAllowanceQuestion.filter(_ == true).flatMap { _ =>
      annualAllowancesViewModel.aboveAnnualAllowanceQuestion.filter(_ == true).flatMap( _ =>
        annualAllowancesViewModel.reducedAnnualAllowanceQuestion.filter(_ == true)
        .map(_ =>
          annualAllowancesViewModel.pensionProvidePaidAnnualAllowanceQuestion match {
            case Some(true) if annualAllowancesViewModel.taxPaidByPensionProvider.isDefined =>
              summaryListRowWithOptionalAmountValue(
                "lifetimeAllowance.cya.annualAllowanceTax",
                annualAllowancesViewModel.taxPaidByPensionProvider,
                routes.PensionProviderPaidTaxController.show(taxYear))(messages)
            case _ =>
              summaryListRowWithBooleanValue(
                "lifetimeAllowance.cya.annualAllowanceTax",
                annualAllowancesViewModel.pensionProvidePaidAnnualAllowanceQuestion,
                routes.PensionProviderPaidTaxController.show(taxYear))(messages)
          }
        )
      )
    }
  }

  private def summaryRowForAnnualAllowanceSchemeTaxReferences(lifetimeAllowancesViewModel: PensionLifetimeAllowancesViewModel, annualAllowancesViewModel: PensionAnnualAllowancesViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    lifetimeAllowancesViewModel.aboveLifetimeAllowanceQuestion.filter(_ == true).flatMap(_ =>
    annualAllowancesViewModel.reducedAnnualAllowanceQuestion.filter(_ == true).flatMap(_ =>
    annualAllowancesViewModel.aboveAnnualAllowanceQuestion.filter(_ == true).flatMap(_ =>
      annualAllowancesViewModel.pensionProvidePaidAnnualAllowanceQuestion.filter(_ == true).map(_ =>
        summaryListRowWithString(
          "lifetimeAllowance.cya.annualPensionSchemeTaxReferences",
          annualAllowancesViewModel.pensionSchemeTaxReferences,
          annualRoutes.PstrSummaryController.show(taxYear))(messages)
      ))))
  }

  private def summaryRowForAboveLifetimeAllowanceQuestion(lifetimeAllowancesViewModel: PensionLifetimeAllowancesViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    lifetimeAllowancesViewModel.aboveLifetimeAllowanceQuestion.filter(_ == true).map(_ =>
      summaryListRowWithBooleanValue(
        "lifetimeAllowance.cya.aboveLifetimeAllowance",
        None,
        routes.AnnualLifetimeAllowanceCYAController.show(taxYear))(messages)
    )
  }

  private def summaryRowForLumpSumAmounts(lifetimeAllowancesViewModel: PensionLifetimeAllowancesViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    lifetimeAllowancesViewModel.aboveLifetimeAllowanceQuestion.filter(_ == true).map { _ =>
      lifetimeAllowancesViewModel.pensionAsLumpSumQuestion match {
        case Some(true) if lifetimeAllowancesViewModel.pensionAsLumpSumQuestion.isDefined =>
          summaryListRowWithAmountAndTaxValue(
            "lifetimeAllowance.cya.lumpSum",
            lifetimeAllowancesViewModel.pensionAsLumpSum.flatMap(_.amount),
            lifetimeAllowancesViewModel.pensionAsLumpSum.flatMap(_.taxPaid),
            routes.PensionLumpSumController.show(taxYear))(messages)
        case _ =>
          summaryListRowWithBooleanValue(
            "lifetimeAllowance.cya.lumpSum",
            lifetimeAllowancesViewModel.pensionAsLumpSumQuestion,
            routes.PensionLumpSumController.show(taxYear))(messages)
      }
    }
  }

  private def summaryRowForOtherPayments(lifetimeAllowancesViewModel: PensionLifetimeAllowancesViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    lifetimeAllowancesViewModel.aboveLifetimeAllowanceQuestion.filter(_ == true).map { _ =>
      lifetimeAllowancesViewModel.pensionPaidAnotherWayQuestion match {
        case Some(true) if lifetimeAllowancesViewModel.pensionPaidAnotherWayQuestion.isDefined =>
          summaryListRowWithAmountAndTaxValue(
            "lifetimeAllowance.cya.otherPayments",
            lifetimeAllowancesViewModel.pensionPaidAnotherWay.amount,
            lifetimeAllowancesViewModel.pensionPaidAnotherWay.taxPaid,
            routes.LifeTimeAllowanceAnotherWayController.show(taxYear))(messages)
        case _ =>
          summaryListRowWithBooleanValue(
            "lifetimeAllowance.cya.otherPayments",
            lifetimeAllowancesViewModel.pensionPaidAnotherWayQuestion,
            routes.LifeTimeAllowanceAnotherWayController.show(taxYear))(messages)
      }
    }
  }

  private def summaryRowForLifetimeAllowanceSchemeTaxReferences(lifetimeAllowancesViewModel: PensionLifetimeAllowancesViewModel, taxYear: Int)(implicit messages: Messages): Option[SummaryListRow] = {
    //TODO: Update call to correct page

    val combinedLumpSumOtherPaymentsQuestion = (lifetimeAllowancesViewModel.pensionPaidAnotherWayQuestion, lifetimeAllowancesViewModel.pensionAsLumpSumQuestion) match {
      case (_, Some(true)) => Some(true)
      case (Some(true), _) => Some(true)
      case _ => None
    }

    lifetimeAllowancesViewModel.aboveLifetimeAllowanceQuestion.filter(_ == true).flatMap(_ =>
      combinedLumpSumOtherPaymentsQuestion.filter(_ == true).map { _ =>
        summaryListRowWithString(
          "lifetimeAllowance.cya.lifetimePensionSchemeTaxReferences",
          lifetimeAllowancesViewModel.pensionSchemeTaxReferences,
          routes.AnnualLifetimeAllowanceCYAController.show(taxYear))(messages)
      }
    )
  }
}