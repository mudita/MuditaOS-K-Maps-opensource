package com.mudita.search.ui.utils

import android.content.Context
import android.graphics.drawable.VectorDrawable
import androidx.annotation.DrawableRes

fun isCorrectDrawable(context: Context, @DrawableRes resId: Int): Boolean {
    val drawable = context.getDrawable(resId)
    return drawable is VectorDrawable
}