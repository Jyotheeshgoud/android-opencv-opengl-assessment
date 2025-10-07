# Android OpenCV OpenGL Assessment

# Real-Time Edge Detection Viewer

Android app with OpenCV C++ integration and TypeScript web viewer for real-time camera frame processing using OpenGL ES rendering.

## 🚀 Features Implemented

### Android App
- [ ] Camera feed integration (TextureView/SurfaceTexture)
- [ ] OpenCV C++ frame processing via JNI
- [ ] OpenGL ES 2.0 rendering
- [ ] Real-time edge detection (Canny filter)
- [ ] Toggle between raw/processed feed
- [ ] FPS counter

### Web Viewer (TypeScript)
- [ ] Static processed frame display
- [ ] Frame stats overlay (FPS, resolution)
- [ ] Modular TypeScript architecture

## 🏗️ Project Structure

```
android-opencv-opengl-assessment/
├── app/              # Android Java/Kotlin code
├── jni/              # OpenCV C++ processing
├── gl/               # OpenGL ES renderer classes
├── web/              # TypeScript web viewer
├── README.md         # This file
└── .gitignore        # Git ignore patterns
```

## ⚙️ Tech Stack

- **Android**: Java/Kotlin, NDK, Camera2 API
- **Native**: C++, OpenCV, JNI bridge
- **Graphics**: OpenGL ES 2.0, GLSL shaders
- **Web**: TypeScript, HTML5 Canvas
- **Build**: Gradle, CMake, npm/tsc

## 🔧 Setup Instructions

### Prerequisites
- Android Studio Arctic Fox or later
- NDK 21.0+ installed
- OpenCV Android SDK 4.5+
- Node.js 14+ (for TypeScript web viewer)

### Android Setup
1. Open project in Android Studio
2. Download OpenCV Android SDK
3. Extract to `opencv/` directory
4. Sync Gradle project
5. Connect Android device or start emulator

### Web Viewer Setup
```bash
cd web/
npm install
npm run build
npm start
```

## 🧠 Architecture Overview

### Frame Processing Flow
1. **Camera Capture**: Camera2 API → SurfaceTexture
2. **JNI Bridge**: Java → C++ frame transfer
3. **OpenCV Processing**: Canny edge detection in C++
4. **OpenGL Rendering**: Processed texture display
5. **Web Export**: Sample frame to TypeScript viewer

### JNI Integration
- Native methods for frame processing
- Efficient memory management (no frame copying)
- Direct OpenGL texture updates from C++

## 📸 Screenshots

*Screenshots will be added after implementation*

## 🎯 Performance Targets

- **Target FPS**: 15-30 FPS real-time processing
- **Latency**: < 100ms camera to display
- **Memory**: Efficient frame buffer management

## 📝 Development Log

This project follows incremental development with meaningful Git commits for each feature milestone.

## 🎯 Project Overview

This project demonstrates integration of:
- **Android SDK** (Java/Kotlin)
- **NDK** (Native Development Kit)
- **OpenCV (C++)** for image processing
- **OpenGL ES 2.0+** for rendering
- **JNI** for Java ↔ C++ communication
- **TypeScript** for web-based viewer

## 🏗️ Architecture

```
android-opencv-opengl-assessment/
├── app/          # Android Java/Kotlin code
├── jni/          # C++ OpenCV processing
├── gl/           # OpenGL renderer classes
└── web/          # TypeScript web viewer
```

## 🚀 Features

- [ ] Real-time camera feed capture
- [ ] OpenCV-based edge detection (Canny filter)
- [ ] OpenGL ES texture rendering
- [ ] TypeScript web viewer for processed frames
- [ ] Performance optimization (10-15 FPS target)

## 📋 Setup Instructions

### Prerequisites
- Android Studio with NDK
- OpenCV for Android
- Node.js and TypeScript

### Building
(Instructions will be updated as development progresses)

## 📝 Development Progress

This project is being developed with proper Git commit history to demonstrate the development process step by step.

## 🧠 Technical Architecture

(Architecture details will be added as components are implemented)