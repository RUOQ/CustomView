package com.ruoq.customview.surfaceviewcharge

import android.content.Context
import android.graphics.*
import kotlin.jvm.JvmOverloads
import android.view.SurfaceView
import android.view.SurfaceHolder
import android.util.AttributeSet
import android.util.TypedValue
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

class BubbleViscosity @JvmOverloads constructor(
    mContext: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(mContext, attrs, defStyleAttr), SurfaceHolder.Callback, Runnable {
    //画笔颜色
    private var paintColor = "#25DA29" //不透明圆弧颜色
    private var centerColor = "#00000000" //中间圆的颜色
    private var minCenterColor = "#9025DA29" //透明的圆弧

    //屏幕宽高
    private var screenHeight = 0
    private var screenWidth = 0

    //底部半圆的半径
    private var lastRadius = 0f
    //底部曲线的控制点
    private val rate = 0.32f
    //底部曲线的控制点2
    private val rate2 = 0.45f
    //底部圆的起点坐标
    private val lastCurveStart = PointF()
    //底部圆的结束坐标
    private val lastCurveEnd = PointF()
    //中间圆的坐标
    private val centreCirclePoint = PointF()
    //中间圆的半径
    private var centreRadius = 0f
    private var bubbleRadius = 0f

    //所有圆弧的坐标数组
    private val arcPointStart = arrayOfNulls<PointF>(8)
    private val arcPointEnd = arrayOfNulls<PointF>(8)
    private val control = arrayOfNulls<PointF>(8)
    private val arcStart = PointF()
    private val arcEnd = PointF()
    private val controlP = PointF()
    private var bubbleList: MutableList<PointF> = ArrayList()
    private var bubbleBeans: MutableList<BubbleBean> = ArrayList()
    //旋转角度
    private var rotateAngle = 0
    //圆弧的控制点
    private var controlRate = 1.66f
    //可变圆弧的控制点
    private var controlRates = 1.3f
    private var index = 0
    //圆的开口值
    private var scale = 0f

    private var arcPaint: Paint? = null
    private var minCentrePaint: Paint? = null
    private var bubblePaint: Paint? = null
    private var centrePaint: Paint? = null
    private var lastPaint: Paint? = null
    private var lastPath: Path? = null
    private var random: Random? = null
    private var textPaint: Paint? = null
    private var text = "78 %"
    private var rect: Rect? = null
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        screenHeight = measuredHeight
        screenWidth = measuredWidth
    }

    init {
        initTool()
    }

    private fun initTool() {
        rect = Rect()
        holder.addCallback(this)
        isFocusable = true
        holder.setFormat(PixelFormat.TRANSPARENT)
        setZOrderOnTop(true)
        //底部半圆的半径
        lastRadius = dip2Dimension(40f, context)
        //中间圆的半径
        centreRadius = dip2Dimension(100f, context)
        //气泡的半径
        bubbleRadius = dip2Dimension(15f, context)
        //产生随机数
        random = Random()

        //底部圆
        lastPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.parseColor(paintColor)
            strokeWidth = 2f
        }

        lastPath = Path()

        //中间圆
        centrePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            strokeWidth = 2f
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)
            color = Color.parseColor(centerColor)
        }

        arcPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.parseColor(paintColor)
            strokeWidth = 2f
        }

        //不透明圆弧的画笔
        minCentrePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.parseColor(minCenterColor)
            strokeWidth = 2f
        }

        //气泡画笔
        bubblePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.parseColor(minCenterColor)
            strokeWidth = 2f
        }

        //文字画笔
        textPaint = Paint().apply {
           isAntiAlias = true
           style = Paint.Style.FILL
           color = Color.parseColor(minCenterColor)
           strokeWidth = 2f
           textSize = dip2Dimension(40f, context)
        }

    }

    private fun onMDraw() {
        val canvas = holder!!.lockCanvas()
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        //画气泡
        bubbleDraw(canvas)
        //画中间的半圆
        lastCircleDraw(canvas)
        //中间的圆
        centreCircleDraw(canvas)
        textPaint!!.getTextBounds(text, 0, text.length, rect)
        canvas.drawText(
            text, centreCirclePoint.x - rect!!.width() / 2,
            centreCirclePoint.y + rect!!.height() / 2, textPaint!!
        )
        holder!!.unlockCanvasAndPost(canvas)
    }

    fun setBatteryLevel(level: String) {
        text = "$level%"
        postInvalidate()
    }

    private fun centreCircleDraw(canvas: Canvas) {
        centreCirclePoint[(screenWidth / 2).toFloat()] = (screenHeight / 2).toFloat()
        circleInCoordinateDraw(canvas)
        canvas.drawCircle(
            centreCirclePoint.x, centreCirclePoint.y,
            centreRadius, centrePaint!!
        )
    }

    private fun lastCircleDraw(canvas: Canvas) {
        //第一段曲线的开始坐标
        lastCurveStart[screenWidth / 2 - lastRadius] = screenHeight.toFloat()
        //第一段曲线的终点坐标
        lastCurveEnd[(screenWidth / 2).toFloat()] = screenHeight.toFloat()
        //三角函数，正切
        val k = lastRadius / 2 / lastRadius
        //任意取一个点，为控制点rate控制曲线的弯曲度
        val aX = lastRadius - lastRadius * rate2
        val aY = lastCurveStart.y - aX * k
        val bX = lastRadius - lastRadius * rate
        val bY = lastCurveEnd.y - bX * k
        lastPath!!.rewind()
        lastPath!!.moveTo(lastCurveStart.x, lastCurveStart.y)
        lastPath!!.cubicTo(
            lastCurveStart.x + aX, aY, lastCurveEnd.x - bX, bY,
            lastCurveEnd.x, lastCurveEnd.y - lastRadius / 2
        )
        lastPath!!.cubicTo(
            lastCurveEnd.x + bX, bY, lastCurveEnd.x + lastRadius
                    - aX, aY, lastCurveEnd.x + lastRadius, lastCurveEnd.y
        )
        //闭合曲线进行填充
        lastPath!!.lineTo(lastCurveStart.x, lastCurveStart.y)
        canvas.drawPath(lastPath!!, lastPaint!!)
    }

    private var bubbleIndex = 0
    private fun bubbleDraw(canvas: Canvas) {
        for (i in bubbleBeans.indices) {
            if (bubbleBeans[i].y <= (screenHeight / 2 + centreRadius).toInt()) {
                bubblePaint!!.alpha = 0
                canvas.drawCircle(
                    bubbleBeans[i].x, bubbleBeans[i]
                        .y, bubbleRadius, bubblePaint!!
                )
            } else {
                bubblePaint!!.alpha = 150
                canvas.drawCircle(
                    bubbleBeans[i].x, bubbleBeans[i]
                        .y, bubbleRadius, bubblePaint!!
                )
            }
        }
    }

    /**
     * @param dip
     * @param context
     * @return
     */
    private fun dip2Dimension(dip: Float, context: Context): Float {
        val displayMetrics = context.resources
            .displayMetrics
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dip,
            displayMetrics
        )
    }

    /**
     * @param canvas
     * 画四个圆弧
     */
    private fun circleInCoordinateDraw(canvas: Canvas) {
        var angle: Int
        for (i in arcPointStart.indices) {
            angle = if (i in 4..5) {
                if (i == 4) {
                    rotateAngle + i * 60
                } else {
                    rotateAngle + i * 64
                }
            } else if (i > 5) {
                if (i == 6) {
                    rotateAngle + i * 25
                } else {
                    rotateAngle + i * 48
                }
            } else {
                rotateAngle + i * 90
            }
            val radian = Math.toRadians(angle.toDouble()).toFloat()
            val adjacent = cos(radian.toDouble()).toFloat() * centreRadius
            val right = sin(radian.toDouble()).toFloat() * centreRadius
            val radianControl = Math.toRadians((90 - (45 + angle)).toDouble())
                .toFloat()
            val xStart = cos(radianControl.toDouble()).toFloat() * centreRadius
            val yEnd = sin(radianControl.toDouble()).toFloat() * centreRadius
            if (i == 0 || i == 1) {
                if (i == 1) {
                    arcStart[centreCirclePoint.x + adjacent - scale] =
                        centreCirclePoint.y + right + scale
                    arcEnd[centreCirclePoint.x - right] = (centreCirclePoint.y
                            + adjacent)
                } else {
                    arcStart[centreCirclePoint.x + adjacent] = centreCirclePoint.y + right
                    arcEnd[centreCirclePoint.x - right - scale] =
                        centreCirclePoint.y + adjacent + scale
                }
                controlP[centreCirclePoint.x + yEnd * controlRate] =
                    centreCirclePoint.y + xStart * controlRate
            } else {
                arcStart[centreCirclePoint.x + adjacent] = centreCirclePoint.y + right
                arcEnd[centreCirclePoint.x - right] = (centreCirclePoint.y
                        + adjacent)
                if (i > 5) {
                    controlP[centreCirclePoint.x + yEnd * controlRates] =
                        centreCirclePoint.y + xStart * controlRates
                } else {
                    controlP[centreCirclePoint.x + yEnd * controlRate] =
                        centreCirclePoint.y + xStart * controlRate
                }
            }
            arcPointStart[i] = arcStart
            arcPointEnd[i] = arcEnd
            control[i] = controlP
            lastPath!!.rewind()
            lastPath!!.moveTo(arcPointStart[i]!!.x, arcPointStart[i]!!.y)

            //绘制二阶贝塞尔曲线
            lastPath!!.quadTo(
                control[i]!!.x,
                control[i]!!.y,
                arcPointEnd[i]!!.x,
                arcPointEnd[i]!!.y
            )
            if (i in 4..5) {
                canvas.drawPath(lastPath!!, minCentrePaint!!)
            } else {
                canvas.drawPath(lastPath!!, arcPaint!!)
            }
            //清空Path中的所有直线和曲线，不保留填充模式设置，但会保留Path上相关的数据结构，以便高效地复用
            lastPath!!.rewind()
        }
    }

    private fun setAnimation() {
        setScheduleWithFixedDelay(this, 0, 5)
        setScheduleWithFixedDelay({
            if (bubbleIndex > 2) bubbleIndex = 0
            if (bubbleBeans.size < 8) {
                bubbleBeans.add(
                    BubbleBean(
                        bubbleList[bubbleIndex].x,
                        bubbleList[bubbleIndex].y,
                        (random!!.nextInt(4) + 2).toFloat(),
                        bubbleIndex
                    )
                )
            } else {
                for (i in bubbleBeans.indices) {
                    if (bubbleBeans[i].y <= (screenHeight / 2 + centreRadius).toInt()) {
                        bubbleBeans[i][bubbleList[bubbleIndex].x, bubbleList[bubbleIndex].y, (random!!.nextInt(4) + 2).toFloat()] = bubbleIndex
                        if (random!!.nextInt(bubbleBeans.size) + 3 == 3) {
                        } else {
                            break
                        }
                    }
                }
            }
            bubbleIndex++
        }, 0, 300)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        bubbleList.clear()
        setBubbleList()
        startBubbleRunnable()
        setAnimation()
    }

    override fun surfaceChanged(
        holder: SurfaceHolder, format: Int, width: Int,
        height: Int
    ) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        onDestroyThread()
    }

    override fun run() {
        index++
        rotateAngle = index
        if (index in 91..179) {
            scale += 0.25f
            if (controlRates < 1.66) controlRates += 0.005f
        } else if (index >= 180) {
            scale -= 0.12f
            if (index > 300) controlRates -= 0.01f
        }
        onMDraw()
        if (index == 360) {
            index = 0
            rotateAngle = 0
            controlRate = 1.66f
            controlRates = 1.3f
            scale = 0f
        }
    }

    private fun setBubbleList() {
        //求弧度坐标点的公式
        val radian = Math.toRadians(35.0).toFloat()
        //邻边
        val adjacent = cos(radian.toDouble()).toFloat() * lastRadius / 3
        //对边
        val right = sin(radian.toDouble()).toFloat() * lastRadius / 3
        if (bubbleList.isNotEmpty()) return
       //左边气泡的位置
        bubbleList.add(
            PointF(
                screenWidth / 2 - adjacent, screenHeight
                        - right
            )
        )
        //中间气泡的位置
        bubbleList.add(
            PointF(
                (screenWidth / 2).toFloat(), screenHeight - lastRadius
                        / 4
            )
        )
        //右边气泡的位置
        bubbleList.add(
            PointF(
                screenWidth / 2 + adjacent, screenHeight
                        - right
            )
        )
        startBubbleRunnable()
    }

    /**
     * 移动气泡的线程
     */
    private fun startBubbleRunnable() {
        setScheduleWithFixedDelay({
            for (i in bubbleBeans.indices) {
                bubbleBeans[i].setMove(
                    screenHeight,
                    (screenHeight / 2 + centreRadius).toInt()
                )
            }
        }, 0, 4)
    }

    companion object {
//        private var scheduledThreadPool: ScheduledExecutorService? = null
//        private val instance: ScheduledExecutorService?
//            get() {
//                if (scheduledThreadPool == null) {
//                    synchronized(BubbleViscosity::class.java) {
//                        if (scheduledThreadPool == null) {
//                            scheduledThreadPool = Executors
//                                .newSingleThreadScheduledExecutor()
//                        }
//                    }
//                }
//                return scheduledThreadPool
//            }
        //使用懒加载代替双重检测锁
        private val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED){
            Executors.newSingleThreadScheduledExecutor() as ScheduledExecutorService
        }


        /**
         * 定时任务，每隔多少秒执行一次
         */
        private fun setScheduleWithFixedDelay(
            var1: Runnable, var2: Long,
            var4: Long
        ) {
            instance.scheduleWithFixedDelay(
                var1, var2, var4,
                TimeUnit.MILLISECONDS
            )
        }

        fun onDestroyThread() {
            instance.shutdownNow()
        }
    }


}