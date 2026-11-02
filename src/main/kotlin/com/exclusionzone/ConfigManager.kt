package com.exclusionzone

import com.exclusionzone.region.DimensionBounds
import com.exclusionzone.region.ProtectedRegion
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class ConfigManager {
    private val plugin: JavaPlugin
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val configFile: File
    var enabled: Boolean = true
    var requireBackup: Boolean = true
    var maxRadius: Int = 10000
    var visualBorder: Boolean = true
    var borderParticleDensity: Int = 8
    val regions: MutableMap<String, ProtectedRegion> = mutableMapOf()

    constructor(plugin: JavaPlugin) {
        this.plugin = plugin
        configFile = File(plugin.dataFolder, "config.json")
        load()
    }

    fun load() {
        if (!configFile.exists()) return
        try {
            val json = configFile.readText()
            val obj = gson.fromJson(json, JsonObject::class.java)
            enabled = obj.get("enabled")?.asBoolean ?: true
            requireBackup = obj.get("requireBackup")?.asBoolean ?: true
            maxRadius = obj.get("maxRadius")?.asInt ?: 10000
            visualBorder = obj.get("visualBorder")?.asBoolean ?: true
            borderParticleDensity = obj.get("borderParticleDensity")?.asInt ?: 8
            regions.clear()
            val regionsObj = obj.getAsJsonObject("regions") ?: return
            regionsObj.entrySet().forEach { entry ->
                val sec = entry.value.asJsonObject
                val x1 = sec.get("x1")?.asInt ?: 0
                val z1 = sec.get("z1")?.asInt ?: 0
                val x2 = sec.get("x2")?.asInt ?: 0
                val z2 = sec.get("z2")?.asInt ?: 0
                regions[entry.key] = ProtectedRegion().apply {
                    bounds = DimensionBounds(x1, z1, x2, z2)
                    dimension = entry.key
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to load config: ${e.message}")
        }
    }

    fun save() {
        val obj = JsonObject()
        obj.addProperty("enabled", enabled)
        obj.addProperty("requireBackup", requireBackup)
        obj.addProperty("maxRadius", maxRadius)
        obj.addProperty("visualBorder", visualBorder)
        obj.addProperty("borderParticleDensity", borderParticleDensity)
        val regionsObj = JsonObject()
        regions.forEach { (dim, region) ->
            val sec = JsonObject()
            sec.addProperty("x1", region.bounds.x1)
            sec.addProperty("z1", region.bounds.z1)
            sec.addProperty("x2", region.bounds.x2)
            sec.addProperty("z2", region.bounds.z2)
            regionsObj.add(dim, sec)
        }
        obj.add("regions", regionsObj)
        try {
            configFile.writeText(gson.toJson(obj))
        } catch (e: Exception) {
            plugin.logger.warning("Failed to save config: ${e.message}")
        }
    }

    fun getRegion(dimension: String): ProtectedRegion? = regions[dimension]
}
