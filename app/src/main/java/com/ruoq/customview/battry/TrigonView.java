package com.ruoq.customview.battry;



import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * 三角形
 * Created by LGL on 2016/1/7.
 */
public class TrigonView extends View {

    //无参
    public TrigonView(Context context) {
        super(context);
    }

    //有参
    public TrigonView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint p = new Paint();
        p.setColor(Color.BLACK);
        //实例化路径
        Path path = new Path();

        path.moveTo(80, 200);// 此点为多边形的起点
        path.lineTo(120, 250);
        path.lineTo(80, 250);
        path.close(); // 使这些点构成封闭的多边形
        canvas.drawPath(path, p);

    }
}

