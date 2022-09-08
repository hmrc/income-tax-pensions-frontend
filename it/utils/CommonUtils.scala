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

package utils

import builders.AllPensionsDataBuilder.anAllPensionsData
import builders.IncomeTaxUserDataBuilder.anIncomeTaxUserData
import builders.PensionsUserDataBuilder.pensionsUserDataWithUnauthorisedPayments
import builders.UserBuilder.aUserRequest
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import models.IncomeTaxUserData
import models.mongo.PensionsUserData
import models.pension.charges.UnauthorisedPaymentsViewModel
import play.api.http.HeaderNames
import play.api.libs.ws.WSResponse
import utils.PageUrls.UnAuthorisedPayments.noSurchargeAmountUrl
import utils.PageUrls.{fullUrl, pensionSummaryUrl}

trait CommonUtils extends IntegrationTest with ViewHelpers with PensionsDatabaseHelper {

  def showPage[A, B](user: UserScenario[A, B],
                     pensionsUserData: PensionsUserData)
                    (implicit url: Int => String): WSResponse = {
    lazy val result: WSResponse = {
      dropPensionsDB()
      authoriseAgentOrIndividual(user.isAgent)
      insertCyaData(pensionsUserData, aUserRequest)
      urlGet(fullUrl(url(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }
    result
  }

  def showPage[A, B](user: UserScenario[A, B],
                     pensionsUserData: PensionsUserData,
                     userData: IncomeTaxUserData)
                    (implicit url: Int => String): WSResponse = {
    lazy val result: WSResponse = {
      dropPensionsDB()
      authoriseAgentOrIndividual(user.isAgent)
      insertCyaData(pensionsUserData, aUserRequest)
      userDataStub(userData, nino, taxYearEOY)
      urlGet(fullUrl(url(taxYearEOY)), user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }
    result
  }

  def showPage(pensionsUserData: PensionsUserData)
              (implicit url: Int => String): WSResponse = {
    lazy val result: WSResponse = {
      dropPensionsDB()
      authoriseAgentOrIndividual(isAgent = false)
      insertCyaData(pensionsUserData, aUserRequest)
      urlGet(fullUrl(url(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }
    result
  }

  def showUnauthorisedPage[A,B](userScenario: UserScenario[A, B])
                               (implicit url: Int => String): WSResponse = {
    lazy val result: WSResponse = {
      unauthorisedAgentOrIndividual(userScenario.isAgent)
      urlGet(fullUrl(url(taxYear)), welsh = userScenario.isWelsh,
        headers = Seq(Predef.ArrowAssoc(HeaderNames.COOKIE) -> playSessionCookies(taxYear, validTaxYearList)))
    }
    result
  }

  def submitPage[A, B](user: UserScenario[A, B],
                       pensionsUserData: PensionsUserData,
                       form: Map[String, String])
                      (implicit url: Int => String): WSResponse = {
    val result: WSResponse = {
      dropPensionsDB()
      insertCyaData(pensionsUserData, aUserRequest)
      authoriseAgentOrIndividual(user.isAgent)
      urlPost(fullUrl(url(taxYearEOY)), body = form, welsh = user.isWelsh, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }

    result
  }

  def submitPage(pensionsUserData: PensionsUserData,
                 form: Map[String, String])
                (implicit url: Int => String): WSResponse = {
    val result: WSResponse = {
      dropPensionsDB()
      insertCyaData(pensionsUserData, aUserRequest)
      authoriseAgentOrIndividual(isAgent = false)
      urlPost(fullUrl(url(taxYearEOY)), body = form, follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }

    result
  }

  def submitPageNoSessionData(form: Map[String, String])(implicit url: Int => String): WSResponse = {
    val result: WSResponse = {
      dropPensionsDB()
      authoriseAgentOrIndividual(isAgent = false)
      urlPost(fullUrl(url(taxYearEOY)), body = form, follow = false,
        headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }
    result
  }

  def getResponseNoSessionData(implicit url: Int => String): WSResponse = {
    val result: WSResponse = {
      dropPensionsDB()
      authoriseAgentOrIndividual(isAgent = false)
      urlGet(fullUrl(url(taxYearEOY)), follow = false, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList)))
    }
    result
  }
}
