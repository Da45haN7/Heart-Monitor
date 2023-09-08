package com.example.monitorheart;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.core.VideoCapture;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private VideoCapture videoCapture;
    Button bHrtRate, bRspRate, bSym, uBtn;
    PreviewView previewView;
    private Handler handler = new Handler(Looper.getMainLooper());
    private CameraControl cameraControl;
    public TextView textView, textView2;
    private long timestamp;
    private Handler resultHandler = new Handler();
    private boolean recording = false;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float[] accelValuesX = new float[451];
    private float[] accelValuesY = new float[451];
    private float[] accelValuesZ = new float[451];
    private int currentIndex = 0;
    public int respiratoryRate=0;
    public int result= 0;
    ArrayList<Map<String, Object>> symptomRatingList = new ArrayList<>();
    private SymptomDatabaseHelper dbHelper;
    private Float[] ratings = new Float[12];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bHrtRate = findViewById(R.id.bHeartRate);
        bRspRate = findViewById(R.id.button2);
        bSym = findViewById(R.id.button);
        uBtn = findViewById(R.id.button4);
        previewView = findViewById(R.id.previewView);
        textView = findViewById(R.id.textView);
        textView2 = findViewById(R.id.textView2);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        bHrtRate.setOnClickListener(this);
        bRspRate.setOnClickListener(this);
        dbHelper = new SymptomDatabaseHelper(this);

        String[] symptoms = {"Select a symptom","Nausea", "Headache", "Diarrhea", "SoreThroat", "Fever", "MuscleAche", "Loss of Smell or Taste", "Cough", "Shortness of Breath", "Feeling Tired"};
        bSym.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, Symptoms.class);
            intent.putExtra("heartRate", result);
            intent.putExtra("respiratoryRate", respiratoryRate);
            startActivity(intent);
        });
        for (String symptom : symptoms) {
            if(Objects.equals(symptom, "Select a symptom"))
            {
                continue;
            }
            Map<String, Object> symptomData = new HashMap<>();
            symptomData.put("SymptomName", symptom);
            symptomData.put("Rating", 0f);
            symptomRatingList.add(symptomData);
        }
        uBtn.setOnClickListener(view -> {
            ratings[0] = (float) result;
            ratings[1] = (float) respiratoryRate;
            int i = 2;
            for (Map<String, Object> symptomData : symptomRatingList) {

                String symptom = (String) symptomData.get("SymptomName");
                Object rating = symptomData.get("Rating");
                if (!symptom.equals("Select a symptom") && i != 12) {
                    ratings[i] = (Float) rating;
                    i += 1;
                }
            }
            dbHelper.updateSymptomRatings((ratings));
            Toast.makeText(this, "Data Inserted Successfully", Toast.LENGTH_SHORT).show();
        });

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                startCameraX(cameraProvider);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, getExecutor());
    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    @SuppressLint("RestrictedApi")
    private void startCameraX(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());


        videoCapture = new VideoCapture.Builder()
                .setVideoFrameRate(30)
                .build();
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, videoCapture);
        cameraControl = camera.getCameraControl();
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bHeartRate:
                if (recording == false) {
                    recordVideo();
                    handler.postDelayed(() -> {
                        stopRecording();
                        turnOffFlash();
                    }, 45000);
                    getHeartRate();
                }
//                handler.postDelayed(()-> getHeartRate(), 6000);
                if (recording == true) {
                    getHeartRate();
                }
                break;
            case R.id.button2:
                currentIndex = 0;
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                break;
//            }
        }
//        getHeartRate();
    }


    private void turnOnFlash() {
        cameraControl.enableTorch(true);
    }

    private void turnOffFlash() {
        cameraControl.enableTorch(false);
        handler.removeCallbacksAndMessages(null); // Remove any pending callbacks
    }

    private void getHeartRate() {
        ContentResolver contentResolver = getContentResolver();

        String[] projection = {MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.Video.VideoColumns.TITLE,
                MediaStore.Video.VideoColumns.DATA};

        String selection = MediaStore.Video.VideoColumns.TITLE + " LIKE ?";
        String[] selectionArgs = {"%" + timestamp + "%"};
        Cursor cursor = contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        );
        if (cursor != null && cursor.moveToFirst()) {
            int dataColumnIndex = cursor.getColumnIndex(MediaStore.Video.Media.DATA);

            String videoPath = cursor.getString(dataColumnIndex);
            cursor.close();
            Log.wtf("path1", videoPath);
            processSlowTask(videoPath);

        } else {
            // Handle the case where the video was not found
        }
    }

    @SuppressLint("RestrictedApi")
    private void stopRecording() {
        videoCapture.stopRecording();
        recording = true;
//        getHeartRate();
    }

    private void processSlowTask(String videoFilePath) {
        new Thread(() -> {
            Log.wtf("ThreadRunning", "ThreadRunning");
            result = performSlowTask(videoFilePath);
            Log.wtf("Result", "Result = " + result);
            if (result != 0) {
                resultHandler.post(() -> textView.setText(String.valueOf(result)));
            }
        }).start();
    }

    private int performSlowTask(String videoFilePath) {
        Log.wtf("apth1", videoFilePath);
        Log.wtf("ThreadRunning1", "ThreadRunning1");
        Bitmap m_bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        List<Bitmap> frameList = new ArrayList<>();
//        result = 0; // Initialize result
        try {
            Log.wtf("ThreadRunning11", "ThreadRunning11" + videoFilePath);
            retriever.setDataSource(videoFilePath);
            Log.wtf("ThreadRunning12", "ThreadRunning12");
            String duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT);
            Log.wtf("ThreadRunning13", duration);
            assert duration != null;
            int aduration = Integer.parseInt(duration);
            Log.wtf("ThreadRunning14", "ThreadRunning14");
            int j = 10;
            Log.wtf("ThreadRunning2", "ThreadRunning2");
            while (j < aduration) {
                Bitmap bitmap = retriever.getFrameAtIndex(j);
                frameList.add(bitmap);
                j += 5;
            }
            // Rest of the processing
            long redBucket = 0;
            long pixelCount = 0;
            List<Long> a = new ArrayList<>();
            Log.wtf("ThreadRunning3", "ThreadRunning3");
            for (Bitmap i : frameList) {
                redBucket = 0;
                for (int y = 550; y < 650; y++) {
                    for (int x = 550; x < 650; x++) {
                        int c = i.getPixel(x, y);
                        pixelCount++;
                        redBucket += Color.red(c) + Color.blue(c) + Color.green(c);
                    }
                }
                a.add(redBucket);
            }
            Log.wtf("ThreadRunning4", "ThreadRunning4");
            List<Long> b = new ArrayList<>();
            for (int i = 0; i < a.size() - 5; i++) {
                long temp = (a.get(i) + a.get(i + 1) + a.get(i + 2) + a.get(i + 3) + a.get(i + 4)) / 4;
                b.add(temp);
            }
            long x = b.get(0);
            int count = 0;
            for (int i = 1; i < b.size(); i++) {
                long p = b.get(i);
                if ((p - x) > 3500) {
                    count = count + 1;
                }
                x = b.get(i);
            }
            int rate = (int) ((count * 1.0f / 45) * 60);
            result = (rate / 2);
        } catch (Exception m_e) {
            Log.wtf("ThreadRunning77", "ThreadRunning77 " + m_e);
        } finally {
            retriever.release();
        }
        Log.wtf("ThreadRunning5", "ThreadRunning5");
        return result;
    }
//

    @SuppressLint("RestrictedApi")
    private void recordVideo() {
        if (videoCapture != null) {
            timestamp = System.currentTimeMillis();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timestamp);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");

            try {
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
                        ).build(),
                        getExecutor(),
                        new VideoCapture.OnVideoSavedCallback() {
                            @Override
                            public void onVideoSaved(@NonNull VideoCapture.OutputFileResults outputFileResults) {
//                                Log.wtf("path2", String.valueOf(MediaStore.Video.Media.EXTERNAL_CONTENT_URI));
                                Toast.makeText(MainActivity.this, "Video has been Saved.", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(int videoCaptureError, @NonNull String message, @Nullable Throwable cause) {
                                Toast.makeText(MainActivity.this, "Error in saving.", Toast.LENGTH_SHORT).show();
                            }
                        }
                );
                turnOnFlash();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            if (currentIndex < 451) {
                // Store the accelerometer values in your arrays
                accelValuesX[currentIndex] = x;
                accelValuesY[currentIndex] = y;
                accelValuesZ[currentIndex] = z;
                Log.wtf("accel", String.valueOf(accelValuesX[0]));
                currentIndex++;

                if (currentIndex == 451) {
                    // You've collected 451 accelerometer data points; you can now call your calculation function
                    respiratoryRate = callRespiratoryCalculator();
                    Log.wtf("accel", String.valueOf(respiratoryRate));
                    textView2.setText(String.valueOf(respiratoryRate));
                    return;
                }
            }else{
                return;
            }
        }
    }
    private int callRespiratoryCalculator () {
        float previousValue = 10f;
        int k = 0;

        for (int i = 11; i <= 449; i++) {
            float currentValue = (float) Math.sqrt(
                    Math.pow(accelValuesZ[i], 2.0) +
                            Math.pow(accelValuesX[i], 2.0) +
                            Math.pow(accelValuesY[i], 2.0)
            );

            if (Math.abs(previousValue - currentValue) > 0.15) {
                k++;
            }

            previousValue = currentValue;
        }

        double ret = k / 45.00;
        return (int) (ret * 30);
    }
    @Override
    public void onAccuracyChanged (Sensor sensor,int i){

    }
}