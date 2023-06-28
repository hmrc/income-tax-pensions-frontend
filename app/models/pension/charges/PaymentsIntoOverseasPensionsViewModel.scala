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

package models.pension.charges

import models.mongo.TextAndKey
import models.pension.PensionCYABaseModel
import models.pension.income.OverseasPensionContribution
import play.api.libs.json.{Json, OFormat}
import utils.DecryptableSyntax.DecryptableOps
import utils.DecryptorInstances.{bigDecimalDecryptor, booleanDecryptor, stringDecryptor}
import utils.EncryptableSyntax.EncryptableOps
import utils.EncryptorInstances.{bigDecimalEncryptor, booleanEncryptor, stringEncryptor}
import utils.{EncryptedValue, SecureGCMCipher}

object TaxReliefQuestion {
  val NoTaxRelief = "No tax relief"
  val TransitionalCorrespondingRelief = "Transitional corresponding relief"
  val DoubleTaxationRelief = "Double taxation relief"
  val MigrantMemberRelief = "Migrant member relief"

  def validTaxList: Seq[String] = Seq(MigrantMemberRelief, DoubleTaxationRelief, TransitionalCorrespondingRelief, NoTaxRelief)
}


case class Relief(
                   customerReference: Option[String] = None,
                   employerPaymentsAmount: Option[BigDecimal] = None,
                   reliefType: Option[String] = None,
                   alphaTwoCountryCode: Option[String] = None,
                   alphaThreeCountryCode: Option[String] = None,
                   doubleTaxationArticle: Option[String] = None,
                   doubleTaxationTreaty: Option[String] = None,
                   doubleTaxationReliefAmount: Option[BigDecimal] = None,
                   qopsReference: Option[String] = None,
                   sf74Reference: Option[String] = None
                 ) {

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedRelief =
    EncryptedRelief(
      customerReference = customerReference.map(_.encrypted),
      employerPaymentsAmount = employerPaymentsAmount.map(_.encrypted),
      reliefType = reliefType.map(_.encrypted),
      sf74Reference = sf74Reference.map(_.encrypted),
      alphaTwoCountryCode = alphaTwoCountryCode.map(_.encrypted),
      alphaThreeCountryCode = alphaThreeCountryCode.map(_.encrypted),
      doubleTaxationArticle = doubleTaxationArticle.map(_.encrypted),
      doubleTaxationTreaty = doubleTaxationTreaty.map(_.encrypted),
      doubleTaxationReliefAmount = doubleTaxationReliefAmount.map(_.encrypted),
      qualifyingOverseasPensionSchemeReferenceNumber = qopsReference.map(_.encrypted)
    )
}

object Relief {
  implicit val format: OFormat[Relief] = Json.format[Relief]
}

case class EncryptedRelief(
                            customerReference: Option[EncryptedValue] = None,
                            employerPaymentsAmount: Option[EncryptedValue] = None,
                            reliefType: Option[EncryptedValue] = None,
                            alphaTwoCountryCode: Option[EncryptedValue] = None,
                            alphaThreeCountryCode: Option[EncryptedValue] = None,
                            doubleTaxationArticle: Option[EncryptedValue] = None,
                            doubleTaxationTreaty: Option[EncryptedValue] = None,
                            doubleTaxationReliefAmount: Option[EncryptedValue] = None,
                            sf74Reference: Option[EncryptedValue] = None,
                            qualifyingOverseasPensionSchemeReferenceNumber: Option[EncryptedValue] = None
                          ) {
  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): Relief =
    Relief(
      customerReference = customerReference.map(_.decrypted[String]),
      employerPaymentsAmount = employerPaymentsAmount.map(_.decrypted[BigDecimal]),
      reliefType = reliefType.map(_.decrypted[String]),
      sf74Reference = sf74Reference.map(_.decrypted[String]),
      alphaTwoCountryCode = alphaTwoCountryCode.map(_.decrypted[String]),
      alphaThreeCountryCode = alphaThreeCountryCode.map(_.decrypted[String]),
      doubleTaxationArticle = doubleTaxationArticle.map(_.decrypted[String]),
      doubleTaxationTreaty = doubleTaxationTreaty.map(_.decrypted[String]),
      doubleTaxationReliefAmount = doubleTaxationReliefAmount.map(_.decrypted[BigDecimal]),
      qopsReference = qualifyingOverseasPensionSchemeReferenceNumber.map(_.decrypted[String])
    )
}

object EncryptedRelief {
  implicit val format: OFormat[EncryptedRelief] = Json.format[EncryptedRelief]
}


case class PaymentsIntoOverseasPensionsViewModel(paymentsIntoOverseasPensionsQuestions: Option[Boolean] = None,
                                                 paymentsIntoOverseasPensionsAmount: Option[BigDecimal] = None,
                                                 employerPaymentsQuestion: Option[Boolean] = None,
                                                 taxPaidOnEmployerPaymentsQuestion: Option[Boolean] = None,
                                                 reliefs: Seq[Relief] = Seq.empty[Relief]
                                                ) extends PensionCYABaseModel {

  def isEmpty: Boolean = paymentsIntoOverseasPensionsQuestions.isEmpty && paymentsIntoOverseasPensionsAmount.isEmpty &&
    employerPaymentsQuestion.isEmpty && taxPaidOnEmployerPaymentsQuestion.isEmpty && reliefs.isEmpty

  def isFinished: Boolean = {
    paymentsIntoOverseasPensionsQuestions.exists {
      if (_) {
        paymentsIntoOverseasPensionsAmount.isDefined &&
          employerPaymentsQuestion.exists {
            if (_) {
              taxPaidOnEmployerPaymentsQuestion.exists { x =>
                if (x) x
                else reliefs.map(reliefIsCompleted(_)).forall(x => x)
              }
            } else true
          }
      } else true
    }
  }

  def journeyIsNo: Boolean =
    (!paymentsIntoOverseasPensionsQuestions.getOrElse(true)
      && paymentsIntoOverseasPensionsAmount.isEmpty
      && employerPaymentsQuestion.isEmpty
      && taxPaidOnEmployerPaymentsQuestion.isEmpty
      && reliefs.isEmpty)

  def journeyIsUnanswered: Boolean = this.productIterator.forall(_ == None)

  private def reliefIsCompleted(relief: Relief): Boolean = {
    relief.customerReference.isDefined &&
      relief.employerPaymentsAmount.isDefined &&
      relief.reliefType.exists { reliefType =>
        reliefType match {
          case TaxReliefQuestion.MigrantMemberRelief => relief.qopsReference.isDefined
          case TaxReliefQuestion.DoubleTaxationRelief =>
            relief.alphaTwoCountryCode.isDefined &&
              relief.alphaThreeCountryCode.isDefined &&
              relief.doubleTaxationReliefAmount.isDefined
          case TaxReliefQuestion.TransitionalCorrespondingRelief => relief.sf74Reference.isDefined
          case TaxReliefQuestion.NoTaxRelief => true
          case _ => false
        }
      }
  }

  def toPensionContributions: Seq[OverseasPensionContribution] = {
    reliefs.map {
      relief =>
        OverseasPensionContribution(
          customerReference = relief.customerReference,
          exemptEmployersPensionContribs = relief.employerPaymentsAmount.getOrElse(0),
          migrantMemReliefQopsRefNo = relief.qopsReference,
          dblTaxationRelief = relief.doubleTaxationReliefAmount,
          dblTaxationCountry = relief.alphaThreeCountryCode,
          dblTaxationArticle = relief.doubleTaxationArticle,
          dblTaxationTreaty = relief.doubleTaxationTreaty,
          sf74Reference = relief.sf74Reference
        )
    }
  }

  def encrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): EncryptedPaymentsIntoOverseasPensionsViewModel =
    EncryptedPaymentsIntoOverseasPensionsViewModel(
      paymentsIntoOverseasPensionsQuestions = paymentsIntoOverseasPensionsQuestions.map(_.encrypted),
      paymentsIntoOverseasPensionsAmount = paymentsIntoOverseasPensionsAmount.map(_.encrypted),
      employerPaymentsQuestion = employerPaymentsQuestion.map(_.encrypted),
      taxPaidOnEmployerPaymentsQuestion = taxPaidOnEmployerPaymentsQuestion.map(_.encrypted),
      reliefs = reliefs.map(_.encrypted())
    )
}


object PaymentsIntoOverseasPensionsViewModel {
  implicit val format: OFormat[PaymentsIntoOverseasPensionsViewModel] = Json.format[PaymentsIntoOverseasPensionsViewModel]
}


case class EncryptedPaymentsIntoOverseasPensionsViewModel(paymentsIntoOverseasPensionsQuestions: Option[EncryptedValue] = None,
                                                          paymentsIntoOverseasPensionsAmount: Option[EncryptedValue] = None,
                                                          employerPaymentsQuestion: Option[EncryptedValue] = None,
                                                          taxPaidOnEmployerPaymentsQuestion: Option[EncryptedValue] = None,
                                                          customerReference: Option[EncryptedValue] = None,
                                                          employerPaymentsAmount: Option[EncryptedValue] = None,
                                                          reliefs: Seq[EncryptedRelief] = Seq.empty[EncryptedRelief]
                                                         ) {

  def decrypted()(implicit secureGCMCipher: SecureGCMCipher, textAndKey: TextAndKey): PaymentsIntoOverseasPensionsViewModel =
    PaymentsIntoOverseasPensionsViewModel(
      paymentsIntoOverseasPensionsQuestions = paymentsIntoOverseasPensionsQuestions.map(_.decrypted[Boolean]),
      paymentsIntoOverseasPensionsAmount = paymentsIntoOverseasPensionsAmount.map(_.decrypted[BigDecimal]),
      employerPaymentsQuestion = employerPaymentsQuestion.map(_.decrypted[Boolean]),
      taxPaidOnEmployerPaymentsQuestion = taxPaidOnEmployerPaymentsQuestion.map(_.decrypted[Boolean]),
      reliefs = reliefs.map(_.decrypted())
    )
}

object EncryptedPaymentsIntoOverseasPensionsViewModel {
  implicit val format: OFormat[EncryptedPaymentsIntoOverseasPensionsViewModel] = Json.format[EncryptedPaymentsIntoOverseasPensionsViewModel]
}

