package com.exclusionzone

import com.exclusionzone.region.DimensionBounds
import com.exclusionzone.region.ProtectedRegion
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class CommandManager constructor(private val plugin: ExclusionZonePlugin) : CommandExecutor, TabCompleter {

    fun register() {
        plugin.server.getPluginCommand("xz")?.let { cmd ->
            cmd.setExecutor(this)
            cmd.setTabCompleter(this)
        }
    }

    override fun onCommand(sender: CommandSender, cmd: Command, label: String, args: Array<String>): Boolean {
        if (!sender.isOp()) {
            sender.sendMessage("§cRequires OP permission.")
            return true
        }
        if (args.isEmpty()) {
            sender.sendMessage("§eUsage: /xz <define|clear|info|regenerate> [args]")
            return true
        }
        return when (args[0]) {
            "define" -> define(sender, args.copyOfRange(1, args.size))
            "clear" -> clear(sender, args.copyOfRange(1, args.size))
            "info" -> info(sender)
            "regenerate" -> regenerate(sender, args.copyOfRange(1, args.size))
            else -> { sender.sendMessage("§cUnknown command."); true }
        }
    }

    private fun define(sender: CommandSender, args: Array<String>): Boolean {
        if (!(sender is Player)) {
            sender.sendMessage("§cMust be run by a player.")
            return true
        }
        if (args.size < 4) {
            sender.sendMessage("§eUsage: /xz define <x1> <z1> <x2> <z2>")
            return true
        }
        val world = sender.world
        val dim = world.name
        val region = plugin.configManager.getRegion(dim) ?: ProtectedRegion().apply { dimension = dim }
        try {
            region.bounds = DimensionBounds(args[0].toInt(), args[1].toInt(), args[2].toInt(), args[3].toInt())
        } catch (e: NumberFormatException) {
            sender.sendMessage("§cInvalid coordinates.")
            return true
        }
        plugin.configManager.regions[dim] = region
        plugin.configManager.save()
        val b = region.bounds
        sender.sendMessage("§aProtected region for $dim set to X[${b.x1},${b.x2}] Z[${b.z1},${b.z2}].")
        return true
    }

    private fun clear(sender: CommandSender, args: Array<String>): Boolean {
        if (!(sender is Player)) {
            sender.sendMessage("§cMust be run by a player.")
            return true
        }
        val dim = sender.world.name
        plugin.configManager.regions.remove(dim)
        plugin.configManager.save()
        sender.sendMessage("§aCleared protected region for $dim.")
        return true
    }

    private fun info(sender: CommandSender): Boolean {
        val dim = if (sender is Player) sender.world.name else "overworld"
        val region = plugin.configManager.getRegion(dim)
        if (region == null) {
            sender.sendMessage("§eNo protected region for $dim.")
            return true
        }
        val b = region.bounds
        sender.sendMessage("§eRegion for $dim: X[${b.x1},${b.x2}] Z[${b.z1},${b.z2}]")
        return true
    }

    private fun regenerate(sender: CommandSender, args: Array<String>): Boolean {
        if (!(sender is Player)) {
            sender.sendMessage("§cMust be run by a player.")
            return true
        }
        val confirm = args.contains("confirm")
        if (plugin.configManager.requireBackup && !confirm) {
            sender.sendMessage("§cBackup required. Use: /xz regenerate confirm [radius]")
            return true
        }
        val world = sender.world
        val dim = world.name
        val region = plugin.configManager.getRegion(dim)
        if (region == null) {
            sender.sendMessage("§eNo protected region for $dim.")
            return true
        }
        val radiusArg = args.filter { it != "confirm" }.firstOrNull()
        val radius = radiusArg?.toIntOrNull() ?: plugin.configManager.maxRadius
        val maxRadius = Math.min(radius, plugin.configManager.maxRadius)

        val playerCX = sender.location.blockX / 16
        val playerCZ = sender.location.blockZ / 16
        val loCX = playerCX - maxRadius
        val hiCX = playerCX + maxRadius
        val loCZ = playerCZ - maxRadius
        val hiCZ = playerCZ + maxRadius

        val targets = mutableListOf<IntPair>()
        for (cx in loCX..hiCX) {
            for (cz in loCZ..hiCZ) {
                if (region.bounds.contains(cx, cz)) continue
                val chunk = world.getChunkAt(cx, cz)
                if (chunk != null) targets.add(IntPair(cx, cz))
            }
        }

        sender.sendMessage("§ePreparing to regenerate ${targets.size} chunks outside the protected region in $dim.")
        if (targets.isEmpty()) {
            sender.sendMessage("§eNo chunks to regenerate.")
            return true
        }

        val removed = mutableListOf<IntPair>()
        val manipulator = RegionFileManipulator.instance
        for (pair in targets) {
            try {
                world.unloadChunk(pair.x, pair.z)
                manipulator.removeChunk(world, pair.x, pair.z)
                removed.add(pair)
            } catch (e: Exception) {
                plugin.logger.severe("Failed to regenerate chunk (${pair.x},${pair.z}): ${e.message}")
            }
        }
        sender.sendMessage("§aRegenerated ${removed.size} chunks.")
        return true
    }

    override fun onTabComplete(sender: CommandSender, cmd: Command, alias: String, args: Array<String>): List<String> {
        return when (args.isEmpty()) {
            true -> listOf("define", "clear", "info", "regenerate")
            else -> {
                val first = args[0]
                when (first) {
                    "define" -> listOf("<x1>", "<z1>", "<x2>", "<z2>")
                    "clear" -> listOf()
                    "info" -> listOf()
                    "regenerate" -> listOf("confirm", "<radius>")
                    else -> listOf()
                }
            }
        }
    }
}

private data class IntPair(val x: Int, val z: Int)
