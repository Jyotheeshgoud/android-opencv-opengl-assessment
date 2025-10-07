package com.assessment.edgedetector.gl;

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * OpenGL ES 2.0 renderer for displaying camera frames and processed images
 */
public class FrameRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "FrameRenderer";

    // Vertex shader for texture rendering
    private static final String VERTEX_SHADER = 
        "attribute vec4 aPosition;\n" +
        "attribute vec2 aTexCoord;\n" +
        "varying vec2 vTexCoord;\n" +
        "uniform mat4 uMVPMatrix;\n" +
        "void main() {\n" +
        "    gl_Position = uMVPMatrix * aPosition;\n" +
        "    vTexCoord = aTexCoord;\n" +
        "}\n";

    // Fragment shader for external texture (camera)
    private static final String FRAGMENT_SHADER_EXT = 
        "#extension GL_OES_EGL_image_external : require\n" +
        "precision mediump float;\n" +
        "varying vec2 vTexCoord;\n" +
        "uniform samplerExternalOES uTexture;\n" +
        "void main() {\n" +
        "    gl_FragColor = texture2D(uTexture, vTexCoord);\n" +
        "}\n";

    // Fragment shader for regular 2D texture (processed frames)
    private static final String FRAGMENT_SHADER_2D = 
        "precision mediump float;\n" +
        "varying vec2 vTexCoord;\n" +
        "uniform sampler2D uTexture;\n" +
        "void main() {\n" +
        "    gl_FragColor = texture2D(uTexture, vTexCoord);\n" +
        "}\n";

    // Quad vertices for full screen rendering
    private static final float[] VERTICES = {
        -1.0f, -1.0f, 0.0f, 0.0f, 1.0f, // Bottom-left
         1.0f, -1.0f, 0.0f, 1.0f, 1.0f, // Bottom-right
        -1.0f,  1.0f, 0.0f, 0.0f, 0.0f, // Top-left
         1.0f,  1.0f, 0.0f, 1.0f, 0.0f  // Top-right
    };

    private static final int COORDS_PER_VERTEX = 3;
    private static final int TEX_COORDS_PER_VERTEX = 2;
    private static final int VERTEX_STRIDE = (COORDS_PER_VERTEX + TEX_COORDS_PER_VERTEX) * 4;

    private FloatBuffer vertexBuffer;
    private int programExternal, program2D;
    private int externalTextureId, texture2DId;
    private int surfaceWidth, surfaceHeight;
    
    private final float[] mvpMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];

    private SurfaceTexture surfaceTexture;
    private boolean updateSurface = false;
    private boolean useProcessedFrame = false;
    private byte[] processedFrameData;
    private int frameWidth, frameHeight;

    public FrameRenderer() {
        initializeVertexBuffer();
    }

    private void initializeVertexBuffer() {
        ByteBuffer bb = ByteBuffer.allocateDirect(VERTICES.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(VERTICES);
        vertexBuffer.position(0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        
        // Create shader programs
        programExternal = createProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT);
        program2D = createProgram(VERTEX_SHADER, FRAGMENT_SHADER_2D);
        
        // Create textures
        externalTextureId = createExternalTexture();
        texture2DId = create2DTexture();
        
        // Create surface texture for camera
        surfaceTexture = new SurfaceTexture(externalTextureId);
        surfaceTexture.setOnFrameAvailableListener(texture -> {
            updateSurface = true;
        });
        
        Log.d(TAG, "OpenGL surface created successfully");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        surfaceWidth = width;
        surfaceHeight = height;
        GLES20.glViewport(0, 0, width, height);
        
        // Setup projection matrix
        float ratio = (float) width / height;
        Matrix.setIdentityM(projectionMatrix, 0);
        Matrix.orthoM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);
        
        // Setup view matrix
        Matrix.setLookAtM(viewMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        
        // Calculate MVP matrix
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        
        Log.d(TAG, "Surface changed: " + width + "x" + height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        
        if (updateSurface && surfaceTexture != null) {
            surfaceTexture.updateTexImage();
            updateSurface = false;
        }
        
        if (useProcessedFrame && processedFrameData != null) {
            drawProcessedFrame();
        } else {
            drawCameraFrame();
        }
    }

    private void drawCameraFrame() {
        GLES20.glUseProgram(programExternal);
        
        int positionHandle = GLES20.glGetAttribLocation(programExternal, "aPosition");
        int texCoordHandle = GLES20.glGetAttribLocation(programExternal, "aTexCoord");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(programExternal, "uMVPMatrix");
        int textureHandle = GLES20.glGetUniformLocation(programExternal, "uTexture");
        
        // Enable attributes
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glEnableVertexAttribArray(texCoordHandle);
        
        // Set vertex positions
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, 
            GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer);
        
        // Set texture coordinates
        vertexBuffer.position(COORDS_PER_VERTEX);
        GLES20.glVertexAttribPointer(texCoordHandle, TEX_COORDS_PER_VERTEX, 
            GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer);
        
        // Set uniforms
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, externalTextureId);
        GLES20.glUniform1i(textureHandle, 0);
        
        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        
        // Disable attributes
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);
    }

    private void drawProcessedFrame() {
        GLES20.glUseProgram(program2D);
        
        int positionHandle = GLES20.glGetAttribLocation(program2D, "aPosition");
        int texCoordHandle = GLES20.glGetAttribLocation(program2D, "aTexCoord");
        int mvpMatrixHandle = GLES20.glGetUniformLocation(program2D, "uMVPMatrix");
        int textureHandle = GLES20.glGetUniformLocation(program2D, "uTexture");
        
        // Update 2D texture with processed frame data
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture2DId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE,
            frameWidth, frameHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
            ByteBuffer.wrap(processedFrameData));
        
        // Enable attributes
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glEnableVertexAttribArray(texCoordHandle);
        
        // Set vertex positions
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, 
            GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer);
        
        // Set texture coordinates
        vertexBuffer.position(COORDS_PER_VERTEX);
        GLES20.glVertexAttribPointer(texCoordHandle, TEX_COORDS_PER_VERTEX, 
            GLES20.GL_FLOAT, false, VERTEX_STRIDE, vertexBuffer);
        
        // Set uniforms
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniform1i(textureHandle, 0);
        
        // Draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        
        // Disable attributes
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);
    }

    private int createProgram(String vertexShaderCode, String fragmentShaderCode) {
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);
        
        int program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);
        GLES20.glAttachShader(program, fragmentShader);
        GLES20.glLinkProgram(program);
        
        return program;
    }

    private int loadShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        return shader;
    }

    private int createExternalTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 
            GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 
            GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        return textures[0];
    }

    private int create2DTexture() {
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, 
            GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, 
            GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        return textures[0];
    }

    // Public methods for external control
    public SurfaceTexture getSurfaceTexture() {
        return surfaceTexture;
    }

    public void setProcessedFrame(byte[] data, int width, int height) {
        this.processedFrameData = data;
        this.frameWidth = width;
        this.frameHeight = height;
    }

    public void toggleProcessingMode(boolean useProcessed) {
        this.useProcessedFrame = useProcessed;
    }
}