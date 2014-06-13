package com.sungard.poc.pricingengine.actors

import akka.actor._
import com.sungard.poc.pricingengine.pricing.{GaussianStockQuoteGenerator, StockQuoteGenerator, StockQuote}
import com.sungard.poc.pricingengine.actors.StockPricerActor.RetrieveStockQuote
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.{StockDataRetrieved, RetrieveStockData}
import com.sungard.poc.pricingengine.stock_data.StockData
import akka.remote.transport.ThrottlerTransportAdapter.Direction.Receive
import com.sungard.poc.pricingengine.calculate.StockPriceSeries
import akka.pattern.{ask,pipe}
import com.sungard.poc.pricingengine.actors.ServiceLookupActor.{DataRetrieverReturned, GetDataRetriever}
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.RetrieveStockData
import com.sungard.poc.pricingengine.stock_data.StockData
import com.sungard.poc.pricingengine.actors.StockPricerActor.CurrentStockQuote
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.StockDataRetrieved
import com.sungard.poc.pricingengine.actors.ServiceLookupActor.DataRetrieverReturned
import com.sungard.poc.pricingengine.pricing.StockQuote
import com.sungard.poc.pricingengine.actors.StockPricerActor.StockDataRetrievedInternal
import com.sungard.poc.pricingengine.calculate.StockPriceSeries
import com.sungard.poc.pricingengine.cluster.{ClusterNodeParameters, ClusterPubSubAware}
import com.sungard.poc.pricingengine.cluster.ClusterUtils._
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.RetrieveStockData
import com.sungard.poc.pricingengine.actors.StockPricerActor.CurrentStockQuote
import com.sungard.poc.pricingengine.pricing.StockQuote
import com.sungard.poc.pricingengine.calculate.StockPriceSeries
import com.sungard.poc.pricingengine.stock_data.StockData
import com.sungard.poc.pricingengine.actors.StockDataRetrieverActor.StockDataRetrieved
import com.sungard.poc.pricingengine.actors.ServiceLookupActor.DataRetrieverReturned
import com.sungard.poc.pricingengine.actors.StockPricerActor.StockDataRetrievedInternal

/**
 * User: bposerow
 * Date: 2/11/14
 * Time: 11:23 PM
 *
 * Actor used to retrieve random stock quotes for a particular stock ticker.  These random stock quotes
 *    are based on the average and standard deviation of the stock prices retrieved for the specified stock
 *    from Yahoo Finance.  There should be a single one of these actors per stock.
 *
 *    Collaborators with this actor include:
 *       - StockDataRetrieverActor, for retrieving the stock prices for the stock over a specified period
 *             and then observing the mean and std dev of these prices when an
 *             instance of this actor is first loaded.  The observed mean and standard deviation is used
 *             to create a Gaussian random generator of stock quotes.
 *       - ServiceLookupActor, for looking up the StockDataRetrieverActor
 *       - StockQuoteGenerator - used to generate the random stock quotes based on the observed prices for
 *                                  the stock
 *
 *     Messages accepted:
 *        - Initialize - kick off initialization sequence for this actor.  In this case, perform necessary
 *              lookups for ServiceLookupActor
 *        - RetrieveStockQuote - retrieve one random stock quote for this stock ticker.
 *        - CurrentStockQuote - return the random stock quote to the actor who sent the RetrieveStockQuote
 *                message
 */

object StockPricerActor {
     /*
      * Kick off initialization sequence for this actor.  In this case, perform necessary
      *         lookups for ServiceLookupActor
      */
     case object Initialize

     private case class RetrieveStockDataForPricer(retrieveStockData : RetrieveStockData, pricer : ActorRef)

     /*
      * Retrieve one random stock quote for this stock ticker.
      */
     case object RetrieveStockQuote

     /*
      * Return the random stock quote to the actor who sent the RetrieveStockQuote message
      *
      */
     case class CurrentStockQuote(stockQuote : StockQuote)

     /*
      * Used to send a message within this actor with the retrieved observed StockData (representing the
      *    prices retrieved from Yahoo Finance).  The original sender who sent the request for the stock
      *    quote is sent as well.
      */
     private[actors] case class StockDataRetrievedInternal(stockData : StockData, originalSender : ActorRef)

  /**
   *  Private trait used to provide a default implementation of the StockQuoteGeneratorProvider, which
   *       allows configuration of the strategy used to generate stock quotes.  The default strategy here
   *       generates stock quotes using a Gaussian random distrubituion based on the series of prices observed
   *       from Yahoo finance.
   */
     trait DefaultStockQuoteGeneratorProvider extends StockQuoteGeneratorProvider {
       def createStockQuoteGenerator(ticker : String, priceSeries : StockPriceSeries) : StockQuoteGenerator = {
            new GaussianStockQuoteGenerator(ticker, priceSeries)
       }
     }

  /**
   *
   *
   * @param ticker The stock ticker associated with the StockPricerActor to be created
   * @param system ActorSystem used to lookup helper actors
   * @return
   */

     private def props(ticker : String, system : ActorSystem, dataRetrieverActor : ActorRef) = {
        new StockPricerActor(ticker,
            dataRetrieverActor) with DefaultStockQuoteGeneratorProvider
     }

  /**
   * Create a nonclustered version of the properties for a stock pricer actor
   *
   * @param ticker
   * @param system
   * @return
   */
     def singleNodeProps(ticker : String, system : ActorSystem) : Props = Props(
        props(ticker, system,
          defaultStockPricerDataRetrieverActor(ticker, system)))

     /**
      * Create a clustered version of the properties for a stock pricer actor
      */
     def clusteredProps(ticker : String, system : ActorSystem) : Props = Props(
        props(ticker, system, clusteredDataRetrieverActor(ticker, system))
     )

  /**
   * The nonclustered version of the stock pricer actor uses a "DefaultStockPricerDataRetriever"
   *   which uses the ServiceLookupActor to get the data retriever actor that will help us
   *   lookup price data for a given ticker.
   *
   * @param ticker
   * @param system
   * @return
   */
     def defaultStockPricerDataRetrieverActor(ticker : String, system : ActorSystem) = {
       system.actorOf(Props(
         new DefaultStockPricerDataRetriever(ServiceLookupActor.getServiceLookupActor(system),
             ticker)))
     }

  /**
   * The clustered version of the stock pricer actor uses a "ClusteredStockPricerDataRetriever"
   *   to find data retriever actors in the cluster (via a pub sub mechanism) that will help
   *    us lookup pricing information for a given ticker
   *
   * @param ticker
   * @param system
   * @return
   */
     def clusteredDataRetrieverActor(ticker : String, system : ActorSystem) = {
        system.actorOf(Props(new ClusteredStockPricerDataRetriever(ticker)))
     }

  /**
   * Mixin trait for a helper actor that serves as a communication wrapper around the StockDataRetrieverActor
   *      which, in turn, allows retrieval of stock price data for a ticker.  There will be
   *      clustered and nonclustered implementations of this helper, thus allow me to abstract
   *      out those differences.
   */
     trait StockPricerDataRetriever {
         this : Actor =>

         def preStart()

    /**
     * Send the actual message to retrieve stock price data for a given ticker.  Abstracts out the
     *   differences in how this is done between clustered and nonclustered versions.
     *
     * @param ticker
     * @param originator The actor that originally requested a price for this stock ticker (basically
     *                   the actor which sent a message to the outer StockPricerActor)
     * @param replyDest   The actor which should receive the response from the StockDataRetrieverActor,
     *                          which will be the outer StockPricerActor
     */
         def sendRetrieveStockDataMessage(ticker : String, originator : ActorRef, replyDest : ActorRef)

    /**
     * Retrieve stock price data for a given stock ticker.
     *
     * @param ticker
     * @param pricer    The outer StockPricer actor
     * @param originator The actor that originally requested a price for this stock ticker (basically
     *                   the actor which sent a message to the outer StockPricerActor)
     */
         def retrieveStockData(ticker : String, pricer : ActorRef, originator : ActorRef) = {
           val dataRetrieverActor = context.actorOf(Props(new Actor {
             def receive : Receive = {
               case RetrieveStockData(_, originator) => {
                   // The StockPricer actor needs to retrieve the stock price info, it sends a
                   //   message to this anonymous actor to help it do so, and then uses this method
                   //   call to actually send the message to the StockDataRetrieverActor, however that
                   //   is to happen
                   sendRetrieveStockDataMessage(ticker, originator, pricer)
               }


             }
           }))
           // Actually ask our anonymous actor to retrieve the required stock data
           dataRetrieverActor ! RetrieveStockData(ticker, originator)
         }

    /**
     * Once our StockPricerDataRetriever has been properly initialized, it will transition to
     *   a state where it receives messages via this message.  It can only receive requests
     *   to retrieve stock price info once it has been properly initialized.
     *
     * @param ticker
     * @return
     */
       def initialized(ticker : String) : Receive = {
         case RetrieveStockDataForPricer(RetrieveStockData(ticker, originator), pricer) => {
           // We got a message to RetrieveStockData for the pricer actor on behalf of the originator
           //   actor (the one that sent the original message), let's actually go and retrieve it
           retrieveStockData(ticker, pricer, originator)
         }

         case m@_ => {
           throw new RuntimeException("Unexpected message")
         }
       }
     }

  /**
   * Clustered version of the StockPricerDataRetrieverActor which wraps access to the
   *    StockDataRetrieverActor.  It uses clustered pub sub mechanisms in Akka to
   *    interact with the StockPricerDataRetrieverActor
   *
   * @param ticker
   */
     class ClusteredStockPricerDataRetriever(ticker : String) extends Actor
              with StockPricerDataRetriever with ClusterPubSubAware {
          private val StockDataRetrieverPath = "/user/StockDataRetriever"

          private case object SendRetrieveRequest

          override def preStart() = {
              // Setup the DistributedPubSubMediator required in Akka pub sub to actually
              //   communicate with the StockDataRetrieverActor
              initializePubSub()
          }

          // Actually send the message to retrieve stock price data for the ticker
          def sendRetrieveStockDataMessage(ticker : String, originator : ActorRef, replyDest : ActorRef) = {
              // Use the Akka pub sub mechanism to find a StockDataRetrieverActor in the cluster
              // that can satisfy the request and send it the message to retrieve stock price data
              this.send(StockDataRetrieverPath, RetrieveStockData(ticker, originator))
              // We have sent out the message to retrieve stock price data, transition to a state
              //   where it can start receiving responses to this request
              context.become(receiveRetrieve(ticker, originator, replyDest))
          }


          private def receiveRetrieve(ticker: String, originator : ActorRef, replyDest : ActorRef) : Receive = {
            case msg@StockDataRetrieved(_, _) => {
              // We got our data back, send it back to the StockPricerActor which owns this actor
              replyDest ! msg
            }
          }

          def receive() : Receive = {
              // Just immediately go to the initialized state for the clustered case, nothing to
              // initialize here
              initialized(ticker)
          }
     }

     class DefaultStockPricerDataRetriever(serviceLookupActor : ActorSelection, ticker : String)
            extends Actor with StockPricerDataRetriever with Stash {
          var stockDataRetrieverActor : ActorRef = context.system.deadLetters

          override def preStart() = {
            serviceLookupActor ! GetDataRetriever
          }

          def receive : Receive = {
             /*
              * Once the StockDataRetrieverActor is returned, enter an initialized state in which it can start
              *    processing messages.  Unstash any messages that were received prior to this point so they can
              *    now be processed.
              */
             case DataRetrieverReturned(dataRetriever) => {
               unstashAll()
               stockDataRetrieverActor = dataRetriever
               context.become(initialized(dataRetriever))
             }

             /*
              * Stash any messages received prior to initialization so they can replayed later
               */
             case _ => {
               stash()
             }
       }

       /**
        * Now that the StockPricerActor has received a StockDataRetrieverActor, it is ready to receive messages.
        *    Start out by retrieving stock data for the specified ticker, which will later be used to generate
        *    stock quotes.  This can be viewed as a second stage of initialization.
        */
       private def initialized(stockDataRetrieverActor : ActorRef) : Receive = {
             initialized(ticker)
       }

       override def sendRetrieveStockDataMessage(ticker : String, originator : ActorRef,
                                                 replyDest : ActorRef)  = {
         stockDataRetrieverActor.tell(RetrieveStockData(ticker, originator), replyDest)
       }
    }
}



import StockPricerActor._

/**
 * Uses AskTimeoutProvider to provide a timeout for ask request-response like requests
 *
 * @param ticker  The stock ticker used to create the StockTickerActor
  *
 */
class StockPricerActor(ticker : String, stockPricerDataRetriever : ActorRef) extends Actor
                       with AskTimeoutProvider {
  this : StockQuoteGeneratorProvider =>


  /**
   * Now that the StockPricerActor has received a StockDataRetrieverActor, it is ready to receive messages.
   *    Start out by retrieving stock data for the specified ticker, which will later be used to generate
   *    stock quotes.  This can be viewed as a second stage of initialization.
   */
  override def receive : Receive = {
    case RetrieveStockQuote => {
      // Actually ask our anonymous actor to retrieve the required stock data
         stockPricerDataRetriever ! RetrieveStockDataForPricer(RetrieveStockData(ticker, sender), self)
    }
    /*
     * Message received from the anonymous stock data retriever actor with the retrieved stock prices
     *   and the actor which sent the original request to obtain a stock quote.  Mainly this switches
     *   the actor into a third and final state in which it can truly process requests for stock quotes.
     */
    case StockDataRetrieved(stockData : StockData, originalSender : ActorRef) => {
        // Resend the message to retrieve the stock quote once the state of the actor has flipped
        self.tell(RetrieveStockQuote, originalSender)
        // Change the state of the actor to its final state.  Before doing so, retrieve a stream of
        //   random stock quotes using the configured stockQuoteGenerator (which uses the retrieved
        //   stock prices to generate these random stock quotes).
        context.become(stockDataRetrieved(stockQuoteGenerator(ticker, stockData).getStockQuotes))
    }

  }

  /**
   * Steady state message processor for the StockPricerActor.  Receives a stream of random stock quotes and
   *    uses this to return stock quotes, one by one.
   *
   * @param stockQuotes  Stream of stock quotes
   * @return
   */
  def stockDataRetrieved(stockQuotes : Seq[StockQuote]) : Receive = {
      case RetrieveStockQuote => {
        // Return the next stock quote from the stream
         sender ! CurrentStockQuote(stockQuotes.head)
         // Remove this current quote from the stream and get ready to handle the next request
         context.become(stockDataRetrieved(stockQuotes.tail))
      }
  }
}
