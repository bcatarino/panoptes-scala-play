package com.newbyte.panoptes.model

import scala.collection.Set

trait Subject {
  def getRoles: Set[_ <: Role]

  def getPermissions: Set[_ <: Permission]

  def getIdentifier: String
}
