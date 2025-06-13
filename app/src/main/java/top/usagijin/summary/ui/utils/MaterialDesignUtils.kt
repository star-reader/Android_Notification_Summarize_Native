package top.usagijin.summary.ui.utils

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.android.material.elevation.SurfaceColors
import top.usagijin.summary.R

/**
 * Material Design UI工具类
 */
object MaterialDesignUtils {
    
    /**
     * 创建统计卡片
     */
    fun createStatsCard(
        context: Context,
        title: String,
        value: String,
        icon: Int,
        color: Int
    ): MaterialCardView {
        val card = MaterialCardView(context).apply {
            radius = context.resources.getDimension(R.dimen.card_corner_radius)
            cardElevation = context.resources.getDimension(R.dimen.card_elevation)
            setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))
            strokeWidth = 1
            strokeColor = ContextCompat.getColor(context, R.color.card_stroke)
            isClickable = true
            isFocusable = true
            
            // 添加触摸反馈
            foreground = ContextCompat.getDrawable(context, android.R.drawable.list_selector_background)
        }
        
        val cardContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.margin_medium),
                context.resources.getDimensionPixelSize(R.dimen.margin_large),
                context.resources.getDimensionPixelSize(R.dimen.margin_medium),
                context.resources.getDimensionPixelSize(R.dimen.margin_large)
            )
        }
        
        // 图标
        val iconView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                context.resources.getDimensionPixelSize(R.dimen.icon_size_medium),
                context.resources.getDimensionPixelSize(R.dimen.icon_size_medium)
            ).apply {
                bottomMargin = context.resources.getDimensionPixelSize(R.dimen.margin_small)
            }
            setImageResource(icon)
            imageTintList = ContextCompat.getColorStateList(context, color)
        }
        cardContent.addView(iconView)
        
        // 数值
        val valueText = TextView(context).apply {
            text = value
            textSize = context.resources.getDimension(R.dimen.text_size_large) / context.resources.displayMetrics.scaledDensity
            setTextColor(ContextCompat.getColor(context, R.color.on_surface))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        cardContent.addView(valueText)
        
        // 标题
        val titleText = TextView(context).apply {
            text = title
            textSize = context.resources.getDimension(R.dimen.text_size_small) / context.resources.displayMetrics.scaledDensity
            setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 0)
        }
        cardContent.addView(titleText)
        
        card.addView(cardContent)
        return card
    }
    
    /**
     * 创建操作卡片
     */
    fun createActionCard(
        context: Context,
        title: String,
        subtitle: String,
        icon: Int,
        onClick: () -> Unit
    ): MaterialCardView {
        val card = MaterialCardView(context).apply {
            radius = context.resources.getDimension(R.dimen.card_corner_radius)
            cardElevation = context.resources.getDimension(R.dimen.card_elevation)
            setCardBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))
            strokeWidth = 1
            strokeColor = ContextCompat.getColor(context, R.color.card_stroke)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            
            // 添加触摸反馈和涟漪效果
            foreground = ContextCompat.getDrawable(context, android.R.drawable.list_selector_background)
        }
        
        val cardContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.margin_medium),
                context.resources.getDimensionPixelSize(R.dimen.margin_medium),
                context.resources.getDimensionPixelSize(R.dimen.margin_medium),
                context.resources.getDimensionPixelSize(R.dimen.margin_medium)
            )
        }
        
        // 图标
        val iconView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                context.resources.getDimensionPixelSize(R.dimen.icon_size_medium),
                context.resources.getDimensionPixelSize(R.dimen.icon_size_medium)
            ).apply {
                bottomMargin = context.resources.getDimensionPixelSize(R.dimen.margin_small)
            }
            setImageResource(icon)
            imageTintList = ContextCompat.getColorStateList(context, R.color.primary)
        }
        cardContent.addView(iconView)
        
        // 标题
        val titleText = TextView(context).apply {
            text = title
            textSize = context.resources.getDimension(R.dimen.text_size_medium) / context.resources.displayMetrics.scaledDensity
            setTextColor(ContextCompat.getColor(context, R.color.on_surface))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        cardContent.addView(titleText)
        
        // 副标题
        val subtitleText = TextView(context).apply {
            text = subtitle
            textSize = context.resources.getDimension(R.dimen.text_size_caption) / context.resources.displayMetrics.scaledDensity
            setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 0)
        }
        cardContent.addView(subtitleText)
        
        card.addView(cardContent)
        return card
    }
    
    /**
     * 创建信息卡片
     */
    fun createInfoCard(
        context: Context,
        title: String,
        content: String,
        icon: Int? = null,
        backgroundColor: Int? = null
    ): MaterialCardView {
        val card = MaterialCardView(context).apply {
            radius = context.resources.getDimension(R.dimen.card_corner_radius)
            cardElevation = context.resources.getDimension(R.dimen.card_elevation)
            setCardBackgroundColor(
                backgroundColor?.let { ContextCompat.getColor(context, it) }
                    ?: SurfaceColors.SURFACE_1.getColor(context)
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = context.resources.getDimensionPixelSize(R.dimen.margin_medium)
            }
        }
        
        val cardContent = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.margin_medium),
                context.resources.getDimensionPixelSize(R.dimen.margin_medium),
                context.resources.getDimensionPixelSize(R.dimen.margin_medium),
                context.resources.getDimensionPixelSize(R.dimen.margin_medium)
            )
        }
        
        // 图标（可选）
        icon?.let {
            val iconView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    context.resources.getDimensionPixelSize(R.dimen.icon_size_small),
                    context.resources.getDimensionPixelSize(R.dimen.icon_size_small)
                ).apply {
                    rightMargin = context.resources.getDimensionPixelSize(R.dimen.margin_small)
                }
                setImageResource(it)
                imageTintList = ContextCompat.getColorStateList(context, R.color.on_surface)
            }
            cardContent.addView(iconView)
        }
        
        // 文本容器
        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        
        // 标题
        val titleText = TextView(context).apply {
            text = title
            textSize = context.resources.getDimension(R.dimen.text_size_medium) / context.resources.displayMetrics.scaledDensity
            setTextColor(ContextCompat.getColor(context, R.color.on_surface))
            typeface = Typeface.DEFAULT_BOLD
        }
        textContainer.addView(titleText)
        
        // 内容
        val contentText = TextView(context).apply {
            text = content
            textSize = context.resources.getDimension(R.dimen.text_size_small) / context.resources.displayMetrics.scaledDensity
            setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
            setPadding(0, 4, 0, 0)
        }
        textContainer.addView(contentText)
        
        cardContent.addView(textContainer)
        card.addView(cardContent)
        
        return card
    }
    
    /**
     * 创建列表项
     */
    fun createListItem(
        context: Context,
        title: String,
        subtitle: String? = null,
        leadingIcon: Int? = null,
        trailingIcon: Int? = null,
        onClick: (() -> Unit)? = null
    ): View {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                context.resources.getDimensionPixelSize(R.dimen.margin_medium),
                context.resources.getDimensionPixelSize(R.dimen.margin_small),
                context.resources.getDimensionPixelSize(R.dimen.margin_medium),
                context.resources.getDimensionPixelSize(R.dimen.margin_small)
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            
            onClick?.let { clickHandler ->
                isClickable = true
                isFocusable = true
                setOnClickListener { clickHandler() }
                
                // 添加触摸反馈
                val outValue = android.util.TypedValue()
                context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
                setBackgroundResource(outValue.resourceId)
            }
        }
        
        // 前置图标
        leadingIcon?.let {
            val iconView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    context.resources.getDimensionPixelSize(R.dimen.icon_size_small),
                    context.resources.getDimensionPixelSize(R.dimen.icon_size_small)
                ).apply {
                    rightMargin = context.resources.getDimensionPixelSize(R.dimen.margin_medium)
                }
                setImageResource(it)
                imageTintList = ContextCompat.getColorStateList(context, R.color.on_surface_variant)
            }
            container.addView(iconView)
        }
        
        // 文本容器
        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        
        // 标题
        val titleText = TextView(context).apply {
            text = title
            textSize = context.resources.getDimension(R.dimen.text_size_medium) / context.resources.displayMetrics.scaledDensity
            setTextColor(ContextCompat.getColor(context, R.color.on_surface))
        }
        textContainer.addView(titleText)
        
        // 副标题
        subtitle?.let {
            val subtitleText = TextView(context).apply {
                text = it
                textSize = context.resources.getDimension(R.dimen.text_size_small) / context.resources.displayMetrics.scaledDensity
                setTextColor(ContextCompat.getColor(context, R.color.on_surface_variant))
                setPadding(0, 2, 0, 0)
            }
            textContainer.addView(subtitleText)
        }
        
        container.addView(textContainer)
        
        // 后置图标
        trailingIcon?.let {
            val iconView = ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    context.resources.getDimensionPixelSize(R.dimen.icon_size_small),
                    context.resources.getDimensionPixelSize(R.dimen.icon_size_small)
                ).apply {
                    leftMargin = context.resources.getDimensionPixelSize(R.dimen.margin_medium)
                }
                setImageResource(it)
                imageTintList = ContextCompat.getColorStateList(context, R.color.on_surface_variant)
            }
            container.addView(iconView)
        }
        
        return container
    }
} 