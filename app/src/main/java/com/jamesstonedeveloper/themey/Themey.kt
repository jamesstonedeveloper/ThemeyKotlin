package com.jamesstonedeveloper.themey

import android.animation.Animator
import android.animation.Animator.AnimatorListener
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewAnimationUtils
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.animation.addListener
import kotlin.math.abs
import kotlin.math.hypot

class Themey {

    private lateinit var context: Context
    private lateinit var oldThemeSnapshot: Bitmap
    private lateinit var newThemeSnapshot: Bitmap
    private lateinit var oldThemeImageView: ImageView
    private lateinit var newThemeImageView: ImageView
    private lateinit var themeLayout: ViewGroup
    private var currentTheme: Int = -1
    private var shouldKeepTheme: Boolean = false
    private var centerX = 0
    private var centerY = 0
    private var circleAnimation: CircleAnimation = CircleAnimation.INWARD
    private var showSnapshot = false
    private val PREFS_NAME = "THEME_CHANGE_PREFS"
    private val THEME_KEY = "THEME"
    private var isAnimating = false
    var animationDuration: Long = 1000
    var elevation: Float = 10f
    var defaultTheme: Int = -1

    companion object {
        var instance: Themey = Themey()
    }

    fun init(activityContext: Context, rootLayout: ViewGroup, shouldKeepTheme: Boolean) {
        this.context = activityContext
        this.shouldKeepTheme = shouldKeepTheme

        if (showSnapshot) {
            oldThemeImageView = oldThemeSnapshot.createImageView(context)
            oldThemeImageView.visibility = View.GONE
        }
        setRootLayout(rootLayout)
    }

    fun delayedInit(activityContext: Context, shouldKeepTheme: Boolean) {
        this.context = activityContext
        this.shouldKeepTheme = shouldKeepTheme

        if (showSnapshot) {
            oldThemeImageView = oldThemeSnapshot.createImageView(context)
            oldThemeImageView.visibility = View.GONE
        }

        val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, 0)
        currentTheme = sharedPreferences.getInt(THEME_KEY, -1)
        context.setTheme(currentTheme)
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
            override fun onLayoutChange(
                    v: View?,
                    left: Int,
                    top: Int,
                    right: Int,
                    bottom: Int,
                    oldLeft: Int,
                    oldTop: Int,
                    oldRight: Int,
                    oldBottom: Int
            ) {
                initTheme()
                themeLayout.removeOnLayoutChangeListener(this)
            }
        })
    }

    private fun initTheme() {
        when {
            showSnapshot -> {
                when (circleAnimation) {
                    CircleAnimation.NONE -> return
                    CircleAnimation.INWARD -> drawAnimationInwards()
                    CircleAnimation.OUTWARD -> drawAnimationOutwards()
                }
            }
            shouldKeepTheme -> {
                val sharedPreferences = context.getSharedPreferences(PREFS_NAME, 0)
                val keptTheme = sharedPreferences.getInt(THEME_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                if (isDayNightTheme(keptTheme)) {
                    AppCompatDelegate.setDefaultNightMode(keptTheme)
                }
            }
            else -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
        }
    }

    fun toggleDayNight() {
        changeTheme(if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES)
    }

    fun toggleDayNight(circleAnimation: CircleAnimation) {
        changeTheme(
                if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES,
                circleAnimation
        )
    }

    fun toggleDayNight(circleAnimation: CircleAnimation, centerX: Int, centerY: Int) {
        changeTheme(
                if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES,
                centerX,
                centerY
        )
    }

    fun toggleDayNight(centerX: Int, centerY: Int) {
        changeTheme(
                if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES,
                centerX,
                centerY
        )
    }

    fun changeTheme(theme: Int) {
        if (!isAnimating) {
            changeTheme(theme, circleAnimation)
        }
    }

    fun changeTheme(theme: Int, circleAnimation: CircleAnimation) {
        if (!isAnimating) {
            changeTheme(theme, circleAnimation, 0, 0)
        }
    }

    fun changeTheme(theme: Int, centerX: Int, centerY: Int) {
        if (!isAnimating) {
            changeTheme(theme, circleAnimation, centerX, centerY)
        }
    }

    fun changeTheme(theme: Int, circleAnimation: CircleAnimation, centerX: Int, centerY: Int) {
        if (!isAnimating) {
            if (isDayNightTheme(theme)) {
                if (isCurrentDayNightTheme(theme) && isCurrentCustomTheme(defaultTheme)) {
                    return
                }
            } else {
                if (isCurrentCustomTheme(theme)) {
                    return
                }
            }
            this.circleAnimation = circleAnimation
            this.centerX = centerX
            this.centerY = centerY
            applyTheme(theme)
        }
    }

    private fun applyTheme(theme: Int) {
        oldThemeSnapshot = createScreenshot()
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, 0)
        sharedPreferences.edit().putInt(THEME_KEY, theme).apply()
        showSnapshot = true
        currentTheme = theme

        if (isDayNightTheme(theme)) {
            if (AppCompatDelegate.getDefaultNightMode() == theme) {
                changeTheme(defaultTheme, circleAnimation, centerX, centerY)
                return
            }
            AppCompatDelegate.setDefaultNightMode(theme)
            if (defaultTheme != -1) {
                changeTheme(defaultTheme, circleAnimation, centerX, centerY)
            }
        } else {
            (context as? Activity)?.recreate()
        }

    }

    private fun drawAnimationInwards() {
        oldThemeImageView.visibility = View.VISIBLE
        oldThemeImageView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val animator = ViewAnimationUtils.createCircularReveal(
                    oldThemeImageView,
                    centerX,
                    centerY,
                    getCircleRadius(),
                    0f
            )
            animator.duration = animationDuration
            animator.addListener(object : AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    isAnimating = true
                }

                override fun onAnimationEnd(animation: Animator?) {
                    themeLayout.removeView(oldThemeImageView)
                    isAnimating = false
                    resetVariables()
                }

                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {}
            })
            animator.start()
        }
    }

    private fun drawAnimationOutwards() {
        newThemeSnapshot = createScreenshot()
        oldThemeImageView.visibility = View.VISIBLE
        newThemeImageView = newThemeSnapshot.createImageView(context)
        newThemeImageView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            val animator = ViewAnimationUtils.createCircularReveal(
                    newThemeImageView,
                    centerX,
                    centerY,
                    0f,
                    getCircleRadius()
            )
            animator.duration = animationDuration
            animator.addListener(object : AnimatorListener {
                override fun onAnimationStart(animation: Animator?) {
                    isAnimating = true
                }

                override fun onAnimationEnd(animation: Animator?) {
                    themeLayout.removeView(oldThemeImageView)
                    themeLayout.removeView(newThemeImageView)
                    isAnimating = false
                    resetVariables()
                }

                override fun onAnimationRepeat(animation: Animator?) {}
                override fun onAnimationCancel(animation: Animator?) {}
            })
            animator.start()
        }
        themeLayout.addView(newThemeImageView)
        newThemeImageView.bringToFront()
    }

    private fun createScreenshot(): Bitmap {
        val bitmap =
                Bitmap.createBitmap(themeLayout.width, themeLayout.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        themeLayout.draw(canvas)
        return bitmap
    }

    private fun resetVariables() {
        centerY = 0
        centerX = 0
        showSnapshot = false
        circleAnimation = CircleAnimation.INWARD
    }

    private fun getCircleRadius(): Float {
        var longestRadius = 0f
        val cornerCoordinates = listOf<Pair<Int, Int>>(
                Pair(0, 0),
                Pair(themeLayout.width, 0),
                Pair(0, themeLayout.height),
                Pair(themeLayout.width, themeLayout.height)
        )

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

    private fun isDayNightTheme(theme: Int): Boolean {
        return theme == AppCompatDelegate.MODE_NIGHT_YES ||
                theme == AppCompatDelegate.MODE_NIGHT_NO ||
                theme == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM ||
                theme == AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY
    }

    private fun isCurrentDayNightTheme(theme: Int): Boolean {
        return AppCompatDelegate.getDefaultNightMode() == theme
    }

    private fun isCurrentCustomTheme(theme: Int): Boolean {
        return currentTheme == theme
    }

    enum class CircleAnimation {
        NONE,
        INWARD,
        OUTWARD
    }

}

fun Bitmap.createImageView(context: Context): ImageView {
    val imageView = ImageView(context)
    imageView.setImageBitmap(this)
    imageView.layoutParams = ViewGroup.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
    )
    imageView.scaleType = ImageView.ScaleType.FIT_XY
    imageView.elevation = Themey.instance.elevation
    return imageView
}