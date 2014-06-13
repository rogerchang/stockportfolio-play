package com.sungard.poc.pricingengine.actors.cluster

import akka.remote.testkit.{MultiNodeSpec, MultiNodeConfig}
import com.sungard.poc.pricingengine.cluster.{ClusterUtils, ClusterNodeParameters}
import org.scalatest.{Matchers, BeforeAndAfterAll, WordSpecLike}
import akka.testkit.ImplicitSender
import com.sungard.poc.pricingengine.stock_data.{StockDataRetriever, HistoricalStockPriceRow, StockData}
import org.joda.time.DateTime
import com.sungard.poc.pricingengine.actors.{StockQuoteGeneratorProvider, StockPricerActor, StockDataRetrieverActor, StockDataRetrieverProvider}
import akka.actor.Props
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.ClusteredInitialized
import akka.cluster.Cluster
import language.postfixOps
import scala.concurrent.duration._
import com.sungard.poc.pricingengine.actors.StockPricerActor.{RetrieveStockQuote, CurrentStockQuote, ClusteredStockPricerDataRetriever}
import com.sungard.poc.pricingengine.calculate.StockPriceSeries
import com.sungard.poc.pricingengine.pricing.{StockQuote, StockQuoteGenerator}
import actors.{SeqProvider, SeqBasedRandomProvider}
import com.sungard.poc.pricingengine.calculate.StockDataAsPriceSeries._
import akka.cluster.ClusterEvent.MemberUp

object ClusteredStockPricerActorSpecConfig extends MultiNodeConfig {
  // register the named roles (nodes) of the test
  val dataRetriever1 = role("dataRetriever1")
  val dataRetriever2 = role("dataRetriever2")
  val dataRetriever3 = role("dataRetriever3")
  val stockPricer = role("stockPricer")
  val priceRequester = role("priceRequester")

  val dataRetrieverParam = ClusterNodeParameters(host = "127.0.0.1", port = 0,
    role = "dataRetriever")
  val stockPricerParam = ClusterNodeParameters(host = "127.0.0.1", port = 0, role = "stockPricer")
  val pricerRequesterParam = ClusterNodeParameters(host = "127.0.0.1", port = 0, role = "priceRequester")

  nodeConfig(dataRetriever1, dataRetriever2, dataRetriever3) {
    ClusterUtils.config(dataRetrieverParam)
  }

  nodeConfig(stockPricer) {
      ClusterUtils.config(stockPricerParam)
  }

  nodeConfig(priceRequester) {
    ClusterUtils.config(pricerRequesterParam)
  }
}

class ClusteredStockPricerActorSpecMultiJvmNode1 extends ClusteredStockPricerActorSpec
class ClusteredStockPricerActorSpecMultiJvmNode2 extends ClusteredStockPricerActorSpec
class ClusteredStockPricerActorSpecMultiJvmNode3 extends ClusteredStockPricerActorSpec
class ClusteredStockPricerActorSpecMultiJvmNode4 extends ClusteredStockPricerActorSpec
class ClusteredStockPricerActorSpecMultiJvmNode5 extends ClusteredStockPricerActorSpec

import ClusteredStockPricerActorSpecConfig._

abstract class ClusteredStockPricerActorSpec
  extends MultiNodeSpec(ClusteredStockPricerActorSpecConfig)
  with WordSpecLike with Matchers with BeforeAndAfterAll with ImplicitSender
  with MultiNodeTestable with ClusterTestable {


  /*
  override def initialParticipants = roles.size

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()
  */

  val ibmStockData = StockData("IBM", List(
    HistoricalStockPriceRow(DateTime.parse("2013-11-01"), 1.0, 31.0, 15.0, 22.5),
    HistoricalStockPriceRow(DateTime.parse("2013-12-01"), 24.5, 27.0, 21.0, 21.0),
    HistoricalStockPriceRow(DateTime.parse("2014-02-01"), 40.0, 45.0, 35.0, 37.0)))


  trait TestStockDataRetrieverProvider extends StockDataRetrieverProvider {
    val dataRetriever: StockDataRetriever = {
      new StockDataRetriever {
        def retrieve(ticker : String, startDate : DateTime, endDate : DateTime) : StockData = {
          ibmStockData
        }
      }
    }
  }

  private trait TestStockQuoteGeneratorProvider extends StockQuoteGeneratorProvider {
    def createStockQuoteGenerator(ticker: String, priceSeries: StockPriceSeries): StockQuoteGenerator = {
      new StockQuoteGenerator(ticker, priceSeries)
        with SeqBasedRandomProvider with SeqProvider[Double] {
        var seq: Seq[Double] = List(0.25, 0.5, -0.1, -0.5, 0.5)
      }
    }
  }

  /*
  private def join(nodePath : ActorPath) : Unit = {
     Cluster(system) join node(dataRetriever1).address

     awaitAssert {
       expectMsgPF[Unit]() {
         case MemberUp(m) => assert(nodePath.address === m.address)
       }
     }
  }
  */

  "Clustered data retriever" must {
    "receive request to retrieve data" in within(15 seconds) {
      Cluster(system).subscribe(testActor, classOf[MemberUp])

      runOn(dataRetriever1) {
        // this will only run on the 'first' node
        join(node(dataRetriever1), dataRetriever1)
        system.actorOf(Props(new StockDataRetrieverActor with ClusteredInitialized
          with TestStockDataRetrieverProvider {
        }), "StockDataRetriever")
      }

      runOn(dataRetriever2) {
        // this will only run on the 'first' node
        join(node(dataRetriever2), dataRetriever1)
        system.actorOf(Props(new StockDataRetrieverActor with ClusteredInitialized
          with TestStockDataRetrieverProvider {
        }), "StockDataRetriever")
      }

      runOn(dataRetriever3) {
        // this will only run on the 'first' node
        join(node(dataRetriever3), dataRetriever1)
        system.actorOf(Props(new StockDataRetrieverActor with ClusteredInitialized
          with TestStockDataRetrieverProvider {
        }), "StockDataRetriever")
      }


      // this will run on all nodes
      // use barrier to coordinate test steps
      testConductor.enter("backends-started")

      runOn(stockPricer) {
        join(node(stockPricer), dataRetriever1)

        val stockPricerDataRetriever = system.actorOf(Props(
          new ClusteredStockPricerDataRetriever("IBM")), "dataRetrieverActor")

        system.actorOf(Props(new StockPricerActor("IBM",
          stockPricerDataRetriever)
          with TestStockQuoteGeneratorProvider), "pricerActor")
      }

      testConductor.enter("pricer-started")

      runOn(priceRequester) {
        val pricer = system.actorSelection(node(stockPricer) / "user" / "pricerActor")

        val priceSeries: StockPriceSeries = ibmStockData
        val firstPrice = priceSeries.prices.head

        join(node(priceRequester), dataRetriever1)
        awaitAssert {
            pricer ! RetrieveStockQuote
            val retStockQuote = this.expectMsgType[CurrentStockQuote].stockQuote
            assert(retStockQuote.ticker === "IBM")
            assert(retStockQuote.value === firstPrice)
        }

        awaitAssert {
          pricer ! RetrieveStockQuote
          expectMsg(CurrentStockQuote(StockQuote("IBM", firstPrice * 1.25)))
        }

        awaitAssert {
          pricer ! RetrieveStockQuote
          expectMsg(CurrentStockQuote(StockQuote("IBM", firstPrice * 1.25 * 1.5)))
        }

        awaitAssert {
          pricer ! RetrieveStockQuote
          expectMsg(CurrentStockQuote(StockQuote("IBM", firstPrice * 1.25 * 1.5 * 0.9)))
        }
      }

      testConductor.enter("done-1")
    }
  }


}
