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

package models.mongo

import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status.INTERNAL_SERVER_ERROR

trait ServiceError {
  val message: String

  def toAPIErrorModel: APIErrorModel =
    APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.genericError(message))
}

trait DatabaseError extends ServiceError {
  override val message: String

  override def toAPIErrorModel: APIErrorModel =
    APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.dbError(message))

}

case object DataNotUpdated extends DatabaseError {
  override val message: String = "User data was not updated due to mongo exception"
}

case object DataNotFound extends DatabaseError {
  override val message: String = "User data could not be found due to mongo exception"
}

case object SessionNotFound extends DatabaseError {
  override val message: String = "No user session data could be found"
}

case class MongoError(error: String) extends DatabaseError {
  override val message: String = s"Mongo exception occurred. Exception: $error"
}

case class EncryptionDecryptionError(error: String) extends DatabaseError {
  override val message: String = s"Encryption / Decryption exception occurred. Exception: $error"
}
