package com.example.audiocapture.Services;

import android.bluetooth.BluetoothSocket;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class AudioCaptureService implements MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener{
    private static final String TAG = "AudioCaptureService";


    private MediaRecorder recorder;
    private BluetoothSocket bluetoothService;

    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ParcelFileDescriptor[] descriptors = ParcelFileDescriptor.createPipe();
    ParcelFileDescriptor parcelRead = new ParcelFileDescriptor(descriptors[0]);
    ParcelFileDescriptor parcelWrite = new ParcelFileDescriptor(descriptors[1]);
    InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(parcelRead);

    AudioCaptureService() throws IOException {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.WEBM);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.VORBIS);
        recorder.setOutputFile(parcelWrite.getFileDescriptor());

        recorder.prepare();

    }

    public static class AudioCaptureThread extends Thread implements MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener{
        private MediaRecorder recorder;
        private BluetoothSocket bluetoothService;

        private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ParcelFileDescriptor[] descriptors = ParcelFileDescriptor.createPipe();
        ParcelFileDescriptor parcelRead = new ParcelFileDescriptor(descriptors[0]);
        ParcelFileDescriptor parcelWrite = new ParcelFileDescriptor(descriptors[1]);
        InputStream inputStream = new ParcelFileDescriptor.AutoCloseInputStream(parcelRead);

        public AudioCaptureThread(BluetoothSocket socket) throws IOException {
            bluetoothService = socket;
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.REMOTE_SUBMIX);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.WEBM);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.VORBIS);
            recorder.setOutputFile(parcelWrite.getFileDescriptor());

            recorder.prepare();
        }

        public void startService() throws IOException {
            int read;

            Log.d(TAG, "AudioCaptureService trying to start");
            recorder.start();
            byte[] data = new byte[16384];

            Log.d(TAG, "AudioCaptureService started looking for audio data...");
            while ((read = inputStream.read(data, 0, data.length)) != -1) {
                bluetoothService.getOutputStream().write(data, 0, read);
                outputStream.write(data, 0, read);
            }
        }
        public void run() {
            Log.d(TAG, "AudioCaptureService run...");
            try {
                Log.d(TAG, "AudioCaptureService run try...");
                startService();
            } catch (IOException e) {
                Log.e(TAG, "startService() failed, e");
            }
        }

        /**
         * Called when an error occurs while recording.
         *
         * @param mr    the MediaRecorder that encountered the error
         * @param what  the type of error that has occurred:
         * @param extra an extra code, specific to the error type
         */
        @Override
        public void onError(MediaRecorder mr, int what, int extra) {

        }

        /**
         * Called to indicate an info or a warning during recording.
         *
         * @param mr    the MediaRecorder the info pertains to
         * @param what  the type of info or warning that has occurred
         * @param extra an extra code, specific to the info type
         */
        @Override
        public void onInfo(MediaRecorder mr, int what, int extra) {

        }
    }


    public static AudioCaptureService getInstance() throws IOException {
        return new AudioCaptureService();
    }

    public void setBluetoothConnection(BluetoothSocket service) {
        bluetoothService = service;
    }

    public void startService() throws IOException {
        int read;

        recorder.start();
        byte[] data = new byte[16384];

        while ((read = inputStream.read(data, 0, data.length)) != -1) {
            bluetoothService.getOutputStream().write(data, 0, read);
            outputStream.write(data, 0, read);
        }
    }


    /**
     * Called when an error occurs while recording.
     *
     * @param mr    the MediaRecorder that encountered the error
     * @param what  the type of error that has occurred:
     *              <ul>
     *              </ul>
     * @param extra an extra code, specific to the error type
     */
    @Override
    public void onError(MediaRecorder mr, int what, int extra) {

    }

    /**
     * Called to indicate an info or a warning during recording.
     *
     * @param mr    the MediaRecorder the info pertains to
     * @param what  the type of info or warning that has occurred
     * @param extra an extra code, specific to the info type
     */
    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {

    }

    public class AudioBinder extends Binder {
        public AudioCaptureService getService() {
            return AudioCaptureService.this;
        }
    }
}
