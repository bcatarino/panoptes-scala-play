package com.newbyte.panoptes

import objects.{BasicAuthHandler, TestRequestHeader}
import org.specs2.mutable.Specification

class AuthorizationHandlerSpec extends Specification {

  isolated

  lazy val authHandler = new BasicAuthHandler
  authHandler.config

  "BasicAuthHandler#getClosestMatch" should {

    "Match simple path" in {
      val request = new TestRequestHeader("POST", "/products")
      authHandler.getClosestMatch(request) must_!= None
    }

    "Match simple path with slash" in {
      val request = new TestRequestHeader("POST", "/products/")
      authHandler.getClosestMatch(request) must_!= None
    }

    "Not match simple path but different method" in {
      val request = new TestRequestHeader("GET", "/products/")
      authHandler.getClosestMatch(request) must_== None
    }

    "Match simple path with slash on rules" in {
      val request = new TestRequestHeader("POST", "/slash")
      authHandler.getClosestMatch(request) must_!= None
    }

    "Match regex path" in {
      val request = new TestRequestHeader("GET", "/order/123afd")
      authHandler.getClosestMatch(request) must_!= None
    }

    "Match regex path with slash in the end" in {
      val request = new TestRequestHeader("GET", "/order/123afd/")
      authHandler.getClosestMatch(request) must_!= None
    }

    "Match regex path with specifier after id" in {
      val request = new TestRequestHeader("GET", "/product/d2ds2dsa/detail")
      authHandler.getClosestMatch(request) must_!= None
    }

    "Match basic regex path with complex path" in {
      val request = new TestRequestHeader("POST", "/cart/38hdfsui/details")
      authHandler.getClosestMatch(request) must_!= None
    }

    "Match post where rule has no method" in {
      val request = new TestRequestHeader("POST", "/whatever")
      authHandler.getClosestMatch(request) must_!= None
    }

    "Match get where rule has no method" in {
      val request = new TestRequestHeader("GET", "/whatever")
      authHandler.getClosestMatch(request) must_!= None
    }
  }
}
