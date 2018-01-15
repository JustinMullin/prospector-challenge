package xyz.jmullin.prospector.bot

import xyz.jmullin.prospector.Prospector
import xyz.jmullin.prospector.game.Coord
import xyz.jmullin.prospector.game.Probe
import xyz.jmullin.prospector.game.ProspectorBot

import java.util.Random

/**
 * Copy this class to start implementing your bot!
 */
class GlobalNelderMeadBot: ProspectorBot {
  /**
   * Return the name of your bot.
   */
  override val name = "jenget"

  /**
   * Prospect by using a version of the Nelder-Mead minimization algorithm (https://en.wikipedia.org/wiki/Nelder%E2%80%93Mead_method)
   * that includes restarts to locate hopefully many local maximum
   *
   * This general approach is described here: http://www.emse.fr/~leriche/gbnm_cas.pdf
   *
   * The specific MATLAB implementation of this approach that I stole many ideas from is here: https://github.com/ojdo/gbnm/blob/master/gbnm.m
   */
  override fun prospect(probe: Probe) {

    val objectiveFunction: (FloatCoord) -> Float = {floatCoord: FloatCoord ->
      // convert from Float to Int
      val intCoord = Coord(Math.round(floatCoord.x), Math.round(floatCoord.y))
      // make use of the Probe's queue history to avoid querying the same Coord twice
      probe.queryHistory.getOrElse(
          intCoord, {-probe.query(intCoord)}  // '-' because we're using a minimization algorithm to maximize a function
      ).toFloat()
    }

    minimize(
        callback = objectiveFunction,
        bounds = Bounds(
            min = FloatCoord(0f, 0f) - FloatCoord(0.5f, 0.5f),
            max = FloatCoord(Prospector.MapSize.toFloat(), Prospector.MapSize.toFloat()) - FloatCoord(0.5f, 0.5f)
        ),
        options = Options(
            maxRestarts = 100, // maximum (probablistic or degenerated) restarts
            maxEvals = 100,  // maximum function evaluations
            randomPointsPerRestart = 5,  // number of random points per restart
            maxIterationsPerRestart = 15, // maximum iterations per restart
            epsilon = 1f, // T2 convergence test coefficient
            sigma = 10f / Prospector.MapSize  // small simplex convergence test coefficient
        ))
  }

  val println = { x:String ->
    //kotlin.io.println(x)  // enable to get debug print
  }

  // supposedly this Float 'sqrt' exists in the Kotlin standard library (https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.math/sqrt.html),
  // but I haven't been able to effectively import it, so I'll define my own
  val sqrt = {x: Float -> Math.sqrt(x.toDouble()).toFloat()}
  // and do the same for 'pow'
  val pow = {x: Float, e: Float -> Math.pow(x.toDouble(), e.toDouble()).toFloat()}

  val random = Random()

  // a utility to compute the standard deviation for an array of floating point numbers
  fun standardDeviation(numArray: FloatArray): Float {

    val mean = numArray.average().toFloat()
    val sumOfDifferencesSquared = numArray.fold(0.0f, { accumulator, next -> accumulator + pow(next - mean, 2.0f) })
    val sd = sqrt(sumOfDifferencesSquared / numArray.size)

    println(" sd of ${numArray.contentToString()} is $sd")

    return sd
  }

  // a 2D vector with some handy operator overloading
  data class FloatCoord(val x: Float, val y: Float) {
    operator fun times(v: Float) = FloatCoord(x * v, y * v)
    operator fun div(v: Float) = FloatCoord(x / v, y / v)

    operator fun plus(v: FloatCoord) = FloatCoord(x + v.x, y + v.y)
    operator fun minus(v: FloatCoord) = FloatCoord(x - v.x, y - v.y)
    operator fun times(v: FloatCoord) = FloatCoord(x * v.x, y * v.y)
    operator fun div(v: FloatCoord) = FloatCoord(x / v.x, y / v.y)
  }

  // a 2D simplex, known in Old French as a 'triangle'
  data class Simplex(
      val p1: FloatCoord,
      val p2: FloatCoord,
      val p3: FloatCoord
  ) {
    fun bounds(): Bounds = Bounds(
        FloatCoord(minOf(p1.x, p2.x, p3.x), minOf(p1.y, p2.y, p3.y)),
        FloatCoord(maxOf(p1.x, p2.x, p3.x), maxOf(p1.y, p2.y, p3.y))
    )

    // Shoelace formula for the area of a triangle: https://en.wikipedia.org/wiki/Shoelace_formula
    fun area(): Float = Math.abs((p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)) / 2
  }

  // results of evaluating a function at the vertices of a simplex
  data class ValuesAtSimplexPoints(
      val valueAtP1: Float,
      val valueAtP2: Float,
      val valueAtP3: Float
  )

  // a utility for sorting a simplex's vertices from 'best' (smallest) to 'worst' (largest)
  // based on the values associated with the simplex vertices
  fun sortSimplexPointsFromBestToWorst(simplex: Simplex, valuesAtSimplexPoints: ValuesAtSimplexPoints): Pair<Simplex, ValuesAtSimplexPoints> {
    val sorted = listOf(
        Pair(simplex.p1, valuesAtSimplexPoints.valueAtP1),
        Pair(simplex.p2, valuesAtSimplexPoints.valueAtP2),
        Pair(simplex.p3, valuesAtSimplexPoints.valueAtP3)
    ).sortedBy{it.second}

    return Pair(
        Simplex(sorted[0].first, sorted[1].first, sorted[2].first),
        ValuesAtSimplexPoints(sorted[0].second, sorted[1].second, sorted[2].second)
    )
  }

  // represents a 2D bounding box
  data class Bounds(
      val min: FloatCoord,
      val max: FloatCoord
  ) {
    fun snapToBounds(p: FloatCoord) =
        FloatCoord(
            maxOf(minOf(p.x, this.max.x), this.min.x),
            maxOf(minOf(p.y, this.max.y), this.min.y)
        )

    fun range() = FloatCoord(max.x - min.x, max.y - min.y)
  }

  // the options to the minimization function
  data class Options(
      val maxRestarts: Int = 15, // maximum (probablistic or degenerated) restarts
      val maxEvals: Int = 2500,  // maximum function evaluations
      val randomPointsPerRestart: Int = 5,  // number of random points per restart
      val maxIterationsPerRestart: Int = 250, // maximum iterations per restart
      val alpha: Float = 1f, // reflection coefficient
      val beta: Float = 0.5f, // contraction coefficient
      val gamma: Float = 2f, // expansion coefficient
      val epsilon: Float = 1e-9f, // T2 convergence test coefficient
      val sigma: Float = 5e-4f  // small simplex convergence test coefficient
  )

  // pairs of initial points and resultant minimum point found
  data class SingleIterationResult(
      val initialPoint: FloatCoord,
      val valueAtInitialPoint: Float,
      var bestPoint: FloatCoord,
      val valueAtBestPoint: Float
  )

  data class Result(
      val usedPoints: List<SingleIterationResult>
  )

  class MaxEvalsReached() : Exception()

  fun minimize(callback: (FloatCoord) -> Float, bounds: Bounds, options: Options): Result {

    var evalCount = 0

    val function = { coord: FloatCoord ->
      // immediately error out if we're about to exceed the number of evaluations
      if (evalCount >= options.maxEvals) {
        throw MaxEvalsReached()
      }
      evalCount++
      callback(coord)
    }

    val range = bounds.range()
    val usedPoints = mutableListOf<SingleIterationResult>()

    try {
      for (iRestart in 1..options.maxRestarts) {
        println("Restart")

        val initialPoint = probabilisticRestart(usedPoints, bounds, options.randomPointsPerRestart)
        val a = (0.02f + (0.08f * random.nextFloat())) * minOf(range.x, range.y) // simplex size between 2%-10% of min(xrange)
        var simplex = initialSimplexAroundPoint(initialPoint, a, bounds)

        var valuesAtSimplexPoints = ValuesAtSimplexPoints(
            function(simplex.p1),
            function(simplex.p2),
            function(simplex.p3)
        )

        val valueAtInitialPoint = valuesAtSimplexPoints.valueAtP1

        for (iIteration in 1..options.maxIterationsPerRestart) {
          // sort according to function value
          val (sortedSimplex, sortedValuesAtSimplexPoints) = sortSimplexPointsFromBestToWorst(simplex, valuesAtSimplexPoints)
          simplex = sortedSimplex
          valuesAtSimplexPoints = sortedValuesAtSimplexPoints

          // label best and worst
          val best = simplex.p1
          val worst = simplex.p3

          // compute centroid of side not including worst point
          val centroid = (simplex.p1 + simplex.p2) / 2f

          // convergence test
          if (standardDeviation(
                  floatArrayOf(
                      valuesAtSimplexPoints.valueAtP1,
                      valuesAtSimplexPoints.valueAtP2,
                      valuesAtSimplexPoints.valueAtP3
                  )) < options.epsilon) {
            println(" BREAK flat simplex")
            break
          }

          // small test
          val simplexBounds = simplex.bounds()
          if (maxOf((simplexBounds.max.x - simplexBounds.min.x) / range.x, (simplexBounds.max.y - simplexBounds.min.y) / range.y) < options.sigma) {
            println(" BREAK small simplex")
            break
          }

          // degenerate test
          println("simplex area: ${simplex.area()}")
          if (simplex.area() < 2f) {
            println(" BREAK degenerate simplex")
            break
          }

          // simplex iteration
          val reflection = bounds.snapToBounds(centroid + ((centroid - worst) * options.alpha))
          val valueAtReflection = function(reflection)

          // see figure 6 in [Luersen2004] for flow diagram
          if (valueAtReflection < valuesAtSimplexPoints.valueAtP1) {

            // reflection better than best point, so try expansion
            val expansion = bounds.snapToBounds(centroid + (reflection - centroid) * options.gamma)
            val valueAtExpansion = function(expansion)

            if (valueAtExpansion < valueAtReflection) {

              // expansion better than reflection, so use expansion
              simplex = simplex.copy(p3 = expansion)
              valuesAtSimplexPoints = valuesAtSimplexPoints.copy(valueAtP3 = valueAtExpansion)
            } else {

              // expansion worse than reflection, so use reflection
              simplex = simplex.copy(p3 = reflection)
              valuesAtSimplexPoints = valuesAtSimplexPoints.copy(valueAtP3 = valueAtReflection)
            }
          } else {

            // reflection not better than best point
            if (valueAtReflection <= valuesAtSimplexPoints.valueAtP2) {

              // reflection still better than second worst point, so use reflection
              simplex = simplex.copy(p3 = reflection)
              valuesAtSimplexPoints = valuesAtSimplexPoints.copy(valueAtP3 = valueAtReflection)
            } else {

              // reflection worse than second worst point, so contract
              if (valueAtReflection < valuesAtSimplexPoints.valueAtP3) {

                // but still use reflection if it is better than worst point
                simplex = simplex.copy(p3 = reflection)
                valuesAtSimplexPoints = valuesAtSimplexPoints.copy(valueAtP3 = valueAtReflection)
              }

              val contraction = centroid + (worst - centroid) * options.beta;
              val valueAtContraction = function(contraction)

              if (valueAtContraction <= valuesAtSimplexPoints.valueAtP2) {

                // contraction still worse than worst point, so contract all
                // simplex points towards best point (NOT centroid!)
                simplex = simplex.copy(
                    p2 = (simplex.p2 + best) / 2f,
                    p3 = (simplex.p3 + best) / 2f
                )
              } else {

                // contraction at least as good as worst point, so use it
                simplex = simplex.copy(p3 = contraction)
                valuesAtSimplexPoints = valuesAtSimplexPoints.copy(valueAtP3 = valueAtContraction)
              }
            }
          }

        }

        usedPoints.add(
            SingleIterationResult(
                initialPoint = initialPoint,
                valueAtInitialPoint = valueAtInitialPoint,
                bestPoint = simplex.p1,
                valueAtBestPoint = valuesAtSimplexPoints.valueAtP1
            )
        )
      }


    } catch (e: MaxEvalsReached) {
      // fall through
    }

    return Result(usedPoints)
  }

  // initialises new simplex at point p1 with size a
  fun initialSimplexAroundPoint(p1: FloatCoord, a: Float, bounds: Bounds): Simplex {

    // JAKE: I don't understand the specifics of the math here,
    // not sure why there's such a complicated relationship between 'a' and the dimensions of the initial simplex
    val p = a * (sqrt(3f) + 1) / (2 * sqrt(2f))
    val q = a * (sqrt(3f) - 1) / (2 * sqrt(2f))

    return Simplex(
        p1,
        bounds.snapToBounds(FloatCoord(p1.x + p, p1.y + q)),
        bounds.snapToBounds(FloatCoord(p1.x + q, p1.y + p))
    )
  }

  // compute a new point to start a new pass of the Nelder-Mead algorithm at,
  // based on what points have already been used
  fun probabilisticRestart(usedPoints: List<SingleIterationResult>, bounds: Bounds, numberOfPoints: Int) : FloatCoord {
    var bestProbability = Float.POSITIVE_INFINITY
    var bestPoint = FloatCoord(0f, 0f)

    //
    for (i in 0..numberOfPoints) {
      val randomPoint = FloatCoord(
          bounds.min.x + (bounds.range().x * random.nextFloat()),
          bounds.min.y + (bounds.range().y * random.nextFloat())
      )

      val randomProbability = guass(randomPoint, usedPoints, bounds)

      if (randomProbability < bestProbability) {
        bestProbability = randomProbability
        bestPoint = randomPoint
      }
    }

    return bestPoint
  }

  // compute the value of a sum of gaussian functions, each centered at the 'points'
  fun guass(x: FloatCoord, points: List<SingleIterationResult>, bounds: Bounds): Float {
    val glp = 0.01f // Gaussian length parameter (see [Luersen2004, 2.1 Probabilistic restart])
    val range = bounds.range()

    return points.flatMap { listOf(it.bestPoint, it.initialPoint) }.map { point ->
      val diff = x - point
      val ratio = diff / range
      val exponent = (ratio.x * ratio.x + ratio.y * ratio.y) / (-2 * glp)
      val divisor: Float = 2 * Math.PI.toFloat() * glp * range.x * range.y
      Math.pow(Math.E, exponent.toDouble()).toFloat() / divisor
    }.sum()
  }
}