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

import models.User
import models.mongo.PensionsUserData
import org.mongodb.scala.bson.collection.immutable.Document
import repositories.PensionsUserDataRepositoryImpl

trait PensionsDatabaseHelper {
  self: IntegrationTest =>

  lazy val pensionsDatabase: PensionsUserDataRepositoryImpl = app.injector.instanceOf[PensionsUserDataRepositoryImpl]

  def dropPensionsDB(): Unit = {
    await(pensionsDatabase.collection.drop().toFutureOption())
    await(pensionsDatabase.ensureIndexes)
  }

  def insertCyaData(cya: PensionsUserData, user: User[_]): Unit = {
    await(pensionsDatabase.createOrUpdate(cya)(user))
  }

  def findCyaData(taxYear: Int, user: User[_]): Option[PensionsUserData] = {
    await(pensionsDatabase.find(taxYear)(user).map {
      case Left(_) => None
      case Right(value) => value
    })
  }

}
