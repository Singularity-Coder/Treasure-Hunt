package com.singularitycoder.treasurehunt

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.RelativeLayout
import com.singularitycoder.treasurehunt.helpers.color
import kotlin.math.min

// https://github.com/skyfishjy/android-ripple-background
class RippleView : RelativeLayout {

    companion object {
        private const val DEFAULT_RIPPLE_COUNT = 6
        private const val DEFAULT_DURATION = 3000
        private const val DEFAULT_SCALE = 6.0f
        private const val DEFAULT_FILL_TYPE = 0
    }

    var isRippleAnimationRunning = false
        private set

    private var rippleColor = 0
    private var rippleStrokeWidth = 0f
    private var rippleRadius = 0f
    private var rippleDurationTime = 0
    private var rippleAmount = 0
    private var rippleDelay = 0
    private var rippleScale = 0f
    private var rippleType = 0
    private val rippleViewList = ArrayList<RippleView>()

    private lateinit var paint: Paint
    private lateinit var animatorSet: AnimatorSet
    private lateinit var rippleParams: LayoutParams
    private lateinit var animatorList: ArrayList<Animator>

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        if (isInEditMode) return
        requireNotNull(attrs) { "Attributes should be provided to this view," }
        setViewAttributesFromXml(context, attrs)
        createViewShape()
        animateViewShape()
    }

    private fun setViewAttributesFromXml(context: Context, attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RippleView)
        rippleColor = typedArray.getColor(
            R.styleable.RippleView_rv_color,
            context.color(R.color.purple_200)
        )
        rippleStrokeWidth = typedArray.getDimension(
            R.styleable.RippleView_rv_strokeWidth,
            resources.getDimension(R.dimen.rippleStrokeWidth)
        )
        rippleRadius = typedArray.getDimension(
            R.styleable.RippleView_rv_radius,
            resources.getDimension(R.dimen.rippleRadius)
        )
        rippleDurationTime = typedArray.getInt(R.styleable.RippleView_rv_duration, DEFAULT_DURATION)
        rippleAmount = typedArray.getInt(R.styleable.RippleView_rv_rippleAmount, DEFAULT_RIPPLE_COUNT)
        rippleScale = typedArray.getFloat(R.styleable.RippleView_rv_scale, DEFAULT_SCALE)
        rippleType = typedArray.getInt(R.styleable.RippleView_rv_type, DEFAULT_FILL_TYPE)
        typedArray.recycle()
    }

    private fun createViewShape() {
        rippleDelay = rippleDurationTime / rippleAmount
        paint = Paint()
        paint.isAntiAlias = true
        if (rippleType == DEFAULT_FILL_TYPE) {
            rippleStrokeWidth = 0f
            paint.style = Paint.Style.FILL
        } else paint.style = Paint.Style.STROKE
        paint.color = rippleColor
        rippleParams = LayoutParams(
            (2 * (rippleRadius + rippleStrokeWidth)).toInt(),
            (2 * (rippleRadius + rippleStrokeWidth)).toInt()
        )
        rippleParams.addRule(CENTER_IN_PARENT, TRUE)
    }

    private fun animateViewShape() {
        animatorSet = AnimatorSet()
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorList = ArrayList()

        for (i in 0 until rippleAmount) {
            val rippleView = RippleView(context)
            addView(rippleView, rippleParams)
            rippleViewList.add(rippleView)

            val scaleXAnimator = ObjectAnimator.ofFloat(
                /* target = */ rippleView,
                /* propertyName = */ "ScaleX",
                /* ...values = */ 1.0f, rippleScale
            ).apply {
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                startDelay = (i * rippleDelay).toLong()
                duration = rippleDurationTime.toLong()
            }
            val scaleYAnimator = ObjectAnimator.ofFloat(
                /* target = */ rippleView,
                /* propertyName = */ "ScaleY",
                /* ...values = */ 1.0f, rippleScale
            ).apply {
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                startDelay = (i * rippleDelay).toLong()
                duration = rippleDurationTime.toLong()
            }
            val alphaAnimator = ObjectAnimator.ofFloat(
                /* target = */ rippleView,
                /* propertyName = */ "Alpha",
                /* ...values = */ 1.0f, 0f
            ).apply {
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                startDelay = (i * rippleDelay).toLong()
                duration = rippleDurationTime.toLong()
            }

            animatorList.add(scaleXAnimator)
            animatorList.add(scaleYAnimator)
            animatorList.add(alphaAnimator)
        }
        animatorSet.playTogether(animatorList)
    }

    fun startRippleAnimation() {
        if (!isRippleAnimationRunning) {
            for (rippleView in rippleViewList) {
                rippleView.visibility = VISIBLE
            }
            animatorSet.start()
            isRippleAnimationRunning = true
        }
    }

    fun stopRippleAnimation() {
        if (isRippleAnimationRunning) {
            animatorSet.end()
            isRippleAnimationRunning = false
        }
    }

    private inner class RippleView(context: Context?) : View(context) {
        init {
            this.visibility = INVISIBLE
        }

        override fun onDraw(canvas: Canvas) {
            val radius = min(width, height) / 2
            canvas.drawCircle(
                /* cx = */ radius.toFloat(),
                /* cy = */ radius.toFloat(),
                /* radius = */ radius - rippleStrokeWidth,
                /* paint = */ paint
            )
        }
    }
}