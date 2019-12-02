package com.example.audiocapture.WaveformRenderer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import androidx.annotation.ColorInt;

import com.example.audiocapture.Interfaces.WaveformRenderer;
import com.example.audiocapture.R;

/**
 * This class renders (draws) on behalf of a calling custom View.
 * In this case, this class is used to render on VisualizerView.
 */
public class SimpleWaveformRenderer implements WaveformRenderer {

    private static final String TAG = "WaveformRenderer :: ";

    @ColorInt
    private final int backgroundColor;
    private final Paint foregroundPaint;
    private final Path waveformPath;
    private final Paint fftPaint;
    private final Path fftPath;
    private Context callingContext;

    private static final int Y_FACTOR = 0xFF;
    private static final float HALF_FACTOR = 0.5f;

    private SimpleWaveformRenderer(Context context, @ColorInt int backgroundColor, Paint foregroundPaint, Path waveformPath, Paint fftPaint, Path fftPath) {
        this.callingContext = context;
        this.backgroundColor = backgroundColor;
        this.foregroundPaint = foregroundPaint;
        this.waveformPath = waveformPath;
        this.fftPaint = fftPaint;
        this.fftPath = fftPath;
    }

    static SimpleWaveformRenderer newInstance(Context context, @ColorInt int backgroundColor, @ColorInt int foregroundColor, @ColorInt int fftColor) {
        Paint paint = new Paint();
        Paint paint1 = new Paint();
        paint.setColor(foregroundColor);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint1.setColor(fftColor);
        paint1.setAntiAlias(true);
        paint1.setStyle(Paint.Style.STROKE);
        Path waveformPath = new Path();
        Path fftPath = new Path();
        return new SimpleWaveformRenderer(context, backgroundColor, paint, waveformPath, paint1, fftPath);
    }

    private void renderWaveform(byte[] waveform, float width, float height, boolean path) {
        Log.i(TAG, this.callingContext.getResources().getString(R.string.simpleWR_renderWaveform));
        // basically screen width divided by number of points
        float xInc = width / (float) waveform.length;
        // just decided on this value for Y_FACTOR
        float yInc = height / Y_FACTOR;
        int halfHeight = (int) (height * HALF_FACTOR);
        // move the waveform to the middle of the View
        if (path) {
            waveformPath.moveTo(0, halfHeight);
        } else {
            fftPath.moveTo(0, halfHeight);
        }
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
            if (path) {
                waveformPath.lineTo(xInc * i, yPosition);
            } else {
                fftPath.lineTo(xInc * i, yPosition);
            }
        }

        if (path) {
            waveformPath.lineTo(width, halfHeight);
        } else {
            fftPath.lineTo(width, halfHeight);
        }
    }


    private void renderBlank(float width, float height, boolean path) {
        Log.i(TAG, this.callingContext.getResources().getString(R.string.simpleWR_renderBlank));
        int y = (int) (height * HALF_FACTOR);
        if (path) {
            waveformPath.moveTo(0, y);
            waveformPath.lineTo(width, y);
        } else {
            fftPath.moveTo(0, y);
            fftPath.lineTo(width, y);
        }

    }

    /**
     * render()
     * This method draws an array of waveforms on a canvas.
     * As of now, the 0th element of {@param waveform} is waveform data
     * while the second is FFT data. Both can be null, which means blank data
     * should be rendered.
     * This method makes used of {@link #renderWaveform(byte[], float, float, boolean)}
     * and {@link #renderBlank(float, float, boolean)} to draw on the canvas.
     * @param canvas
     * @param waveform
     */
    @Override
    public void render(Canvas canvas, byte[][] waveform) {
        canvas.drawColor(backgroundColor);
        float width = canvas.getWidth();
        float height = canvas.getHeight();
        waveformPath.reset();
        fftPath.reset();
        if (waveform != null) {
            if (waveform[0] != null) {
                renderWaveform(waveform[0], width, height, true);
            } else {
                renderBlank(width, height, this.callingContext.getResources().getBoolean(R.bool.simpleWR_isWaveform));
            }
            if (waveform[1] != null) {
                renderWaveform(waveform[1], width, height, this.callingContext.getResources().getBoolean(R.bool.simpleWR_isFFT));
            } else {
                renderBlank(width, height, false);
            }
        } else {
            renderBlank(width, height, this.callingContext.getResources().getBoolean(R.bool.simpleWR_isWaveform));
            renderBlank(width, height, this.callingContext.getResources().getBoolean(R.bool.simpleWR_isFFT));
        }
        canvas.drawPath(waveformPath, foregroundPaint);
        canvas.drawPath(fftPath, fftPaint);
    }
}
