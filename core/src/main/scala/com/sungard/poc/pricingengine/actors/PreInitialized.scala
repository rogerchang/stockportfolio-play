package com.sungard.poc.pricingengine.actors

import akka.actor.Actor

/**
 * Created by admin on 5/6/14.
 */
trait PreInitialized {
  this : Actor =>

  override def preStart() = {

  }
}
