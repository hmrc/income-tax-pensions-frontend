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

import javax.inject.Inject
import models.mongo.{EncryptedPensionCYAModel, EncryptedPensionsUserData, PensionsCYAModel, PensionsUserData, TextAndKey}
import models.pension.charges.{EncryptedPensionChargesViewModel, PensionChargesViewModel}
import models.pension.reliefs.{EncryptedPensionReliefsViewModel, PensionReliefsViewModel}
import models.pension.statebenefits.{EncryptedStateBenefitsViewModel, StateBenefitsViewModel}
import utils.SecureGCMCipher

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

  def encryptPensionReliefsViewModel(p: PensionReliefsViewModel)(implicit textAndKey: TextAndKey): EncryptedPensionReliefsViewModel = {
    EncryptedPensionReliefsViewModel(
      question1 = p.question1.map(secureGCMCipher.encrypt),
      answer1 = p.answer1.map(secureGCMCipher.encrypt)
    )
  }

  def encryptPensionChargesViewModel(p: PensionChargesViewModel)(implicit textAndKey: TextAndKey): EncryptedPensionChargesViewModel = {
    EncryptedPensionChargesViewModel(
      question1 = p.question1.map(secureGCMCipher.encrypt),
      answer1 = p.answer1.map(secureGCMCipher.encrypt)
    )
  }

  def encryptStateBenefitsViewModel(p: StateBenefitsViewModel)(implicit textAndKey: TextAndKey): EncryptedStateBenefitsViewModel = {
    EncryptedStateBenefitsViewModel(
      question1 = p.question1.map(secureGCMCipher.encrypt),
      answer1 = p.answer1.map(secureGCMCipher.encrypt)
    )
  }

  private def encryptPension(pension: PensionsCYAModel)(implicit textAndKey: TextAndKey): EncryptedPensionCYAModel = {
    EncryptedPensionCYAModel(
      pensionReliefsViewModel = pension.pensionReliefsViewModel.map(encryptPensionReliefsViewModel),
      pensionChargesViewModel = pension.pensionChargesViewModel.map(encryptPensionChargesViewModel),
      stateBenefitsViewModel = pension.stateBenefitsViewModel.map(encryptStateBenefitsViewModel)
    )
  }


  private def decryptPensionReliefsViewModel(p: EncryptedPensionReliefsViewModel)(implicit textAndKey: TextAndKey): PensionReliefsViewModel = {
    PensionReliefsViewModel(
      question1 = p.question1.map(x => secureGCMCipher.decrypt[String](x.value, x.nonce)),
      answer1 = p.answer1.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce))
    )
  }

  private def decryptPensionChargesViewModel(p: EncryptedPensionChargesViewModel)(implicit textAndKey: TextAndKey): PensionChargesViewModel = {
    PensionChargesViewModel(
      question1 = p.question1.map(x => secureGCMCipher.decrypt[String](x.value, x.nonce)),
      answer1 = p.answer1.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce))
    )
  }

  private def decryptStateBenefitsViewModel(p: EncryptedStateBenefitsViewModel)(implicit textAndKey: TextAndKey): StateBenefitsViewModel = {
    StateBenefitsViewModel(
      question1 = p.question1.map(x => secureGCMCipher.decrypt[String](x.value, x.nonce)),
      answer1 = p.answer1.map(x => secureGCMCipher.decrypt[BigDecimal](x.value, x.nonce))
    )
  }

  private def decryptPensions(pension: EncryptedPensionCYAModel)(implicit textAndKey: TextAndKey): PensionsCYAModel = {
    PensionsCYAModel(
      pensionReliefsViewModel = pension.pensionReliefsViewModel.map(decryptPensionReliefsViewModel),
      pensionChargesViewModel = pension.pensionChargesViewModel.map(decryptPensionChargesViewModel),
      stateBenefitsViewModel = pension.stateBenefitsViewModel.map(decryptStateBenefitsViewModel)
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
