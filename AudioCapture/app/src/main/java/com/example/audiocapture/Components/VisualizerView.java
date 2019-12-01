package com.example.audiocapture.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.example.audiocapture.Interfaces.WaveformRenderer;

import java.util.Arrays;

/**
 * TODO: document your custom view class.
 */
public class VisualizerView extends View {
    private static final String TAG = "VisualizerView :: ";
    private byte[] waveform;
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

    public void setWaveformRenderer(WaveformRenderer renderer) {
        Log.i(TAG, "setWaveformRenderer");
        this.waveformRenderer = renderer;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.i(TAG, "onDraw");
        if (waveformRenderer != null) {
            waveformRenderer.render(canvas, waveform);
        }
    }

    public void setWaveform(byte[] waveform) {
        Log.i(TAG, "setWaveform");
        // We have to make a copy as the waveform data is
        // supposedly only valid within the scope of a waveform callback
        this.waveform = Arrays.copyOf(waveform, waveform.length);
        // this forces the view to be redrawn. AKA onDraw will be called sometime in the future
        invalidate();
    }
}
