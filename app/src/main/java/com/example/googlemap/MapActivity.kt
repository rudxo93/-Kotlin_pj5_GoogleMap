package com.example.googlemap

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.googlemap.databinding.ActivityMapBinding
import com.example.googlemap.model.LocationLatLngEntity
import com.example.googlemap.model.SearchResultEntity
import com.example.googlemap.utility.RetrofitUtil
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/*
    검색 위치를 표시하는 과정
    맵 엑티비티 실행 -> getParcelableExtra로 SearchResultEntity를 받아온다
     -> OnMapReadyCallback을 구현한 맵 엑티비티에서 onMapReady를 처리 -> 이때 마커를 만들고 지도에 이를 표시
*/
class MapActivity : AppCompatActivity(), OnMapReadyCallback,CoroutineScope {

    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private lateinit var binding: ActivityMapBinding
    private lateinit var map: GoogleMap
    private var currentSelectMarker: Marker? = null

    private lateinit var searchResult: SearchResultEntity

    // 위치 매니저 프로퍼티 정의
    // 위치 매니저는 앱 프레임워크에 해당하는데 앱이 위치 변경 정보를 수신할 수 있게 해준다.(수신할 수 있는 서비스를 사용할 수 있게 한다.)
    private lateinit var locationManager: LocationManager // 안드로이드 에서 위치정보 불러올 때 관리해주는 유틸 클래스

    private lateinit var myLocationListener: MyLocationListener // 나의 위치를 불러올 리스너

    companion object {
        const val SEARCH_RESULT_EXTRA_KEY: String = "SEARCH_RESULT_EXTRA_KEY"
        const val CAMERA_ZOOM_LEVEL = 17f
        const val PERMISSION_REQUEST_CODE = 2021
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        job = Job()

        // 검색 결과 entity 가져오기
        if (::searchResult.isInitialized.not()) {
            intent?.let {
                searchResult = it.getParcelableExtra<SearchResultEntity>(SEARCH_RESULT_EXTRA_KEY)
                    ?: throw Exception("데이터가 존재하지 않습니다.")
                setupGoogleMap()
            }
        }

        bindViews()
    }

    private fun bindViews() = with(binding) {
        // 현재 위치 버튼 리스너
        currentLocationButton.setOnClickListener {
            binding.progressCircular.isVisible = true
            getMyLocation()
        }
    }

    // SupportMapFragment 가져와서 Callback전달
    private fun setupGoogleMap() {
        val mapFragment =
            supportFragmentManager.findFragmentById(binding.mapFragment.id) as SupportMapFragment

        mapFragment.getMapAsync(this) // callback 구현 (onMapReady)

        // 마커 데이터 보여주기

    }

    // onMapReady 콜백을 재정의
    // 이때 searchResult를 사용해서 마커를 설정하고 이를 보여준다.
    override fun onMapReady(map: GoogleMap) {
        this.map = map
        currentSelectMarker = setupMarker(searchResult)

        currentSelectMarker?.showInfoWindow()
    }

    // 구글 맵 마커 만들기
    private fun setupMarker(searchResult: SearchResultEntity): Marker {
        // 위도와 경도 정보를 가지고 LatLng객체를 하나 만들어주고 마커 객체 옵션을 markerOptions()를 통해 생성
        // 위치, 제목, 스니펫 등을 설정하고 맵의 moveCamera를 통해 줌을 설정, 위치를 지정해준 뒤 마커를 추가해서 반환

        // 구글맵 전용 위도/경도 객체
        val positionLatLng = LatLng(
            searchResult.locationLatLng.latitude.toDouble(),
            searchResult.locationLatLng.longitude.toDouble()
        )

        // 구글맵 마커 객체 설정
        val markerOptions = MarkerOptions().apply {
            position(positionLatLng)
            title(searchResult.name)
            snippet(searchResult.fullAddress)
        }

        // 카메라 줌 설정
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(positionLatLng, CAMERA_ZOOM_LEVEL))

        return map.addMarker(markerOptions)
    }

    // getMyLocation 매서드 정의
    // 현위치 버튼의 onClick시 호출
    private fun getMyLocation() {
        // 위치 매니저 초기화
        // 먼저 위치 매니저의 초기화가 안되었다면(lateinit이라 초기화가 필요하다.) 초기화를 해준다.
        if (::locationManager.isInitialized.not()) {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        }

        // GPS 이용 가능한지
        // GPS이 이용이 가능한지 확인하고 GPS이용이 불가능하다면 권한을 얻어야함
        val isGpsEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        // 권한 얻기
        // 권한이 전에 한번 거절되었다면 권한이 왜 필요한지 설명할 팝업을 띄우고 권한이 부여되지 않은 경우에는 퍼미션 요청작업 진행
        if (isGpsEnable) {
            when {
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) && shouldShowRequestPermissionRationale(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) -> {
                    showPermissionContextPop()
                }

                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED -> {
                    makeRequestAsync()
                }

                else -> {
                    setMyLocationListener()
                }
            }
        }
    }

    // 퍼미션
    private fun showPermissionContextPop() {
        AlertDialog.Builder(this)
            .setTitle("권한이 필요합니다.")
            .setMessage("내 위치를 불러오기위해 권한이 필요합니다.")
            .setPositiveButton("동의") { _, _ ->
                makeRequestAsync()
            }
            .create()
            .show()
    }

    // 현재 위치 요청
    // 현재 위치를 요청할 때 myLocationListener를 리스너로 전달해서 현재 위치를 요청하고 현위치가 변경되는 경우
    @SuppressLint("MissingPermission")
    private fun setMyLocationListener() {
        val minTime = 3000L // 현재 위치를 불러오는데 기다릴 최소 시간
        val minDistance = 100f // 최소 거리 허용

        // 로케이션 리스너 초기화
        if (::myLocationListener.isInitialized.not()) {
            myLocationListener = MyLocationListener()
        }

        // 현재 위치 업데이트 요청
        with(locationManager) {
            requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                minTime,
                minDistance,
                myLocationListener
            )
            requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                minTime,
                minDistance,
                myLocationListener
            )
        }
    }

    // 현위치가 변경되는 경우 호출
    // 위치를 현재 위치로 이동시키고 loadReverseGeoInformation으로 위도 경도 정보로
    // 해당 위치의 지역명을 T-map서버에서 가져오도록 요청해서 지도에 마커로 표시
    private fun onCurrentLocationChanged(locationLatLngEntity: LocationLatLngEntity) {
        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    locationLatLngEntity.latitude.toDouble(),
                    locationLatLngEntity.longitude.toDouble()
                ), CAMERA_ZOOM_LEVEL
            )
        )

        loadReverseGeoInformation(locationLatLngEntity)
        removeLocationListener() // 위치 불러온 경우 더이상 리스너가 필요 없으므로 제거
    }

    private fun loadReverseGeoInformation(locationLatLngEntity: LocationLatLngEntity) {
        // 코루틴 사용
        launch(coroutineContext) {
            try {
                binding.progressCircular.isVisible = true

                // IO 스레드에서 위치 정보를 받아옴
                withContext(Dispatchers.IO) {
                    val response = RetrofitUtil.apiService.getReverseGeoCode(
                        lat = locationLatLngEntity.latitude.toDouble(),
                        lon = locationLatLngEntity.longitude.toDouble()
                    )
                    if (response.isSuccessful) {
                        val body = response.body()

                        // 응답 성공한 경우 UI 스레드에서 처리
                        withContext(Dispatchers.Main) {
                            Log.e("list", body.toString())
                            body?.let {
                                currentSelectMarker = setupMarker(
                                    SearchResultEntity(
                                        fullAddress = it.addressInfo.fullAddress ?: "주소 정보 없음",
                                        name = "내 위치",
                                        locationLatLng = locationLatLngEntity
                                    )
                                )
                                // 마커 보여주기
                                currentSelectMarker?.showInfoWindow()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MapActivity, "검색하는 과정에서 에러가 발생했습니다.", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressCircular.isVisible = false
            }
        }
    }

    // 현재 위치를 불러온 경우에는 myLocationListener를 업데이트 대상에서 지워준다.
    private fun removeLocationListener() {
        if (::locationManager.isInitialized && ::myLocationListener.isInitialized) {
            locationManager.removeUpdates(myLocationListener)
        }
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    setMyLocationListener()
                } else {
                    Toast.makeText(this, "권한을 받지 못했습니다.", Toast.LENGTH_SHORT).show()
                    binding.progressCircular.isVisible = false
                }
            }
        }
    }

    // 권한이 모두 주어졌다면 위치정보를 받아온다.
    private fun makeRequestAsync() {
        // 퍼미션 요청 작업. 아래 작업은 비동기로 이루어짐
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            PERMISSION_REQUEST_CODE
        )
    }

    // 현재 위치가 변경되는 이벤트를 받아오는 리스너 클래스 정의
    // 현위치가 변경되는 경우 onCurrentLocationChanged가 호출되어 위치 업데이트
    inner class MyLocationListener : LocationListener {
        // onLocationChanged를 재정의하여 위치가 변경되는 경우 처리 구현
        override fun onLocationChanged(location: Location) {
            // 현재 위치 콜백
            val locationLatLngEntity = LocationLatLngEntity(
                location.latitude.toFloat(),
                location.longitude.toFloat()
            )

            onCurrentLocationChanged(locationLatLngEntity)
        }

    }
}