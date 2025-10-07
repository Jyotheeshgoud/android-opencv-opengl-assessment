cmake_minimum_required(VERSION 3.18.1)

project("edgedetector")

# Set C++ standard
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find required packages
find_library(log-lib log)
find_library(android-lib android)
find_library(jnigraphics-lib jnigraphics)

# OpenCV configuration
set(OpenCV_DIR ${CMAKE_SOURCE_DIR}/../../../../opencv/sdk/native/jni)
find_package(OpenCV REQUIRED)

# Include directories
include_directories(${CMAKE_SOURCE_DIR})
include_directories(${OpenCV_INCLUDE_DIRS})

# Add the native library
add_library(
    edgedetector
    SHARED
    native-lib.cpp
    frame_processor.cpp
)

# Link libraries
target_link_libraries(
    edgedetector
    ${log-lib}
    ${android-lib}
    ${jnigraphics-lib}
    ${OpenCV_LIBS}
)