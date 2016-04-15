package com.newbyte.doorman

import javax.inject.Inject

import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

final class SecurityFilter @Inject()(authorizationHandler: AuthorizationHandler) extends Filter {

  authorizationHandler.config

  override def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
    request.headers.get(authorizationHandler.authHeaderName) match {
      case None =>
        authorizationHandler.getClosestMatch(request) match {
          case None => next(request)
          case _ => Future(authorizationHandler.authHeaderNotPresentAction(request))
        }
      case Some(sessionId) =>
        authorizationHandler.getUser(sessionId) match {
          case None => Future(authorizationHandler.userNotAllowedStatus)
          case userData =>
            if (authorizationHandler.isAllowed(request, userData)) {
              next(request)
            } else {
              Future(authorizationHandler.userNotAllowedStatus)
            }
        }
    }
  }
}
