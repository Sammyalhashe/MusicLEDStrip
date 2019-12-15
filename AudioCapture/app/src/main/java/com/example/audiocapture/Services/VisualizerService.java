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

import java.lang.ref.WeakReference;
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

import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;


public class VisualizerService extends Service implements Visualizer.OnDataCaptureListener {
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

    // Subjects created to communicate back to the MainActivity
    private BehaviorSubject<byte[]> waveformSubject = BehaviorSubject.create();
    private BehaviorSubject<byte[]> fftSubject = BehaviorSubject.create();

    private AsyncTask mBackgroundTask;
    private AsyncHttpServer httpServer;
    private List<WebSocket> webSockets;

    /**
     * Method called when a new waveform capture is available.
     * <p>Data in the waveform buffer is valid only within the scope of the callback.
     * Applications which need access to the waveform data after returning from the callback
     * should make a copy of the data instead of holding a reference.
     *
     * @param visualizer   Visualizer object on which the listener is registered.
     * @param waveform     array of bytes containing the waveform representation.
     * @param samplingRate sampling rate of the visualized audio.
     */
    @Override
    public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
        Log.i(TAG, getResources().getString(R.string.vis_onWaveformCapture) + waveform.length);
        waveformSubject.onNext(waveform);
        for (WebSocket socket: webSockets) {
            Log.i(TAG, getResources().getString(R.string.vis_sendingSockets));
            socket.send(waveform);
        }
    }

    /**
     * Method called when a new frequency capture is available.
     * <p>Data in the fft buffer is valid only within the scope of the callback.
     * Applications which need access to the fft data after returning from the callback
     * should make a copy of the data instead of holding a reference.
     *
     * <p>In order to obtain magnitude and phase values the following formulas can
     * be used:
     * <pre class="prettyprint">
     *       for (int i = 0; i &lt; fft.size(); i += 2) {
     *           float magnitude = (float)Math.hypot(fft[i], fft[i + 1]);
     *           float phase = (float)Math.atan2(fft[i + 1], fft[i]);
     *       }</pre>
     *
     * @param visualizer   Visualizer object on which the listener is registered.
     * @param fft          array of bytes containing the frequency representation.
     *                     The fft array only contains the first half of the actual
     *                     FFT spectrum (frequencies up to Nyquist frequency), exploiting
     *                     the symmetry of the spectrum. For each frequencies bin <code>i</code>:
     *                     <ul>
     *                     <li>the element at index <code>2*i</code> in the array contains
     *                     the real part of a complex number,</li>
     *                     <li>the element at index <code>2*i+1</code> contains the imaginary
     *                     part of the complex number.</li>
     *                     </ul>
     * @param samplingRate sampling rate of the visualized audio.
     */
    @Override
    public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
        Log.i(TAG, getResources().getString(R.string.vis_onFFTDataCapture) + fft.length);
        fftSubject.onNext(fft);
    }

    /**
     * Returns the waveform subject as an Observable for the Main Activity to read from
     * @return {Observable<byte[]>}
     */
    public Observable<byte[]> getWaveformObservable() {
        // in RxJava2 they renamed asObservable() to hide() for some reason
        return waveformSubject.hide();
    }
    /**
     * Returns the fft subject as an Observable for the Main Activity to read from
     * @return {Observable<byte[]>}
     */
    public Observable<byte[]> getFFTObservable() {
        // in RxJava2 they renamed asObservable() to hide() for some reason
        return fftSubject.hide();
    }

    private static class BackgroundTask extends AsyncTask<Void, Void, Void> {

        // private WeakReference<VisualizerService> serviceWeakReference;
        // private VisualizerService serviceReference;

        BackgroundTask(VisualizerService context) {
            // this.serviceWeakReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // serviceReference = serviceWeakReference.get();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            /*
            if (serviceReference != null) {
                int[] res;
                byte[] res3;
                List<Integer> res2;
                Log.i(TAG, "Inside service");
                while (serviceReference.isCapturing) {
                    res = serviceReference.getFormattedData(1, 1);
                    res3 = serviceReference.getRawData();

                    res2 = new ArrayList<>(res.length);
                    for (int j : res) {
                        res2.add(j);
                    }

                    for (WebSocket socket : serviceReference.webSockets) {
                        socket.send(res3);
                    }

                    SystemClock.sleep(MAX_IDLE_TIME_MS);
                }
            }
            */
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
        Log.i(TAG, getResources().getString(R.string.vis_onBind));
        // get devices ip address and stream it to firebase for synchronization
        String ip = getDeviceWifiAddress();
        Log.i(TAG, String.format(getResources().getString(R.string.vis_ipIs), ip));
        Map<String, String> ipMap = new HashMap<>();
        ipMap.put(getResources().getString(R.string.vis_ipFirebaseAddressField), ip);
        db.collection(getResources().getString(R.string.vis_AudioData))
                .document(getResources().getString(R.string.vis_IpDocumentPath))
                .set(ipMap);
        // initialize websocket
        webSockets = new ArrayList<>();
        try {
            httpServer = new AsyncHttpServer();
            httpServer.setErrorCallback(new CompletedCallback() {
                @Override
                public void onCompleted(Exception ex) {
                    Log.e(TAG, String.format("%s", ex));
                }
            });
            httpServer.listen(AsyncServer.getDefault(), 5000);

            httpServer.websocket(getResources().getString(R.string.vis_websocketRegexPath), new AsyncHttpServer.WebSocketRequestCallback() {
                @Override
                public void onConnected(WebSocket webSocket, AsyncHttpServerRequest request) {
                    webSockets.add(webSocket);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "onBind: ", e);
        }


        visInitialized = this.initializeVisualizer();
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        httpServer.stop();
        return super.onUnbind(intent);
    }

    private boolean initializeVisualizer() {
        Log.i(TAG, "initializeVisualizer: initializing");
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
                // my guess is that it just initially disables
                // audio capture - as we don't need it now
                mVisualizer.setDataCaptureListener(this, Visualizer.getMaxCaptureRate(), true, true);
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
        Log.i(TAG, getResources().getString(R.string.vis_streamData));
        this.mBackgroundTask = new BackgroundTask(this).execute();
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
     * This inner class is a local binder that allows the "client"
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
