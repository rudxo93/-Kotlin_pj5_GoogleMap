package com.example.googlemap.utility

import com.example.googlemap.BuildConfig
import com.example.googlemap.Url
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitUtil { // RetrofitUtil 정의

    val apiService: ApiService by lazy { getRetrofit().create(ApiService::class.java) }

    private fun getRetrofit(): Retrofit { // 인스턴스 생성 코드 별도로 필요 없이 바로 호출 가능

        return Retrofit.Builder()
            .baseUrl(Url.TMAP_URL) // Tmap 베이스 주소
            .addConverterFactory(GsonConverterFactory.create()) // gson으로 파싱(컨버터)
            .client(buildOkHttpClient()) // OkHttp 사용
            .build()
    }

    private fun buildOkHttpClient(): OkHttpClient { // okHttp를 사용 -> api호출 결과를 매번 로그를 통해 확인
        val interceptor = HttpLoggingInterceptor() // 매번 api 호출 시 마다 로그 확인 할것
        if (BuildConfig.DEBUG) {
            interceptor.level = HttpLoggingInterceptor.Level.BODY
        } else {
            interceptor.level = HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS) // 5초 동안 응답 없으면 에러
            .addInterceptor(interceptor)
            .build()
    }

}