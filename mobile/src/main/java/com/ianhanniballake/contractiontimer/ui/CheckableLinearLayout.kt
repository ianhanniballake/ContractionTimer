package com.ianhanniballake.contractiontimer.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import android.widget.LinearLayout

/**
 * This is a simple wrapper for [LinearLayout] that implements the [Checkable]
 * interface by keeping an internal 'checked' state flag.
 *
 * This can be used as the root view for a custom list item layout for
 * [android.widget.AbsListView] elements with a
 * [choiceMode][android.widget.AbsListView.setChoiceMode] set.
 *
 * From https://developer.android.com/samples/CustomChoiceList/src/com.example.android.customchoicelist/CheckableLinearLayout.html
 */
class CheckableLinearLayout(
        context: Context,
        attrs: AttributeSet
) : LinearLayout(context, attrs), Checkable {
    companion object {
        private val CHECKED_STATE_SET = intArrayOf(android.R.attr.state_checked)
    }

    private var checked = false

    override fun isChecked(): Boolean {
        return checked
    }

    override fun setChecked(b: Boolean) {
        if (b != checked) {
            checked = b
            refreshDrawableState()
        }
    }

    override fun toggle() {
        isChecked = !checked
    }

    public override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (isChecked) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET)
        }
        return drawableState
    }
}
