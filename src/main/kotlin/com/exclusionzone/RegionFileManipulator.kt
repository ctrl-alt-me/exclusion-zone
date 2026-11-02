package com.exclusionzone

import com.exclusionzone.region.DimensionBounds
import org.bukkit.World
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class RegionFileManipulator {
    companion object {
        val instance = RegionFileManipulator()
    }

    // Region files hold 32x32 chunks. Locate the region file for a chunk.
    private fun regionFileFor(world: World, chunkX: Int, chunkZ: Int): File? {
        val worldFolder = world.worldFolder
        val regionDir = File(worldFolder, "region")
        if (!regionDir.exists()) return null
        val regionX = Math.floorDiv(chunkX, 32)
        val regionZ = Math.floorDiv(chunkZ, 32)
        val file = File(regionDir, "r.$regionX.$regionZ.mca")
        return if (file.exists()) file else null
    }

    // Mark a chunk as empty in its region file by clearing the location table entry.
    // This is the least invasive method: the old data remains on disk but is no longer
    // referenced, so the chunk regenerates from seed on next load.
    fun removeChunk(world: World, chunkX: Int, chunkZ: Int) {
        val file = regionFileFor(world, chunkX, chunkZ) ?: return
        val path = file.toPath()
        try {
            val data = ByteBuffer.wrap(Files.readAllBytes(path)).order(ByteOrder.BIG_ENDIAN)
            // Location table: 1024 entries * 8 bytes, starting at offset 0
            val localX = chunkX % 32
            val localZ = chunkZ % 32
            val index = localX + localZ * 32
            val entryOffset = index * 8
            // Set offset = -1 (empty), length = 0
            data.putInt(entryOffset, -1)
            data.putInt(entryOffset + 4, 0)
            Files.write(path, data.array(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
        } catch (e: Exception) {
            throw RuntimeException("Failed to manipulate region file $file", e)
        }
    }

    // Find all region files that overlap with a chunk rectangle, for bulk ops.
    fun regionFilesForBounds(world: World, bounds: DimensionBounds): List<File> {
        val worldFolder = world.worldFolder
        val regionDir = File(worldFolder, "region")
        if (!regionDir.exists()) return emptyList()
        val loX = Math.floorDiv(minOf(bounds.x1, bounds.x2), 32)
        val hiX = Math.floorDiv(maxOf(bounds.x1, bounds.x2), 32)
        val loZ = Math.floorDiv(minOf(bounds.z1, bounds.z2), 32)
        val hiZ = Math.floorDiv(maxOf(bounds.z1, bounds.z2), 32)
        val files = mutableListOf<File>()
        for (rx in loX..hiX) {
            for (rz in loZ..hiZ) {
                val f = File(regionDir, "r.$rx.$rz.mca")
                if (f.exists()) files.add(f)
            }
        }
        return files
    }
}
