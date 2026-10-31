package com.nova.gamingsuite.device

data class DeviceProfile(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val androidVersion: String,
    val sdk: Int,
    val abis: List<String>,
    val cpuCores: Int,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val totalStorageGb: Double,
    val availableStorageGb: Double,
    val openGlesVersion: String
)
