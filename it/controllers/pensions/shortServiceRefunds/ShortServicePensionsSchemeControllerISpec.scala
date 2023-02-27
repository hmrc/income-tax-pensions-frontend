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

package controllers.pensions.shortServiceRefunds

import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import controllers.ControllerSpec
import controllers.ControllerSpec.UserConfig
import models.mongo.PensionsUserData
import models.pension.charges.OverseasRefundPensionScheme
import play.api.http.Status.BAD_REQUEST
import play.api.libs.ws.WSResponse

class ShortServicePensionsSchemeControllerISpec extends ControllerSpec ("/overseas-pensions/short-service-refunds/short-service-refunds-pension-scheme") {
  
  val providerNameIF = "providerName"
  val schemeRefIF = "schemeReference"
  val providerAddressIF = "providerAddress"
  val countryIF = "countryId"
  
  "This page" when { //scalastyle:off magic.number line.size.limit
     
     "requested to be shown" should {
       
       "not show the form page but redirect to the summary page" when {
         "the user has no stored session data at all" in {
           implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
           implicit val response: WSResponse = getPageWithIndex()
           assertRedirectionAsExpected(PageRelativeURLs.overseasPensionsSummary)
         }
       }
       "show the form page" when {
         "the user has relevant session data and" when {
           val sessionData = pensionsUserData(aPensionsCYAModel)
           
           for (isUKCountry <- Seq(true, false)) {
             
             s"the user submits a correct ${if (isUKCountry) "a UK" else "an Overseas"} pension scheme form with no prior pensions scheme data to update and redirects to the relevant page" in {
               val (testOverseasRefundPensionScheme, formData, ukOrOverseasAlignedSessionData) = setupTestData(isUKCountry, sessionData, hasPriorPensionsSchemeData = false)
               
               implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(ukOrOverseasAlignedSessionData))
               implicit val response: WSResponse = submitForm(formData, Map("index" -> "0"))

               val redirectPage = relativeUrl("/overseas-pensions/short-service-refunds/short-service-refunds-pension-scheme?index=0") //TODO: redirect to SSR summary page
               assertRedirectionAsExpected(redirectPage)
               
               val expectedViewModel = ukOrOverseasAlignedSessionData.pensions.shortServiceRefunds
                 .copy(refundPensionScheme = Seq(testOverseasRefundPensionScheme))
               getShortServicePensionsViewModel mustBe Some(expectedViewModel)
             }

             s"the user submits a correct ${if (isUKCountry) "a UK" else "an Overseas"} pension scheme form with prior pensions scheme data to update and redirects to the relevant page" in {
               val (testOverseasRefundPensionScheme, formData, ukOrOverseasAlignedSessionData) = setupTestData(isUKCountry, sessionData)
               
               implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(ukOrOverseasAlignedSessionData))
               implicit val response: WSResponse = submitForm(formData, Map("index" -> "0"))

               val redirectPage = relativeUrl("/overseas-pensions/short-service-refunds/short-service-refunds-pension-scheme?index=0") //TODO: redirect to SSR summary page
               assertRedirectionAsExpected(redirectPage)

               val expectedViewModel = ukOrOverseasAlignedSessionData.pensions.shortServiceRefunds
                 .copy(refundPensionScheme = Seq(testOverseasRefundPensionScheme))
               getShortServicePensionsViewModel mustBe Some(expectedViewModel)
             }
           }
           
           "the user submits an incorrect form should  and stay on the form page and produce a Bad Request response" in {
             implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
             implicit val response: WSResponse = submitForm(setFormData("","","",Some("")), Map("index" -> "0"))
             
             response must haveStatus(BAD_REQUEST)
           }
         }
       }
     }
    
     def setFormData(pName: String, tRef: String, pAddress: String, countryOpt: Option[String]): Map[String, String] =
       Map(providerNameIF -> pName, schemeRefIF -> tRef, providerAddressIF -> pAddress) ++
         countryOpt.fold(Map[String, String]())(cc => Map(countryIF -> cc))

    def setupTestData(isUKCountry: Boolean, sessionData: PensionsUserData, hasPriorPensionsSchemeData: Boolean = true): (OverseasRefundPensionScheme, Map[String, String], PensionsUserData) = {
      
      val ssrPensionSchemes = sessionData.pensions.shortServiceRefunds.refundPensionScheme

      val ssrPenScheme =
        if (isUKCountry) {
          OverseasRefundPensionScheme(ukRefundCharge = Some(true), Some("Scheme Name"),
            pensionSchemeTaxReference = Some("12345678RF"), qualifyingRecognisedOverseasPensionScheme = None, Some("Scheme Address"), alphaTwoCountryCode = None, alphaThreeCountryCode = None)
        } else {
          OverseasRefundPensionScheme(ukRefundCharge = Some(false), Some("Scheme Name"),
            pensionSchemeTaxReference = None, qualifyingRecognisedOverseasPensionScheme = Some("654321"), Some("Scheme Address"), alphaTwoCountryCode = Some("CY"), alphaThreeCountryCode = Some("CYP"))
        }
      val formData =
        if (isUKCountry) {
          setFormData("Scheme Name", "12345678RF", "Scheme Address", None)
        } else {
          setFormData("Scheme Name", "654321", "Scheme Address", Some("CY"))
        }
      
      val ukOrOverseasAlignedSessionData = {
        val alignedSsrPensionSchemes = ssrPensionSchemes.map { tcps =>
          if (hasPriorPensionsSchemeData) {
            if (isUKCountry) {
              tcps.copy(ukRefundCharge = Some(isUKCountry), qualifyingRecognisedOverseasPensionScheme = None, alphaTwoCountryCode = None, alphaThreeCountryCode = None)
            } else {
              tcps.copy(ukRefundCharge = Some(isUKCountry), pensionSchemeTaxReference = None)
            }
          } else {
            tcps.copy(ukRefundCharge = Some(isUKCountry), pensionSchemeTaxReference = None, qualifyingRecognisedOverseasPensionScheme = None, alphaTwoCountryCode = None, alphaThreeCountryCode = None)
          }
        }
        
        sessionData.copy(pensions = sessionData.pensions
          .copy(shortServiceRefunds = sessionData.pensions.shortServiceRefunds
            .copy(refundPensionScheme = alignedSsrPensionSchemes)))
      }

      (ssrPenScheme, formData, ukOrOverseasAlignedSessionData)
    }
   }

}
