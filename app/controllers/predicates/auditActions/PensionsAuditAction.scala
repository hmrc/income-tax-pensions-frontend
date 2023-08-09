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

import models.audit._
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
        if (req.pensions.isEmpty) () => auditModel.toAuditModelCreate
        else () => auditModel.toAuditModelAmend
      }
      auditService.sendAudit(toAuditModel())(hc(req.request), ec, PaymentsIntoPensionsAudit.writes)
      None
    }
  }

  case class UnauthorisedPaymentsViewAuditAction @Inject()(auditService: AuditService)(implicit val ec: ExecutionContext)
    extends ActionFilter[UserSessionDataRequest] with PensionsAuditAction {

    override protected[auditActions] def filter[A](req: UserSessionDataRequest[A]): Future[Option[Result]] = Future.successful {
      val auditModel = UnauthorisedPaymentsAudit.standardAudit(req.user, req.pensionsUserData)
      auditService.sendAudit(auditModel.toAuditModelView)(hc(req.request), ec, UnauthorisedPaymentsAudit.writes)
      None
    }
  }

  case class UnauthorisedPaymentsUpdateAuditAction @Inject()(auditService: AuditService)(implicit val ec: ExecutionContext)
    extends ActionFilter[UserPriorAndSessionDataRequest] with PensionsAuditAction {

    override protected[auditActions] def filter[A](req: UserPriorAndSessionDataRequest[A]): Future[Option[Result]] = Future.successful {
      val toAuditModel = {
        val auditModel = UnauthorisedPaymentsAudit.amendAudit(
          req.user, req.pensionsUserData, req.pensions)
        if (req.pensions.isEmpty) () => auditModel.toAuditModelCreate
        else () => auditModel.toAuditModelAmend
      }
      auditService.sendAudit(toAuditModel())(hc(req.request), ec, UnauthorisedPaymentsAudit.writes)
      None
    }
  }

  case class IncomeFromOverseasPensionsViewAuditAction @Inject()(auditService: AuditService)(implicit val ec: ExecutionContext)
    extends ActionFilter[UserSessionDataRequest] with PensionsAuditAction {

    override protected[auditActions] def filter[A](req: UserSessionDataRequest[A]): Future[Option[Result]] = {
      val auditModel = IncomeFromOverseasPensionsAudit.standardAudit(req.user, req.pensionsUserData)
      auditService.sendAudit(auditModel.toAuditModelView)(hc(req.request), ec, IncomeFromOverseasPensionsAudit.writes)
      Future.successful(None)
    }
  }

  case class IncomeFromOverseasPensionsUpdateAuditAction @Inject()(auditService: AuditService)(implicit val ec: ExecutionContext)
    extends ActionFilter[UserPriorAndSessionDataRequest] with PensionsAuditAction {
    override protected[auditActions] def filter[A](req: UserPriorAndSessionDataRequest[A]): Future[Option[Result]] = {
      val toAuditModel = {
        val auditModel = IncomeFromOverseasPensionsAudit.amendAudit(
          req.user, req.pensionsUserData, req.pensions)
        if (req.pensions.isEmpty) {
          () => auditModel.toAuditModelCreate
        } else {
          () => auditModel.toAuditModelAmend
        }
      }
      auditService.sendAudit(toAuditModel())(hc(req.request), ec, IncomeFromOverseasPensionsAudit.writes)
      Future.successful(None)
    }
  }

  case class PaymentsIntoOverseasPensionsViewAuditAction @Inject()(auditService: AuditService)(implicit val ec: ExecutionContext)
    extends ActionFilter[UserSessionDataRequest] with PensionsAuditAction {

    override protected[auditActions] def filter[A](req: UserSessionDataRequest[A]): Future[Option[Result]] = {
      val auditModel = PaymentsIntoOverseasPensionsAudit.standardAudit(req.user, req.pensionsUserData)
      auditService.sendAudit(auditModel.toAuditModelView)(hc(req.request), ec, PaymentsIntoOverseasPensionsAudit.writes)
      Future.successful(None)
    }
  }

  case class PaymentsIntoOverseasPensionsUpdateAuditAction @Inject()(auditService: AuditService)(implicit val ec: ExecutionContext)
    extends ActionFilter[UserPriorAndSessionDataRequest] with PensionsAuditAction {

    override protected[auditActions] def filter[A](req: UserPriorAndSessionDataRequest[A]): Future[Option[Result]] = {
      val toAuditModel = {
        val auditModel = PaymentsIntoOverseasPensionsAudit.amendAudit(
          req.user, req.pensionsUserData, req.pensions)
        if (req.pensions.isEmpty) {
          () => auditModel.toAuditModelCreate
        } else {
          () => auditModel.toAuditModelAmend
        }
      }
      auditService.sendAudit(toAuditModel())(hc(req.request), ec, PaymentsIntoOverseasPensionsAudit.writes)
      Future.successful(None)
    }
  }

  case class ShortServiceRefundsViewAuditAction @Inject()(auditService: AuditService)(implicit val ec: ExecutionContext)
    extends ActionFilter[UserSessionDataRequest] with PensionsAuditAction {

    override protected[auditActions] def filter[A](req: UserSessionDataRequest[A]): Future[Option[Result]] = {
      val auditModel = ShortServiceRefundsAudit.standardAudit(req.user, req.pensionsUserData)
      auditService.sendAudit(auditModel.toAuditModelView)(hc(req.request), ec, ShortServiceRefundsAudit.writes)
      Future.successful(None)
    }
  }

  case class ShortServiceRefundsUpdateAuditAction @Inject()(auditService: AuditService)(implicit val ec: ExecutionContext)
    extends ActionFilter[UserPriorAndSessionDataRequest] with PensionsAuditAction {

    override protected[auditActions] def filter[A](req: UserPriorAndSessionDataRequest[A]): Future[Option[Result]] = {
      val toAuditModel = {
        val auditModel = ShortServiceRefundsAudit.amendAudit(
          req.user, req.pensionsUserData, req.pensions)
        if (req.pensions.isEmpty) {
          () => auditModel.toAuditModelCreate
        } else {
          () => auditModel.toAuditModelAmend
        }
      }
      auditService.sendAudit(toAuditModel())(hc(req.request), ec, ShortServiceRefundsAudit.writes)
      Future.successful(None)
    }
  }

  case class IncomeFromStatePensionsViewAuditAction @Inject()(auditService: AuditService)(implicit val ec: ExecutionContext)
    extends ActionFilter[UserSessionDataRequest] with PensionsAuditAction {

    override protected[auditActions] def filter[A](req: UserSessionDataRequest[A]): Future[Option[Result]] = {
      val auditModel = IncomeFromStatePensionsAudit.standardAudit(req.user, req.pensionsUserData)
      auditService.sendAudit(auditModel.toAuditModelView)(hc(req.request), ec, IncomeFromStatePensionsAudit.writes)
      Future.successful(None)
    }
  }

  case class IncomeFromStatePensionsUpdateAuditAction @Inject()(auditService: AuditService)(implicit val ec: ExecutionContext)
    extends ActionFilter[UserPriorAndSessionDataRequest] with PensionsAuditAction {

    override protected[auditActions] def filter[A](req: UserPriorAndSessionDataRequest[A]): Future[Option[Result]] = {
      val toAuditModel = {
        val auditModel = IncomeFromStatePensionsAudit.amendAudit(
          req.user, req.pensionsUserData, req.pensions)
        if (req.pensions.isEmpty) {
          () => auditModel.toAuditModelCreate
        } else {
          () => auditModel.toAuditModelAmend
        }
      }
      auditService.sendAudit(toAuditModel())(hc(req.request), ec, IncomeFromStatePensionsAudit.writes)
      Future.successful(None)
    }
  }

  case class UkPensionIncomeViewAuditAction @Inject()(auditService: AuditService)(implicit val ec: ExecutionContext)
    extends ActionFilter[UserSessionDataRequest] with PensionsAuditAction {

    override protected[auditActions] def filter[A](req: UserSessionDataRequest[A]): Future[Option[Result]] = {
      val auditModel = UkPensionIncomeAudit.standardAudit(req.user, req.pensionsUserData)
      auditService.sendAudit(auditModel.toAuditModelView)(hc(req.request), ec, UkPensionIncomeAudit.writes)
      Future.successful(None)
    }
  }

  case class UkPensionIncomeUpdateAuditAction @Inject()(auditService: AuditService)(implicit val ec: ExecutionContext)
    extends ActionFilter[UserPriorAndSessionDataRequest] with PensionsAuditAction {
    override protected[auditActions] def filter[A](req: UserPriorAndSessionDataRequest[A]): Future[Option[Result]] = {
      val toAuditModel = {
        val auditModel = UkPensionIncomeAudit.amendAudit(
          req.user, req.pensionsUserData, req.pensions)
        if (req.pensions.isEmpty) {
          () => auditModel.toAuditModelCreate
        } else {
          () => auditModel.toAuditModelAmend
        }
      }
      auditService.sendAudit(toAuditModel())(hc(req.request), ec, UkPensionIncomeAudit.writes)
      Future.successful(None)
    }
  }

  case class AnnualAllowancesViewAuditAction @Inject()(auditService: AuditService)(implicit val ec: ExecutionContext)
    extends ActionFilter[UserSessionDataRequest] with PensionsAuditAction {

    override protected[auditActions] def filter[A](req: UserSessionDataRequest[A]): Future[Option[Result]] = {
      val auditModel = AnnualAllowancesAudit.standardAudit(req.user, req.pensionsUserData)
      auditService.sendAudit(auditModel.toAuditModelView)(hc(req.request), ec, AnnualAllowancesAudit.writes)
      Future.successful(None)
    }
  }

  case class AnnualAllowancesUpdateAuditAction @Inject()(auditService: AuditService)(implicit val ec: ExecutionContext)
    extends ActionFilter[UserPriorAndSessionDataRequest] with PensionsAuditAction {

    override protected[auditActions] def filter[A](req: UserPriorAndSessionDataRequest[A]): Future[Option[Result]] = {
      val toAuditModel = {
        val auditModel = AnnualAllowancesAudit.amendAudit(
          req.user, req.pensionsUserData, req.pensions)
        if (req.pensions.isEmpty) {
          () => auditModel.toAuditModelCreate
        } else {
          () => auditModel.toAuditModelAmend
        }
      }
      auditService.sendAudit(toAuditModel())(hc(req.request), ec, AnnualAllowancesAudit.writes)
      Future.successful(None)
    }
  }

  case class LifetimeAllowancesViewAuditAction @Inject()(auditService: AuditService)(implicit val ec: ExecutionContext)
    extends ActionFilter[UserSessionDataRequest] with PensionsAuditAction {

    override protected[auditActions] def filter[A](req: UserSessionDataRequest[A]): Future[Option[Result]] = {
      val auditModel = LifetimeAllowancesAudit.standardAudit(req.user, req.pensionsUserData)
      auditService.sendAudit(auditModel.toAuditModelView)(hc(req.request), ec, LifetimeAllowancesAudit.writes)
      Future.successful(None)
    }
  }

  case class LifetimeAllowancesUpdateAuditAction @Inject()(auditService: AuditService)(implicit val ec: ExecutionContext)
    extends ActionFilter[UserPriorAndSessionDataRequest] with PensionsAuditAction {
    override protected[auditActions] def filter[A](req: UserPriorAndSessionDataRequest[A]): Future[Option[Result]] = {
      val toAuditModel = {
        val auditModel = LifetimeAllowancesAudit.amendAudit(
          req.user, req.pensionsUserData, req.pensions)
        if (req.pensions.isEmpty) {
          () => auditModel.toAuditModelCreate
        } else {
          () => auditModel.toAuditModelAmend
        }
      }
      auditService.sendAudit(toAuditModel())(hc(req.request), ec, LifetimeAllowancesAudit.writes)
      Future.successful(None)
    }
  }

}
