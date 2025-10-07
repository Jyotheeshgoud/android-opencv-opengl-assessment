package com.assessment.edgedetector.gl;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

/**
 * Custom GLSurfaceView for camera frame rendering
 */
public class CameraGLSurfaceView extends GLSurfaceView {
    private static final String TAG = "CameraGLSurfaceView";
    
    private FrameRenderer renderer;
    private boolean rendererSet = false;

    public CameraGLSurfaceView(Context context) {
        super(context);
        init();
    }

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Set OpenGL ES 2.0 context
        setEGLContextClientVersion(2);
        
        // Create and set renderer
        renderer = new FrameRenderer();
        setRenderer(renderer);
        
        // Only render when there's a change
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        
        rendererSet = true;
        Log.d(TAG, "CameraGLSurfaceView initialized");
    }

    public FrameRenderer getFrameRenderer() {
        return renderer;
    }

    public SurfaceTexture getSurfaceTexture() {
        return renderer != null ? renderer.getSurfaceTexture() : null;
    }

    public void updateProcessedFrame(byte[] data, int width, int height) {
        if (renderer != null) {
            renderer.setProcessedFrame(data, width, height);
            requestRender();
        }
    }

    public void toggleProcessingMode(boolean useProcessed) {
        if (renderer != null) {
            renderer.toggleProcessingMode(useProcessed);
            requestRender();
        }
    }

    @Override
    public void onPause() {
        if (rendererSet) {
            super.onPause();
        }
    }

    @Override
    public void onResume() {
        if (rendererSet) {
            super.onResume();
        }
    }
}