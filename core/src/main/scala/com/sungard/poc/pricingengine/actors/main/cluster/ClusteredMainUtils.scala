package com.sungard.poc.pricingengine.actors.main.cluster

import com.typesafe.config.Config
import com.sungard.poc.pricingengine.cluster.{ClusterClientAccessible, ClusterUtils, ClusterNodeParameters}
import akka.actor.ActorSystem
import com.sungard.poc.pricingengine.actors.RemoteUtils

/**
 * Created by admin on 6/15/14.
 */
object ClusteredMainUtils {
      private def DefaultClusterName = "ClusterSystem";

      def asClientConfig(args : Array[String], systemName : String) : Config = {
        val host = args(0)
        val port = args(1).toInt
        //val remotePort = args(2).toInt
        ClusterClientAccessible.config(host, port)//.withFallback(RemoteUtils.config(host, remotePort, systemName))
      }

      def createClusterClientSystem(args : Array[String], systemName : String) = {
         ActorSystem(systemName, asClientConfig(args, systemName))
      }

      def asConfig(args : Array[String], clusterName : String,
                           configName : String = null,
                           role : String = null) : Config = {
            val host = args(0)
            val port = args(1).toInt
            var seedHostPorts = List[String]()

            if (args.length > 2) {
                seedHostPorts ++= List(args(2));
            }

            if (args.length > 3) {
                seedHostPorts ++= List(args(3));
            }

            ClusterUtils.config(ClusterNodeParameters(host = host, port = port,
                 clusterName = clusterName, configName = configName, role = role, seedHostPorts = seedHostPorts))
      }

      def createClusteredSystem(args : Array[String],
                                configName : String = null,
                                role : String = null,
                                clusterName : String = DefaultClusterName) = {
            ActorSystem(clusterName, asConfig(args, clusterName, configName, role))
      }
}
