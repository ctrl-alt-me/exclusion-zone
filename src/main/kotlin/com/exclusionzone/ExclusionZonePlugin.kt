package com.exclusionzone

import org.bukkit.plugin.java.JavaPlugin

class ExclusionZonePlugin : JavaPlugin() {
    internal lateinit var configManager: ConfigManager
    internal lateinit var commandManager: CommandManager
    internal lateinit var borderRenderer: BorderRenderer

    override fun onEnable() {
        configManager = ConfigManager(this)
        commandManager = CommandManager(this)
        borderRenderer = BorderRenderer(this)
        commandManager.register()
        borderRenderer.register()
        logger.info("ExclusionZone enabled.")
    }

    override fun onDisable() {
        configManager.save()
        borderRenderer.unregister()
        logger.info("ExclusionZone disabled.")
    }
}
