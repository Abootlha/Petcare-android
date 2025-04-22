package com.basic.petproject.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.basic.petproject.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class CustomBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr) {

    private val selectedItemBackground: Drawable? = ContextCompat.getDrawable(context, R.drawable.bottom_nav_item_selected)
    private val selectedItemPaint = Paint()
    private var selectedItemPosition = 0

    init {
        // Set up item selection listener
        setOnItemSelectedListener { item ->
            // Find the position of the selected item
            for (i in 0 until menu.size()) {
                if (menu.getItem(i).itemId == item.itemId) {
                    selectedItemPosition = i
                    break
                }
            }
            invalidate()
            true
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // No items or invalid position - skip drawing
        if (menu.size() <= 0 || selectedItemPosition < 0 || selectedItemPosition >= menu.size()) {
            return
        }

        // Get item view bounds
        val selectedItemView = getChildAt(selectedItemPosition) ?: return
        val itemWidth = selectedItemView.width
        val itemHeight = selectedItemView.height
        
        // Calculate center position of the selected item
        val rect = Rect()
        selectedItemView.getGlobalVisibleRect(rect)
        
        // Adjust coordinates relative to this view
        rect.offset(-left, -top)
        
        // Calculate center coordinates
        val centerX = rect.left + rect.width() / 2
        val centerY = rect.top + rect.height() / 2
        
        // Set background drawable bounds and draw it
        selectedItemBackground?.let {
            val backgroundSize = resources.getDimensionPixelSize(R.dimen.bottom_nav_selected_size)
            val halfBackgroundSize = backgroundSize / 2
            it.setBounds(
                centerX - halfBackgroundSize,
                centerY - halfBackgroundSize,
                centerX + halfBackgroundSize,
                centerY + halfBackgroundSize
            )
            it.draw(canvas)
        }
    }
} 