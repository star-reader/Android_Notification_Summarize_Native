package top.usagijin.summary.ui.utils

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.elevation.SurfaceColors
import top.usagijin.summary.R

/**
 * 权限状态卡片构建器
 */
class PermissionCardBuilder(private val context: Context) {
    
    private var onPermissionClickListener: (() -> Unit)? = null
    
    fun setOnPermissionClick(listener: () -> Unit): PermissionCardBuilder {
        onPermissionClickListener = listener
        return this
    }
    
    fun build(): MaterialCardView {
        val card = MaterialCardView(context).apply {
            radius = context.resources.getDimension(R.dimen.card_corner_radius)
            cardElevation = context.resources.getDimension(R.dimen.card_elevation)
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.status_warning_background))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = context.resources.getDimensionPixelSize(R.dimen.margin_medium)
            }
        }
        
        val cardContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.margin_medium),
                context.resources.getDimensionPixelSize(R.dimen.margin_medium),
                context.resources.getDimensionPixelSize(R.dimen.margin_medium),
                context.resources.getDimensionPixelSize(R.dimen.margin_medium)
            )
        }
        
        // 顶部容器（图标 + 标题）
        val headerContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = context.resources.getDimensionPixelSize(R.dimen.margin_small)
            }
        }
        
        // 警告图标
        val warningIcon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                context.resources.getDimensionPixelSize(R.dimen.icon_size_small),
                context.resources.getDimensionPixelSize(R.dimen.icon_size_small)
            ).apply {
                rightMargin = context.resources.getDimensionPixelSize(R.dimen.margin_small)
            }
            setImageResource(R.drawable.ic_warning)
            imageTintList = ContextCompat.getColorStateList(context, R.color.status_warning)
        }
        headerContainer.addView(warningIcon)
        
        // 标题
        val titleText = TextView(context).apply {
            text = "需要权限"
            textSize = context.resources.getDimension(R.dimen.text_size_medium) / context.resources.displayMetrics.scaledDensity
            setTextColor(ContextCompat.getColor(context, R.color.on_surface))
            typeface = Typeface.DEFAULT_BOLD
        }
        headerContainer.addView(titleText)
        
        cardContent.addView(headerContainer)
        
        // 描述文本
        val descriptionText = TextView(context).apply {
            text = "应用需要通知访问权限才能正常工作。请点击下方按钮开启权限。"
            textSize = context.resources.getDimension(R.dimen.text_size_small) / context.resources.displayMetrics.scaledDensity
            setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = context.resources.getDimensionPixelSize(R.dimen.margin_medium)
            }
        }
        cardContent.addView(descriptionText)
        
        // 权限按钮
        val permissionButton = Button(context).apply {
            text = "开启通知权限"
            textSize = context.resources.getDimension(R.dimen.text_size_small) / context.resources.displayMetrics.scaledDensity
            setTextColor(ContextCompat.getColor(context, R.color.white))
            background = ContextCompat.getDrawable(context, R.drawable.button_primary)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START
            }
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.margin_medium),
                context.resources.getDimensionPixelSize(R.dimen.margin_small),
                context.resources.getDimensionPixelSize(R.dimen.margin_medium),
                context.resources.getDimensionPixelSize(R.dimen.margin_small)
            )
            setOnClickListener {
                onPermissionClickListener?.invoke()
            }
        }
        cardContent.addView(permissionButton)
        
        card.addView(cardContent)
        return card
    }
} 