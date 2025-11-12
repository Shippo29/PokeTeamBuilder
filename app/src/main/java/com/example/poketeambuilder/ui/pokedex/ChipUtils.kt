package com.example.poketeambuilder.ui.pokedex

import android.content.Context
import android.graphics.drawable.GradientDrawable

object ChipUtils {
    fun createPillDrawable(context: Context, color: Int, cornerDp: Float = 12f): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.cornerRadius = context.resources.displayMetrics.density * cornerDp
        drawable.setColor(color)
        return drawable
    }
}
