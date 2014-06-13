package com.sungard.poc.pricingengine.cluster

/**
 * Created by admin on 4/30/14.
 */

 object ClusterNodeParameters {
  final val CLUSTER_URL_PREFIX = "akka.tcp://"
  final val DEFAULT_CLUSTER_NAME = "ClusterSystem"
  final val DEFAULT_HOST = "127.0.0.1"
}

case class ClusterNodeParameters(host : String = ClusterNodeParameters.DEFAULT_HOST, port : Integer,
                             clusterName : String = ClusterNodeParameters.DEFAULT_CLUSTER_NAME,
                             configName : String = null,
                             role : String = null, seedHostPorts : Seq[String] = null) {
    private def formatURL(hostPort : String) = {
      "\"%s%s@%s\"".format(ClusterNodeParameters.CLUSTER_URL_PREFIX, clusterName, hostPort)
    }

    def seedURLConfig = {
      if (seedHostPorts != null) "[" + seedHostPorts.map(formatURL).mkString(",\n") + "]" else null;
    }

    def nodeURL = {
        formatURL(host + ":" + port)
    }
}
