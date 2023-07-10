package com.ruoq.customview.battry

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View


class BatteryView : View {
    //电量
    private var mPower : Float = 0f
    //画笔宽度
    private val mBatteryStrokeWidth = 20f
    //电池宽度
    private val mBatteryWidth = 70f * 2
    private val mBatteryHeight = 30f * 2

    private lateinit var mPath:Path

    //电池头部的宽高
    private var mCapHeight = 30f
    private var mCapWidth = 20f
    private lateinit var mPaint: Paint
    private lateinit var mCapRect : RectF
    private lateinit var mBatteryRect : RectF
    private lateinit var mPowerRect : RectF

    //
    private val mPowerPadding = 1f
    private val mPowerHeight = mBatteryHeight - mBatteryStrokeWidth - mPowerPadding * 2
    private val mPowerWidth = mBatteryWidth - mBatteryStrokeWidth - mPowerPadding * 2

    constructor(context: Context):this(context, null)
    constructor(context: Context, attrs: AttributeSet?):this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr:Int):super(context, attrs, defStyleAttr){

        init()
    }

    private fun init(){

        mPaint = Paint()
        mPath = Path()


    }

    override fun onDraw(canvas: Canvas){
        super.onDraw(canvas)

        val p = mPaint
        p.color = Color.BLACK
        //实例化路径
        //实例化路径
        val path = Path()

        path.moveTo(80f, 200f) // 此点为多边形的起点

        path.lineTo(80f, 450f)
        path.lineTo(320f, 450f)
        path.close() // 使这些点构成封闭的多边形

        canvas.drawPath(path, p)


    }


    public fun setPower(power: Float){
        mPower = power
        if(mPower > 100){
            mPower = 100f
        }else if(mPower < 0){
            mPower = 0f
        }

        mPowerRect = RectF(
             mBatteryStrokeWidth,
             mBatteryStrokeWidth ,
            mPower/100 * (mBatteryWidth -  mBatteryStrokeWidth),
            mBatteryHeight - mBatteryStrokeWidth
        )

        invalidate()
    }

    @SuppressLint("Recycle")
    public fun setChangeAnimator(){
        val valueAnimator = ValueAnimator.ofInt(20,50,80,100).apply {
            duration = 3000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
        }
        valueAnimator.addUpdateListener {
            val value = it.animatedValue as Int
            setPower(value.toFloat())
        }
        valueAnimator.start()
    }



}