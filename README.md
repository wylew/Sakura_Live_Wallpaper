# Sakura Live Wallpaper

<img src="https://github.com/wylew/Sakura_Live_Wallpaper/blob/main/ic_launcher_round.png" width="128" height="128" align="right" />

A beautiful, highly customizable Android Live Wallpaper that brings the serene beauty of falling cherry blossoms to your home screen. Designed with modern Android standards and a focus on performance and "Expressive" Material 3 aesthetics.

## ✨ Features

- **Realistic Animation**: Smoothly animated petals with individual rotation, swaying, and vertical oscillation for a natural look.
- **Dynamic Physics**:
    - **Wind Strength**: Control the horizontal drift and breeze intensity.
    - **Fall Speed**: Adjust how quickly the petals descend.
    - **Rotation Speed**: Fine-tune the spinning motion of falling blossoms.
- **Visual Customization**:
    - **Petal Count**: Scale from a few drifting petals to a full blizzard (up to 500 active).
    - **Petal Size & Transparency**: Customize the scale and alpha of the blossoms.
    - **Custom Colors**: Interpolate between soft whites and deep Sakura pinks.
- **Interactive Elements**:
    - **Collect at Bottom**: Let petals settle on the bottom of your screen to form a pile.
    - **Shake to Clear**: Uses the device accelerometer—shake your phone to blow away grounded petals in a gust of wind.
- **Personalized Backgrounds**: Select any image from your gallery to serve as the backdrop for your falling petals.
- **Modern UI**: A sleek settings menu using **Material 3 Expressive** design norms, featuring:
    - Interactive live preview window.
    - Two-column slider layout for efficiency.
    - Responsive design for both phones and tablets/landscape orientations.

## 📸 Screenshots

| Settings Menu | Home Screen |
| :---: | :---: |
| <img src="https://github.com/wylew/Sakura_Live_Wallpaper/blob/main/Screenshot_20260324_154414.png" width="300" /> | <img src="https://github.com/wylew/Sakura_Live_Wallpaper/blob/main/Screenshot_20260324_154438.png" width="300" /> |

## 🛠️ How It Was Made

This project was built using modern Android development practices:

- **Kotlin**: The entire logic is written in Kotlin for safety and conciseness.
- **Custom Canvas Rendering**: The wallpaper engine uses a low-level `Canvas` and `SurfaceHolder` approach to render hundreds of petals at 60 FPS without the overhead of heavy graphics libraries.
- **Vector-based Math**: Each petal follows a unique path calculated using sine wave oscillations and basic physics vectors for drift and gravity.
- **Material 3 (M3)**: The settings interface utilizes the latest Google Material 3 components, including `Slider`, `MaterialSwitch`, and `MaterialCardView`, ensuring a native look and feel on modern Android devices.
- **Sensor API**: Integrated `SensorManager` to detect G-force thresholds, enabling the immersive "Shake to Clear" interaction.
- **Responsive Layouts**: Uses resource qualifiers (`layout-sw600dp`) to provide a optimized side-by-side experience for tablets and large-screen devices.

## 🚀 Installation

1. Clone this repository.
2. Open in **Android Studio Hedgehog** or newer.
3. Build and run the `:app` module on your device or emulator.
4. Open the app to configure your settings, then tap **Set Wallpaper** to apply.

---
*Created with ❤️ for the Android community.*
