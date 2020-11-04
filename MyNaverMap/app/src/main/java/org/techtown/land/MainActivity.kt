package org.techtown.land

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    lateinit var naverMap:NaverMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as MapFragment
        mapFragment?.getMapAsync {
            naverMap = it

            naverMap.mapType = NaverMap.MapType.Basic
            naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true)
        }

        button.setOnClickListener {
            val cameraUpdate = CameraUpdate.scrollTo(LatLng(37.464776, 127.499138))
            naverMap.moveCamera(cameraUpdate)
        }

    }
}