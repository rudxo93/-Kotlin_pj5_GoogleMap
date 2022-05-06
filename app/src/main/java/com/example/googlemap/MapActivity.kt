package com.example.googlemap

import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.isVisible
import com.example.googlemap.databinding.ActivityMapBinding
import com.example.googlemap.model.SearchResultEntity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Job

/*
    검색 위치를 표시하는 과정
    맵 엑티비티 실행 -> getParcelableExtra로 SearchResultEntity를 받아온다
     -> OnMapReadyCallback을 구현한 맵 엑티비티에서 onMapReady를 처리 -> 이때 마커를 만들고 지도에 이를 표시
*/
class MapActivity : AppCompatActivity() {

    private lateinit var job: Job

    private lateinit var binding: ActivityMapBinding

    private lateinit var searchResult: SearchResultEntity

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
}