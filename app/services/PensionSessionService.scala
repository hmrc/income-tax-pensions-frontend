/*
 * Copyright 2022 HM Revenue & Customs
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

package services

import config.{AppConfig, ErrorHandler}
import connectors.IncomeTaxUserDataConnector
import connectors.httpParsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import forms.{No, Yes}
import models.User
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.AllPensionsData
import models.pension.charges.{PensionAnnualAllowancesViewModel, PensionLifetimeAllowancesViewModel, UnauthorisedPaymentsViewModel}
import models.pension.reliefs.PaymentsIntoPensionViewModel
import models.pension.statebenefits.{IncomeFromPensionsViewModel, StateBenefit, StateBenefitViewModel, UkPensionIncomeViewModel}
import org.joda.time.DateTimeZone
import play.api.Logging
import play.api.http.Status.INTERNAL_SERVER_ERROR
import play.api.mvc.Results.Redirect
import play.api.mvc.{Request, Result}
import repositories.PensionsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier
import utils.Clock

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PensionSessionService @Inject()(pensionUserDataRepository: PensionsUserDataRepository,
                                      incomeTaxUserDataConnector: IncomeTaxUserDataConnector,
                                      implicit private val appConfig: AppConfig,
                                      errorHandler: ErrorHandler,
                                      implicit val ec: ExecutionContext) extends Logging {


  def getPriorData(taxYear: Int, user: User)(implicit hc: HeaderCarrier): Future[IncomeTaxUserDataResponse] = {
    incomeTaxUserDataConnector.getUserData(user.nino, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid))
  }

  private def getSessionData(taxYear: Int, user: User)(implicit request: Request[_]): Future[Either[Result, Option[PensionsUserData]]] = {
    pensionUserDataRepository.find(taxYear, user).map {
      case Left(_) => Left(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(value) => Right(value)
    }
  }

  def getPensionSessionData(taxYear: Int, user: User): Future[Either[Unit, Option[PensionsUserData]]] = {
    pensionUserDataRepository.find(taxYear, user).map {
      case Left(_) => Left(())
      case Right(data) => Right(data)
    }
  }

  @deprecated("We should avoid using this method, as it's more difficult to mock. use 'getPensionSessionData' above")
  def getPensionsSessionDataResult(taxYear: Int, user: User)(result: Option[PensionsUserData] => Future[Result])
                                  (implicit request: Request[_]): Future[Result] = {
    pensionUserDataRepository.find(taxYear, user).flatMap {
      case Left(_) => Future.successful(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      case Right(value) => result(value)
    }
  }

  def getAndHandle(taxYear: Int, user: User, redirectWhenNoPrior: Boolean = false)
                  (block: (Option[PensionsUserData], Option[AllPensionsData]) => Future[Result])
                  (implicit request: Request[_], hc: HeaderCarrier): Future[Result] = {
    val result = for {
      optionalCya <- getSessionData(taxYear, user)
      priorDataResponse <- getPriorData(taxYear, user)
    } yield {
      if (optionalCya.isRight) {
        if (optionalCya.right.get.isEmpty) {
          logger.info(s"[PensionSessionService][getAndHandle] No pension CYA data found for user. SessionId: ${user.sessionId}")
        }
      }

      val pensionDataResponse = priorDataResponse.map(_.pensions)
      (optionalCya, pensionDataResponse) match {
        case (Right(None), Right(None)) if redirectWhenNoPrior => logger.info(s"[PensionSessionService][getAndHandle] No pension data found for user." +
          s"Redirecting to overview page. SessionId: ${user.sessionId}")
          Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        case (Right(optionalCya), Right(pensionData)) => block(optionalCya, pensionData)
        case (_, Left(error)) => Future(errorHandler.handleError(error.status))
        case (Left(_), _) => Future(errorHandler.handleError(INTERNAL_SERVER_ERROR))
      }
    }
    result.flatten
  }

  //scalastyle:off
  def createOrUpdateSessionData[A](user: User, cyaModel: PensionsCYAModel, taxYear: Int, isPriorSubmission: Boolean)
                                  (onFail: A)(onSuccess: A)(implicit clock: Clock): Future[A] = {

    val userData = PensionsUserData(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      isPriorSubmission,
      cyaModel,
      clock.now(DateTimeZone.UTC)
    )

    pensionUserDataRepository.createOrUpdate(userData, user).map {
      case Right(_) => onSuccess
      case Left(_) => onFail
    }
  }

  def generateCyaFromPrior(prior: AllPensionsData): PensionsCYAModel = {

    val statePension: Option[StateBenefit] = prior.stateBenefits.flatMap(_.stateBenefits.flatMap(_.statePension))
    val statePensionLumpSum: Option[StateBenefit] = prior.stateBenefits.flatMap(_.stateBenefits.flatMap(_.statePensionLumpSum))

    PensionsCYAModel(
      PaymentsIntoPensionViewModel(
        prior.pensionReliefs.map(a => a.pensionReliefs.regularPensionContributions.isDefined),
        prior.pensionReliefs.flatMap(a => a.pensionReliefs.regularPensionContributions),
        prior.pensionReliefs.map(a => a.pensionReliefs.oneOffPensionContributionsPaid.isDefined),
        prior.pensionReliefs.flatMap(a => a.pensionReliefs.oneOffPensionContributionsPaid),
        Some(true),
        prior.pensionReliefs.map(a =>
          a.pensionReliefs.retirementAnnuityPayments.isDefined || a.pensionReliefs.paymentToEmployersSchemeNoTaxRelief.isDefined
        ),
        prior.pensionReliefs.map(a => a.pensionReliefs.retirementAnnuityPayments.isDefined),
        prior.pensionReliefs.flatMap(a => a.pensionReliefs.retirementAnnuityPayments),
        prior.pensionReliefs.map(a => a.pensionReliefs.paymentToEmployersSchemeNoTaxRelief.isDefined),
        prior.pensionReliefs.flatMap(a => a.pensionReliefs.paymentToEmployersSchemeNoTaxRelief)
      ),

      //TODO: validate and amend if necessary when building the annual allowance CYA page
      PensionAnnualAllowancesViewModel(
        prior.pensionCharges.flatMap(a => a.pensionSavingsTaxCharges).map(_.isAnnualAllowanceReduced),
        prior.pensionCharges.flatMap(a => a.pensionSavingsTaxCharges).flatMap(_.moneyPurchasedAllowance),
        prior.pensionCharges.flatMap(a => a.pensionSavingsTaxCharges).flatMap(_.taperedAnnualAllowance),
        prior.pensionCharges.map(a => a.pensionContributions.isDefined),
        prior.pensionCharges.flatMap(a => a.pensionContributions).map(_.inExcessOfTheAnnualAllowance),
        prior.pensionCharges.flatMap(a => a.pensionContributions.map(x => x.annualAllowanceTaxPaid)) match {
          case Some(taxVal) if taxVal > 0 => Some(Yes.toString)
          case _ => Some(No.toString)
        },
        prior.pensionCharges.flatMap(a => a.pensionContributions).map(_.annualAllowanceTaxPaid),
        prior.pensionCharges.flatMap(a => a.pensionContributions).map(_.pensionSchemeTaxReference)
      ),

      //TODO: validate and amend if necessary when building the lifetime allowance CYA page
      pensionLifetimeAllowances = PensionLifetimeAllowancesViewModel(
        aboveLifetimeAllowanceQuestion = getAboveLifetimeAllowanceQuestion(prior),
        pensionAsLumpSumQuestion = prior.pensionCharges.flatMap(
          a => a.pensionSavingsTaxCharges).map(_.lumpSumBenefitTakenInExcessOfLifetimeAllowance.isDefined),
        pensionAsLumpSum = prior.pensionCharges.flatMap(
          a => a.pensionSavingsTaxCharges).flatMap(_.lumpSumBenefitTakenInExcessOfLifetimeAllowance),
        pensionPaidAnotherWayQuestion = prior.pensionCharges.flatMap(
          a => a.pensionSavingsTaxCharges).map(_.benefitInExcessOfLifetimeAllowance.isDefined),
        pensionPaidAnotherWay = prior.pensionCharges.flatMap(
          a => a.pensionSavingsTaxCharges).flatMap(_.benefitInExcessOfLifetimeAllowance)

      ),

      //TODO: validate as necessary on building CYA page
      incomeFromPensions = IncomeFromPensionsViewModel(
        statePension = getStatePensionModel(statePension),
        statePensionLumpSum = getStatePensionModel(statePensionLumpSum),
        //TODO: set the question below based on the list from backend
        uKPensionIncomesQuestion = Some(getUkPensionIncome(prior).nonEmpty),
        uKPensionIncomes = getUkPensionIncome(prior)
      ),

      unauthorisedPayments = UnauthorisedPaymentsViewModel(
        unauthorisedPaymentsQuestion = getUnauthorisedPaymentsQuestion(prior),
        surchargeQuestion = prior.pensionCharges.map(_.pensionSchemeUnauthorisedPayments.flatMap(_.surcharge).isDefined),
        noSurchargeQuestion = prior.pensionCharges.map(_.pensionSchemeUnauthorisedPayments.flatMap(_.noSurcharge).isDefined),
        surchargeAmount = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.flatMap(_.surcharge.map(_.amount))),
        surchargeTaxAmountQuestion = prior.pensionCharges.map(_.pensionSchemeUnauthorisedPayments.flatMap(_.surcharge.map(_.foreignTaxPaid)).isDefined),
        surchargeTaxAmount = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.flatMap(_.surcharge.map(_.foreignTaxPaid))),
        noSurchargeAmount = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.flatMap(_.noSurcharge.map(_.amount))),
        noSurchargeTaxAmountQuestion = prior.pensionCharges.map(_.pensionSchemeUnauthorisedPayments.map(_.noSurcharge.map(_.foreignTaxPaid)).isDefined),
        noSurchargeTaxAmount = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.flatMap(_.noSurcharge.map(_.foreignTaxPaid))),
        ukPensionSchemesQuestion = prior.pensionCharges.map(_.pensionSchemeUnauthorisedPayments.map(_.pensionSchemeTaxReference).isDefined),
        pensionSchemeTaxReference = prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.map(_.pensionSchemeTaxReference))
      )
    )
  }

  private def getUnauthorisedPaymentsQuestion(prior: AllPensionsData): Option[Boolean] = {
    if(prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.flatMap(_.surcharge)).isDefined ||
      prior.pensionCharges.flatMap(_.pensionSchemeUnauthorisedPayments.flatMap(_.noSurcharge)).isDefined) {
      Some(true)
    } else {
      None
    }
  }

  private def getStatePensionModel(statePension: Option[StateBenefit]): Option[StateBenefitViewModel] = {
    statePension match {
      case Some(benefit) => Some(StateBenefitViewModel(
        benefitId = Some(benefit.benefitId),
        startDateQuestion = Some(true),
        startDate = Some(benefit.startDate),
        endDateQuestion = Some(benefit.endDate.isDefined),
        endDate = benefit.endDate,
        submittedOnQuestion = Some(benefit.submittedOn.isDefined),
        submittedOn = benefit.submittedOn,
        dateIgnoredQuestion = Some(benefit.dateIgnored.isDefined),
        dateIgnored = benefit.dateIgnored,
        amountPaidQuestion = Some(benefit.amount.isDefined),
        amount = benefit.amount,
        taxPaidQuestion = Some(benefit.taxPaid.isDefined),
        taxPaid = benefit.taxPaid)
      )
      case _ => None
    }
  }

  private def getAboveLifetimeAllowanceQuestion(prior: AllPensionsData): Option[Boolean] = {
    if (prior.pensionCharges.flatMap(a => a.pensionSavingsTaxCharges).map(
      _.benefitInExcessOfLifetimeAllowance).isDefined || prior.pensionCharges.flatMap(
      a => a.pensionSavingsTaxCharges).map(_.lumpSumBenefitTakenInExcessOfLifetimeAllowance).isDefined) {
      Some(true)
    } else {
      None
    }
  }

  private def getUkPensionIncome(prior: AllPensionsData): Seq[UkPensionIncomeViewModel] = {
    prior.employmentPensions match {
      case Some(ep) =>
        ep.employmentData.map(data =>
          UkPensionIncomeViewModel(
            employmentId = Some(data.employmentId),
            pensionId = data.pensionId,
            startDate = data.startDate,
            endDate = data.endDate,
            pensionSchemeName = Some(data.pensionSchemeName),
            pensionSchemeRef = data.pensionSchemeRef,
            amount = data.amount,
            taxPaid = data.taxPaid,
            isCustomerEmploymentData = data.isCustomerEmploymentData
          )
        )
      case _ => Seq()
    }
  }

}

