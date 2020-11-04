package org.techtown.land.data

data class Feature (
    val type: String,
    val geometry: Geometry,
    val properties: Properties,
    val id: String
)