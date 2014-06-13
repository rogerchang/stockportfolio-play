package com.sungard.poc.pricingengine.actors

import akka.actor._
import akka.routing.ConsistentHashingRouter._
import akka.routing.{ConsistentHashingGroup, FromConfig, ConsistentHashingPool}
import com.sungard.poc.pricingengine.actors.StockDirectoryActor.GetStockPricer
import com.sungard.poc.pricingengine.actors.StockDirectoryActor.GetStockPricer
import akka.actor.SupervisorStrategy.{Escalate, Restart, Stop}
import akka.actor.OneForOneStrategy
import com.sungard.poc.pricingengine.actors.StockDirectoryActor.GetStockPricer
import akka.cluster.routing.{ClusterRouterGroup, ClusterRouterGroupSettings}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import akka.cluster.routing.ClusterRouterGroup
import akka.actor.ActorKilledException
import com.sungard.poc.pricingengine.actors.StockDirectoryActor.GetStockPricer
import akka.cluster.ClusterEvent.MemberRemoved
import akka.routing.ConsistentHashingGroup
import scala.Some
import akka.cluster.ClusterEvent.UnreachableMember
import akka.actor.OneForOneStrategy
import akka.contrib.pattern.ClusterReceptionistExtension

/**
 * Actor used to route requests to the various StockDirectoryActor(s), each of which is responsible for a
 *    number of tickers.  The general routing algorithm is a consistent hashing algorithm, in order to
 *    make sure that a given ticker is always mapped to the same StockDirectoryActor(s) as well as to
 *    allow for roughly equal distribution of StockTicker(s).  It also serves as a supervisor for
 *    the StockDirectoryActor(s), restarting them when an exception occurs.
 *
 * User: bposerow
 * Date: 3/4/14
 * Time: 10:59 PM
 */

object StockDirectoryRouterActor {
  /**
   * Hashing that maps requests to get a StockPricerActor reference consistently based on the stock ticker
   *
   * @return
   */
    def hashMapping: ConsistentHashMapping = {
      case GetStockPricer(stock) => {
        stock
      }
    }

  /**
   * Create a version of this router that will run in a non-clustered environment
   *
   * @return
   */
    def props() : Props = Props(new StockDirectoryRouterActor() with StockDirectoryRouterActorProvider)

  /**
   * Create properties for equivalent version of the router that will run in a clustered environment
   *
   * @return
   */
    def clusterProps() : Props = Props(new StockDirectoryRouterActor() with ClusterStockDirectoryRouterActorProvider)
}

/**
 * Mixin trait used to help create StockDirectoryRouterActor(s).  This allows distringuishing
 *   the creation of these actors in clustered and non-clustered environments.
 */
trait StockDirectoryRouterActorProvider {
  /**
   * Configure the properties for the non-clustered version of this actor
   *
   * @return
   */
     def directoryActorProps : Props = { FromConfig.props(StockDirectoryActor.props()) }

  /**
   * Allow the stock directory router actor, once created, to be registered to the actor system,
   *   however this is done in either the clustered or non-clustered environments
   *
   * @param router
   * @param system
   */
     def registerRouterActor(router : ActorRef, system : ActorSystem) : Unit = {}
}

/**
 * Specific provider for stock directory router actors that are able to run in the cluster.
 */
trait ClusterStockDirectoryRouterActorProvider extends StockDirectoryRouterActorProvider {
  /**
   * Properties for clustered router.  For now hardcoded in the code because was having some
   *   issues getting them from the config file.  Key amongst these is the fact that it uses
   *   a consistent hashing group and a cluster routing group , meaning that a particular ticker
   *   will always hash to the same node in the cluster, but that the cluster nodes and corresponding
   *   stock directory actors must exist before starting up the cluster.  It will expect those
   *   StockDirectory actors to be exposed at path /user/StockDirectory (relative to the actor
   *   system) and that they are started on nodes with role stockDirectory.  It will start up
   *   10 instances.
   *
   * @return
   */
    override def directoryActorProps : Props = { ClusterRouterGroup(ConsistentHashingGroup(Nil), ClusterRouterGroupSettings(
      totalInstances = 10, routeesPaths = List("/user/StockDirectory"),
      allowLocalRoutees = false, useRole = Some("stockDirectory"))).props() }

  /**
   * Register the stock directory router actor with the cluster receptionist, so that external
   *   actors (to the cluster) can access the router.  The router is at the edge of the cluster,
   *    the router routes to multiple nodes containing StockDirectoryActor(s), etc.
   *
   * @param router
   * @param system
   */
    override def registerRouterActor(router : ActorRef, system : ActorSystem) = {
      ClusterReceptionistExtension(system).registerService(router)
    }
}

class StockDirectoryRouterActor extends Actor  {
  this : StockDirectoryRouterActorProvider =>
  // The consistently hashed router for the StockDirectoryActor(s)
  private var cache: ActorRef = null
  private val config = context.system.settings.config

  // StockDirectoryActor(s) should be restarted when an exception occurs within them.  This will cause
  //   these actors to lose their existing references to StockPricerActor(s) and for all of the existing
  //   StockPricerActor(s) to be stopped.  This should clean the slate and allow new requests to be served.
  //   But for the most part, most exceptions should occur at the StockPricerActor level and should not be
  //   escalated to the level of the StockDirectoryActor.
  override val supervisorStrategy =  OneForOneStrategy() {
    case _ : ActorInitializationException => Stop
    case _ : ActorKilledException => Stop
    case _ : Exception => Restart
    case _ => Escalate
  }

  override def preStart() = {
      super.preStart()

      // Create an appropriate consistently hashed router for StockDirectoryActor(s), configured with an
      //   initial number of stock directory actors based on Akka config
      cache = context.actorOf(directoryActorProps, "router")

      // If need be, register the router actor with the actor system, whatever that means in the
      //   clustered or non-clustered environments
      registerRouterActor(self, context.system)
  }

  def receive : Receive = {
    // Simply forward all messages to the consistently hashed router, which in turn will forward the
    //    message to the appropriate StockDirectoryActor
    case m => {
      cache forward m
    }
  }

}
