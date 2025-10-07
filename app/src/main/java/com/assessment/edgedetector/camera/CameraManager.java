package com.assessment.edgedetector.camera;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Camera manager for Camera2 API integration with real-time processing
 */
public class CameraManager {
    private static final String TAG = "CameraManager";
    
    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;
    
    private Context context;
    private android.hardware.camera2.CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    
    private Size previewSize;
    private String cameraId;
    private ImageReader imageReader;
    private final Semaphore cameraOpenCloseLock = new Semaphore(1);
    
    // Callbacks
    private SurfaceTexture surfaceTexture;
    private FrameProcessingCallback frameCallback;
    
    public interface FrameProcessingCallback {
        void onFrameAvailable(byte[] frameData, int width, int height, long timestamp);
        void onError(String error);
    }
    
    // Camera state callbacks
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.d(TAG, "Camera opened successfully");
            cameraOpenCloseLock.release();
            cameraDevice = camera;
            createCameraPreviewSession();
        }
        
        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.w(TAG, "Camera disconnected");
            cameraOpenCloseLock.release();
            camera.close();
            cameraDevice = null;
        }
        
        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e(TAG, "Camera error: " + error);
            cameraOpenCloseLock.release();
            camera.close();
            cameraDevice = null;
            if (frameCallback != null) {
                frameCallback.onError("Camera error: " + error);
            }
        }
    };
    
    // Capture session callback
    private final CameraCaptureSession.StateCallback sessionCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            Log.d(TAG, "Capture session configured");
            if (cameraDevice == null) return;
            
            captureSession = session;
            try {
                // Create capture request
                CaptureRequest.Builder requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                requestBuilder.addTarget(new Surface(surfaceTexture));
                requestBuilder.addTarget(imageReader.getSurface());
                
                // Set auto-focus mode
                requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                
                // Start repeating capture
                CaptureRequest captureRequest = requestBuilder.build();
                captureSession.setRepeatingRequest(captureRequest, null, backgroundHandler);
                
                Log.d(TAG, "Camera preview started");
                
            } catch (CameraAccessException e) {
                Log.e(TAG, "Failed to start camera preview", e);
                if (frameCallback != null) {
                    frameCallback.onError("Failed to start camera preview: " + e.getMessage());
                }
            }
        }
        
        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
            Log.e(TAG, "Capture session configuration failed");
            if (frameCallback != null) {
                frameCallback.onError("Capture session configuration failed");
            }
        }
    };
    
    // Image reader callback for frame processing
    private final ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            if (image == null) return;
            
            try {
                // Convert Image to byte array
                byte[] frameData = convertImageToByteArray(image);
                if (frameData != null && frameCallback != null) {
                    frameCallback.onFrameAvailable(frameData, image.getWidth(), image.getHeight(), image.getTimestamp());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing frame", e);
            } finally {
                image.close();
            }
        }
    };
    
    public CameraManager(Context context) {
        this.context = context;
        this.cameraManager = (android.hardware.camera2.CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }
    
    public void setSurfaceTexture(SurfaceTexture surfaceTexture) {
        this.surfaceTexture = surfaceTexture;
    }
    
    public void setFrameProcessingCallback(FrameProcessingCallback callback) {
        this.frameCallback = callback;
    }
    
    @SuppressLint("MissingPermission")
    public boolean openCamera() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Camera permission not granted");
            return false;
        }
        
        startBackgroundThread();
        
        try {
            // Get camera ID (back camera)
            cameraId = getCameraId();
            if (cameraId == null) {
                Log.e(TAG, "No suitable camera found");
                return false;
            }
            
            // Get camera characteristics
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            
            // Choose optimal preview size
            previewSize = chooseOptimalSize(map.getOutputSizes(ImageFormat.YUV_420_888));
            Log.d(TAG, "Selected preview size: " + previewSize.getWidth() + "x" + previewSize.getHeight());
            
            // Setup image reader for frame processing
            imageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(), 
                ImageFormat.YUV_420_888, 2);
            imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler);
            
            // Configure surface texture
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            
            // Acquire camera open/close lock
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                Log.e(TAG, "Time out waiting to lock camera opening");
                return false;
            }
            
            // Open camera
            cameraManager.openCamera(cameraId, stateCallback, backgroundHandler);
            return true;
            
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access exception", e);
            return false;
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while trying to lock camera opening", e);
            return false;
        }
    }
    
    public void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            
            if (captureSession != null) {
                captureSession.close();
                captureSession = null;
            }
            
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
            
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
            
        } catch (InterruptedException e) {
            Log.e(TAG, "Interrupted while trying to lock camera closing", e);
        } finally {
            cameraOpenCloseLock.release();
            stopBackgroundThread();
        }
        
        Log.d(TAG, "Camera closed");
    }
    
    private void createCameraPreviewSession() {
        try {
            // Create surfaces
            Surface previewSurface = new Surface(surfaceTexture);
            Surface readerSurface = imageReader.getSurface();
            
            // Create capture session
            cameraDevice.createCaptureSession(Arrays.asList(previewSurface, readerSurface),
                sessionCallback, backgroundHandler);
                
        } catch (CameraAccessException e) {
            Log.e(TAG, "Failed to create camera preview session", e);
            if (frameCallback != null) {
                frameCallback.onError("Failed to create camera preview session: " + e.getMessage());
            }
        }
    }
    
    private String getCameraId() {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                
                // Use back camera
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error getting camera ID", e);
        }
        return null;
    }
    
    private Size chooseOptimalSize(Size[] choices) {
        // Filter sizes that are not too large
        java.util.List<Size> bigEnough = new java.util.ArrayList<>();
        for (Size option : choices) {
            if (option.getWidth() <= MAX_PREVIEW_WIDTH && option.getHeight() <= MAX_PREVIEW_HEIGHT) {
                bigEnough.add(option);
            }
        }
        
        // Pick the largest suitable size
        if (bigEnough.size() > 0) {
            return Collections.max(bigEnough, new CompareSizesByArea());
        } else {
            Log.w(TAG, "Couldn't find suitable preview size, using first available");
            return choices[0];
        }
    }
    
    private byte[] convertImageToByteArray(Image image) {
        if (image.getFormat() != ImageFormat.YUV_420_888) {
            Log.e(TAG, "Unsupported image format: " + image.getFormat());
            return null;
        }
        
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();
        
        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();
        
        byte[] data = new byte[ySize + uSize + vSize];
        
        yBuffer.get(data, 0, ySize);
        uBuffer.get(data, ySize, uSize);
        vBuffer.get(data, ySize + uSize, vSize);
        
        return data;
    }
    
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }
    
    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping background thread", e);
            }
        }
    }
    
    public Size getPreviewSize() {
        return previewSize;
    }
    
    // Comparator for choosing optimal size
    private static class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                              (long) rhs.getWidth() * rhs.getHeight());
        }
    }
}