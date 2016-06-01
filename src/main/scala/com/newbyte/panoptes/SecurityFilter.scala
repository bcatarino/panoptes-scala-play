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
    request.headers.get(authorizationHandler.authHeaderName) match {
      case None =>
        authorizationHandler.getClosestMatch(request) match {
          case None => nextHandlerOrFilter(iterator, request, next)
          case _ =>
            if (iterator.hasNext) {
              applyHandler(iterator, request, next)
            } else {
              nextHandlerOrError(authorizationHandler, iterator, request, next)
            }
        }
      case Some(sessionId) =>
        authorizationHandler.getUser(sessionId) match {
          case None => Future(authorizationHandler.userNotAllowedStatus)
          case userData =>
            if (authorizationHandler.isAllowed(request, userData)) {
              nextHandlerOrFilter(iterator, request, next)
            } else {
              Future(authorizationHandler.userNotAllowedStatus)
            }
        }
    }
  }

  private def nextHandlerOrFilter(iterator: Iterator[AuthorizationHandler], request: RequestHeader, next: (RequestHeader) => Future[Result]) = {
    if (iterator.hasNext) {
      applyHandler(iterator, request, next)
    } else {
      next(request)
    }
  }

  private def nextHandlerOrError(current: AuthorizationHandler, iterator: Iterator[AuthorizationHandler], request: RequestHeader, next: (RequestHeader) => Future[Result]) = {
    if (iterator.hasNext) {
      applyHandler(iterator, request, next)
    } else {
      current.authHeaderNotPresentAction(request, next)
    }
  }
}
