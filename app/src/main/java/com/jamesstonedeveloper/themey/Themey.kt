package com.jamesstonedeveloper.themey

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.animation.addListener
import kotlin.math.abs
import kotlin.math.hypot

class Themey {

    lateinit var context: Context
    lateinit var oldThemeSnapshot: Bitmap
    lateinit var newThemeSnapshot: Bitmap
    lateinit var oldThemeImageView: ImageView
    lateinit var newThemeImageView: ImageView
    lateinit var themeLayout: ViewGroup
    var currentTheme: Int = -1
    var shouldKeepTheme: Boolean = false
    var elevation: Float = 10f
    var centerX = 0
    var centerY = 0
    var circleAnimation: CircleAnimation = CircleAnimation.NONE
    var showSnapshot = false
    val PREFS_NAME = "THEME_CHANGE_PREFS"
    val THEME_KEY = "THEME"
    var animationDuration: Long = 1000
    var isAnimating = false


    companion object {
        private var instance: Themey = Themey()
    }

    fun setRootLayout(rootLayout: ViewGroup) {
        themeLayout = rootLayout
        if (showSnapshot) {
            themeLayout.addView(oldThemeImageView)
            oldThemeImageView.bringToFront()
        }
        initThemeLayout()
    }

    private fun initThemeLayout() {
        themeLayout.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(v: View?, left: Int, top: Int, right: Int, bottom: Int, oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int) {
                initTheme()
                themeLayout.removeOnLayoutChangeListener(this)
            }
        })
    }

    fun initTheme() {
        if (showSnapshot) {

            when(circleAnimation) {
                CircleAnimation.NONE -> return
                CircleAnimation.INWARD -> drawAnimationInwards()
                CircleAnimation.OUTWARD -> drawAnimationOutwards()
            }


        } else {

        }
    }

    private fun drawAnimationInwards() {
        oldThemeImageView.visibility = View.VISIBLE
        oldThemeImageView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val animator = ViewAnimationUtils.createCircularReveal(oldThemeImageView, centerX, centerY, getCircleRadius(), 0f)
            animator.duration = animationDuration
            animator.addListener(object: AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    isAnimating = true
                }
                override fun onAnimationEnd(animation: Animator?) {
                    themeLayout.removeView(oldThemeImageView)
                    isAnimating = false
                    resetVariables()
                }
                override fun onAnimationRepeat(animation: Animator?) { }
                override fun onAnimationCancel(animation: Animator?) {}
            })
            animator.start()
        }
    }

    private fun drawAnimationOutwards() {
        newThemeSnapshot = createScreenshot()
        oldThemeImageView.visibility = View.VISIBLE
        newThemeImageView = newThemeSnapshot.createImageView(context, elevation)
        newThemeImageView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val animator = ViewAnimationUtils.createCircularReveal(newThemeImageView, centerX, centerY, getCircleRadius(), 0f)
            animator.duration = animationDuration
            animator.addListener(object: AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    isAnimating = true
                }
                override fun onAnimationEnd(animation: Animator?) {
                    themeLayout.removeView(oldThemeImageView)
                    themeLayout.removeView(newThemeImageView)
                    isAnimating = false
                    resetVariables()
                }
                override fun onAnimationRepeat(animation: Animator?) { }
                override fun onAnimationCancel(animation: Animator?) {}
            })
            animator.start()
        }
        themeLayout.addView(newThemeImageView)
        newThemeImageView.bringToFront()
    }

    private fun createScreenshot(): Bitmap {
        val bitmap = Bitmap.createBitmap(themeLayout.width, themeLayout.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        themeLayout.draw(canvas)
        return bitmap
    }

    private fun resetVariables() {
        centerY = 0
        centerX = 0
        showSnapshot = false
    }

    private fun getCircleRadius(): Float {
        var longestRadius = 0f
        val cornerCoordinates  = listOf<Pair<Int, Int>>(Pair(0 ,0), Pair(themeLayout.width ,0), Pair(0, themeLayout.height), Pair(themeLayout.width, themeLayout.height))

        for (coordinates in cornerCoordinates) {
            var xDifference = coordinates.first - centerX
            var yDifference = coordinates.second - centerY
            xDifference = abs(xDifference)
            yDifference = abs(yDifference)
            val radius = hypot(xDifference.toDouble(), yDifference.toDouble())
            longestRadius = if (radius > longestRadius) radius.toFloat() else longestRadius
        }
        return longestRadius
    }

    public enum class CircleAnimation {
        NONE,
        INWARD,
        OUTWARD
    }

}

fun Themey.init(activityContext: Context, rootLayout: ViewGroup, shouldKeepTheme: Boolean) {
    context = activityContext
    this.shouldKeepTheme = shouldKeepTheme

    if (showSnapshot) {
        oldThemeImageView = oldThemeSnapshot.createImageView(context, elevation)
        oldThemeImageView.visibility = View.GONE
    }

    setRootLayout(rootLayout)

}

fun Themey.delayedInit(activityContext: Context, shouldKeepTheme: Boolean) {
    context = activityContext
    this.shouldKeepTheme = shouldKeepTheme

    if (showSnapshot) {
        oldThemeImageView = oldThemeSnapshot.createImageView(context, elevation)
        oldThemeImageView.visibility = View.GONE
    }

    val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, 0)
    currentTheme = sharedPreferences.getInt(THEME_KEY, -1)
    context.setTheme(currentTheme)
}

fun Bitmap.createImageView(context: Context, elevation: Float?) : ImageView {
   val imageView = ImageView(context)
    imageView.setImageBitmap(this)
    imageView.layoutParams = ViewGroup.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
    imageView.scaleType = ImageView.ScaleType.FIT_XY
    imageView.elevation = elevation ?: 0f
    return imageView
}


