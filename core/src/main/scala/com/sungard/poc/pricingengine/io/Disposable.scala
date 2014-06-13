package com.sungard.poc.pricingengine.io

/**
 * Make sure that a resource, defined as some type with a close method defined, is closed successfully
 *    after some arbitrary operation with that resource, f, finishes, successfully or unsuccessfully
 *
 * User: bposerow
 * Date: 10/16/13
 * Time: 10:33 PM
 * To change this template use File | Settings | File Templates.
 */
trait Disposable {
  /**
   * Safely use a resource, making sure it is closed afterwards
   *
   * @param resource  Something that can be closed, defined as a type having a close operation
   * @param f   Some operation you perform with the resource
   * @tparam A
   * @tparam B
   * @return
   */
        def using[A <: { def close() : Unit }, B](resource: A)(f : A => B): B =
            try {
              f(resource)
            } finally {
               resource.close()
            }

}
