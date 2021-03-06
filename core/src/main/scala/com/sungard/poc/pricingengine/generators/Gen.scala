package com.sungard.poc.pricingengine.generators

/**
 * Used to generate random objects of various sorts
 *
 * @param sample  State object that represents a random value along with a random number generator that
 *                   will generate the next random value
 * @tparam A  The type of object returned ultimately from the generator
 */
case class Gen[+A](sample: State[RNG,A]) {
  /**
   * Convert from a generator of values of type A to values of type B
   *
   * @param f
   * @tparam B
   * @return
   */
  def map[B](f: A => B): Gen[B] =
    Gen(sample.map(f))

  /**
   * Given this generator, another generator of values of type B, and a function which takes the
   *   values from the two generators and converts them into a value of type C, create a Generator
   *   of values of type C
   *
   * @param g
   * @param f
   * @tparam B
   * @tparam C
   * @return
   */
  def map2[B,C](g: Gen[B])(f: (A,B) => C): Gen[C] =
    Gen(sample.map2(g.sample)(f))

  /**
   * Given this generator of values of type A, and a function which converts those values of type A
   *   into a generator of type B, returns a generator of values of type B
   *
   * @param f
   * @tparam B
   * @return
   */
  def flatMap[B](f: A => Gen[B]): Gen[B] =
    Gen(sample.flatMap(a => f(a).sample))

  /**
   * Generate lists of type A of the specified size
   *
   * @param size  The size of the list we want to generate
   * @return
   */
  def listOfN(size: Int): Gen[List[A]] =
    Gen.listOfN(size, this)

  /**
   *  A version of `listOfN` that generates the size to use dynamically.
   *
   */
  def listOfN(size: Gen[Int]): Gen[List[A]] =
    size flatMap (n => this.listOfN(n))

  /**
   * Given a function, returns a function from the size of a desired list to a generated list of this
   *    size
   */
  def listOf: SGen[List[A]] = Gen.listOf(this)

  /**
   * Given a function, returns a function from the size of a desired list to a generated list of this
   *    size, guaranteeing that the list will be at least of size 1 (i.e. guaranteeing that the list
   *    will not be empty)
   */
  def listOf1: SGen[List[A]] = Gen.listOf1(this)

  /**
   * Returns a function to a generator of lists that doesn't care about its size parameter
   *
   * @return
   */
  def unsized = SGen(_ => this)

  /**
   * Given this generator of type A and another generator of type B, creates a generator of tuples
   *     of pairs of values of type A and B
   *
   * @param g
   * @tparam B
   * @return
   */
  def **[B](g: Gen[B]): Gen[(A,B)] =
    (this map2 g)((_,_))
}

object Gen {
  /**
   * Creates a generator that always produces values of the parameter a
   *
    * @param a
   * @tparam A
   * @return
   */
  def unit[A](a: => A): Gen[A] =
    Gen(State.unit(a))

  /**
   * Producers a generator of random boolean values
   */
  val boolean: Gen[Boolean] =
    Gen(State(RNG.boolean))

  /**
   * Given a list, returns a generator of random permutations of this list
   *
   * @param original
   * @tparam A
   * @return
   */
  def permutedList[A](original : List[A]) : Gen[List[A]] = {
     Gen(State(RNG.permute(original)))
  }

  /**
   * Returns a generator that generates random integers between start and stopExclusive, not
   *    including the value of stopExclusive
   *
   * @param start
   * @param stopExclusive
   * @return
   */
  def choose(start: Int, stopExclusive: Int): Gen[Int] =
    Gen(State(RNG.positiveInt).map(n => {
        start + n % (stopExclusive-start)
    }))

  /**
   * Given a generator of values of type A and a parameter specifying the size of a list, generates a
   *    list of values of type A of the specified size
   *
   * @param n
   * @param g
   * @tparam A
   * @return
   */
  def listOfN[A](n: Int, g: Gen[A]): Gen[List[A]] =
    Gen(State.sequence(List.fill(n)(g.sample)))

  /**
   * Given a generator of values of type A, creates a stream of states corresponding to each random
   *    value generated by the generator
   *
    * @param g
   * @tparam A
   * @return
   */
  def streamOfStates[A](g : Gen[A]) : Stream[State[RNG, A]] = {
      g.sample #:: streamOfStates(g)
  }

  /**
   * Given a generator of values of type A, creates a generator of a potential infinite stream of such values
   *    of type A
   *
   * @param g
   * @tparam A
   * @return
   */
  def stream[A](g : Gen[A]): Gen[Stream[A]] =
     Gen(State.sequence(streamOfStates(g)))

  /**
   * Creates a generator of random double values
   *
   */
  val uniform: Gen[Double] = Gen(State(RNG.double))

  /**
   * Creates a generator of random double values between i and j
   *
   * @param i
   * @param j
   * @return
   */
  def choose(i: Double, j: Double): Gen[Double] =
    Gen(State(RNG.fraction).map(d => i + d*(j-i)))

  /*
  def listOfN_1[A](n: Int, g: Gen[A]): Gen[List[A]] =
    List.fill(n)(g).foldRight(unit(List[A]()))((a,b) => a.map2(b)(_ :: _))
  */

  /**
   * Given 2 generators of values of type A, it uses a random boolean generator to determine which of the
   *   two generators to choose for a particular value.  In other words, it randomly chooses between the
   *   two generators for each value it emits
   *
   * @param g1
   * @param g2
   * @tparam A
   * @return
   */
  def union[A](g1: Gen[A], g2: Gen[A]): Gen[A] =
    boolean.flatMap(b => if (b) g1 else g2)

  /**
   * Randomly chooses a threshold.  Then for each value it emits, it randomly chooses another value;
   *      if that value is below the threshhold it takes a value from the first generator, otherwise
   *      it takes a value from the second generator
   *
   * @param g1
   * @param g2
   * @tparam A
   * @return
   */
  def weighted[A](g1: (Gen[A],Double), g2: (Gen[A],Double)): Gen[A] = {
    /* The probability we should pull from `g1`. */
    val g1Threshold = g1._2.abs / (g1._2.abs + g2._2.abs)

    Gen(State(RNG.double).flatMap(d => if (d < g1Threshold) g1._1.sample else g2._1.sample))
  }

  /**
   * Given a generator of values of type A, converts into a function from list length of a generator
   *    of lists of type A
   *
   * @param g
   * @tparam A
   * @return
   */
  def listOf[A](g: Gen[A]): SGen[List[A]] =
    SGen(n => g.listOfN(n))

  /* Not the most efficient implementation, but it's simple.
   * This generates ASCII strings.
   */
  def stringN(n: Int): Gen[String] =
    listOfN(n, choose(0,127)).map(_.map(_.toChar).mkString)

  /**
   * Performs the equivalent of the scanLeft combinator for generators.  A scanLeft operation
   *    accumulates values according to some function and for a given index,
   *     stores the accumulated value from applying a function of all values up to and including that
   *     value at that index.
   *
   * @param g   A generator of sequences of values of type A
   * @param initVal  The initial value before any accumulation is performed
   * @param f  A function that takes an accumlated value and another element of the sequence and performs
   *               a further accumulation by combining these 2 values
   * @tparam A
   * @tparam B
   * @return
   */
  def scanLeft[A, B](g : Gen[Seq[A]], initVal : B)(f : (B, A) => B) : Gen[Seq[B]] =
  {
        g.map((seq : Seq[A]) => seq.scanLeft(initVal)(f))
  }

  val string: SGen[String] = SGen(stringN)

  implicit def unsized[A](g: Gen[A]): SGen[A] = SGen(_ => g)

  def listOf1[A](g: Gen[A]): SGen[List[A]] =
    SGen(n => g.listOfN(n max 1))

  object ** {
    def unapply[A,B](p: (A,B)) = Some(p)
  }
}

case class SGen[+A](g: Int => Gen[A]) {
  def apply(n: Int): Gen[A] = g(n)

  def map[B](f: A => B): SGen[B] =
    SGen(g andThen (_ map f))

  def flatMap[B](f: A => Gen[B]): SGen[B] =
    SGen(g andThen (_ flatMap f))

  def **[B](s2: SGen[B]): SGen[(A,B)] =
    SGen(n => apply(n) ** s2(n))
}