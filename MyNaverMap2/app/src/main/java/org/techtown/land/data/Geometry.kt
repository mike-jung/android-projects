package org.techtown.land.data

data class Geometry (
    val type: String,
    val coordinates: ArrayList<ArrayList<ArrayList<ArrayList<Double>>>> = ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>()
)