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

import config.AppConfig
import models.mongo._
import models.pension.charges.{EncryptedPensionAnnualAllowancesViewModel, PensionAnnualAllowancesViewModel, PensionLifetimeAllowancesViewModel}
import models.pension.reliefs.{EncryptedPaymentsIntoPensionViewModel, PaymentsIntoPensionViewModel}
import utils.SecureGCMCipher

import javax.inject.Inject

class EncryptionService @Inject()(secureGCMCipher: SecureGCMCipher, appConfig: AppConfig) {


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

  private def encryptPaymentsIntoPension(p: PaymentsIntoPensionViewModel)(implicit textAndKey: TextAndKey): EncryptedPaymentsIntoPensionViewModel = {
    EncryptedPaymentsIntoPensionViewModel(
      gateway = p.gateway.map(secureGCMCipher.encrypt),
      rasPensionPaymentQuestion = p.rasPensionPaymentQuestion.map(secureGCMCipher.encrypt),
      totalRASPaymentsAndTaxRelief = p.totalRASPaymentsAndTaxRelief.map(secureGCMCipher.encrypt),
      oneOffRasPaymentPlusTaxReliefQuestion = p.oneOffRasPaymentPlusTaxReliefQuestion.map(secureGCMCipher.encrypt),
      totalOneOffRasPaymentPlusTaxRelief = p.totalOneOffRasPaymentPlusTaxRelief.map(secureGCMCipher.encrypt),
      totalPaymentsIntoRASQuestion = p.totalPaymentsIntoRASQuestion.map(secureGCMCipher.encrypt),
      pensionTaxReliefNotClaimedQuestion = p.pensionTaxReliefNotClaimedQuestion.map(secureGCMCipher.encrypt),
      retirementAnnuityContractPaymentsQuestion = p.retirementAnnuityContractPaymentsQuestion.map(secureGCMCipher.encrypt),
      totalRetirementAnnuityContractPayments = p.totalRetirementAnnuityContractPayments.map(secureGCMCipher.encrypt),
      workplacePensionPaymentsQuestion = p.workplacePensionPaymentsQuestion.map(secureGCMCipher.encrypt),
      totalWorkplacePensionPayments = p.totalWorkplacePensionPayments.map(secureGCMCipher.encrypt)
    )
  }

  private def encryptedPensionAnnualAllowances(p: PensionAnnualAllowancesViewModel)
                                              (implicit textAndKey: TextAndKey): EncryptedPensionAnnualAllowancesViewModel = {

    EncryptedPensionAnnualAllowancesViewModel(
      reducedAnnualAllowanceQuestion = p.reducedAnnualAllowanceQuestion.map(secureGCMCipher.encrypt),
      moneyPurchaseAnnualAllowance = p.moneyPurchaseAnnualAllowance.map(secureGCMCipher.encrypt),
      taperedAnnualAllowance = p.taperedAnnualAllowance.map(secureGCMCipher.encrypt),
      aboveAnnualAllowanceQuestion = p.aboveAnnualAllowanceQuestion.map(secureGCMCipher.encrypt),
      aboveAnnualAllowance = p.aboveAnnualAllowance.map(secureGCMCipher.encrypt),
      pensionProvidePaidAnnualAllowanceQuestion = p.pensionProvidePaidAnnualAllowanceQuestion.map(secureGCMCipher.encrypt),
      taxPaidByPensionProvider = p.taxPaidByPensionProvider.map(secureGCMCipher.encrypt),
      pensionSchemeTaxReferences = p.pensionSchemeTaxReferences.map(_.map(secureGCMCipher.encrypt))
    )
  }

  private def encryptPension(pension: PensionsCYAModel)(implicit textAndKey: TextAndKey): EncryptedPensionCYAModel = {
    //TODO: temporary implicit - this service will be removed completely go once all models updated for new encryption
    implicit val secureGCMCipher: SecureGCMCipher = this.secureGCMCipher
    EncryptedPensionCYAModel(
      encryptedPaymentsIntoPension = encryptPaymentsIntoPension(pension.paymentsIntoPension),
      encryptedPensionAnnualAllowances = encryptedPensionAnnualAllowances(pension.pensionsAnnualAllowances),
      pensionLifetimeAllowances = pension.pensionLifetimeAllowances.encrypted(),
      incomeFromPensions = pension.incomeFromPensions.encrypted(),
      unauthorisedPayments = pension.unauthorisedPayments.encrypted(),
      paymentsIntoOverseasPensions = pension.paymentsIntoOverseasPensions.encrypted(),
      incomeFromOverseasPensionsViewModel = pension.incomeFromOverseasPensionsViewModel.encrypted()
    )
  }

  private def decryptPaymentsIntoPensionViewModel(p: EncryptedPaymentsIntoPensionViewModel)(implicit textAndKey: TextAndKey): PaymentsIntoPensionViewModel = {
    PaymentsIntoPensionViewModel(
      gateway = p.gateway.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      rasPensionPaymentQuestion = p.rasPensionPaymentQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      totalRASPaymentsAndTaxRelief = p.totalRASPaymentsAndTaxRelief.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      oneOffRasPaymentPlusTaxReliefQuestion = p.oneOffRasPaymentPlusTaxReliefQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      totalOneOffRasPaymentPlusTaxRelief = p.totalOneOffRasPaymentPlusTaxRelief.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      totalPaymentsIntoRASQuestion = p.totalPaymentsIntoRASQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      pensionTaxReliefNotClaimedQuestion = p.pensionTaxReliefNotClaimedQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      retirementAnnuityContractPaymentsQuestion = p.retirementAnnuityContractPaymentsQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      totalRetirementAnnuityContractPayments = p.totalRetirementAnnuityContractPayments.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      workplacePensionPaymentsQuestion = p.workplacePensionPaymentsQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      totalWorkplacePensionPayments = p.totalWorkplacePensionPayments.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce))
    )
  }

  private def decryptPensionAnnualAllowanceViewModel(p: EncryptedPensionAnnualAllowancesViewModel)
                                                    (implicit textAndKey: TextAndKey): PensionAnnualAllowancesViewModel = {
    PensionAnnualAllowancesViewModel(
      reducedAnnualAllowanceQuestion = p.reducedAnnualAllowanceQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      moneyPurchaseAnnualAllowance = p.moneyPurchaseAnnualAllowance.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      taperedAnnualAllowance = p.taperedAnnualAllowance.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      aboveAnnualAllowanceQuestion = p.aboveAnnualAllowanceQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      aboveAnnualAllowance = p.aboveAnnualAllowance.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      pensionProvidePaidAnnualAllowanceQuestion = p.pensionProvidePaidAnnualAllowanceQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      taxPaidByPensionProvider = p.taxPaidByPensionProvider.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      pensionSchemeTaxReferences = p.pensionSchemeTaxReferences.map(_.map(x => secureGCMCipher.decrypt[String](x.value, x.nonce)))

    )
  }

  private def decryptPensions(pension: EncryptedPensionCYAModel)(implicit textAndKey: TextAndKey): PensionsCYAModel = {

    //TODO: temporary implicit - this service will be removed completely go once all models updated for new encryption
    implicit val secureGCMCipher: SecureGCMCipher = this.secureGCMCipher
    PensionsCYAModel(
      paymentsIntoPension = decryptPaymentsIntoPensionViewModel(pension.encryptedPaymentsIntoPension),
      pensionsAnnualAllowances = decryptPensionAnnualAllowanceViewModel(pension.encryptedPensionAnnualAllowances),
      pensionLifetimeAllowances = pension.pensionLifetimeAllowances.decrypted(),
      incomeFromPensions = pension.incomeFromPensions.decrypted(),
      unauthorisedPayments = pension.unauthorisedPayments.decrypted(),
      paymentsIntoOverseasPensions = pension.paymentsIntoOverseasPensions.decrypted(),
      incomeFromOverseasPensionsViewModel = pension.incomeFromOverseasPensionsViewModel.decrypted()
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
