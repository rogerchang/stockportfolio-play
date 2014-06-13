package com.sungard.poc.pricingengine.io

import scala.io.Source
import scala.collection.mutable.ArrayBuffer
import java.util.regex.Pattern

/**
 * Allow parsing of CSV files into a sequence of sequence of strings
 *
 * User: bposerow
 * Date: 10/16/13
 * Time: 11:02 PM
 */
trait CsvParser extends Disposable {
      /**
       * Simply skip the first line if skipHeaders is true
       *
       * @param lines  Lazily obtains the lines of an input source
       * @param skipHeaders true if the first line should be skipped, false if not
       * @return
       */
       private def optionallySkipHeaders(lines : => Iterator[String], skipHeaders : Boolean)  = {
         // zipWithIndex provides tuples of elements with their index, this allows easy filtering out of the first line
         lines.zipWithIndex.filter(row => row match { case (s, i) => if (skipHeaders) i > 0 else true } ).map(_._1)
       }

      /**
       * Parse the lazily obtained lines from an input source, using the specified delimiter into a
       *   sequence of sequence of strings
       *
       * @param lines  The lazily obtained lines of the input source
       * @param delimiter
       * @param skipHeaders  true if the first line of the source, containing headers, should be skipped
       * @return
       */
       def parse(lines : => Iterator[String], delimiter : String, skipHeaders : Boolean) =
       {
            var rows = ArrayBuffer[Array[String]]()

           for (line <- optionallySkipHeaders(lines, skipHeaders)) {
              val fields = line.split(Pattern.quote(delimiter)).map(_.trim)
              rows += fields
           }

             rows
       }

      /**
       * Given an input source, parse the contents into a sequence of sequence of strings
       *
       * @param source   A standard Scala input source, e.g. a file
       * @param delimiter
       * @param skipHeaders true if the first line of the source, containing headers, should be skipped
       * @return
       */
       def parse(source : Source, delimiter : String = ",", skipHeaders : Boolean = false) : ArrayBuffer[Array[String]]  =
       {
             // Guarantee proper closing of the resource
             using(source) {
               source =>
                  parse(source.getLines(), delimiter, skipHeaders)
             }
       }
}
