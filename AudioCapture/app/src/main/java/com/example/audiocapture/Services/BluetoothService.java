package com.example.audiocapture.Services;

import android.app.Activity;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

public class BluetoothService {
    private static final boolean TESTING = false;
    private static final String TAG = "BluetoothService";
    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothA2dp a2dp;
    private BluetoothServerSocket mServerSocket;
    private BluetoothDevice mmDevice;
    private UUID deviceUUID;
    private AcceptThread mAcceptThread;
    private AudioCaptureService service;
    private AudioCaptureService.AudioCaptureThread captureThread;

    // Get the default adapter
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

    private BluetoothProfile.ServiceListener profileListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.A2DP) {
                a2dp = (BluetoothA2dp) proxy;
            }
        }

        @Override
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.A2DP) a2dp = null;
        }
    };


    private BluetoothService(Context context) {
        bluetoothAdapter.getProfileProxy(context, profileListener, BluetoothProfile.A2DP);
        Log.d(TAG, "bluetooth service constructor");
        Toast.makeText(context, "bluetooth service constructor", Toast.LENGTH_LONG);
        // connectAsServer();
    }

    boolean connectAsServer() {
        BluetoothServerSocket serverSocket = null;
        try {
            // DONE: figure out the UUID (I guess for the server [this])
            // generated with a random service: https://onlinerandomtools.com/generate-random-uuid
            serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord("APP_NAME", UUID.fromString("f434a330-d19f-4167-92c8-916d9b81195d"));
        } catch (IOException e) {
            Log.e(TAG, "Bluetooth listening failed", e);
            return false;
        }
        Log.d(TAG, "Bluetooth listening succeeded");
        mServerSocket = serverSocket;
        return true;
    }

    public void run(AudioCaptureService service) {
        BluetoothSocket socket = null;

        // essentially infinite loop until a connection is made

        while (true) {
            Log.d(TAG, "Socket looking...");
            try {
                socket = mServerSocket.accept();
            } catch (IOException e) {
                Log.e(TAG, "Socket accept() failed", e);
            }

            if (socket != null) {
                // a connnection was accepted
                // TODO: pass bluetooth connection to a seperate thread ...
                service.setBluetoothConnection(socket);
                try {
                    mServerSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Socket close() failed", e);
                }
                break;
            }
        }
    }

    private class AcceptThread extends Thread {
        private AudioCaptureService service;
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                // DONE: figure out the UUID (I guess for the server [this])
                // generated with a random service: https://onlinerandomtools.com/generate-random-uuid
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("APP_NAME", UUID.fromString("f434a330-d19f-4167-92c8-916d9b81195d"));
            } catch (IOException e) {
                Log.e(TAG, "Bluetooth listening failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;
            // essentially infinite loop until a connection is made

            while (true) {
                Log.d(TAG, "Socket looking...");
                try {
                    Log.d(TAG, "Socket accepting...");
                    socket = mmServerSocket.accept();
                    Log.d(TAG, "Socket accepting after...");
                } catch (IOException e) {
                    Log.e(TAG, "Socket accept() failed", e);
                }

                Log.d(TAG, String.format("Socket result") + socket.toString());
                if (socket != null) {
                    // a connection was accepted
                    // TODO: pass bluetooth connection to a seperate thread ...
                    // service.setBluetoothConnection(socket);
                    Log.d(TAG, "connected() about to be called");
                    try {
                        connected(socket);
                    } catch (IOException e) {
                        Log.e(TAG, "connected() failed", e);
                    }
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Socket close() failed", e);
                    }
                    break;
                }
            }
        }
    }

    public synchronized void start() {
        Log.d(TAG, "start Bluetooth thread");
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
    }


    public void connected(BluetoothSocket socket) throws IOException {
        Log.d(TAG, "connected() call");
        captureThread = new AudioCaptureService.AudioCaptureThread(socket);
        captureThread.start();
    }


    public static BluetoothService getInstance(Context context) {
        if (BluetoothAdapter.getDefaultAdapter() != null && context instanceof Activity) {
            if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                ((Activity) context).startActivityForResult(enableBTIntent, REQUEST_ENABLE_BT);
            }
            return new BluetoothService(context);
        }
        return null;
    }
}
