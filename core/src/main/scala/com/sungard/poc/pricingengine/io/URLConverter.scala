package com.sungard.poc.pricingengine.io

import java.net.{URL, HttpURLConnection}

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 10/17/13
 * Time: 9:03 AM
 * To change this template use File | Settings | File Templates.
 */
object URLConverter {
     @throws(classOf[java.io.IOException])
     @throws(classOf[java.net.SocketTimeoutException])
     implicit def urlToSource(url : URL)(implicit timeouts : IOTimeouts = IOTimeouts(5000, 5000)) = {
           import java.net._
           val connection = url.openConnection.asInstanceOf[HttpURLConnection]
           connection.setConnectTimeout(timeouts.connectTimeout)
           connection.setReadTimeout(timeouts.readTimeout)
           val inputStream = connection.getInputStream()
           io.Source.fromInputStream(inputStream)
     }

     val PARAM_START = "?"
     val MULT_VAL_DELIMITER = "+"
     val KEY_VAL_DELIMITER = "="
     val DIFF_VAL_DELIMITER = "&"

     implicit def urlComponentsToUrl(comps : URLComponents) = {
       val paramString = comps.getParams.foldLeft("")( (acc, kv) => kv match {
             case (k, v) => acc + DIFF_VAL_DELIMITER + k + KEY_VAL_DELIMITER + v.mkString(MULT_VAL_DELIMITER)
       }).stripPrefix(DIFF_VAL_DELIMITER)

       val queryString = if (paramString.isEmpty) paramString else s"${PARAM_START}${paramString}"

       new URL(s"${comps.prefix}${queryString}")
     }
}
