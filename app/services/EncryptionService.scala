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

package services

import config.AppConfig
import models.mongo._
import models.pension.charges.{EncryptedPensionAnnualAllowancesViewModel, PensionAnnualAllowancesViewModel}
import models.pension.reliefs.{EncryptedPaymentsIntoPensionViewModel, PaymentsIntoPensionsViewModel}
import utils.AesGCMCrypto

import javax.inject.Inject

class EncryptionService @Inject() (aesGCMCrypto: AesGCMCrypto, appConfig: AppConfig) {

  def encryptUserData(userData: PensionsUserData): EncryptedPensionsUserData = {
    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId, appConfig.encryptionKey)

    EncryptedPensionsUserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      isPriorSubmission = userData.isPriorSubmission,
      pensions = encryptPension(userData.pensions),
      lastUpdated = userData.lastUpdated
    )
  }

  private def encryptPaymentsIntoPension(p: PaymentsIntoPensionsViewModel)(implicit textAndKey: TextAndKey): EncryptedPaymentsIntoPensionViewModel =
    EncryptedPaymentsIntoPensionViewModel(
      rasPensionPaymentQuestion = p.rasPensionPaymentQuestion.map(aesGCMCrypto.encrypt),
      totalRASPaymentsAndTaxRelief = p.totalRASPaymentsAndTaxRelief.map(aesGCMCrypto.encrypt),
      oneOffRasPaymentPlusTaxReliefQuestion = p.oneOffRasPaymentPlusTaxReliefQuestion.map(aesGCMCrypto.encrypt),
      totalOneOffRasPaymentPlusTaxRelief = p.totalOneOffRasPaymentPlusTaxRelief.map(aesGCMCrypto.encrypt),
      totalPaymentsIntoRASQuestion = p.totalPaymentsIntoRASQuestion.map(aesGCMCrypto.encrypt),
      pensionTaxReliefNotClaimedQuestion = p.pensionTaxReliefNotClaimedQuestion.map(aesGCMCrypto.encrypt),
      retirementAnnuityContractPaymentsQuestion = p.retirementAnnuityContractPaymentsQuestion.map(aesGCMCrypto.encrypt),
      totalRetirementAnnuityContractPayments = p.totalRetirementAnnuityContractPayments.map(aesGCMCrypto.encrypt),
      workplacePensionPaymentsQuestion = p.workplacePensionPaymentsQuestion.map(aesGCMCrypto.encrypt),
      totalWorkplacePensionPayments = p.totalWorkplacePensionPayments.map(aesGCMCrypto.encrypt)
    )

  private def encryptedPensionAnnualAllowances(p: PensionAnnualAllowancesViewModel)(implicit
      textAndKey: TextAndKey): EncryptedPensionAnnualAllowancesViewModel =
    EncryptedPensionAnnualAllowancesViewModel(
      reducedAnnualAllowanceQuestion = p.reducedAnnualAllowanceQuestion.map(aesGCMCrypto.encrypt),
      moneyPurchaseAnnualAllowance = p.moneyPurchaseAnnualAllowance.map(aesGCMCrypto.encrypt),
      taperedAnnualAllowance = p.taperedAnnualAllowance.map(aesGCMCrypto.encrypt),
      aboveAnnualAllowanceQuestion = p.aboveAnnualAllowanceQuestion.map(aesGCMCrypto.encrypt),
      aboveAnnualAllowance = p.aboveAnnualAllowance.map(aesGCMCrypto.encrypt),
      pensionProvidePaidAnnualAllowanceQuestion = p.pensionProvidePaidAnnualAllowanceQuestion.map(aesGCMCrypto.encrypt),
      taxPaidByPensionProvider = p.taxPaidByPensionProvider.map(aesGCMCrypto.encrypt),
      pensionSchemeTaxReferences = p.pensionSchemeTaxReferences.map(_.map(aesGCMCrypto.encrypt))
    )

  private def encryptPension(pension: PensionsCYAModel)(implicit textAndKey: TextAndKey): EncryptedPensionCYAModel = {
    // TODO: temporary implicit - this service will be removed completely go once all models updated for new encryption
    implicit val aesGCMCrypto: AesGCMCrypto = this.aesGCMCrypto
    EncryptedPensionCYAModel(
      encryptedPaymentsIntoPension = encryptPaymentsIntoPension(pension.paymentsIntoPension),
      encryptedPensionAnnualAllowances = encryptedPensionAnnualAllowances(pension.pensionsAnnualAllowances),
      incomeFromPensions = pension.incomeFromPensions.encrypted(),
      unauthorisedPayments = pension.unauthorisedPayments.encrypted(),
      paymentsIntoOverseasPensions = pension.paymentsIntoOverseasPensions.encrypted(),
      incomeFromOverseasPensions = pension.incomeFromOverseasPensions.encrypted(),
      transfersIntoOverseasPensions = pension.transfersIntoOverseasPensions.encrypted(),
      shortServiceRefunds = pension.shortServiceRefunds.encrypted()
    )
  }

  private def decryptPaymentsIntoPensionViewModel(p: EncryptedPaymentsIntoPensionViewModel)(implicit
      textAndKey: TextAndKey): PaymentsIntoPensionsViewModel =
    PaymentsIntoPensionsViewModel(
      rasPensionPaymentQuestion = p.rasPensionPaymentQuestion.map(x => aesGCMCrypto.decrypt[Boolean](x.value, x.nonce)),
      totalRASPaymentsAndTaxRelief = p.totalRASPaymentsAndTaxRelief.map(x => aesGCMCrypto.decrypt[BigDecimal](x.value, x.nonce)),
      oneOffRasPaymentPlusTaxReliefQuestion = p.oneOffRasPaymentPlusTaxReliefQuestion.map(x => aesGCMCrypto.decrypt[Boolean](x.value, x.nonce)),
      totalOneOffRasPaymentPlusTaxRelief = p.totalOneOffRasPaymentPlusTaxRelief.map(x => aesGCMCrypto.decrypt[BigDecimal](x.value, x.nonce)),
      totalPaymentsIntoRASQuestion = p.totalPaymentsIntoRASQuestion.map(x => aesGCMCrypto.decrypt[Boolean](x.value, x.nonce)),
      pensionTaxReliefNotClaimedQuestion = p.pensionTaxReliefNotClaimedQuestion.map(x => aesGCMCrypto.decrypt[Boolean](x.value, x.nonce)),
      retirementAnnuityContractPaymentsQuestion =
        p.retirementAnnuityContractPaymentsQuestion.map(x => aesGCMCrypto.decrypt[Boolean](x.value, x.nonce)),
      totalRetirementAnnuityContractPayments = p.totalRetirementAnnuityContractPayments.map(x => aesGCMCrypto.decrypt[BigDecimal](x.value, x.nonce)),
      workplacePensionPaymentsQuestion = p.workplacePensionPaymentsQuestion.map(x => aesGCMCrypto.decrypt[Boolean](x.value, x.nonce)),
      totalWorkplacePensionPayments = p.totalWorkplacePensionPayments.map(x => aesGCMCrypto.decrypt[BigDecimal](x.value, x.nonce))
    )

  private def decryptPensionAnnualAllowanceViewModel(p: EncryptedPensionAnnualAllowancesViewModel)(implicit
      textAndKey: TextAndKey): PensionAnnualAllowancesViewModel =
    PensionAnnualAllowancesViewModel(
      reducedAnnualAllowanceQuestion = p.reducedAnnualAllowanceQuestion.map(x => aesGCMCrypto.decrypt[Boolean](x.value, x.nonce)),
      moneyPurchaseAnnualAllowance = p.moneyPurchaseAnnualAllowance.map(x => aesGCMCrypto.decrypt[Boolean](x.value, x.nonce)),
      taperedAnnualAllowance = p.taperedAnnualAllowance.map(x => aesGCMCrypto.decrypt[Boolean](x.value, x.nonce)),
      aboveAnnualAllowanceQuestion = p.aboveAnnualAllowanceQuestion.map(x => aesGCMCrypto.decrypt[Boolean](x.value, x.nonce)),
      aboveAnnualAllowance = p.aboveAnnualAllowance.map(x => aesGCMCrypto.decrypt[BigDecimal](x.value, x.nonce)),
      pensionProvidePaidAnnualAllowanceQuestion =
        p.pensionProvidePaidAnnualAllowanceQuestion.map(x => aesGCMCrypto.decrypt[Boolean](x.value, x.nonce)),
      taxPaidByPensionProvider = p.taxPaidByPensionProvider.map(x => aesGCMCrypto.decrypt[BigDecimal](x.value, x.nonce)),
      pensionSchemeTaxReferences = p.pensionSchemeTaxReferences.map(_.map(x => aesGCMCrypto.decrypt[String](x.value, x.nonce)))
    )

  private def decryptPensions(pension: EncryptedPensionCYAModel)(implicit textAndKey: TextAndKey): PensionsCYAModel = {

    // TODO: temporary implicit - this service will be removed completely go once all models updated for new encryption
    implicit val aesGCMCrypto: AesGCMCrypto = this.aesGCMCrypto
    PensionsCYAModel(
      paymentsIntoPension = decryptPaymentsIntoPensionViewModel(pension.encryptedPaymentsIntoPension),
      pensionsAnnualAllowances = decryptPensionAnnualAllowanceViewModel(pension.encryptedPensionAnnualAllowances),
      incomeFromPensions = pension.incomeFromPensions.decrypted(),
      unauthorisedPayments = pension.unauthorisedPayments.decrypted(),
      paymentsIntoOverseasPensions = pension.paymentsIntoOverseasPensions.decrypted(),
      incomeFromOverseasPensions = pension.incomeFromOverseasPensions.decrypted(),
      transfersIntoOverseasPensions = pension.transfersIntoOverseasPensions.decrypted(),
      shortServiceRefunds = pension.shortServiceRefunds.decrypted()
    )
  }

  def decryptUserData(userData: EncryptedPensionsUserData): PensionsUserData = {
    implicit val textAndKey: TextAndKey = TextAndKey(userData.mtdItId, appConfig.encryptionKey)
    PensionsUserData(
      sessionId = userData.sessionId,
      mtdItId = userData.mtdItId,
      nino = userData.nino,
      taxYear = userData.taxYear,
      isPriorSubmission = userData.isPriorSubmission,
      pensions = decryptPensions(userData.pensions),
      lastUpdated = userData.lastUpdated
    )
  }
}
