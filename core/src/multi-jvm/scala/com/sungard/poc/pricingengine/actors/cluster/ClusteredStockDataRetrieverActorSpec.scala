package com.sungard.poc.pricingengine.actors.cluster

import akka.remote.testkit.{MultiNodeSpec, MultiNodeConfig}
import com.sungard.poc.pricingengine.cluster.ClusterNodeParameters
import com.typesafe.config.ConfigFactory
import org.scalatest.{Matchers, BeforeAndAfterAll, WordSpecLike}
import akka.testkit.ImplicitSender
import akka.cluster.Cluster
import akka.actor.Props
import language.postfixOps
import scala.concurrent.duration._
import com.sungard.poc.pricingengine.actors.{StockDataRetrieverActor, StockDataRetrieverProvider}
import com.sungard.poc.pricingengine.stock_data.{HistoricalStockPriceRow, StockData, StockDataRetriever}
import org.joda.time.DateTime
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.{StockDataRetrieved, RetrieveStockData, ClusteredInitialized}
import akka.contrib.pattern.DistributedPubSubMediator.Send
import akka.contrib.pattern.DistributedPubSubExtension


/**
 * Created by admin on 5/12/14.
 */

object ClusteredStockDataRetrieverActorSpecConfig extends MultiNodeConfig {
  // register the named roles (nodes) of the test
  val dataRetriever1 = role("dataRetriever1")
  val dataRetriever2 = role("dataRetriever2")
  val dataRetriever3 = role("dataRetriever3")
  val testPublisher = role("publisher")

  val param1 =  ClusterNodeParameters(host = null, port = null, role = "dataRetriever")

  commonConfig(ConfigFactory.parseString("""
    akka.actor.provider = "akka.cluster.ClusterActorRefProvider"
    akka.remote.log-remote-lifecycle-events = on
                                         """).withFallback(ConfigFactory.load()))

  nodeConfig(dataRetriever1, dataRetriever2, dataRetriever3)(
    ConfigFactory.parseString(
      """
        akka.cluster.roles =[dataRetriever]
        akka {
             remote {
                     transport = "akka.remote.netty.NettyRemoteTransport"
                     netty.tcp {
                       hostname = "127.0.0.1"
                       port = 0
                     }
               }
           }
      """.stripMargin)
   )

  /*
  nodeConfig(dataRetriever2)(
    ConfigFactory.parseString(
      """
        akka.cluster.roles =[dataRetriever]
        akka {
            remote {
                     transport = "akka.remote.netty.NettyRemoteTransport"
                     netty.tcp {
                       hostname = "127.0.0.1"
                       port = 2554
                     }
               }
          }
      """.stripMargin)
      )


      nodeConfig(dataRetriever3)(
        ConfigFactory.parseString(
          """
          akka.cluster.roles =[dataRetriever]
          akka {
          remote {
                       transport = "akka.remote.netty.NettyRemoteTransport"
                       netty.tcp {
                         hostname = "127.0.0.1"
                         port = 2555
                       }
                 }
           }
          """.stripMargin)

  )
  */

  nodeConfig(testPublisher)(
    ConfigFactory.parseString("akka.cluster.roles =[frontEnd]"))
}

class ClusteredStockDataRetrieverActorSpecMultiJvmNode1 extends ClusteredStockDataRetrieverActorSpec
class ClusteredStockDataRetrieverActorSpecMultiJvmNode2 extends ClusteredStockDataRetrieverActorSpec
class ClusteredStockDataRetrieverActorSpecMultiJvmNode3 extends ClusteredStockDataRetrieverActorSpec
class ClusteredStockDataRetrieverActorSpecMultiJvmNode4 extends ClusteredStockDataRetrieverActorSpec


abstract class ClusteredStockDataRetrieverActorSpec
  extends MultiNodeSpec(ClusteredStockDataRetrieverActorSpecConfig)
with WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender {

  import ClusteredStockDataRetrieverActorSpecConfig._

  override def initialParticipants = roles.size

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()

  val ibmStockData1 = StockData("IBM", List(
      HistoricalStockPriceRow(DateTime.parse("2013-11-01"), 1.0, 31.0, 15.0, 22.5),
      HistoricalStockPriceRow(DateTime.parse("2013-12-01"), 24.5, 27.0, 21.0, 21.0),
      HistoricalStockPriceRow(DateTime.parse("2014-02-01"), 40.0, 45.0, 35.0, 37.0)))

  val ibmStockData2 = StockData("IBM", List(
    HistoricalStockPriceRow(DateTime.parse("2013-11-02"), 2.0, 31.0, 15.0, 22.5),
    HistoricalStockPriceRow(DateTime.parse("2013-12-02"), 24.5, 27.0, 21.0, 21.0),
    HistoricalStockPriceRow(DateTime.parse("2014-02-02"), 40.0, 45.0, 35.0, 37.0)))

  val ibmStockData3 = StockData("IBM", List(
    HistoricalStockPriceRow(DateTime.parse("2013-11-03"), 3.0, 31.0, 15.0, 22.5),
    HistoricalStockPriceRow(DateTime.parse("2013-12-03"), 24.5, 27.0, 21.0, 21.0),
    HistoricalStockPriceRow(DateTime.parse("2014-02-03"), 40.0, 45.0, 35.0, 37.0)))


  val aaplStockData = StockData("AAPL", List(
      HistoricalStockPriceRow(DateTime.parse("2013-11-01"), 200.0, 400.0, 150.0, 300.0)))

  trait TestStockDataRetrieverProvider extends StockDataRetrieverProvider {
    val testActorNo : Int

    val dataRetriever: StockDataRetriever = {
      new StockDataRetriever {
        def retrieve(ticker : String, startDate : DateTime, endDate : DateTime) : StockData = {
          if ("IBM".equals(ticker))
            if (testActorNo == 1) ibmStockData1  else if (testActorNo == 2) ibmStockData2 else ibmStockData3
          else
            aaplStockData
        }
      }
    }
  }

  "Clustered data retriever" must {
    "receive request to retrieve data" in within(15 seconds) {

      runOn(dataRetriever1) {
        // this will only run on the 'first' node
        Cluster(system) join node(dataRetriever1).address
        system.actorOf(Props(new StockDataRetrieverActor with ClusteredInitialized
             with TestStockDataRetrieverProvider {
                  val testActorNo = 1
        }), "stockDataRetriever")
      }

      runOn(dataRetriever2) {
        // this will only run on the 'first' node
        Cluster(system) join node(dataRetriever1).address
        system.actorOf(Props(new StockDataRetrieverActor with ClusteredInitialized
          with TestStockDataRetrieverProvider {
          val testActorNo = 2
        }), "stockDataRetriever")
      }

      runOn(dataRetriever3) {
        // this will only run on the 'first' node
        Cluster(system) join node(dataRetriever1).address
        system.actorOf(Props(new StockDataRetrieverActor with ClusteredInitialized
          with TestStockDataRetrieverProvider {
          val testActorNo = 3
        }), "stockDataRetriever")
      }


      // this will run on all nodes
      // use barrier to coordinate test steps
      testConductor.enter("backends-started")

      runOn(testPublisher) {
         Cluster(system) join node(dataRetriever1).address
         val mediator = DistributedPubSubExtension(system).mediator


         awaitAssert {
           var lastPrice : Double = 0.0
           var foundDiff = false

           for (i <- 1 to 10)
           {
             mediator ! Send("/user/stockDataRetriever", RetrieveStockData("IBM", testActor), true)
             val retrievedMsg = expectMsgType[StockDataRetrieved](10.seconds)
             assert(retrievedMsg.originator == testActor)
             assert(retrievedMsg.stockData.symbol == "IBM")

             val openPrice: Double = retrievedMsg.stockData.stockPriceRows(0).open

             if (lastPrice > 0.0 && lastPrice != openPrice) foundDiff = true

             lastPrice = openPrice
             assert(openPrice == 1.0 || openPrice == 2.0 || openPrice == 3.0)
           }

           assert(foundDiff)
           //println("Formatted date = " + retrievedMsg.stockData.stockPriceRows(0).date.formatted("yyyy-MM-dd"))
           /*
           assert(retrievedMsg.stockData.stockPriceRows(0).date.formatted("yyyy-MM-dd") == "2013-11-01" ||
             retrievedMsg.stockData.stockPriceRows(0).date.formatted("yyyy-MM-dd") == "2013-11-02" ||
             retrievedMsg.stockData.stockPriceRows(0).date.formatted("yyyy-MM-dd") == "2013-11-03"
           )
           */
           /*
           assert(retrievedMsg.stockData.stockPriceRows(0).date.dayOfMonth == 0 ||
                  retrievedMsg.stockData.stockPriceRows(0).date.dayOfMonth == 1 ||
                  retrievedMsg.stockData.stockPriceRows(0).date.dayOfMonth == 2
           )
           */
         }
      }

      testConductor.enter("done-1")
    }
  }


}
