package com.sungard.poc.pricingengine.cluster

import akka.actor.{Actor, ActorSelection, ActorRef, ActorSystem}
import akka.contrib.pattern.{ClusterClient, ClusterReceptionistExtension}
import com.sungard.poc.pricingengine.actors.WrappedMessageProducer
import com.typesafe.config.ConfigFactory

/**
 * Represents a mixin trait for an actor that needs to acts as an Akka cluster client.  A
 *   cluster client is a plugin to Akka that allows actors outside of a cluster to access actors
 *   within it.  In this case, it is used by the StockPortfolioValuationActor to access actors
 *   such as the StockPricerActor and StockDirectoryActor inside the cluster.
 */
object ClusterClientAccessible {
  /**
   *  The default path of the "receptionist" actor, a concept of the Akka cluster client module.
   *    The receptionist actor lives on one or more cluster nodes.  It receives all requests from the cluster client and dispatches them
   *    to the appropriate place within the cluster.
   */
  final val ReceptionistActorPath = "/user/receptionist"

  /**
   * Creates and returns the receptionist actor that allows the cluster client to access the cluster.
   *
   * @param contactPoints   Configuration of the nodes of the "contact points" of the cluster, one
   *                           or more nodes that can be used to access the cluster from outside.
   *                           This configuration contains hosts and ports, which are used to
   *                           construct the node URLs that are needed to lookup the receptionist actors
   *                           on these nodes.
   * @param system
   * @param actorName    The name of the actor that should be used to refer to the receptionist.
   * @return
   */
  def lookupClient(contactPoints : Seq[ClusterNodeParameters], system : ActorSystem, actorName : String = "receptionist") = {
    // Construct paths to the receptionist actors on the contact nodes and then looks up the actor refs
    //   for these receptionists
    val initialContacts : Set[ActorSelection]  = contactPoints.map(pt => {
      system.actorSelection(pt.nodeURL.replaceAll("\"", "") + ReceptionistActorPath)
    }).toSet

    println("Initial contacts for receptionist = " + initialContacts)

    val receptionist = system.actorOf(ClusterClient.props(initialContacts), actorName)
    receptionist
  }

  /**
   * Setup and return the Akka config for some actor that wants to act as a cluster client.
   *    The key configuration is the remote host and port associated with the actor, but there
   *    is some basic configuration that is required for a cluster client, and that is brought in
   *    via the cluster_client config file.
   *
   * @param host
   * @param port
   * @return
   */
  def config(host : String, port : Integer) = {
    // Setup the
    var clusterConfig = ConfigFactory.parseString(s"""
                                        akka.remote.netty.tcp.port= $port
                                        akka.remote.netty.tcp.host= $host
                               """)

    // Load the cluster_client config file
    clusterConfig.withFallback(ConfigFactory.load("cluster_client"))
  }
}

import ClusterClientAccessible._

trait ClusterClientAccessible extends WrappedMessageProducer {
  this : Actor =>

  // The Akka path of the ultimate actor to which this cluster client wants to send message
  val targetActorPath : String

  /**
   * Wrap the message in a a special cluster client Send message, which is used to send a
   *   particular message to an actor on a cluster node located at targetActorPath
   *
   * @param msg
   * @return
   */
  override def wrap(msg : Any) = {
    val wrappedMessage = ClusterClient.Send(targetActorPath, msg, false)
    wrappedMessage
  }
}
