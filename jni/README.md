# OpenCV Native Library

This directory contains the C++ implementation for OpenCV frame processing using JNI.

## Files Structure

- `opencv_processor.cpp` - Main OpenCV processing functions
- `jni_bridge.cpp` - JNI interface for Java ↔ C++ communication
- `frame_utils.h` - Frame manipulation utilities
- `CMakeLists.txt` - CMake build configuration

## Features

- Real-time Canny edge detection
- Grayscale conversion
- Frame buffer management
- Direct OpenGL texture updates

## Build Requirements

- OpenCV Android SDK 4.5+
- NDK 21.0+
- CMake 3.18+