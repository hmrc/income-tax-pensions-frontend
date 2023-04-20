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

package controllers.pensions.paymentsIntoOverseasPensions

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PaymentsIntoOverseasPensionsViewModelBuilder.aPaymentsIntoOverseasPensionsViewModel
import builders.PensionsCYAModelBuilder._
import builders.PensionsUserDataBuilder
import builders.PensionsUserDataBuilder.pensionUserDataWithOnlyOverseasPensions
import builders.UserBuilder.{aUser, aUserRequest}
import forms.PensionCustomerReferenceNumberForm
import models.mongo.{PensionsCYAModel, PensionsUserData}
import models.pension.charges.Relief
import models.pension.charges.TaxReliefQuestion.TransitionalCorrespondingRelief
import org.scalatest.BeforeAndAfterEach
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoOverseasPensions._
import utils.PageUrls._
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class PensionsCustomerReferenceNumberControllerISpec extends IntegrationTest with ViewHelpers with BeforeAndAfterEach with PensionsDatabaseHelper {

  override val userScenarios: Seq[UserScenario[_,_]] = Seq.empty
  val inputName: String = "pensionsCustomerReferenceNumberId"
  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel, isPrior: Boolean = false): PensionsUserData = {
    PensionsUserDataBuilder.aPensionsUserData.copy(
      isPriorSubmission = isPrior,
      pensions = pensionsCyaModel
    )
  }

  val noCrnRelief = Relief(reliefType = Some(TransitionalCorrespondingRelief),
    customerReference = None,
    employerPaymentsAmount = Some(1999.99),
    qopsReference = None,
    alphaTwoCountryCode = None,
    alphaThreeCountryCode = None,
    doubleTaxationCountryArticle = None,
    doubleTaxationCountryTreaty = None,
    doubleTaxationReliefAmount = None,
    sf74Reference = Some("SF74-123456")
  )
  val someCrnRelief = Relief(reliefType = Some(TransitionalCorrespondingRelief),
    customerReference = Some("PENSIONSINCOME480"),
    employerPaymentsAmount = Some(1999.99),
    qopsReference = None,
    alphaTwoCountryCode = None,
    alphaThreeCountryCode = None,
    doubleTaxationCountryArticle = None,
    doubleTaxationCountryTreaty = None,
    doubleTaxationReliefAmount = None,
    sf74Reference = Some("SF74-123456")
  )

  ".show" should {

    "return Ok response with empty Customer Reference Number page" when {
      "index is None" in {
        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          insertCyaData(pensionsUsersData(aPensionsCYAEmptyModel), aUserRequest)
          urlGet(fullUrl(pensionCustomerReferenceNumberUrl(taxYearEOY, None)), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
          )
        }

        result.status shouldBe OK
        result.body.contains("""value="""")
        result.body.contains("/pensions-customer-reference-number")
      }
      "customer reference number is None" in {
        lazy implicit val result: WSResponse = {
          dropPensionsDB()
          authoriseAgentOrIndividual(aUser.isAgent)
          val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(reliefs =  Seq(noCrnRelief, noCrnRelief, noCrnRelief))
          val pensionCYAModel = aPensionsCYAModel.copy(paymentsIntoOverseasPensions = pensionsViewModel)
          insertCyaData(pensionsUsersData(pensionCYAModel), aUserRequest)
          urlGet(fullUrl(pensionCustomerReferenceNumberUrl(taxYearEOY, Some(1))), follow = false,
            headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
          )
        }

        result.status shouldBe OK
        result.body.contains("""value="""")
        result.body.contains("/pensions-customer-reference-number?index=1")
      }
    }

    "return Ok with Customer Reference Number page when user data exists" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel), aUserRequest)
        userDataStub(anIncomeTaxUserData.copy(pensions = Some(anAllPensionsData)), nino, taxYearEOY)
        urlGet(fullUrl(pensionCustomerReferenceNumberUrl(taxYearEOY, Some(1))), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      result.status shouldBe OK
      result.body.contains("""value="PENSIONINCOME245""")
      result.body.contains("/pensions-customer-reference-number?index=1")
    }
    
    "Redirect to the Customer Reference page if index is invalid and there are No pension schemes" in {
      val pensionsNoSchemesViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(reliefs = Seq())
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionUserDataWithOnlyOverseasPensions(pensionsNoSchemesViewModel),  aUserRequest)
        urlGet(fullUrl(pensionCustomerReferenceNumberUrl(taxYearEOY, Some(3))), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe SEE_OTHER
      result.header("location").head shouldBe pensionCustomerReferenceNumberUrl(taxYearEOY, None)
    }

    "Redirect to the pension scheme summary page if index is invalid and there are pension schemes" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel, isPrior = true), aUserRequest)
        urlGet(fullUrl(pensionCustomerReferenceNumberUrl(taxYearEOY, Some(-1))), follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe SEE_OTHER
      result.header("location") shouldBe Some(pensionReliefSchemeSummaryUrl(taxYearEOY))
    }
  }

  ".submit" should {

    "Redirect to CRN page and add to user relief data when user submits a valid CRN with no prior data" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val form: Map[String, String] = Map(PensionCustomerReferenceNumberForm.pensionsCustomerReferenceNumberId -> "PENSIONSINCOME25")
        insertCyaData(pensionsUsersData(aPensionsCYAEmptyModel), aUserRequest)
        urlPost(fullUrl(pensionCustomerReferenceNumberUrl(taxYearEOY, None)), body = form,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").head shouldBe untaxedEmployerPaymentsUrl(taxYearEOY, 0)

      lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
      cyaModel.pensions.paymentsIntoOverseasPensions.reliefs.head.customerReference.get shouldBe "PENSIONSINCOME25"
    }

    "Redirect to CRN page and update user data reliefs when user submits a valid CRN with prior data" in {
      implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        val pensionsViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(reliefs = Seq(someCrnRelief, someCrnRelief))
        insertCyaData(pensionsUsersData(aPensionsCYAModel.copy(paymentsIntoOverseasPensions = pensionsViewModel)), aUserRequest)
        val form: Map[String, String] = Map(PensionCustomerReferenceNumberForm.pensionsCustomerReferenceNumberId -> "PENSIONSINCOME24")
        urlPost(fullUrl(pensionCustomerReferenceNumberUrl(taxYearEOY, Some(1))), body = form,
          follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
      }

      result.status shouldBe SEE_OTHER
      result.header("location").head shouldBe untaxedEmployerPaymentsUrl(taxYearEOY, 1)

      lazy val cyaModel = findCyaData(taxYearEOY, aUserRequest).get
      cyaModel.pensions.paymentsIntoOverseasPensions.reliefs(1).customerReference.get shouldBe "PENSIONSINCOME24"
    }

    "return BadRequest error when an empty CRN value is submitted" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        lazy val form: Map[String, String] = Map(PensionCustomerReferenceNumberForm.pensionsCustomerReferenceNumberId -> "")
        insertCyaData(pensionsUsersData(aPensionsCYAModel), aUserRequest)
        urlPost(
          fullUrl(pensionCustomerReferenceNumberUrl(taxYearEOY, Some(0))),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }

      result.status shouldBe BAD_REQUEST
    }

    "redirect to the Customer Reference page if index is invalid and there are No pension schemes" in {
      val pensionsNoSchemesViewModel = aPaymentsIntoOverseasPensionsViewModel.copy(reliefs = Seq())
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        lazy val form: Map[String, String] = Map(PensionCustomerReferenceNumberForm.pensionsCustomerReferenceNumberId -> "")
        insertCyaData(pensionUserDataWithOnlyOverseasPensions(pensionsNoSchemesViewModel),  aUserRequest)
        urlPost(
          fullUrl(pensionCustomerReferenceNumberUrl(taxYearEOY, Some(-1))),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe SEE_OTHER
      result.header("location") .head shouldBe pensionCustomerReferenceNumberUrl(taxYearEOY, None)
    }
    
    "Redirect to the pension scheme summary page if index is invalid and there are pension schemes" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        lazy val form: Map[String, String] = Map(PensionCustomerReferenceNumberForm.pensionsCustomerReferenceNumberId -> "")
        insertCyaData(pensionsUsersData(aPensionsCYAModel), aUserRequest)
        urlPost(
          fullUrl(pensionCustomerReferenceNumberUrl(taxYearEOY, Some(-1))),
          body = form,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe SEE_OTHER
      result.header("location") .head shouldBe pensionReliefSchemeSummaryUrl(taxYearEOY)
    }
  }
}