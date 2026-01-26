# Alakey Podcast App

Alakey is a premium, modern podcast application for Android featuring a rich "glassmorphic" UI, smart sleep timers, and automated downloads.

![Alakey UI Concept](https://img.shields.io/badge/UI-Jetpack_Compose-blue?style=for-the-badge)
![Android](https://img.shields.io/badge/Platform-Android_11%2B-green?style=for-the-badge)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-orange?style=for-the-badge)

## ✨ Key Features

*   **Premium Visuals**: Vibrant dark-mode design with glassmorphism and subtle gradients.
*   **Smart Sleep Timer**: Includes a motion-reset feature—simply move your phone to extend your listening time.
*   **Offline First**: Automatic and manual episode downloads for seamless offline support.
*   **Smart Playlists**: Intelligent filtering for "Continue", "New", and "Short" episodes.
*   **Auto-Sync**: Background feed synchronization using WorkManager.
*   **Marketplace Discovery**: Built-in integration with the iTunes Search API for finding new content.

## 🏗️ Architecture

The app follows modern Android development best practices:
- **View**: Jetpack Compose with custom "Flux" design tokens.
- **Model**: Room Database with Reactive Flows.
- **Playback**: Jetpack Media3 (ExoPlayer) with an integrated `MediaLibraryService`.
- **DI**: Hilt for robust dependency management.

For more details, see [ARCHITECTURE.md](ARCHITECTURE.md).

## 🚀 Getting Started

1.  **Clone the Repo**: `git clone https://github.com/criticalinsight/Alakey_Android11.git`
2.  **Open in Android Studio**: Ensure you have Arctic Fox or later.
3.  **Build & Run**: Use a device or emulator running Android 11+ (API 30+).

## 🛠️ Development

-   **PRD**: Detailed requirements can be found in [PRD.md](PRD.md).
-   **Roadmap**: Current and future tasks are tracked in [TASKS.md](TASKS.md).
-   **Automation**: This project uses `sly` for autonomous learning and documentation management.

## 📜 License

[MIT](LICENSE)
