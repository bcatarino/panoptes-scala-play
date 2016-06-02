package com.newbyte.panoptes

import javax.inject.Inject

class AuthorizationHandlers @Inject()(val handlers: AuthorizationHandler*) {
  handlers.foreach(_.config)
}
