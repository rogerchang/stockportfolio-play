package com.sungard.poc.pricingengine.generators

import scala.util.Random

trait RNG {
   def nextDouble : (Double, RNG)
}

object RNG {
  val LargeDouble = 9.99E20

  def getNextSeed(seed : Long) = {
    (seed * 0x5DEECE66DL + 0xBL) & 0xFFFFFFFFFFFFL // `&` is bitwise AND. We use the current seed to generate a new seed.
  }

  object ScalaRandom {
      def apply() = {
         new ScalaRandom()
      }

     def apply(seed:Long) = {
        new ScalaRandom(seed)
     }
  }

  class ScalaRandom(seed : Long) extends RNG {
    protected val random = new Random(seed)

    def this() = {
       this(System.currentTimeMillis)
    }

    protected def nextSeed = {
      getNextSeed(random.nextLong())
    }

    def nextDouble: (Double, RNG) = {
       // Just subtracting 0.5 to allow value to be negative as well as positive
       val n = ((random.nextDouble() - 0.5) * LargeDouble) * 2;
       val nextRNG = ScalaRandom(nextSeed)
       (n, nextRNG) // The return value is a tuple containing both a pseudo-random integer and the next `RNG` state.
    }
  }

  object GaussianRandom {
    def apply(mean : Double, stdDev : Double) = {
         new GaussianRandom(mean, stdDev)
    }
  }

  case class GaussianRandom(val seed : Long, mean : Double, stdDev : Double) extends ScalaRandom(seed) {
    def this(mean : Double, stdDev : Double) = {
       this(System.currentTimeMillis(), mean, stdDev)
    }


    override def nextDouble: (Double, RNG) = {
      val n = random.nextGaussian() * stdDev + mean;
      val nextRNG = ScalaRandom(nextSeed)
      (n, nextRNG) // The return value is a tuple containing both a pseudo-random integer and the next `RNG` state.
    }
  }

  type Rand[+A] = RNG => (A, RNG)

  val double : Rand[Double] = {
    _.nextDouble
  }

  val fraction : Rand[Double] = {
     map(double)(dub => (dub / LargeDouble).abs)
  }

  val int: Rand[Int] = {
      map(double)(_.toInt)
  }

  val positiveInt : Rand[Int] = map(int)(value => if (value < 0) -(value + 1) else value)

  val boolean : Rand[Boolean] = {
     map(int)(_ % 2 == 0)
  }

  def unit[A](a: A): Rand[A] =
    rng => (a, rng)

  def permute[A](seq : List[A]) : Rand[List[A]] = {
       rng => (seq.foldLeft((seq, List[A]())) {
         (acc, a) => acc match {
           case (seq, result) => {
              val nextIdx = nonNegativeLessThan(seq.size)(rng)._1
              (seq.patch(nextIdx, Nil, 1), seq(nextIdx) :: result)
           }
         }
       }._2, rng)
  }


  // In `sequence`, the base case of the fold is a `unit` action that returns
  // the empty list. At each step in the fold, we accumulate in `acc`
  // and `f` is the current element in the list.
  // `map2(f, acc)(_ :: _)` results in a value of type `Rand[List[A]]`
  // We map over that to prepend (cons) the element onto the accumulated list.
  //
  // We are using `foldRight`. If we used `foldLeft` then the values in the
  // resulting list would appear in reverse order. It would be arguably better
  // to use `foldLeft` followed by `reverse`. What do you think?
  def sequence[A](fs: List[Rand[A]]): Rand[List[A]] =
    fs.foldRight(unit(List[A]()))((f, acc) => map2(f, acc)(_ :: _))

  def flatMap[A,B](f: Rand[A])(g: A => Rand[B]): Rand[B] =
    rng => {
      val (a, r1) = f(rng)
      g(a)(r1) // We pass the new state along
    }

  def nonNegativeLessThan(n: Int): Rand[Int] = {
    flatMap(positiveInt) { i =>
      unit(i % n)
    }
  }

  def map[A,B](s: Rand[A])(f: A => B): Rand[B] =
    flatMap(s)(a => unit(f(a)))

  def map2[A,B,C](ra: Rand[A], rb: Rand[B])(f: (A, B) => C): Rand[C] =
    flatMap(ra)(a => map(rb)(b => f(a, b)))
}

