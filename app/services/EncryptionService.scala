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
      pensions = userData.pensions.map(encryptPension),
      lastUpdated = userData.lastUpdated
    )
  }

  def encryptPaymentsIntoPension(p: PaymentsIntoPensionViewModel)(implicit textAndKey: TextAndKey): EncryptedPaymentsIntoPensionViewModel = {
    EncryptedPaymentsIntoPensionViewModel(
      rasPensionPaymentQuestion = p.rasPensionPaymentQuestion.map(secureGCMCipher.encrypt),
      totalRASPaymentsAndTaxReliefAnswer = p.totalRASPaymentsAndTaxReliefAnswer.map(secureGCMCipher.encrypt),
      retirementAnnuityContractPaymentsQuestion = p.retirementAnnuityContractPaymentsQuestion.map(secureGCMCipher.encrypt),
      totalRetirementAnnuityContractPayments = p.totalRetirementAnnuityContractPayments.map(secureGCMCipher.encrypt),
      workplacePensionPaymentsQuestion = p.workplacePensionPaymentsQuestion.map(secureGCMCipher.encrypt),
      totalWorkplacePensionPayments = p.totalWorkplacePensionPayments.map(secureGCMCipher.encrypt)
    )
  }


  private def encryptPension(pension: PensionsCYAModel)(implicit textAndKey: TextAndKey): EncryptedPensionCYAModel = {
    EncryptedPensionCYAModel(
      encryptedPaymentsIntoPension = pension.paymentsIntoPension.map(encryptPaymentsIntoPension)
    )
  }


  private def decryptPaymentsIntoPensionViewModel(p: EncryptedPaymentsIntoPensionViewModel)(implicit textAndKey: TextAndKey): PaymentsIntoPensionViewModel = {
    PaymentsIntoPensionViewModel(
      rasPensionPaymentQuestion = p.rasPensionPaymentQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      totalRASPaymentsAndTaxReliefAnswer = p.totalRASPaymentsAndTaxReliefAnswer.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      retirementAnnuityContractPaymentsQuestion = p.retirementAnnuityContractPaymentsQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      totalRetirementAnnuityContractPayments = p.totalRetirementAnnuityContractPayments.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce)),
      workplacePensionPaymentsQuestion = p.workplacePensionPaymentsQuestion.map(x => secureGCMCipher.decrypt[Boolean](x.value, x.nonce)),
      totalWorkplacePensionPayments = p.totalWorkplacePensionPayments.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce))
    )
  }

  private def decryptPensions(pension: EncryptedPensionCYAModel)(implicit textAndKey: TextAndKey): PensionsCYAModel = {
    PensionsCYAModel(
      paymentsIntoPension = pension.encryptedPaymentsIntoPension.map(decryptPaymentsIntoPensionViewModel)
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
      pensions = userData.pensions.map(decryptPensions),
      lastUpdated = userData.lastUpdated
    )
  }
}
