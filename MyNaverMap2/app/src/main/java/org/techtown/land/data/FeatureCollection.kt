package org.techtown.land.data

data class FeatureCollection (
    val type: String,
    val bbox: ArrayList<Double> = ArrayList<Double>(),
    val features: ArrayList<Feature> = ArrayList<Feature>()
)