# OpenGL ES 2.0 Renderer Module

This directory contains OpenGL ES 2.0 shader and rendering implementations for real-time camera frame display.

## Components Implemented

### FrameRenderer.java
- **OpenGL ES 2.0 Renderer**: Implements GLSurfaceView.Renderer interface  
- **Dual Texture Support**: Handles both external camera textures and 2D processed frame textures
- **Shader Programs**: Vertex and fragment shaders for texture rendering
- **Matrix Operations**: MVP matrix calculations for proper frame orientation

### CameraGLSurfaceView.java
- **Custom GLSurfaceView**: Extends GLSurfaceView for camera rendering
- **Surface Texture Management**: Handles SurfaceTexture for camera feed
- **Frame Updates**: Manages processed frame updates and rendering mode switching
- **Performance**: Render-on-demand mode for optimal performance

## Key Features

- ✅ **External Texture Rendering**: Camera feed via SurfaceTexture
- ✅ **2D Texture Rendering**: Processed frames from OpenCV  
- ✅ **Real-time Switching**: Toggle between raw and processed feeds
- ✅ **Optimized Rendering**: Minimal draw calls and efficient vertex buffers

## Features

- Real-time texture rendering
- Efficient frame buffer swapping
- Shader-based effects
- Performance optimization

## Requirements

- OpenGL ES 2.0+
- Android API 24+