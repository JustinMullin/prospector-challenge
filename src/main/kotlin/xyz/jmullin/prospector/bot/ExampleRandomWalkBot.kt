package xyz.jmullin.prospector.bot

import xyz.jmullin.prospector.game.Coord
import xyz.jmullin.prospector.game.Probe
import xyz.jmullin.prospector.game.ProspectorBot
import java.util.*

class ExampleRandomWalkBot : ProspectorBot {
    private val rand = Random()

    override val name: String
        get() = "randomWalkBot"

    override fun prospect(probe: Probe) {
        var coord = Coord(256, 256)
        repeat(100) {
            probe.query(coord)
            coord = Coord(coord.x + rand.nextInt(20)-10, coord.y + rand.nextInt(20)-10)
        }
    }
}