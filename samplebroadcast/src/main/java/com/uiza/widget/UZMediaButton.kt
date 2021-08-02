package com.uiza.widget

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageButton
import com.uiza.activity.R

class UZMediaButton : AppCompatImageButton, Checkable {

    private var activeDrawableId = -1
    private var inActiveDrawableId = -1
    private var checked = false

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null) : super(context, attrs) {
        initView(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        initView(attrs, defStyleAttr)
    }

    private fun initView(attrs: AttributeSet?, defStyleAttr: Int) {
        if (attrs != null) {
            val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.UZMediaButton,
                defStyleAttr,
                0
            )
            activeDrawableId = a.getResourceId(R.styleable.UZMediaButton_srcActive, -1)
            inActiveDrawableId = a.getResourceId(R.styleable.UZMediaButton_srcInactive, -1)
        } else {
            activeDrawableId = -1
            inActiveDrawableId = -1
        }
        updateDrawable()
    }

    private fun updateDrawable() {
        if (checked) {
            setImageDrawable(AppCompatResources.getDrawable(context, activeDrawableId))
        } else {
            setImageDrawable(AppCompatResources.getDrawable(context, inActiveDrawableId))
        }
    }

    override fun isChecked(): Boolean {
        return checked
    }

    override fun setChecked(checked: Boolean) {
        if (this.checked != checked) {
            this.checked = checked
            updateDrawable()
            refreshDrawableState()
        }
    }

    override fun toggle() {
        checked = !checked
    }
}
