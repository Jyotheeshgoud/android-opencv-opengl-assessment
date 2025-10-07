# 📱 Android Edge Detection App - Running Guide

## Prerequisites

### 1. Install Android Studio
- Download from: https://developer.android.com/studio
- Install Android SDK (API 34)
- Install NDK (Native Development Kit)

### 2. Setup Android Device/Emulator
**Option A: Physical Device**
- Enable "Developer Options" and "USB Debugging"
- Connect via USB cable

**Option B: Android Emulator**
- Create AVD in Android Studio
- Use API 30+ with Google APIs
- Enable hardware acceleration

## 🚀 Build & Run Instructions

### Step 1: Open Project in Android Studio
```bash
# Open Android Studio and select "Open an Existing Project"
# Navigate to: C:\Users\HEMU\android-opencv-opengl-assessment
```

### Step 2: Setup OpenCV SDK
1. Download OpenCV Android SDK (4.8.0) from: https://opencv.org/releases/
2. Extract to: `C:\opencv-android-sdk`
3. Update paths in `CMakeLists.txt` if needed

### Step 3: Sync Project
- Android Studio will auto-sync gradle files
- Wait for indexing to complete

### Step 4: Build Project
```bash
# In Android Studio Terminal or Command Line:
cd "C:\Users\HEMU\android-opencv-opengl-assessment"
.\gradlew build
```

### Step 5: Install & Run
```bash
# Connect device/start emulator, then:
.\gradlew installDebug
```

## 📱 App Features Demo

When the app runs, you'll see:

### Main Interface
- **Camera Preview** with real-time feed
- **Edge Detection Toggle** button
- **Render Mode Switch** (Camera/Processed/Split)
- **FPS Counter** showing performance
- **Processing Controls** for optimization

### Test the Features
1. **Grant Camera Permission** when prompted
2. **Toggle Edge Detection** to see OpenCV processing
3. **Switch Render Modes**:
   - Camera: Raw camera feed
   - Processed: Edge-detected output
   - Split: Side-by-side comparison
4. **Monitor Performance** via FPS counter

## 🔧 Alternative: Command Line Build

If you prefer command line only:

```powershell
# Navigate to project
cd "C:\Users\HEMU\android-opencv-opengl-assessment"

# Clean and build
.\gradlew clean
.\gradlew assembleDebug

# Install APK (device connected)
adb install app\build\outputs\apk\debug\app-debug.apk

# Launch app
adb shell am start -n com.assessment.edgedetector/.MainActivity
```

## 📊 Expected Performance

- **Target FPS**: 15+ (optimized for real-time)
- **Processing**: Canny edge detection via OpenCV
- **Rendering**: Hardware-accelerated OpenGL ES 2.0
- **Memory**: Optimized bitmap recycling

## 🐛 Troubleshooting

### Common Issues:
1. **Build Error**: Check NDK installation and paths
2. **Camera Permission**: Grant in Settings > Apps > EdgeDetector
3. **Low FPS**: Try lower resolution or reduce processing complexity
4. **JNI Error**: Verify OpenCV SDK path in CMakeLists.txt

### Debug Commands:
```bash
# Check device connection
adb devices

# View app logs
adb logcat | findstr "EdgeDetector"

# Clear app data
adb shell pm clear com.assessment.edgedetector
```

## 📷 Screenshots to Capture

1. **Main interface** with camera preview
2. **Edge detection enabled** showing processed output
3. **Split view mode** comparing original vs processed
4. **FPS counter** showing performance metrics
5. **Settings/controls** panel

---
*This Android app demonstrates real-time computer vision processing using Camera2 API, OpenCV C++, and OpenGL ES rendering.*