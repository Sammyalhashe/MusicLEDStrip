package com.example.audiocapture.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.example.audiocapture.Interfaces.WaveformRenderer;
import com.example.audiocapture.R;

import java.util.Arrays;

/**
 * Custom View that is where I draw the waveforms.
 * It is passed a helper instance of a custom renderer class
 * that draws on the View's canvas by passing it a reference to
 * this View's canvas.
 */
public class VisualizerView extends View {
    private static final String TAG = "VisualizerView :: ";
    private byte[][] waveform;
    private WaveformRenderer waveformRenderer;

    public VisualizerView(Context context) {
        super(context);
    }

    public VisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * setWaveformRenderer()
     * Sets this classes renderer for the canvas.
     * The canvas is set in setWaveformRenderer()
     * @param renderer
     */
    public void setWaveformRenderer(WaveformRenderer renderer) {
        Log.i(TAG, getResources().getString(R.string.visView_setWaveformRenderer));
        this.waveformRenderer = renderer;
    }

    /**
     * onDraw()
     * This method is called whenever the View is initialized
     * or the view is invalidated, like how it is in {@link #setWaveform(byte[][])}
     * @param canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.i(TAG, getResources().getString(R.string.visView_onDraw));
        if (waveformRenderer != null) {
            waveformRenderer.render(canvas, waveform);
        }
    }

    public void setWaveform(byte[][] waveform) {
        Log.i(TAG, getResources().getString(R.string.visView_setWaveform));
        // We have to make a copy as the waveform data is
        // supposedly only valid within the scope of a waveform callback
        this.waveform = Arrays.copyOf(waveform, waveform.length);
        // this forces the view to be redrawn. AKA onDraw will be called sometime in the future
        invalidate();
    }
}
