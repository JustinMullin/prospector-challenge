package xyz.jmullin.prospector.bot

import xyz.jmullin.prospector.game.Coord
import xyz.jmullin.prospector.game.Probe
import xyz.jmullin.prospector.game.ProspectorBot
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * nscott@nerdery.com Prospector Bot.
 *
 * Much love to DDL and PTA.
 */
class DanielPlainviewBot : ProspectorBot {
    companion object {
        private val MAX_COORDINATE_VALUE = 511
        private val MIN_START_PLOT_VALUE = 300
        private val DIAG_DISTANCE = 7
        private val AXIS_DISTANCE = 10
        private val MIN_POINT_DISTANCE = 10
    }

    private val random = Random()

    /**
     * "Mr. Plainview?" "Yes."
     */
    override val name = "DanielPlainviewBot"

    /**
     * Prospect the plot by calling .query(coord) on the provided probe instance with the desired
     * coordinate to query. Coordinates are in the range of 0 <= x < 512, 0 <= y < 512.
     * Each query will return the value of the plot at the given coordinate. A maximum of 100 queries
     * are allowed per plot; queries after this will return a value of 0.
     * After returning from this method, your score for the plot will be the value of the largest
     * query you made from the plot.
     */
    override fun prospect(probe: Probe) {
        try {
            while (probe.queriesRemaining > 0) {
                val coord = findStartingCoord(probe)
                searchAroundCoordinate(coord, probe)
            }
        }catch(e: Exception) {
            //Daniel Plainview does not fail...stay away from bowling alleys.
        }
    }

    /**
     * "I have a competition in me. I want no one else to succeed."
     */
    private fun findStartingCoord(probe: Probe): Coord {
        var plotValue = 0
        var coord: Coord? = null

        while (plotValue < MIN_START_PLOT_VALUE && probe.queriesRemaining > 0) {
            coord = randomUniqueCoordinate(probe)
            plotValue = probe.query(coord)
        }

        return coord!!
    }

    /**
     * "What's this? Why don't I own this? Why don't I own this?"
     */
    private fun randomUniqueCoordinate(probe: Probe): Coord {
        var uniqueCoord = false
        var coord: Coord? = null

        while (!uniqueCoord) {
            coord = Coord(random.nextInt(MAX_COORDINATE_VALUE + 1), random.nextInt(MAX_COORDINATE_VALUE + 1))
            uniqueCoord = isCoordUnique(coord, probe)
        }

        return coord!!
    }

    /**
     * "Now go. Go and play some more, and don't come back."
     */
    private fun isCoordUnique(coord: Coord, probe: Probe): Boolean {
        var uniqueCoord = true

        probe.queryHistory.keys.forEach { queriedCoord ->
            uniqueCoord = uniqueCoord && MIN_POINT_DISTANCE <= distanceBetweenPoints(coord, queriedCoord)
        }

        return uniqueCoord
    }

    /**
     * "We offer you the bond of family that very few oilmen can understand."
     */
    private fun distanceBetweenPoints(coord: Coord, queriedCoord: Coord): Int {
        return sqrt((queriedCoord.x - coord.x).toDouble().pow(2) + (queriedCoord.y - coord.y).toDouble().pow(2)).toInt()
    }

    /**
     * "There's a whole ocean of oil under our feet! No one can get at it except for me!"
     */
    private fun searchAroundCoordinate(coord: Coord, probe: Probe) {
        var highestValueCoord = coord
        var highestValue = 0

        while (probe.queriesRemaining > 0) {
            val coordList = createSurroundingCoords(highestValueCoord, probe)

            if (coordList.isEmpty()) {
                return
            }

            coordList.forEach { surroundingCoord ->
                val value = probe.query(surroundingCoord)

                if (value > highestValue) {
                    highestValue = value
                    highestValueCoord = surroundingCoord
                }
            }
        }
    }

    /**
     * "Yeah, you fellows just scratch around in the dirt and find it like the rest of us instead of buying up someone else's hard work."
     */
    private fun createSurroundingCoords(coord: Coord, probe: Probe): List<Coord> {
        val coordList = ArrayList<Coord>(8)

        if (coord.x >= AXIS_DISTANCE) {
            coordList.add(Coord(coord.x - AXIS_DISTANCE, coord.y))
        }

        if (coord.y >= AXIS_DISTANCE) {
            coordList.add(Coord(coord.x, coord.y - AXIS_DISTANCE))
        }

        if (coord.x <= MAX_COORDINATE_VALUE - AXIS_DISTANCE) {
            coordList.add(Coord(coord.x + AXIS_DISTANCE, coord.y))
        }

        if (coord.y <= MAX_COORDINATE_VALUE - AXIS_DISTANCE) {
            coordList.add(Coord(coord.x, coord.y + AXIS_DISTANCE))
        }

        if (coord.x >= AXIS_DISTANCE && coord.y >= AXIS_DISTANCE) {
            coordList.add(Coord(coord.x - DIAG_DISTANCE, coord.y - DIAG_DISTANCE))
        }

        if (coord.x <= MAX_COORDINATE_VALUE - AXIS_DISTANCE && coord.y <= MAX_COORDINATE_VALUE - AXIS_DISTANCE) {
            coordList.add(Coord(coord.x + DIAG_DISTANCE, coord.y + DIAG_DISTANCE))
        }

        if (coord.x >= AXIS_DISTANCE && coord.y <= MAX_COORDINATE_VALUE - AXIS_DISTANCE) {
            coordList.add(Coord(coord.x - DIAG_DISTANCE, coord.y + DIAG_DISTANCE))
        }

        if (coord.y >= AXIS_DISTANCE && coord.x <= MAX_COORDINATE_VALUE - AXIS_DISTANCE) {
            coordList.add(Coord(coord.x + DIAG_DISTANCE, coord.y - DIAG_DISTANCE))
        }

        return coordList.filter { surroundingCoord -> isCoordUnique(surroundingCoord, probe) }
    }
}