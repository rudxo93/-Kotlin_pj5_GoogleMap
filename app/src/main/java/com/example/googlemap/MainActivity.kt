package com.example.googlemap

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.googlemap.databinding.ActivityMainBinding
import com.example.googlemap.model.LocationLatLngEntity
import com.example.googlemap.model.SearchResultEntity
import com.example.googlemap.response.search.Pois
import com.example.googlemap.response.search.SearchPoiInfo
import com.example.googlemap.utility.RetrofitUtil
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope { // CoroutineScope구현

    private lateinit var job: Job

    override val coroutineContext: CoroutineContext // 코루틴 컨텍스트 프로퍼티 재정의
        get() = Dispatchers.Main + job

    lateinit var binding: ActivityMainBinding
    lateinit var adapter: SearchRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        job = Job()

        initAdapter()
        initViews()

    }

    // initAdapter 매서드 정의
    private fun initAdapter() {
        adapter = SearchRecyclerAdapter()
    }

    // RecyclerView 무한 스크롤  구현
    private fun initViews() = with(binding) {
        emptyResultTextView.isVisible = false
        recyclerView.adapter = adapter

        // 무한 스크롤 기능 구현
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                recyclerView.adapter ?: return

                val lastVisibleItemPosition =
                    (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                val totalItemCount = recyclerView.adapter!!.itemCount - 1

                // 페이지 끝에 도달한 경우
                if (!recyclerView.canScrollVertically(1) && lastVisibleItemPosition == totalItemCount) {
                    loadNext()
                }
            }
        })
    }

    private fun loadNext() {
        if (binding.recyclerView.adapter?.itemCount == 0)
            return

        searchWithPage(adapter.currentSearchString, adapter.currentPage + 1)
    }

    private fun setData(searchInfo: SearchPoiInfo, keywordString: String) {

        val pois: Pois = searchInfo.pois
        // mocking data
        val dataList = pois.poi.map {
            SearchResultEntity(
                name = it.name ?: "빌딩명 없음",
                fullAddress = makeMainAddress(it),
                locationLatLng = LocationLatLngEntity(
                    it.noorLat,
                    it.noorLon
                )
            )
        }
        // 어댑터에 데이터 리스트를 갱신해줄 때는 해당 아이템의 클릭리스너를 같이 지정하는데 이때 아이템 클릭 시
        // 해당 데이터에 맞는 지도가 Map Activity에서 보여지도록 해당 위치 데이터 entity를 인텐트의 Extra에 넣어서 실행
        adapter.setSearchResultList(dataList) {
            Toast.makeText(
                this,
                "빌딩이름 : ${it.name}, 주소 : ${it.fullAddress} 위도/경도 : ${it.locationLatLng}",
                Toast.LENGTH_SHORT
            )
                .show()

            // map 액티비티 시작
            startActivity(Intent(this, MapActivity::class.java).apply {
                putExtra(SEARCH_RESULT_EXTRA_KEY, it)
            })
        }
        adapter.currentPage = searchInfo.page.toInt()
        adapter.currentSearchString = keywordString
    }

    // 검색 버튼이 눌리면 입력한 검색어를 바탕으로 검색을 실시
    private fun searchWithPage(keywordString: String, page: Int) {
        // 비동기 처리
        launch(coroutineContext) { // 미리 정의해둔 coroutineContext로 launch해서 코루틴 시작
            try {
                binding.progressCircular.isVisible = true // 로딩 표시
                if (page == 1) {
                    adapter.clearList()
                }
                // IO 스레드 사용
                withContext(Dispatchers.IO) { // 네트워크 작업 할때는 withContext를 통해 IO스레드 사용해서 처리
                    val response = RetrofitUtil.apiService.getSearchLocation(
                        keyword = keywordString,
                        page = page
                    )
                    if (response.isSuccessful) {
                        val body = response.body()
                        // Main (UI) 스레드 사용
                        withContext(Dispatchers.Main) {
                            Log.e("response LSS", body.toString())
                            body?.let { searchResponse ->
                                setData(searchResponse.searchPoiInfo, keywordString)
                            }
                        }
                    }
                    // 레트로핏을 사용해서 Tmap Api를 통해서 지도 검색을 한다.
                    // 요청 body가 응답 성공이라면 이를 통해서 데이터를 파싱
                    // 리사이클러뷰의 어뎁터에 데이터를 등록하게된다.
                    // 이때는 UI스레드 즉, 메인 스레드를 사용한다.
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // error 해결 방법
                // Permission denied (missing INTERNET permission?) 인터넷 권한 필요
                // 또는 앱 삭제 후 재설치
            } finally {
                binding.progressCircular.isVisible = false // 로딩 표시 완료
            }
        }
    }
}