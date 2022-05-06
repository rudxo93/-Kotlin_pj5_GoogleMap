package com.example.googlemap

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.view.isVisible
import com.example.googlemap.databinding.ActivityMainBinding
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
        setContentView(R.layout.activity_main)
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