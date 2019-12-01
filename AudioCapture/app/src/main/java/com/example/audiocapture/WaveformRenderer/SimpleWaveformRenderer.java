package com.example.audiocapture.WaveformRenderer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import androidx.annotation.ColorInt;

import com.example.audiocapture.Interfaces.WaveformRenderer;

public class SimpleWaveformRenderer implements WaveformRenderer {

    private static final String TAG = "SimpleWaveformRenderer :: ";

    @ColorInt
    private final int backgroundColor;
    private final Paint foregroundPaint;
    private final Path waveformPath;

    private static final int Y_FACTOR = 0xFF;
    private static final float HALF_FACTOR = 0.5f;

    private SimpleWaveformRenderer(@ColorInt int backgroundColor, Paint foregroundPaint, Path waveformPath) {
        this.backgroundColor = backgroundColor;
        this.foregroundPaint = foregroundPaint;
        this.waveformPath = waveformPath;
    }

    public static SimpleWaveformRenderer newInstance(@ColorInt int backgroundColor, @ColorInt int foregroundColor) {
        Paint paint = new Paint();
        paint.setColor(foregroundColor);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        Path waveformPath = new Path();
        return new SimpleWaveformRenderer(backgroundColor, paint, waveformPath);
    }

    private void renderWaveform(byte[] waveform, float width, float height) {
        Log.i(TAG, "renderWaveform");
        // basically screen width divided by number of points
        float xInc = width / (float) waveform.length;
        // just decided on this value for Y_FACTOR
        float yInc = height / Y_FACTOR;
        int halfHeight = (int) (height * HALF_FACTOR);
        // move the waveform to the middle of the View
        waveformPath.moveTo(0, halfHeight);
        /*
            Turns out that the byte values are bounded between -127 - 128
            However, while the positive values remain true to what normally they
            should be (larger positive being higher amplitude and vice-versa),
            negative amplitudes are inversed. That is -127 is a smaller amplitude while
            -1 is a large amplitude.
            Therefore, for negative values we invert the amplitudes
         */
        for (int i = 1; i < waveform.length; i++) {
            float yPosition = waveform[i] > 0 ? height - (yInc * waveform[i]) : -(yInc * waveform[i]);
            waveformPath.lineTo(xInc * i, yPosition);
        }
        waveformPath.lineTo(width, halfHeight);
    }


    private void renderBlank(float width, float height) {
        Log.i(TAG, "renderBlank");
        int y = (int) (height * HALF_FACTOR);
        waveformPath.moveTo(0, y);
        waveformPath.lineTo(width, y);
    }

    @Override
    public void render(Canvas canvas, byte[] waveform) {
        canvas.drawColor(backgroundColor);
        float width = canvas.getWidth();
        float height = canvas.getHeight();
        waveformPath.reset();
        if (waveform != null) {
            renderWaveform(waveform, width, height);
        } else {
            renderBlank(width, height);
        }
        canvas.drawPath(waveformPath, foregroundPaint);
    }
}
