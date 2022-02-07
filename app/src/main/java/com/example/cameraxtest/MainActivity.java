package com.example.cameraxtest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_CAMERA_PERMISSION = 200;
    private final String[] requiredPermissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
    };

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private VideoCapture videoCapture;
    private Button recordButton;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkPermission()) {
            startCamera();
        }

        Button captureButton = findViewById(R.id.captureButton);
        captureButton.setOnClickListener(view -> takePicture());

        recordButton = findViewById(R.id.recordButton);
        recordButton.setOnClickListener(view -> {
            if (!isRecording) {
                startRecording();
            } else {
                stopRecording();
            }
        });
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            boolean pass = (grantResults.length > 0);

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    pass = false;
                    break;
                }
            }

            if (pass) {
                startCamera();
            } else {
                Toast.makeText(this,
                        getResources().getString(R.string.request_permissions_fail),
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private boolean checkPermission() {
        boolean pass = true;

        for (String permission : requiredPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                pass = false;
                break;
            }
        }

        if (!pass) {
            ActivityCompat.requestPermissions(this, requiredPermissions, REQUEST_CAMERA_PERMISSION);
        }

        return pass;
    }

    @SuppressLint("RestrictedApi")
    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                Preview preview = new Preview.Builder().build();
                PreviewView previewView = findViewById(R.id.previewView);
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setTargetRotation(previewView.getDisplay().getRotation())
                        .build();

                videoCapture = new VideoCapture.Builder()
                        .setTargetRotation(previewView.getDisplay().getRotation())
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(getClass().getName(), e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePicture() {
        String fileName = getExternalFilesDir("").toString() + "/image_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg";

        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(new File(fileName)).build();

        imageCapture.takePicture(outputFileOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(MainActivity.this,
                                getResources().getString(R.string.take_picture_success),
                                Toast.LENGTH_LONG).show();
                        Log.d(getClass().getName(), "Saved picture to " + fileName);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(MainActivity.this,
                                getResources().getString(R.string.take_picture_fail),
                                Toast.LENGTH_LONG).show();
                        Log.e(getClass().getName(), exception.getMessage());
                        Log.e(getClass().getName(), "Failed to save picture to " + fileName);
                    }
                });
    }

    private void updateUi() {
        if (!isRecording) {
            isRecording = true;
            recordButton.setText(R.string.stop_button_label);
        } else {
            isRecording = false;
            recordButton.setText(R.string.record_button_label);
        }
    }

    @SuppressLint({"RestrictedApi", "MissingPermission"})
    private void startRecording() {
        updateUi();

        String fileName = getExternalFilesDir("").toString() + "/video_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".mp4";

        VideoCapture.OutputFileOptions outputFileOptions =
                new VideoCapture.OutputFileOptions.Builder(new File(fileName)).build();

        videoCapture.startRecording(outputFileOptions,
                ContextCompat.getMainExecutor(this),
                new VideoCapture.OnVideoSavedCallback() {
                    @Override
                    public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(MainActivity.this,
                                getResources().getString(R.string.record_video_success),
                                Toast.LENGTH_LONG).show();
                        Log.d(getClass().getName(), "Saved video to " + fileName);
                        updateUi();
                    }

                    @Override
                    public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                        Toast.makeText(MainActivity.this,
                                getResources().getString(R.string.record_video_fail),
                                Toast.LENGTH_LONG).show();
                        Log.e(getClass().getName(), message);
                        Log.e(getClass().getName(), "Failed to save video to " + fileName);
                        updateUi();
                    }
                });
    }

    @SuppressLint("RestrictedApi")
    private void stopRecording() {
        videoCapture.stopRecording();
    }
}