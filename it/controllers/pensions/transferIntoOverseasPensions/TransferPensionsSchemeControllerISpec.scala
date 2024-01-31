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

package controllers.pensions.transferIntoOverseasPensions

import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import controllers.ControllerSpec
import controllers.ControllerSpec.UserConfig
import models.mongo.PensionsUserData
import models.pension.charges.{TransferPensionScheme, TransfersIntoOverseasPensionsViewModel}
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.ws.WSResponse

class TransferPensionsSchemeControllerISpec
    extends ControllerSpec("/overseas-pensions/overseas-transfer-charges/overseas-transfer-charge-pension-scheme") {

  val providerNameIF    = "providerName"
  val schemeRefIF       = "schemeReference"
  val providerAddressIF = "providerAddress"
  val countryIF         = "countryId"

  "This page" when { // scalastyle:off magic.number line.size.limit

    "requested to be shown" should {

      "redirect to the summary page" which {
        "the user has no stored session data at all" in {
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(None)
          implicit val response: WSResponse   = getPageWithIndex()
          assertRedirectionAsExpected(PageRelativeURLs.pensionsSummaryPage)
        }
      }

      "redirect to first page of journey" when {
        "previous question has not been answered" in {
          val incompleteCYAModel = aPensionsCYAModel.copy(
            transfersIntoOverseasPensions =
              TransfersIntoOverseasPensionsViewModel(pensionSchemeTransferCharge = Some(false), pensionSchemeTransferChargeAmount = None))
          val sessionData                     = pensionsUserData(incompleteCYAModel)
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
          implicit val response: WSResponse   = getPageWithIndex()

          assertRedirectionAsExpected(PageRelativeURLs.transferPensionSavings)
        }
      }

      "show the form page when the user has relevant session data" which {
        val sessionData = pensionsUserData(aPensionsCYAModel)

        for (isUKCountry <- Seq(true, false)) {

          val (testTransferPensionScheme, formData, ukOrOverseasAlignedSessionData) =
            setupTestData(isUKCountry, sessionData, hasPriorPensionsSchemeData = false)
          implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(ukOrOverseasAlignedSessionData))

          val ukOrOverseas = if (isUKCountry) "UK" else "Overseas"

          s"the user shows a page with $ukOrOverseas pension scheme" which {
            "has an OK status" in {
              implicit val response: WSResponse = getPageWithIndex()
              response.status mustBe OK
            }
          }

          s"the user submits a correct $ukOrOverseas pension scheme form with NO prior pensions scheme data" which {
            "gets updated and redirects to the relevant page" in {

              implicit val response: WSResponse = submitForm(formData, Map("index" -> "0"))

              val redirectPage = relativeUrl("/overseas-pensions/overseas-transfer-charges/transfer-charges-summary")
              assertRedirectionAsExpected(redirectPage)

              val expectedViewModel =
                ukOrOverseasAlignedSessionData.pensions.transfersIntoOverseasPensions.copy(transferPensionScheme = Seq(testTransferPensionScheme))
              getTransferPensionsViewModel mustBe Some(expectedViewModel)
            }
          }

          s"the user submits a correct $ukOrOverseas pension scheme form with prior pensions scheme data" which {
            "gets updated and redirects to the relevant page" in {
              val (testTransferPensionScheme, formData, ukOrOverseasAlignedSessionData) = setupTestData(isUKCountry, sessionData)

              implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(ukOrOverseasAlignedSessionData))
              implicit val response: WSResponse   = submitForm(formData, Map("index" -> "0"))

              val redirectPage = relativeUrl("/overseas-pensions/overseas-transfer-charges/transfer-charges-summary")
              assertRedirectionAsExpected(redirectPage)

              val expectedViewModel =
                ukOrOverseasAlignedSessionData.pensions.transfersIntoOverseasPensions.copy(transferPensionScheme = Seq(testTransferPensionScheme))
              getTransferPensionsViewModel mustBe Some(expectedViewModel)
            }
          }
        }

        "the user submits an incorrect form" which {
          "stays on the form page and produces a Bad Request response" in {
            implicit val userConfig: UserConfig = userConfigWhenIrrelevant(Some(sessionData))
            implicit val response: WSResponse   = submitForm(setFormData("", "", "", Some("")), Map("index" -> "0"))
            response must haveStatus(BAD_REQUEST)
          }
        }
      }
    }

    def setFormData(pName: String, tRef: String, pAddress: String, countryOpt: Option[String]): Map[String, String] =
      Map(providerNameIF -> pName, schemeRefIF -> tRef, providerAddressIF -> pAddress) ++
        countryOpt.fold(Map[String, String]())(cc => Map(countryIF -> cc))

    def setupTestData(isUKCountry: Boolean,
                      sessionData: PensionsUserData,
                      hasPriorPensionsSchemeData: Boolean = true): (TransferPensionScheme, Map[String, String], PensionsUserData) = {

      val tcPensionScheme = sessionData.pensions.transfersIntoOverseasPensions.transferPensionScheme.filter(_.ukTransferCharge.get)

      val transferPenScheme =
        if (isUKCountry) {
          TransferPensionScheme(
            ukTransferCharge = Some(true),
            Some("Scheme Name"),
            pstr = Some("12345678RF"),
            qops = None,
            Some("Scheme Address"),
            alphaTwoCountryCode = None,
            alphaThreeCountryCode = None
          )
        } else {
          TransferPensionScheme(
            ukTransferCharge = Some(false),
            Some("Scheme Name"),
            pstr = None,
            qops = Some("654321"),
            Some("Scheme Address"),
            if (hasPriorPensionsSchemeData) Some("CY") else None,
            alphaThreeCountryCode = Some("CYP")
          )
        }
      val formData =
        if (isUKCountry) {
          setFormData("Scheme Name", "12345678RF", "Scheme Address", None)
        } else {
          setFormData("Scheme Name", "654321", "Scheme Address", Some("CY"))
        }

      val ukOrOverseasAlignedSessionData = {
        val alignedTCPensionSchemes = tcPensionScheme.map { tcps =>
          if (hasPriorPensionsSchemeData) {
            if (isUKCountry) {
              tcps.copy(ukTransferCharge = Some(isUKCountry), qops = None, alphaTwoCountryCode = None, alphaThreeCountryCode = None)
            } else {
              tcps.copy(ukTransferCharge = Some(isUKCountry), pstr = None, alphaTwoCountryCode = Some("CY"))
            }
          } else {
            tcps.copy(ukTransferCharge = Some(isUKCountry), pstr = None, qops = None, alphaTwoCountryCode = None, alphaThreeCountryCode = None)
          }
        }

        sessionData.copy(pensions = sessionData.pensions
          .copy(transfersIntoOverseasPensions = sessionData.pensions.transfersIntoOverseasPensions
            .copy(transferPensionScheme = alignedTCPensionSchemes)))
      }

      (transferPenScheme, formData, ukOrOverseasAlignedSessionData)
    }
  }

}
