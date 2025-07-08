package com.example.servomotor;


/*
HECHO POR RODRIGO SOSA ROMERO
GITHUB: https://github.com/05545
 */

/*
Este archivo es un renderizado para la vista dinámica del elemento Tacometro en la vista principal.
Se hace como extends ya que lo que se busca es que tenga dinamismo al interactuar con los datos
internos de la app y todos aquellos recibidos desde el dispositivo esp32, con el potenciometro.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class TachometerView extends View {
    private Paint paintArc;
    private Paint paintProgress;
    private Paint paintNeedle;
    private Paint paintCenter;
    private Paint paintText;
    private RectF rectF;

    private float currentAngle = 90;
    private float targetAngle = 90;
    private static final float START_ANGLE = 135;
    private static final float SWEEP_ANGLE = 270;

    public TachometerView(Context context) {
        super(context);
        init();
    }

    public TachometerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintArc = new Paint();
        paintArc.setColor(Color.parseColor("#E0E0E0"));
        paintArc.setStyle(Paint.Style.STROKE);
        paintArc.setStrokeWidth(20);
        paintArc.setAntiAlias(true);
        paintArc.setStrokeCap(Paint.Cap.ROUND);
        paintProgress = new Paint();
        paintProgress.setColor(Color.parseColor("#6C0C91"));
        paintProgress.setStyle(Paint.Style.STROKE);
        paintProgress.setStrokeWidth(20);
        paintProgress.setAntiAlias(true);
        paintProgress.setStrokeCap(Paint.Cap.ROUND);
        paintNeedle = new Paint();
        paintNeedle.setColor(Color.parseColor("#E74C3C"));
        paintNeedle.setStyle(Paint.Style.STROKE);
        paintNeedle.setStrokeWidth(4);
        paintNeedle.setAntiAlias(true);
        paintNeedle.setStrokeCap(Paint.Cap.ROUND);
        paintCenter = new Paint();
        paintCenter.setColor(Color.parseColor("#2C3E50"));
        paintCenter.setStyle(Paint.Style.FILL);
        paintCenter.setAntiAlias(true);
        paintText = new Paint();
        paintText.setColor(Color.parseColor("#7F8C8D"));
        paintText.setTextSize(24);
        paintText.setAntiAlias(true);
        paintText.setTextAlign(Paint.Align.CENTER);

        rectF = new RectF();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int radius = Math.min(width, height) / 2 - 40;
        int centerX = width / 2;
        int centerY = height / 2;

        rectF.set(centerX - radius, centerY - radius,
                centerX + radius, centerY + radius);

        canvas.drawArc(rectF, START_ANGLE, SWEEP_ANGLE, false, paintArc);
        float progressAngle = (currentAngle / 180f) * SWEEP_ANGLE;

        canvas.drawArc(rectF, START_ANGLE, progressAngle, false, paintProgress);
        drawGraduations(canvas, centerX, centerY, radius);
        drawNeedle(canvas, centerX, centerY, radius - 30);
        canvas.drawCircle(centerX, centerY, 12, paintCenter);

        if (Math.abs(currentAngle - targetAngle) > 0.5f) {
            currentAngle += (targetAngle - currentAngle) * 0.1f;
            invalidate();
        }
    }

    private void drawGraduations(Canvas canvas, int centerX, int centerY, int radius) {
        Paint gradPaint = new Paint();
        gradPaint.setColor(Color.parseColor("#BDC3C7"));
        gradPaint.setStrokeWidth(2);
        gradPaint.setAntiAlias(true);

        for (int i = 0; i <= 180; i += 30) {
            float angle = START_ANGLE + (i / 180f) * SWEEP_ANGLE;
            float radian = (float) Math.toRadians(angle);

            float startX = centerX + (radius - 15) * (float) Math.cos(radian);
            float startY = centerY + (radius - 15) * (float) Math.sin(radian);
            float endX = centerX + radius * (float) Math.cos(radian);
            float endY = centerY + radius * (float) Math.sin(radian);

            canvas.drawLine(startX, startY, endX, endY, gradPaint);

            float textX = centerX + (radius + 25) * (float) Math.cos(radian);
            float textY = centerY + (radius + 25) * (float) Math.sin(radian) + 8;
            canvas.drawText(String.valueOf(i), textX, textY, paintText);
        }
    }

    private void drawNeedle(Canvas canvas, int centerX, int centerY, int radius) {
        float angle = START_ANGLE + (currentAngle / 180f) * SWEEP_ANGLE;
        float radian = (float) Math.toRadians(angle);

        float endX = centerX + radius * (float) Math.cos(radian);
        float endY = centerY + radius * (float) Math.sin(radian);

        canvas.drawLine(centerX, centerY, endX, endY, paintNeedle);
    }

    public void setAngle(float angle) {
        targetAngle = Math.max(0, Math.min(180, angle));
        invalidate();
    }

    public float getCurrentAngle() {
        return currentAngle;
    }
}