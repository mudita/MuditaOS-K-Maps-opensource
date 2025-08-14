package com.mudita.myplaces.ui.add.views

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View.OnTouchListener
import android.widget.EditText
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import com.mudita.kompakt.commonUi.KompaktTypography900
import com.mudita.kompakt.commonUi.components.DashedHorizontalDivider
import com.mudita.map.myplaces.R


@SuppressLint("ClickableViewAccessibility")
@Composable
fun EditPlaceInput(
    modifier: Modifier = Modifier,
    value: String? = null,
    title: String? = null,
    maxLines: Int = 3,
    onTextChanged: (String) -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(horizontal = 16.dp),
    ) {
        if (title != null && isFocused.not()) Text(
            text = title,
            style = KompaktTypography900.labelLarge
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            //TODO refactor to compose TextField
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth(),
                factory = { context ->
                    val et = EditText(context)
                    var lastText = ""
                    et.setText(value.orEmpty())
                    et.textSize = 18f
                    et.setBackgroundColor(Color.White.toArgb())
                    et.maxLines = maxLines
                    et.isSingleLine = false
                    et.setOnTouchListener(OnTouchListener { _, event ->
                        if (et.text.isNotEmpty() && isFocused) {
                            val drawableRight = 2
                            if (event.action == MotionEvent.ACTION_UP) {
                                if (event.rawX >= et.right - et.compoundDrawables[drawableRight].bounds.width()
                                ) {
                                    et.setText("")
                                    onTextChanged(et.text.toString())
                                    return@OnTouchListener true
                                }
                            }
                        }
                        false
                    })
                    et.setOnFocusChangeListener { _, hasFocus ->
                        if (et.text.isNotEmpty() && hasFocus) et.setCompoundDrawablesWithIntrinsicBounds(
                            null,
                            null,
                            ContextCompat.getDrawable(context, com.mudita.map.common.R.drawable.ic_cancel_small),
                            null
                        )
                        else et.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                        isFocused = hasFocus
                    }
                    et.doOnTextChanged { _, _, _, _ ->
                        if (et.text.isNotEmpty() && isFocused) et.setCompoundDrawablesWithIntrinsicBounds(
                            null,
                            null,
                            ContextCompat.getDrawable(context, com.mudita.map.common.R.drawable.ic_cancel_small),
                            null
                        )
                        else et.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
                        if (et.lineCount > maxLines) {
                            et.setText(lastText)
                            et.setSelection(et.text.toString().length)
                        } else {
                            lastText = et.text.toString()
                            onTextChanged(et.text.toString())
                        }
                    }
                    et
                }
            )
        }

        DashedHorizontalDivider(Modifier.fillMaxWidth())
    }
}