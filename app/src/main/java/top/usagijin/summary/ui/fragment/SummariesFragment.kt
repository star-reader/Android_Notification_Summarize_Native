package top.usagijin.summary.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch
import top.usagijin.summary.R
import top.usagijin.summary.adapter.SummaryAdapter
import top.usagijin.summary.viewmodel.MainViewModel

/**
 * 摘要列表Fragment - Material Design 3风格
 */
class SummariesFragment : Fragment() {
    
    private val viewModel: MainViewModel by viewModels()
    private lateinit var summaryAdapter: SummaryAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateCard: MaterialCardView
    private lateinit var emptyStateText: MaterialTextView
    private lateinit var loadingStateCard: MaterialCardView
    private lateinit var buttonRefresh: MaterialButton
    private lateinit var buttonGenerateSummary: MaterialButton
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_summaries, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        setupClickListeners()
        observeData()
    }
    
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewSummaries)
        emptyStateCard = view.findViewById(R.id.cardEmptyState)
        emptyStateText = view.findViewById(R.id.textEmptyState)
        loadingStateCard = view.findViewById(R.id.cardLoadingState)
        buttonRefresh = view.findViewById(R.id.buttonRefresh)
        buttonGenerateSummary = view.findViewById(R.id.buttonGenerateSummary)
    }
    
    private fun setupRecyclerView() {
        summaryAdapter = SummaryAdapter { summary ->
            // 点击摘要项的处理
            // TODO: 导航到详情页面或展开摘要
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = summaryAdapter
            
            // 添加现代化的滚动效果
            isNestedScrollingEnabled = true
            
            // 设置item间距
            val spacing = resources.getDimensionPixelSize(R.dimen.card_spacing)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.bottom = spacing
                    if (parent.getChildAdapterPosition(view) == 0) {
                        outRect.top = spacing
                    }
                }
            })
        }
    }
    
    private fun setupClickListeners() {
        buttonRefresh.setOnClickListener {
            refreshSummaries()
        }
        
        buttonGenerateSummary.setOnClickListener {
            generateSummary()
        }
    }
    
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.summaries.collect { summaries ->
                if (summaries.isEmpty()) {
                    showEmptyState()
                } else {
                    hideEmptyState()
                    summaryAdapter.submitList(summaries)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                if (isLoading) {
                    showLoadingState()
                } else {
                    hideLoadingState()
                }
            }
        }
    }
    
    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        emptyStateCard.visibility = View.VISIBLE
        emptyStateText.text = "暂无摘要\n\n当您收到通知时，智能摘要\n会自动生成并显示在这里。"
    }
    
    private fun hideEmptyState() {
        recyclerView.visibility = View.VISIBLE
        emptyStateCard.visibility = View.GONE
    }
    
    private fun showLoadingState() {
        loadingStateCard.visibility = View.VISIBLE
    }
    
    private fun hideLoadingState() {
        loadingStateCard.visibility = View.GONE
    }
    
    private fun refreshSummaries() {
        viewModel.refreshData()
    }
    
    private fun generateSummary() {
        // TODO: 手动触发摘要生成
        viewModel.generateSummaries()
    }
    
    companion object {
        fun newInstance() = SummariesFragment()
    }
} 