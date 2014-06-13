package com.sungard.poc.pricingengine.cluster

import com.typesafe.config.{Config, ConfigFactory}
import akka.actor.{Props, ActorSystem}

/**
 * Created by admin on 4/30/14.
 */
object ClusterUtils {
     final val CLUSTER_CONFIG = "cluster"

    def actorOf(systemName : String, paramz : ClusterNodeParameters, props : Props) = {
        ActorSystem(systemName, config(paramz)).actorOf(props)
     }

     def config(paramz : ClusterNodeParameters = null) = {
       var clusterConfig = ConfigFactory.empty()

       if (paramz != null && (paramz.host != null && paramz.port != null)) {
          clusterConfig = ConfigFactory.parseString( s"""
                                        akka.remote.netty.tcp.port= ${paramz.port}
                                        akka.remote.netty.tcp.host= "${paramz.host}"
                               """)
       }

       if (paramz.role != null) {
          clusterConfig = clusterConfig.withFallback(ConfigFactory.parseString("akka.cluster.roles = [\"" + paramz.role + "\"]"))
       }

       if (paramz.seedHostPorts != null) {
           clusterConfig = clusterConfig.withFallback(ConfigFactory.parseString(s"akka.cluster.seed-nodes = " +
                   paramz.seedURLConfig
              ));

            println("Setting akka.cluster.seed-nodes to " + paramz.seedURLConfig)
       }

       //clusterConfig = clusterConfig.withFallback(ConfigFactory.load(CLUSTER_CONFIG));

       if (paramz.configName != null) {
          clusterConfig = clusterConfig.withFallback(ConfigFactory.load(paramz.configName))
       } else {
         clusterConfig = clusterConfig.withFallback(ConfigFactory.load(CLUSTER_CONFIG));
       }
       //println("%%%%% Cluster config = " + clusterConfig.entrySet() + "%%%%%%%%")

       clusterConfig
     }

     def system(paramz : ClusterNodeParameters) = {
          ActorSystem(paramz.clusterName, config(paramz))
     }
}
