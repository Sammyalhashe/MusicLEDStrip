package com.example.audiocapture.WaveformRenderer;

import android.graphics.Paint;

import androidx.annotation.ColorInt;

import com.example.audiocapture.Interfaces.WaveformRenderer;

public class SimpleWaveformRendererFactory {
    public WaveformRenderer createSimpleWaveformRenderer(@ColorInt int background, @ColorInt int foreground ) {
        return SimpleWaveformRenderer.newInstance(background, foreground);
    }
}
