package com.sungard.poc.pricingengine.cluster

import com.typesafe.config.{Config, ConfigFactory}
import akka.actor.{Props, ActorSystem}

/**
 * Various methods that help with interacting with Akka clustering
 */
object ClusterUtils {
  // Default Akka clustering config file
  final val CLUSTER_CONFIG = "cluster"

  /**
   * Helpers for returning an actor on a cluster node
   *
   * @param systemName  The name of the actor system
   * @param paramz The configuration parameters of the cluster node
   * @param props
   * @return
   */
  def actorOf(systemName : String, paramz : ClusterNodeParameters, props : Props) = {
    ActorSystem(systemName, config(paramz)).actorOf(props)
  }

  def config(paramz : ClusterNodeParameters = null) = {
    var clusterConfig = ConfigFactory.empty()

    // Setup Akka remote actor hosts and ports
    if (paramz != null && (paramz.host != null && paramz.port != null)) {
      clusterConfig = ConfigFactory.parseString( s"""
                                        akka.remote.netty.tcp.port= ${paramz.port}
                                        akka.remote.netty.tcp.host= "${paramz.host}"
                               """)
    }

    // If initialized, set the rolename associated with this cluster node
    if (paramz.role != null) {
      clusterConfig = clusterConfig.withFallback(ConfigFactory.parseString("akka.cluster.roles = [\"" + paramz.role + "\"]"))
    }

    // If initialized, set a list of host:port seed nodes that are used as contact points to
    //    join the cluster
    if (paramz.seedHostPorts != null) {
      clusterConfig = clusterConfig.withFallback(ConfigFactory.parseString(s"akka.cluster.seed-nodes = " +
        paramz.seedURLConfig
      ));

    }

    // If a configuration file is specified, use it to configure the cluster node
    if (paramz.configName != null) {
      clusterConfig = clusterConfig.withFallback(ConfigFactory.load(paramz.configName))
    } else {
      clusterConfig = clusterConfig.withFallback(ConfigFactory.load(CLUSTER_CONFIG));
    }

    clusterConfig
  }

  def system(paramz : ClusterNodeParameters) = {
    ActorSystem(paramz.clusterName, config(paramz))
  }
}
