package top.usagijin.summary.fragment

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.launch
import top.usagijin.summary.NotificationDetailActivity
import top.usagijin.summary.R
import top.usagijin.summary.adapter.SummaryAdapter
import top.usagijin.summary.viewmodel.MainViewModel

/**
 * 摘要列表Fragment
 */
class SummariesFragment : Fragment() {
    
    private val viewModel: MainViewModel by activityViewModels()
    private lateinit var adapter: SummaryAdapter
    
    // UI组件
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateView: TextView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return createLayout()
    }
    
    /**
     * 创建布局
     */
    private fun createLayout(): View {
        val context = requireContext()
        
        // 创建SwipeRefreshLayout
        swipeRefreshLayout = SwipeRefreshLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
            )
        }
        
        // 创建容器布局
        val containerLayout = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
        }
        
        // 创建RecyclerView
        recyclerView = RecyclerView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
        containerLayout.addView(recyclerView)
        
        // 创建空状态视图
        emptyStateView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            text = "暂无摘要数据"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
            visibility = View.GONE
        }
        containerLayout.addView(emptyStateView)
        
        swipeRefreshLayout.addView(containerLayout)
        
        return swipeRefreshLayout
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupSwipeRefresh()
        observeViewModel()
    }
    
    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        adapter = SummaryAdapter { summary ->
            // 点击摘要项，打开详情页面
            val intent = Intent(requireContext(), NotificationDetailActivity::class.java).apply {
                putExtra("summary", summary)
                putExtra("type", "summary")
            }
            startActivity(intent)
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@SummariesFragment.adapter
            
            // 添加分隔线
            addItemDecoration(DividerItemDecoration(
                requireContext(),
                DividerItemDecoration.VERTICAL
            ))
        }
    }
    
    /**
     * 设置下拉刷新
     */
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
        }
    }
    
    /**
     * 观察ViewModel数据变化
     */
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // 观察摘要数据
            viewModel.summaries.collect { summaries ->
                adapter.submitList(summaries)
                
                // 显示或隐藏空状态视图
                if (summaries.isEmpty()) {
                    emptyStateView.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyStateView.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // 观察加载状态
            viewModel.isLoading.collect { isLoading ->
                swipeRefreshLayout.isRefreshing = isLoading
            }
        }
    }
    
    companion object {
        /**
         * 创建Fragment实例
         */
        fun newInstance(): SummariesFragment {
            return SummariesFragment()
        }
    }
} 