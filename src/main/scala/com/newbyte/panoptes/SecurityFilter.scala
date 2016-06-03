package com.newbyte.panoptes

import javax.inject.Inject

import play.api.mvc.{Filter, RequestHeader, Result}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

final class SecurityFilter @Inject()(authorizationHandlers: AuthorizationHandlers) extends Filter {

  override def apply(next: (RequestHeader) => Future[Result])(request: RequestHeader): Future[Result] = {
    applyHandler(authorizationHandlers.handlers.iterator, request, next)
  }

  private def applyHandler(iterator: Iterator[AuthorizationHandler], request: RequestHeader, next: (RequestHeader) => Future[Result]): Future[Result] = {
    val authorizationHandler = iterator.next()

    authorizationHandler.getClosestMatch(request) match {
      case None => nextHandlerOrAuthorized(iterator, request, next)
      case Some(rule) =>
        request.headers.get(authorizationHandler.authHeaderName) match {
          case None => authorizationHandler.authHeaderNotPresentAction(request, next)
          case Some(sessionId) =>
            authorizationHandler.getUser(sessionId) match {
              case None => Future(authorizationHandler.userNotAllowedStatus)
              case userData =>
                if (rule.applyRule(request, userData)) {
                  next(request)
                } else {
                  Future(authorizationHandler.userNotAllowedStatus)
                }
            }
        }
    }
  }

  private def nextHandlerOrAuthorized(iterator: Iterator[AuthorizationHandler], request: RequestHeader, next: (RequestHeader) => Future[Result]) = {
    if (iterator.hasNext) {
      applyHandler(iterator, request, next)
    } else {
      next(request)
    }
  }
}
