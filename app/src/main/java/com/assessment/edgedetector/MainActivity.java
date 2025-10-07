package com.assessment.edgedetector;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Size;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.assessment.edgedetector.camera.CameraManager;
import com.assessment.edgedetector.gl.CameraGLSurfaceView;
import com.assessment.edgedetector.utils.FPSCounter;

/**
 * Main activity for the Edge Detection app
 * Integrates Camera2, OpenGL ES, and OpenCV processing
 */
public class MainActivity extends AppCompatActivity implements CameraManager.FrameProcessingCallback {
    private static final String TAG = "MainActivity";
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;
    
    // UI components
    private CameraGLSurfaceView glSurfaceView;
    private Button toggleButton;
    private TextView fpsCounterText;
    private TextView statusText;
    
    // Core components
    private CameraManager cameraManager;
    private NativeLib nativeLib;
    private Handler mainHandler;
    private FPSCounter fpsCounter;
    
    // Processing state
    private boolean isProcessingEnabled = false;
    private double lastProcessingTime = 0.0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        Log.d(TAG, "MainActivity created");
        
        // Initialize UI components
        initializeUI();
        
        // Initialize core components
        initializeComponents();
        
        // Check camera permission
        if (checkCameraPermission()) {
            setupCamera();
        } else {
            requestCameraPermission();
        }
    }
    
    private void initializeUI() {
        glSurfaceView = findViewById(R.id.glSurfaceView);
        toggleButton = findViewById(R.id.toggleButton);
        fpsCounterText = findViewById(R.id.fpsCounter);
        statusText = findViewById(R.id.statusText);
        
        // Set up toggle button
        toggleButton.setOnClickListener(v -> toggleProcessingMode());
        
        Log.d(TAG, "UI components initialized");
    }
    
    private void initializeComponents() {
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Initialize native library
        nativeLib = new NativeLib();
        
        // Test JNI connection
        String jniTest = nativeLib.stringFromJNI();
        Log.d(TAG, "JNI test: " + jniTest);
        
        // Initialize FPS counter
        fpsCounter = new FPSCounter(30);
        fpsCounter.setCallback(this::onFPSUpdate);
        
        // Initialize camera manager
        cameraManager = new CameraManager(this);
        cameraManager.setFrameProcessingCallback(this);
        
        Log.d(TAG, "Core components initialized");
    }
    
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
               == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, 
            new String[]{Manifest.permission.CAMERA}, 
            CAMERA_PERMISSION_REQUEST_CODE);
    }
    
    private void setupCamera() {
        updateStatus("Initializing camera...");
        
        // Get surface texture from OpenGL renderer
        glSurfaceView.queueEvent(() -> {
            SurfaceTexture surfaceTexture = glSurfaceView.getSurfaceTexture();
            if (surfaceTexture != null) {
                cameraManager.setSurfaceTexture(surfaceTexture);
                
                // Open camera on background thread
                new Thread(() -> {
                    if (cameraManager.openCamera()) {
                        mainHandler.post(() -> {
                            Size previewSize = cameraManager.getPreviewSize();
                            if (previewSize != null) {
                                // Initialize native processor with preview size
                                boolean initialized = nativeLib.initializeProcessor(
                                    previewSize.getWidth(), previewSize.getHeight());
                                    
                                if (initialized) {
                                    updateStatus("Camera ready - " + previewSize.getWidth() + "x" + previewSize.getHeight());
                                    Log.d(TAG, "Camera and processor initialized successfully");
                                } else {
                                    updateStatus("Failed to initialize processor");
                                    Log.e(TAG, "Failed to initialize native processor");
                                }
                            }
                        });
                    } else {
                        mainHandler.post(() -> {
                            updateStatus("Failed to open camera");
                            Log.e(TAG, "Failed to open camera");
                        });
                    }
                }).start();
            } else {
                Log.e(TAG, "Surface texture not available");
                updateStatus("OpenGL initialization failed");
            }
        });
    }
    
    private void toggleProcessingMode() {
        isProcessingEnabled = !isProcessingEnabled;
        
        // Update UI
        String buttonText = isProcessingEnabled ? 
            getString(R.string.processing_edge) : getString(R.string.processing_raw);
        toggleButton.setText(buttonText);
        
        // Update OpenGL renderer
        glSurfaceView.toggleProcessingMode(isProcessingEnabled);
        
        // Update status
        String statusMsg = isProcessingEnabled ? 
            getString(R.string.status_processing) : getString(R.string.status_ready);
        updateStatus(statusMsg);
        
        Log.d(TAG, "Processing mode toggled: " + (isProcessingEnabled ? "ON" : "OFF"));
    }
    
    @Override
    public void onFrameAvailable(byte[] frameData, int width, int height, long timestamp) {
        // Process frame if processing is enabled
        if (isProcessingEnabled && frameData != null) {
            // Process frame in background thread to avoid blocking camera
            new Thread(() -> {
                try {
                    // Convert YUV to RGB first (simplified - in real implementation, 
                    // you'd handle the actual YUV420 format properly)
                    byte[] processedData = nativeLib.processFrameCanny(frameData, width, height);
                    
                    if (processedData != null) {
                        // Update OpenGL renderer with processed frame
                        glSurfaceView.updateProcessedFrame(processedData, width, height);
                        
                        // Update FPS counter
                        updateFpsCounter(timestamp);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing frame", e);
                }
            }).start();
        }
        
        // Always record frame for FPS calculation
        fpsCounter.recordFrame(lastProcessingTime);
    }
    
    @Override
    public void onError(String error) {
        mainHandler.post(() -> {
            updateStatus("Camera error: " + error);
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Camera error: " + error);
        });
    }
    
    /**
     * FPS counter callback
     */
    private void onFPSUpdate(double fps, long frameCount, double avgProcessingTime) {
        String fpsText = String.format("FPS: %.1f", fps);
        fpsCounterText.setText(fpsText);
        
        // Log performance statistics periodically
        if (frameCount % 100 == 0) {
            FPSCounter.PerformanceStats stats = fpsCounter.getStats();
            Log.i(TAG, "Performance: " + stats.toString());
        }
    }
    
    private void updateStatus(String status) {
        mainHandler.post(() -> {
            statusText.setText(status);
            Log.d(TAG, "Status: " + status);
        });
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted");
                setupCamera();
            } else {
                Log.e(TAG, "Camera permission denied");
                updateStatus("Camera permission required");
                Toast.makeText(this, getString(R.string.camera_permission_denied), 
                             Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
        Log.d(TAG, "Activity resumed");
    }
    
    @Override
    protected void onPause() {
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
        super.onPause();
        Log.d(TAG, "Activity paused");
    }
    
    @Override
    protected void onDestroy() {
        // Cleanup camera
        if (cameraManager != null) {
            cameraManager.closeCamera();
        }
        
        // Cleanup native resources
        if (nativeLib != null) {
            nativeLib.cleanup();
        }
        
        super.onDestroy();
        Log.d(TAG, "Activity destroyed");
    }
}