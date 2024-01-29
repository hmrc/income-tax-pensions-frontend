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

import builders.PensionsCYAModelBuilder.aPensionsCYAModel
import builders.PensionsUserDataBuilder
import builders.UserBuilder.aUser
import models.mongo.{PensionsCYAModel, PensionsUserData}
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.ws.WSResponse
import utils.PageUrls.PaymentIntoOverseasPensions.pensionReliefSchemeSummaryUrl
import utils.PageUrls.fullUrl
import utils.{IntegrationTest, PensionsDatabaseHelper, ViewHelpers}

class ReliefSchemeSummaryControllerISpec extends IntegrationTest with ViewHelpers with PensionsDatabaseHelper {

  private def pensionsUsersData(pensionsCyaModel: PensionsCYAModel): PensionsUserData =
    PensionsUserDataBuilder.aPensionsUserData.copy(isPriorSubmission = false, pensions = pensionsCyaModel)

  ".show" should {

    "show page when EOY" in {
      lazy implicit val result: WSResponse = {
        dropPensionsDB()
        authoriseAgentOrIndividual(aUser.isAgent)
        insertCyaData(pensionsUsersData(aPensionsCYAModel))
        urlGet(
          fullUrl(pensionReliefSchemeSummaryUrl(taxYearEOY)),
          !aUser.isAgent,
          follow = false,
          headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYearEOY, validTaxYearList))
        )
      }
      result.status shouldBe OK
    }
  }
  override val userScenarios: Seq[UserScenario[_, _]] = Nil
}
