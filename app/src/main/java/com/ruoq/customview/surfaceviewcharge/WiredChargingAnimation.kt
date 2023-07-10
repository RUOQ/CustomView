package com.ruoq.customview.surfaceviewcharge


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.BadTokenException
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import com.ruoq.customview.R


class WiredChargingAnimation(
    context: Context,
    @Nullable looper: Looper?,
    batteryLevel: Int,
    isDozing: Boolean
) {
    private val mCurrentWirelessChargingView: WiredChargingView?

    /**
     * Show the view for the specified duration.
     */
    fun show() {
        if (mCurrentWirelessChargingView?.mNextView == null
        ) {
            throw RuntimeException("setView must have been called")
        }

        /*if (mPreviousWirelessChargingView != null) {
            mPreviousWirelessChargingView.hide(0);
        }*/mPreviousWirelessChargingView = mCurrentWirelessChargingView
        mCurrentWirelessChargingView.show()
        mCurrentWirelessChargingView.hide(DURATION)
    }

    private class WiredChargingView(
        context: Context?,
        @Nullable looper: Looper?,
        batteryLevel: Int,
        isDozing: Boolean
    ) {
        private val mParams = WindowManager.LayoutParams()
        private val mHandler: Handler
        private val mGravity: Int
        private var mView: View? = null
        var mNextView: View?
        private var mWM: WindowManager? = null
        fun show() {
            if (DEBUG) Log.d(
                TAG,
                "SHOW: $this"
            )
            mHandler.obtainMessage(SHOW).sendToTarget()
        }

        fun hide(duration: Long) {
            mHandler.removeMessages(HIDE)
            if (DEBUG) Log.d(
                TAG,
                "HIDE: $this"
            )
            mHandler.sendMessageDelayed(Message.obtain(mHandler, HIDE), duration)
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun handleShow() {
            if (DEBUG) {
                Log.d(
                    TAG, "HANDLE SHOW: " + this + " mView=" + mView + " mNextView="
                            + mNextView
                )
            }
            if (mView !== mNextView) {
                // remove the old view if necessary
                handleHide()
                mView = mNextView
                var context: Context = mView!!.context.applicationContext
                val packageName: String = mView!!.context.opPackageName
                if (context == null) {
                    context = mView!!.context
                }
                mWM = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                mParams.packageName = packageName

                if (mView!!.parent != null) {
                    if (DEBUG) Log.d(
                        TAG,
                        "REMOVE! $mView in $this"
                    )
                    mWM!!.removeView(mView)
                }
                if (DEBUG) Log.d(
                    TAG,
                    "ADD! $mView in $this"
                )
                try {
                    mWM!!.addView(mView, mParams)
                } catch (e: BadTokenException) {
                    Log.d(
                        TAG,
                        "Unable to add wireless charging view. $e"
                    )
                }
            }
        }

        private fun handleHide() {
            if (DEBUG) Log.d(
                TAG,
                "HANDLE HIDE: $this mView=$mView"
            )
            if (mView != null) {
                if (mView!!.parent != null) {
                    if (DEBUG) Log.d(
                        TAG,
                        "REMOVE! $mView in $this"
                    )
                    mWM!!.removeViewImmediate(mView)
                }
                mView = null
            }
        }

        companion object {
            private const val SHOW = 0
            private const val HIDE = 1
        }

        init {
            //mNextView = new WirelessChargingLayout(context, batteryLevel, isDozing);
            var looper = looper
            mNextView =
                LayoutInflater.from(context).inflate(R.layout.wired_charging_layout, null, false)
            val shcyBubbleViscosity: BubbleViscosity = mNextView!!.findViewById(R.id.shcy_bubble_view)
            shcyBubbleViscosity.setBatteryLevel(batteryLevel.toString() + "")
            mGravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER
            val params = mParams
            params.height = WindowManager.LayoutParams.MATCH_PARENT
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.format = PixelFormat.TRANSLUCENT
            params.type = WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG
            params.title = "Charging Animation"
            params.flags = (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            params.dimAmount = .3f
            if (looper == null) {
                // Use Looper.myLooper() if looper is not specified.
                looper = Looper.myLooper()
                if (looper == null) {
                    throw RuntimeException(
                        "Can't display wireless animation on a thread that has not called "
                                + "Looper.prepare()"
                    )
                }
            }
            mHandler = object : Handler(looper, null) {
                @RequiresApi(Build.VERSION_CODES.Q)
                override fun handleMessage(msg: Message) {
                    when (msg.what) {
                        SHOW -> {
                            handleShow()
                        }
                        HIDE -> {
                            handleHide()
                            // Don't do this in handleHide() because it is also invoked by
                            // handleShow()
                            mNextView = null
                            isShowingWiredChargingAnimation = false
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val DURATION: Long = 3333
        private const val TAG = "WiredChargingAnimation"
        private const val DEBUG = true
        @SuppressLint("StaticFieldLeak")
        private var mPreviousWirelessChargingView: WiredChargingView? = null
        var isShowingWiredChargingAnimation = false
            private set

        fun makeWiredChargingAnimation(
            context: Context,
            @Nullable looper: Looper?, batteryLevel: Int, isDozing: Boolean
        ): WiredChargingAnimation {
            isShowingWiredChargingAnimation = true
            Log.d(
                TAG,
                "makeWiredChargingAnimation batteryLevel=$batteryLevel"
            )
            return WiredChargingAnimation(context, looper, batteryLevel, isDozing)
        }
    }

    init {
        mCurrentWirelessChargingView = WiredChargingView(
            context, looper,
            batteryLevel, isDozing
        )
    }
}
