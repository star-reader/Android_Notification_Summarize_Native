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
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch
import top.usagijin.summary.R
import top.usagijin.summary.adapter.NotificationAdapter
import top.usagijin.summary.viewmodel.MainViewModel

/**
 * 通知列表Fragment - Material Design 3风格
 */
class NotificationsFragment : Fragment() {
    
    private val viewModel: MainViewModel by viewModels()
    private lateinit var notificationAdapter: NotificationAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateCard: MaterialCardView
    private lateinit var emptyStateText: MaterialTextView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initViews(view)
        setupRecyclerView()
        observeData()
    }
    
    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewNotifications)
        emptyStateCard = view.findViewById(R.id.cardEmptyState)
        emptyStateText = view.findViewById(R.id.textEmptyState)
    }
    
    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter { notification ->
            // 点击通知项的处理
            // TODO: 导航到详情页面
        }
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = notificationAdapter
            
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
    
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notifications.collect { notifications ->
                if (notifications.isEmpty()) {
                    showEmptyState()
                } else {
                    hideEmptyState()
                    notificationAdapter.submitList(notifications)
                }
            }
        }
    }
    
    private fun showEmptyState() {
        recyclerView.visibility = View.GONE
        emptyStateCard.visibility = View.VISIBLE
        emptyStateText.text = "暂无通知\n\n请确保已授予通知访问权限，\n并且有应用发送通知。"
    }
    
    private fun hideEmptyState() {
        recyclerView.visibility = View.VISIBLE
        emptyStateCard.visibility = View.GONE
    }
    
    companion object {
        fun newInstance() = NotificationsFragment()
    }
} 