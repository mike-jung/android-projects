package org.techtown.land

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.PolygonOverlay
import kotlinx.android.synthetic.main.activity_main.*
import org.techtown.land.AppInfo.Companion.KEY_VWORLD
import org.techtown.land.AppInfo.Companion.requestQueue
import org.techtown.land.data.Feature
import org.techtown.land.data.PnuDataResponse
import org.techtown.land.data.VworldFeatureResponse
import org.techtown.land.data.VworldResponse

class MainActivity : AppCompatActivity() {
    lateinit var naverMap:NaverMap
    var polygon:PolygonOverlay? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as MapFragment
        mapFragment?.getMapAsync {
            naverMap = it

            naverMap.mapType = NaverMap.MapType.Basic
            //naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true)

        }

        searchButton.setOnClickListener {

            // 입력상자에 입력된 지번으로 위치 검색
            searchByAddress()

        }

        type1Button.setOnClickListener {
            naverMap.mapType = NaverMap.MapType.Basic
            naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false)
        }

        type2Button.setOnClickListener {
            naverMap.mapType = NaverMap.MapType.Hybrid
            naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false)
        }

        type3Button.setOnClickListener {
            naverMap.mapType = NaverMap.MapType.Terrain
            naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, false)
        }

        type4Button.setOnClickListener {
            naverMap.setLayerGroupEnabled(NaverMap.LAYER_GROUP_CADASTRAL, true)
        }


        // Volley의 RequestQueue 객체 생성
        requestQueue =  Volley.newRequestQueue(applicationContext)

    }

    fun searchByAddress() {
        printLog("searchByAddress called")

        val address = input1.text.toString()
        requestSearchByAddress(address)

    }

    fun requestSearchByAddress(address:String) {
        val url = "http://api.vworld.kr/req/address?service=address&request=getcoord&version=2.0&crs=epsg:4326&address=${address}&refine=true&simple=false&format=json&type=parcel&key=${KEY_VWORLD}"

        val request = object: StringRequest(
            Request.Method.GET,
            url,
            {
                printLog("\n응답 -> ${it}")

                processResponseSearchByAddress(it)
            },
            {
                printLog("\n에러 -> ${it.message}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()

                return params
            }
        }

        request.setShouldCache(false)
        requestQueue?.add(request)
        printLog("\n지번으로 위치 조회 요청함")

    }

    fun processResponseSearchByAddress(response: String) {
        val gson = Gson()
        val vworldResponse = gson.fromJson(response, VworldResponse::class.java)
        printLog("\n위치 좌표 : ${vworldResponse.response.result.point.x}, ${vworldResponse.response.result.point.y}")

        searchByParcelLocation(vworldResponse.response.result.point.x, vworldResponse.response.result.point.y)
    }

    /**
     * 필지 위치로 필지 좌표 조회
     */
    fun searchByParcelLocation(x:String, y:String) {
        printLog("searchByParcelLocation called")

        requestSearchByParcelLocation(x, y)

        // 검색된 위치로 이동
        moveToLocation(x, y)

    }

    fun requestSearchByParcelLocation(x:String, y:String) {
        val url = "http://apis.vworld.kr/2ddata/cadastral/data?apiKey=${KEY_VWORLD}&domain=tech-town.org&geometry=POINT(${x} ${y})&output=json"

        val request = object: StringRequest(
            Request.Method.GET,
            url,
            {
                printLog("\n응답 -> ${it}")

                processResponseSearchByParcelLocation(it)
            },
            {
                printLog("\n에러 -> ${it.message}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()

                return params
            }
        }

        request.setShouldCache(false)
        requestQueue?.add(request)
        printLog("\n필지 위치로 좌표 조회 요청함")

    }

    fun processResponseSearchByParcelLocation(response: String) {
        val gson = Gson()
        val featureResponse = gson.fromJson(response, VworldFeatureResponse::class.java)

        val outFeature = featureResponse.featureCollection.features[0] as Feature
        printLog("\n필지 좌표 : ${outFeature.geometry.coordinates}")

        // 필지 폴리곤 오버레이 추가
        polygon?.map = null
        addPolygonOverlay(outFeature.geometry.coordinates)

        searchByPnu(outFeature.properties.pnu)

    }

    fun addPolygonOverlay(coordinates:ArrayList<ArrayList<ArrayList<ArrayList<Double>>>>) {
        polygon = PolygonOverlay()

        val coords = ArrayList<LatLng>()
        for (coord1 in coordinates) {
            for (coord2 in coord1) {
                for (coord3 in coord2) {
                    val curX = coord3[0]
                    val curY = coord3[1]

                    val curPoint = LatLng(curY, curX)
                    coords.add(curPoint)
                }
            }
        }

        polygon?.apply {
            this.coords = coords
            this.color = Color.TRANSPARENT
            this.outlineColor = Color.parseColor("#FFCB17")
            this.outlineWidth = 5

            this.map = naverMap
        }

    }

    fun moveToLocation(x:String, y:String) {
        //val cameraUpdate = CameraUpdate.scrollTo(LatLng(y.toDouble(), x.toDouble()))
        //naverMap.moveCamera(cameraUpdate)

        val cameraPosition = CameraPosition(LatLng(y.toDouble(), x.toDouble()), 18.0)
        naverMap.cameraPosition = cameraPosition
    }

    fun searchByPnu(pnu:String) {
        val url = "https://www.ddangya.com/server/api/Interface.php?pnu=${pnu}&class=Ddangya&method=getLandDetail"

        val request = object: StringRequest(
            Request.Method.GET,
            url,
            {
                printLog("\n응답 -> ${it}")

                processResponseSearchByPnu(it)
            },
            {
                printLog("\n에러 -> ${it.message}")
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()

                return params
            }
        }

        request.setShouldCache(false)
        requestQueue?.add(request)
        printLog("\nPNU로 필지 정보 조회 요청함")

    }

    fun processResponseSearchByPnu(response: String) {
        val gson = Gson()
        val pnuResponse = gson.fromJson(response, PnuDataResponse::class.java)

        //입력상자 초기화
        output1.setText("")
        output2.setText("")
        output3.setText("")
        output4.setText("")
        output5.setText("")
        output6.setText("")


        if (pnuResponse != null) {
            // 용도지역
            output1.setText(pnuResponse.data.usagetype)

            // 지목
            output2.setText(pnuResponse.data.jimok)

            // 면적
            output3.setText("${pnuResponse.data.area}㎡ (${pnuResponse.data.area33}평)")

            // 실거래가
            output4.setText("${pnuResponse.data.hanprice}원")

            // 거래일
            output5.setText(pnuResponse.data.contractdate)

            // 평당가격
            output6.setText("${pnuResponse.data.price33m}만원")
        }

    }


    fun printLog(message:String) {
        logOutput.append("${message}\n")
    }

}