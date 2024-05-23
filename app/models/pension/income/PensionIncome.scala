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

package models.pension.income

import cats.implicits.{catsSyntaxOptionId, none}
import models.IncomeTaxUserData
import models.pension.{PensionIncomeSubRequestModel, PensionRequestModel, PensionSubRequestModel}
import play.api.libs.json.{JsValue, Json, OFormat, Writes}
import utils.Constants.zero
import utils.EncryptedValue

case class ForeignPension(
    countryCode: String,
    taxableAmount: BigDecimal,
    amountBeforeTax: Option[BigDecimal],
    taxTakenOff: Option[BigDecimal],
    specialWithholdingTax: Option[BigDecimal],
    foreignTaxCreditRelief: Option[Boolean]
) extends PensionIncomeSubRequestModel {
  val isEmpty: Boolean = false
}

object ForeignPension {
  implicit val format: OFormat[ForeignPension] = Json.format[ForeignPension]

  def fromPriorData(prior: IncomeTaxUserData): Option[Seq[ForeignPension]] =
    prior.pensions.flatMap(_.pensionIncome.flatMap(_.foreignPension))
}

case class EncryptedForeignPension(
    countryCode: EncryptedValue,
    taxableAmount: EncryptedValue,
    amountBeforeTax: Option[EncryptedValue],
    taxTakenOff: Option[EncryptedValue],
    specialWithholdingTax: Option[EncryptedValue],
    foreignTaxCreditRelief: Option[EncryptedValue]
)

object EncryptedForeignPension {
  implicit val format: OFormat[EncryptedForeignPension] = Json.format[EncryptedForeignPension]
}

case class OverseasPensionContribution(
    customerReference: Option[String],
    exemptEmployersPensionContribs: BigDecimal,
    migrantMemReliefQopsRefNo: Option[String],
    dblTaxationRelief: Option[BigDecimal],
    dblTaxationCountry: Option[String],
    dblTaxationArticle: Option[String],
    dblTaxationTreaty: Option[String],
    sf74Reference: Option[String]
) extends PensionIncomeSubRequestModel {

  val isBlankSubmission: Boolean =
    // TODO
    // Maybe we do not not need to check if customer ref is empty to take part in our consideration of an empty submission.
    // If they submit customer reference, but no values, maybe this is an empty submission? Could also be that it is not.
    // We need this confirmed.
    if (customerReference.isEmpty &&
      exemptEmployersPensionContribs == zero &&
      migrantMemReliefQopsRefNo.isEmpty &&
      dblTaxationRelief.isEmpty &&
      dblTaxationCountry.isEmpty &&
      dblTaxationArticle.isEmpty &&
      dblTaxationTreaty.isEmpty &&
      sf74Reference.isEmpty) true
    else false

  val isEmpty: Boolean = false
}

object OverseasPensionContribution {
  implicit val format: OFormat[OverseasPensionContribution] = Json.format[OverseasPensionContribution]

  def fromPriorData(prior: IncomeTaxUserData): Option[Seq[OverseasPensionContribution]] =
    prior.pensions.flatMap(_.pensionIncome.flatMap(_.overseasPensionContribution))

  def blankSubmission: OverseasPensionContribution =
    OverseasPensionContribution(None, zero, None, None, None, None, None, None)
}

case class EncryptedOverseasPensionContribution(
    customerReference: Option[EncryptedValue],
    exemptEmployersPensionContribs: EncryptedValue,
    migrantMemReliefQopsRefNo: Option[EncryptedValue],
    dblTaxationRelief: Option[EncryptedValue],
    dblTaxationCountry: Option[EncryptedValue],
    dblTaxationArticle: Option[EncryptedValue],
    dblTaxationTreaty: Option[EncryptedValue],
    sf74Reference: Option[EncryptedValue]
)

object EncryptedOverseasPensionContribution {
  implicit val format: OFormat[EncryptedOverseasPensionContribution] = Json.format[EncryptedOverseasPensionContribution]
}

case class PensionIncome(
    submittedOn: String,
    deletedOn: Option[String],
    foreignPension: Option[Seq[ForeignPension]],
    overseasPensionContribution: Option[Seq[OverseasPensionContribution]]
)

object PensionIncome {
  implicit val format: OFormat[PensionIncome] = Json.format[PensionIncome]
}

case class EncryptedPensionIncome(
    submittedOn: EncryptedValue,
    deletedOn: Option[EncryptedValue],
    foreignPension: Option[Seq[EncryptedForeignPension]],
    overseasPensionContribution: Option[Seq[EncryptedOverseasPensionContribution]]
)

object EncryptedPensionIncome {
  implicit val format: OFormat[EncryptedPensionIncome] = Json.format[EncryptedPensionIncome]
}

// TODO: I think we can remove this container. I see no value in it, looks like a hack.
case class ForeignPensionContainer(foreignPensions: Seq[ForeignPension]) extends PensionIncomeSubRequestModel {
  override def isEmpty: Boolean =
    foreignPensions.isEmpty
}
// TODO: I think we can remove this container. I see no value in it, looks like a hack.
case class OverseasPensionContributionContainer(overseasPensionContributions: Seq[OverseasPensionContribution]) extends PensionIncomeSubRequestModel {
  override def isEmpty: Boolean =
    overseasPensionContributions.isEmpty
}

case class CreateUpdatePensionIncomeRequestModel(foreignPension: Option[ForeignPensionContainer],
                                                 overseasPensionContribution: Option[OverseasPensionContributionContainer])
    extends PensionRequestModel {

  def createSubModel: CreateUpdatePensionIncomeRequestModel = {
    def processModel[T <: PensionIncomeSubRequestModel](model: Option[T]): Option[T] =
      if (model.exists(_.isEmpty) || model.isEmpty) {
        None
      } else {
        model
      }

    CreateUpdatePensionIncomeRequestModel(
      foreignPension = processModel(this.foreignPension),
      overseasPensionContribution = processModel(this.overseasPensionContribution)
    )
  }

  override def otherSubRequestModelsEmpty[T <: PensionSubRequestModel](excludedModel: Option[T]): Boolean =
    excludedModel match {
      case Some(OverseasPensionContributionContainer(_)) => foreignPension.isEmpty || foreignPension.exists(_.isEmpty)
      case Some(ForeignPensionContainer(_))              => overseasPensionContribution.isEmpty || overseasPensionContribution.exists(_.isEmpty)
      case _                                             => true
    }
}
object CreateUpdatePensionIncomeRequestModel {
  implicit val writes: Writes[CreateUpdatePensionIncomeRequestModel] = new Writes[CreateUpdatePensionIncomeRequestModel] {
    override def writes(o: CreateUpdatePensionIncomeRequestModel): JsValue =
      Json.obj(
        "foreignPension"              -> o.foreignPension.map(_.foreignPensions),
        "overseasPensionContribution" -> o.overseasPensionContribution.map(_.overseasPensionContributions)
      )
  }

  def fromPriorData(prior: IncomeTaxUserData): CreateUpdatePensionIncomeRequestModel =
    CreateUpdatePensionIncomeRequestModel(
      foreignPension = ForeignPension
        .fromPriorData(prior)
        .fold(ifEmpty = none[ForeignPensionContainer])(ForeignPensionContainer(_).some),
      overseasPensionContribution = OverseasPensionContribution
        .fromPriorData(prior)
        .fold(ifEmpty = none[OverseasPensionContributionContainer])(OverseasPensionContributionContainer(_).some)
    )
}
