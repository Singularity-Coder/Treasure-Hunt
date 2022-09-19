package com.singularitycoder.treasurehunt

data class Treasure(
    val title: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val filePath: String
)
