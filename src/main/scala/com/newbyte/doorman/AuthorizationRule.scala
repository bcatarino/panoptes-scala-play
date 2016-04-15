package com.newbyte.doorman

import com.newbyte.doorman.model.Subject
import play.api.mvc.RequestHeader

trait AuthorizationRule {
  def applyRule(request: RequestHeader, Subject: Option[Subject]): Boolean
}

case class allow() extends AuthorizationRule {
  override def applyRule(request: RequestHeader, Subject: Option[Subject]) = true
}

case class withRole(role: String) extends AuthorizationRule {
  override def applyRule(request: RequestHeader, Subject: Option[Subject]): Boolean = {
    Subject match {
      case None => false
      case Some(user) =>
        user.getRoles.exists(userRole => userRole.getName.equals(role))
    }
  }
}

case class userPresent() extends AuthorizationRule {
  override def applyRule(request: RequestHeader, Subject: Option[Subject]) = Subject.isDefined
}

case class userNotPresent() extends AuthorizationRule {
  override def applyRule(request: RequestHeader, Subject: Option[Subject]) = Subject.isEmpty
}

case class withPermission(permission: String) extends AuthorizationRule {
  override def applyRule(request: RequestHeader, Subject: Option[Subject]): Boolean = {
    //TODO later
    false
  }
}

case class all(authorizationRules: AuthorizationRule*) extends AuthorizationRule {
  override def applyRule(request: RequestHeader, Subject: Option[Subject]): Boolean = {
    authorizationRules.map(_.applyRule(request, Subject)).reduce(_ && _)
  }
}

case class atLeastOne(authorizationRules: AuthorizationRule*) extends AuthorizationRule {
  override def applyRule(request: RequestHeader, Subject: Option[Subject]): Boolean = {
    authorizationRules.map(_.applyRule(request, Subject)).reduce(_ || _)
  }
}
