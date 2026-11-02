package com.exclusionzone

import com.exclusionzone.region.DimensionBounds
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.Player

class BorderRenderer constructor(private val plugin: ExclusionZonePlugin) {

    fun register() {
        Bukkit.getScheduler().runTaskTimer(plugin, Runnable { renderAll() }, 0L, 40L)
    }

    fun unregister() {
    }

    private fun renderAll() {
        if (!plugin.configManager.visualBorder) return
        for (player in Bukkit.getOnlinePlayers()) {
            val world = player.world
            val dim = world.name
            val region = plugin.configManager.getRegion(dim) ?: continue
            val cx = player.location.blockX / 16
            val cz = player.location.blockZ / 16
            if (!region.bounds.contains(cx, cz)) continue
            renderBorder(player, region.bounds)
        }
    }

    private fun renderBorder(player: Player, bounds: DimensionBounds) {
        val world = player.world
        val loX = if (bounds.x1 <= bounds.x2) bounds.x1 else bounds.x2
        val hiX = if (bounds.x1 >= bounds.x2) bounds.x1 else bounds.x2
        val loZ = if (bounds.z1 <= bounds.z2) bounds.z1 else bounds.z2
        val hiZ = if (bounds.z1 >= bounds.z2) bounds.z1 else bounds.z2
        val density = plugin.configManager.borderParticleDensity
        val particle = Particle.DUST
        val playerLoc = player.location
        val viewDist = player.getViewDistance().toDouble() * 16.0
        val viewDistSq = viewDist * viewDist
        val playerY = player.location.blockY

        val edgeX1 = loX.toLong() * 16
        val edgeX2 = (hiX + 1).toLong() * 16
        val edgeZ1 = loZ.toLong() * 16
        val edgeZ2 = (hiZ + 1).toLong() * 16

        fun spawnLine(x: Double, z: Double) {
            for (y in playerY - 2 until playerY + 8 step 2) {
                val loc = Location(world, x, y.toDouble() + 0.5, z)
                if (loc.distanceSquared(playerLoc) <= viewDistSq) {
                    world.spawnParticle(particle, loc, 16, 0.05, 0.05, 0.05)
                }
            }
        }

        for (x in edgeX1..edgeX2 step density.toLong()) {
            spawnLine(x.toDouble(), edgeZ1.toDouble())
            spawnLine(x.toDouble(), edgeZ2.toDouble())
        }
        for (z in edgeZ1..edgeZ2 step density.toLong()) {
            spawnLine(edgeX1.toDouble(), z.toDouble())
            spawnLine(edgeX2.toDouble(), z.toDouble())
        }
    }
}
