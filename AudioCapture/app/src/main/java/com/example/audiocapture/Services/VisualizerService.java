package com.example.audiocapture.Services;

import android.app.Service;
import android.content.Intent;
import android.media.audiofx.Visualizer;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.audiocapture.R;
import com.google.android.gms.common.util.ArrayUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class VisualizerService extends Service {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static final String TAG = "VISUALIZER_SERVICE :: ";
    private final IBinder binder = new VisualizerBinder();

    // following are for capturing audio data
    private boolean isCapturing = false;
    private static long MAX_IDLE_TIME_MS = 3000;
    private Visualizer mVisualizer;
    private boolean visInitialized = false;
    private byte[] mRawAudioData;
    private int[] mFormattedAudioData;
    private byte [] mRawNullData = new byte[0];
    private int [] mFormattedNullData = new int[0];
    private long lastCapturedTimeMS;


    private AsyncTask mBackgroundTask;
    private AsyncHttpServer httpServer;
    private List<WebSocket> webSockets;

    private class BackgroundTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            int[] res;
            byte[] res3;
            List<Integer> res2;
            Map<String, List<Integer>> map = new HashMap<>();
            Log.i(TAG, "Inside service");
            while (isCapturing) {
                res = getFormattedData(1, 1);
                res3 = getRawData();

                res2 = new ArrayList<>(res.length);
                for (int j: res) {
                    res2.add(j);
                }
                map.put("arr", res2);
                /*
                db.collection("AudioData")
                        .document("data")
                        .set(map);
                 */

                for (WebSocket socket: webSockets) {
                    socket.send(res3);
                }

                SystemClock.sleep(MAX_IDLE_TIME_MS);
            }
            return null;
        }
    }


    private String getDeviceWifiAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        try {
            int ip_address = wifiManager.getConnectionInfo().getIpAddress();
            return InetAddress.getByAddress(
                    ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(ip_address).array()
            ).getHostAddress();
        } catch (UnknownHostException e) {
            Log.e(TAG, e.toString());
            return "0.0.0.0";
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        Log.i(TAG, String.format("ip address: %s", 1));
        // get devices ip address and stream it to firebase for synchronization
        String ip = getDeviceWifiAddress();
        Map<String, String> ipMap = new HashMap<>();
        ipMap.put("address", ip);
        db.collection("AudioData")
                .document("ipi")
                .set(ipMap);
        // initialize websocket
        webSockets = new ArrayList<>();
        httpServer = new AsyncHttpServer();
        httpServer.setErrorCallback(new CompletedCallback() {
            @Override
            public void onCompleted(Exception ex) {
                Log.e(TAG, String.format("%s", ex));
            }
        });
        httpServer.listen(AsyncServer.getDefault(), 5000);

        httpServer.websocket("/ws", new AsyncHttpServer.WebSocketRequestCallback() {
            @Override
            public void onConnected(WebSocket webSocket, AsyncHttpServerRequest request) {
                webSocket.send("Hello");
                webSockets.add(webSocket);
            }
        });

        visInitialized = this.initializeVisualizer();
        return binder;
    }

    private boolean initializeVisualizer() {
        int[] range = Visualizer.getCaptureSizeRange();
        int maxSize = range[1];
        mRawAudioData = new byte[maxSize];
        mFormattedAudioData = new int[maxSize];

        mVisualizer = null;
        isCapturing = false;


        try {
            // the 0 flag will take audio from any arbitrary
            // audio stream instead of a specific one
            // If I used a non-zero flag, it would be for a specific
            // audio instance
            mVisualizer = new Visualizer(0);

            if (mVisualizer != null) {
                // saw this piece of code on a google repository
                // my guess is that it just intitially disables
                // audio capture - as we don't need it now
                if (mVisualizer.getEnabled()) {
                    mVisualizer.setEnabled(isCapturing);
                }
                mVisualizer.setCaptureSize(mRawAudioData.length);
            }
            return true;
        } catch (UnsupportedOperationException e) {
            Log.e(TAG, getResources().getString(R.string.vis_cstor_unsupportedop));
        } catch (IllegalStateException e) {
            Log.e(TAG, getResources().getString(R.string.vis_cstor_illegalstate));
        } catch (RuntimeException e) {
            Log.e(TAG, getResources().getString(R.string.vis_cstor_runtime));
        }
        return false;
    }

    public boolean startCapturingAudio() {
        Log.i(TAG, getResources().getString(R.string.data_capture_begin));
        if (mVisualizer != null) {
            if (!mVisualizer.getEnabled()) {
                try {
                    Log.i(TAG, "Enabling Visualizer");
                    isCapturing = true;
                    mVisualizer.setEnabled(true);
                    lastCapturedTimeMS = System.currentTimeMillis();
                } catch (IllegalStateException e) {
                    isCapturing = false;
                    Log.e(TAG, "start() IllegalStateException");
                }
            }
        }
        return true;
    }

    public void streamData() {
        Log.i(TAG, "streamData()");
        this.mBackgroundTask = new BackgroundTask().execute();
    }

    public boolean stopStreaming() {
        if (this.mBackgroundTask != null) {
            this.mBackgroundTask.cancel(true);
            return true;
        }
        return false;
    }

    public byte[] getRawData() {
        if (captureData()) {
            return mRawAudioData;
        } else {
            return mRawNullData;
        }
    }

    public int[] getFormattedData(int num, int den) {
        if (captureData()) {
            for (int i = 0; i < mFormattedAudioData.length; i++) {
                int tmp = ((int)mRawAudioData[i] & 0xFF) - 128;
                mFormattedAudioData[i] = (tmp * num) / den;
            }
            return mFormattedAudioData;
        } else {
            return mFormattedNullData;
        }
    }

    private boolean captureData() {
        int status = Visualizer.ERROR;
        boolean result = true;

        try {
            if (mVisualizer != null) {
                status = mVisualizer.getWaveForm(mRawAudioData);
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, String.format("captureData() IllegalStateException: %s", this));
        } finally {
            if (status != Visualizer.SUCCESS) {
                Log.e(TAG, String.format("captureData() :  %s error: %s", this, status));
                result = false;
            } else {
                byte nullValue = 0;
                int i;

                nullValue = (byte) 0x80;

                // return a sort of "silence idicator" if the silence lasts
                // longer than MAX_IDLE_TIME_MS
                for (i = 0; i < mRawAudioData.length; i++) {
                    if (mRawAudioData[i] != nullValue) break;
                }

                if (i == mRawAudioData.length) {
                    if ((System.currentTimeMillis() - lastCapturedTimeMS) > MAX_IDLE_TIME_MS) {
                        result = false;
                    }
                } else {
                    lastCapturedTimeMS = System.currentTimeMillis();
                }
            }
        }
        return result;
    }

    /**
     * This innner class is a local binder that allows the "client"
     * Activity to interact with the "server" Service
     */
    public class VisualizerBinder extends Binder {
        public VisualizerService getService() {
            // return an instance of the service which the Bound
            // activity can interact with
            return VisualizerService.this;
        }
    }
}
