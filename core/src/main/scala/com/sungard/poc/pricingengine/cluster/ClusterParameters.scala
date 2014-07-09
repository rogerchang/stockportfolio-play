package com.sungard.poc.pricingengine.cluster

/**
 * Represents basic configuration for a node within a cluster
 */

object ClusterNodeParameters {
  // Normal prefix for remoted Akka actors
  final val CLUSTER_URL_PREFIX = "akka.tcp://"
  // Default actor system for the cluster if none is provided
  final val DEFAULT_CLUSTER_NAME = "ClusterSystem"
  // If no host is provided, default to local
  final val DEFAULT_HOST = "127.0.0.1"
}

/**
 * Represents basic configuration for a node within the cluster.
 *
 * @param host   Server host of the node
 * @param port   Port on which node is running
 * @param clusterName   Name of actor system of cluster, must be consistent throughout cluster
 * @param configName  A config file to be brought in to configure the node
 * @param role        The Akka clustering role
 * @param seedHostPorts   If specified, a list of host:port strings representing the seed nodes for
 *                          the cluster
 */
case class ClusterNodeParameters(host : String = ClusterNodeParameters.DEFAULT_HOST, port : Integer,
                                 clusterName : String = ClusterNodeParameters.DEFAULT_CLUSTER_NAME,
                                 configName : String = null,
                                 role : String = null, seedHostPorts : Seq[String] = null) {
  // Creates the Akka URL for the remote actors on the cluster using the host and port specified
  //   in format host:port
  private def formatURL(hostPort : String) = {
    "\"%s%s@%s\"".format(ClusterNodeParameters.CLUSTER_URL_PREFIX, clusterName, hostPort)
  }

  /**
   * Formats the seed host port strings into a config ready setting to allow these to be used
   *   as seed nodes that this particular node to can connect to in order to join the cluster
   *
   * @return
   */
  def seedURLConfig = {
    if (seedHostPorts != null) "[" + seedHostPorts.map(formatURL).mkString(",\n") + "]" else null;
  }

  def nodeURL = {
    formatURL(host + ":" + port)
  }
}
