package com.example.motionsensorapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LifecycleOwner;

import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class SAMplerActivity extends AppCompatActivity implements ImageAnalysis.Analyzer, View.OnClickListener, SensorEventListener {
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    PreviewView previewView;
    private ImageCapture imageCapture;
    private VideoCapture videoCapture;
    private Button bRecord;
    private String filePath;
    private float averageTimeInterval;

    private static final float NS2S = 1.0f / 1000000000.0f; // Nanoseconds to seconds conversion factor
    private float timestamp;
    private float timeflow;
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private TextView gyroscopeDataTextView;
    private TextView angleTextView;
    private TextView processStateTextView;

    private float[] gyroscopeValues = new float[3];
    private float[] angle = new float[3];
    private List<MotionSensorData> motionSensorData = new ArrayList<>();
    private boolean sensorsStarted = false;
    private boolean isFirstEvent = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sampler);

        previewView = findViewById(R.id.previewView);
        bRecord = findViewById(R.id.bRecord);
        bRecord.setText("start recording"); // Set the initial text of the button

        bRecord.setOnClickListener(this);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCameraX(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, getExecutor());

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        gyroscopeDataTextView = findViewById(R.id.gyroscopeDataTextView);
        angleTextView = findViewById(R.id.angleTextView);
        processStateTextView = findViewById(R.id.processStateTextView);

        if (gyroscopeSensor == null) {
            // Handle the case where sensors are not available
        }
    }

    Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    @SuppressLint("RestrictedApi")
    private void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        Preview preview = new Preview.Builder()
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Image capture use case
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        // Video capture use case
        videoCapture = new VideoCapture.Builder()
                .setVideoFrameRate(30)
                .build();

        // Image analysis use case
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(getExecutor(), this);

        //bind to lifecycle:
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture, videoCapture);
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        // image processing here for the current frame
        Log.d("TAG", "analyze: got the frame at: " + image.getImageInfo().getTimestamp());
        image.close();
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.bRecord) {
            if (bRecord.getText() == "start recording") {
                bRecord.setText("stop recording");
                startMotionSensors();
                recordVideo();
            } else {
                bRecord.setText("start recording");
                stopMotionSensors();
                videoCapture.stopRecording();
            }
        }
    }

    @SuppressLint("RestrictedApi")
    private void recordVideo() {
        if (videoCapture != null) {

            long _timestamp = System.currentTimeMillis();

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, _timestamp);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            videoCapture.startRecording(
                    new VideoCapture.OutputFileOptions.Builder(
                            getContentResolver(),
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                    )
                    .build(),
                    getExecutor(),
                    new VideoCapture.OnVideoSavedCallback() {
                        @Override
                        public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                            Uri savedUri = outputFileResults.getSavedUri();

                            if (savedUri != null) {
                                // Query the MediaStore to get the file path from the content URI
                                String[] projection = {MediaStore.Video.Media.DATA};
                                Cursor cursor = getContentResolver().query(savedUri, projection, null, null, null);

                                if (cursor != null && cursor.moveToFirst()) {
                                    int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                                    filePath = cursor.getString(columnIndex);
                                    cursor.close();
                                    Log.d("Video Path", "onVideoSaved: " + filePath);
                                }
                            }
                            HttpClient httpClient = new HttpClient();
                            new Thread(() -> {
                                httpClient.sendMotionSensorData(motionSensorData);
                                httpClient.sendVideo(filePath);
                            }).start();
                            Toast.makeText(SAMplerActivity.this, "Video has been saved successfully.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                            Toast.makeText(SAMplerActivity.this, "Error saving video: " + message, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (isFirstEvent) {
            timestamp = event.timestamp;
            timeflow = 0.0f;
            averageTimeInterval = 0.0f;
            isFirstEvent = false;
            motionSensorData.clear();
            for(int i = 0; i < 3; i++) {
                gyroscopeValues[i] = 0.0f;
                angle[i] = 0.0f;
            }
        }
        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyroscopeValues[0] = event.values[0];
            gyroscopeValues[1] = event.values[1];
            gyroscopeValues[2] = event.values[2];
        }
        float dt = (event.timestamp - timestamp) * NS2S;
        if(dt > 1.0) dt = averageTimeInterval;
        timeflow += dt;
        for(int i = 0; i < 3; i++) {
            angle[i] += gyroscopeValues[i] * dt;
            // angle[i] = (float) ((angle[i] + 2 * Math.PI) % (4 * Math.PI) - (2 * Math.PI));
        }
        timestamp = event.timestamp;
        motionSensorData.add(new MotionSensorData(timeflow, angle[0], angle[1], angle[2]));
        averageTimeInterval = timeflow / motionSensorData.size();
        updateSensorData();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Handle changes in sensor accuracy if needed
    }

    private void updateSensorData() {
        // Update TextViews with sensor data
        String gyroscopeData = String.format("Gyroscope Data: X = %.2f, Y = %.2f, Z = %.2f",
                gyroscopeValues[0], gyroscopeValues[1], gyroscopeValues[2]);
        gyroscopeDataTextView.setText(gyroscopeData);
        String angleData = String.format("Angle Data: X = %.2f, Y = %.2f, Z = %.2f",
                angle[0], angle[1], angle[2]);
        angleTextView.setText(angleData);
    }

    private void startMotionSensors() {
        if (!sensorsStarted) {
            // Register the sensor listeners when the "Start" button is clicked
            sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
            sensorsStarted = true;
            updateProcessState("Working");
        }
    }

    private void stopMotionSensors() {
        if (sensorsStarted) {
            // Unregister the sensor listeners when the "Stop" button is clicked
            sensorManager.unregisterListener(this);
            sensorsStarted = false;
            for(int i = 0; i < motionSensorData.size(); i++) {
                motionSensorData.get(i).print();
            }
        }
    }

    private void updateProcessState(String state) {
        processStateTextView.setText("State: " + state);
    }
}
