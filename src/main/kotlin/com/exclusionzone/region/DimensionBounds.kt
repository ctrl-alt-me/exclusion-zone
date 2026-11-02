package com.exclusionzone.region

data class DimensionBounds(val x1: Int, val z1: Int, val x2: Int, val z2: Int) {
    fun contains(chunkX: Int, chunkZ: Int): Boolean {
        val loX = if (x1 <= x2) x1 else x2
        val hiX = if (x1 >= x2) x1 else x2
        val loZ = if (z1 <= z2) z1 else z2
        val hiZ = if (z1 >= z2) z1 else z2
        return chunkX >= loX && chunkX <= hiX && chunkZ >= loZ && chunkZ <= hiZ
    }
}
