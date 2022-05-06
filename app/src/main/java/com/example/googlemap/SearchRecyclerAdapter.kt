package com.example.googlemap

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.googlemap.databinding.ViewholderSearchResultItemBinding
import com.example.googlemap.model.SearchResultEntity

class SearchRecyclerAdapter : RecyclerView.Adapter<SearchRecyclerAdapter.SearchResultViewHolder>() {

    private var searchResultList: List<SearchResultEntity> = listOf()
    var currentPage = 1
    var currentSearchString = ""

    private lateinit var searchResultClickListener: (SearchResultEntity) -> Unit

    inner class SearchResultViewHolder( // 뷰홀더 정의
        private val binding: ViewholderSearchResultItemBinding,
        private val searchResultClickListener: (SearchResultEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        // 뷰와 데이터를 바인딩 시키는 bindData와 아이템 클릭 시 발생하는 이벤트 리스너 정의(bindViews)
        fun bindData(data: SearchResultEntity) = with(binding) {
            titleTextView.text = data.name
            subtitleTextView.text = data.fullAddress
        }

        fun bindViews(data: SearchResultEntity) {
            binding.root.setOnClickListener {
                searchResultClickListener(data)
            }
        }
    }

    // 뷰홀더를 생성하는 매커니즘을 정의와 리스너도 전달해서 넘겨준다.
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val binding = ViewholderSearchResultItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SearchResultViewHolder(binding, searchResultClickListener)
    }

    // 매서드 재정의
    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bindData(searchResultList[position])
        holder.bindViews(searchResultList[position])
    }

    // 매서드 재정의
    override fun getItemCount(): Int {
        return searchResultList.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSearchResultList( // 검색 결과를 담을 리스트 지정
        searchResultList: List<SearchResultEntity>,
        searchResultClickListener: (SearchResultEntity) -> Unit
    ) {
        // 무한 스크롤 기능(기존리스트에 더해주도록 설정)
        this.searchResultList = this.searchResultList + searchResultList
        this.searchResultClickListener = searchResultClickListener
        notifyDataSetChanged() // 데이터 변경시 데이터 변경을 알려주는 뷰를 업데이트 할 수 있도록
    }

    // 리스트 초기화 사용
    fun clearList(){
        searchResultList = listOf()
    }

}