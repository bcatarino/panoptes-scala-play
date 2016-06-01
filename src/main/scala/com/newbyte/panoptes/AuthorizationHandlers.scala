package com.newbyte.panoptes

case class AuthorizationHandlers(handlers: AuthorizationHandler*) {
  handlers.foreach(_.config)
}
