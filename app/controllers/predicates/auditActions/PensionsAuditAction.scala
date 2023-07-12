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

package controllers.predicates.auditActions

import models.audit.PaymentsIntoPensionsAudit
import models.requests.{UserPriorAndSessionDataRequest, UserSessionDataRequest}
import play.api.mvc.{ActionFilter, Result}
import services.AuditService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait PensionsAuditAction extends FrontendHeaderCarrierProvider {
  
  def auditService: AuditService
  
  def ec: ExecutionContext

  protected[auditActions] def executionContext: ExecutionContext = ec
}

object PensionsAuditAction {
  
  case class PaymentsIntoPensionsViewAuditAction @Inject()(auditService: AuditService)(implicit val ec: ExecutionContext)
    extends ActionFilter[UserSessionDataRequest] with PensionsAuditAction {

    override protected[auditActions] def filter[A](req: UserSessionDataRequest[A]): Future[Option[Result]] = Future.successful {
      val auditModel = PaymentsIntoPensionsAudit.standardAudit(req.user, req.pensionsUserData)
      auditService.sendAudit(auditModel.toAuditModelView)(hc(req.request), ec, PaymentsIntoPensionsAudit.writes)
      None
    }
  }

  case class PaymentsIntoPensionsUpdateAuditAction @Inject()(auditService: AuditService)(implicit val ec: ExecutionContext)
    extends ActionFilter[UserPriorAndSessionDataRequest] with PensionsAuditAction {

    override protected[auditActions] def filter[A](req: UserPriorAndSessionDataRequest[A]): Future[Option[Result]] = Future.successful {
      val toAuditModel = {
        val auditModel = PaymentsIntoPensionsAudit.amendAudit(
          req.user, req.pensionsUserData, req.pensions)
        if (req.pensions.isEmpty) auditModel.toAuditModelCreate _ else auditModel.toAuditModelAmend _
      }
      auditService.sendAudit(toAuditModel())(hc(req.request), ec, PaymentsIntoPensionsAudit.writes)
      None
    }
  }
  
  
}
