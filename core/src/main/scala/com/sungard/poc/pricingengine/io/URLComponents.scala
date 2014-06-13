package com.sungard.poc.pricingengine.io

/**
 * Components used to form a GET-parameterized URL
 *
 *
 * User: bposerow
 * Date: 10/21/13
 * Time: 10:13 PM
 */

/**
 *
 * @param prefix  The URL prefix
 * @param getParams A sequence of GET parameters, with param names as keys and a sequence of Strings as
 *                    the values
 */
case class URLComponents(prefix : String, getParams : Map[String, Seq[String]])
