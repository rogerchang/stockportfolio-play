package com.sungard.poc.pricingengine.cluster

import akka.actor.{Actor, ActorSelection, ActorRef, ActorSystem}
import akka.contrib.pattern.{ClusterClient, ClusterReceptionistExtension}
import com.sungard.poc.pricingengine.actors.WrappedMessageProducer
import com.typesafe.config.ConfigFactory

/**
 * Created by admin on 4/30/14.
 */

object ClusterClientAccessible {
   final val ReceptionistActorPath = "/user/receptionist"

  def lookupClient(contactPoints : Seq[ClusterNodeParameters], system : ActorSystem, actorName : String = "receptionist") = {
    val initialContacts : Set[ActorSelection]  = contactPoints.map(pt => {
           println("Attempting to find URL " + pt.nodeURL.replaceAll("\"", "") + ReceptionistActorPath)
           system.actorSelection(pt.nodeURL.replaceAll("\"", "") + ReceptionistActorPath)
    }).toSet
    println("Initial contacts found to be " + initialContacts)

    val receptionist = system.actorOf(ClusterClient.props(initialContacts), actorName)
    println("Receptionist = " + receptionist)
    receptionist
  }

  def config(host : String, port : Integer) = {
      var clusterConfig = ConfigFactory.parseString(s"""
                                        akka.remote.netty.tcp.port= $port
                                        akka.remote.netty.tcp.host= $host
                               """)

      clusterConfig.withFallback(ConfigFactory.load("cluster_client"))
  }
}

import ClusterClientAccessible._

trait ClusterClientAccessible extends WrappedMessageProducer {
     this : Actor =>

     val targetActorPath : String

  /*
     def register(clusterSystem : ActorSystem) = {
          ClusterReceptionistExtension(clusterSystem).registerService(self)
     }
  */

     override def wrap(msg : Any) = {
            val wrappedMessage = ClusterClient.Send(targetActorPath, msg, false)
            println("Sending wrapped message = " + wrappedMessage)
            wrappedMessage
     }
}
