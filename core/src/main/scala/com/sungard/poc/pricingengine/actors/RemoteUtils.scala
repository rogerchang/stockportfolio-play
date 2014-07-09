package com.sungard.poc.pricingengine.actors

import com.typesafe.config.{Config, ConfigFactory}

/**
 * Created by admin on 6/18/14.
 */
object RemoteUtils {
  def config(host : String, port : Integer, actorSystem : String) : Config = {
    ConfigFactory.parseString(s"""
                                        akka.actor.deployment="akka.tcp//$actorSystem@$host:$port"
                               """)
  }
}
