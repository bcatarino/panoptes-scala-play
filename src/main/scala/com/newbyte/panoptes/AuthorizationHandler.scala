package com.newbyte.panoptes

import com.newbyte.panoptes.model.Subject
import play.api.mvc.{RequestHeader, Result, Results}

import scala.concurrent.Future

trait AuthorizationHandler {

  private lazy val rules: Set[(Pattern, _ <: AuthorizationRule)] = config

  def authHeaderName = "Authorization"

  def authHeaderNotPresentAction(request: RequestHeader, next: (RequestHeader) => Future[Result]) = {
    Results.Forbidden
  }

  def userNotAllowedStatus: Result = Results.Forbidden

  def isAllowed(request: RequestHeader, user: Option[Subject]): Boolean = {
    getClosestMatch(request).getOrElse(allow()).applyRule(request, user)
  }

  def getClosestMatch(request: RequestHeader) = {
    val exactMatch = rules.find(ruleTuple => ruleTuple._1.method.getOrElse(request.method).equals(request.method)
      && noEndSlash(ruleTuple._1.pattern).equals(noEndSlash(request.path)))

    exactMatch match {
      case Some(rule) => Some(rule._2)
      case None =>
        val closeMatches = findCloseMatches(request)
        findClosestMatch(closeMatches)
    }
  }

  private def noEndSlash(path: String) = {
    if (path.endsWith("/")) path.substring(0, path.length - 1) else path
  }

  private def findCloseMatches(request: RequestHeader) = {
    rules.filter(ruleTuple =>
      ruleTuple._1.method.getOrElse(request.method).equals(request.method) &&
        noEndSlash(request.path).matches(noEndSlash(ruleTuple._1.pattern)))
  }

  private def findClosestMatch(closeMatches: Set[(Pattern, _ <: AuthorizationRule)]) = {
    val matches = closeMatches.map(ruleTuple => (ruleTuple._2, ruleTuple._1.pattern.count(_ == '/')))
    if (matches.isEmpty) {
      None
    } else {
      Some(matches.maxBy(_._2)._1)
    }
  }

  def config: Set[(Pattern, _ <: AuthorizationRule)]

  def getUser(sessionId: String): Option[Subject]
}
