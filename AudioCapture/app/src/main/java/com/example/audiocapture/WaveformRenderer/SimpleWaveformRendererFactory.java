package com.example.audiocapture.WaveformRenderer;

import android.content.Context;
import android.graphics.Paint;

import androidx.annotation.ColorInt;

import com.example.audiocapture.Interfaces.WaveformRenderer;

public class SimpleWaveformRendererFactory {
    public WaveformRenderer createSimpleWaveformRenderer(Context context, @ColorInt int background, @ColorInt int foreground, @ColorInt int fftColor ) {
        return SimpleWaveformRenderer.newInstance(context, background, foreground, fftColor);
    }
}
