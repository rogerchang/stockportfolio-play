package com.sungard.poc.pricingengine.actors

import com.sungard.poc.pricingengine.stock_data.StockDataRetriever

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 3/10/14
 * Time: 11:36 AM
 * To change this template use File | Settings | File Templates.
 */
trait StockDataRetrieverProvider {
       val dataRetriever : StockDataRetriever
}
