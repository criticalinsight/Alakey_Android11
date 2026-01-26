# PRD: Alakey Podcast App

## Overview
Alakey is a premium, modern podcast application for Android (Targeting Android 11/13+). It focuses on a rich, "glassmorphic" user interface with smart features like motion-reset sleep timers and automated downloads.

## Core Features
- **Library Management**: Automatic subscription to RSS feeds, episode tracking, and progress management.
- **Podcast Discovery**: Integration with the iTunes Search API for finding and subscribing to new podcasts.
- **Media Playback**: Robust audio playback using Jetpack Media3, supporting background play and notification controls.
- **Smart Sleep Timer**: 
    - Standard countdown timer.
    - Motion-reset functionality: If the device is moved (accelerometer detection), the timer resets, extending playback.
- **Offline Support**: Manual and automatic downloading of episodes for offline listening.
- **Smart Playlists**: Filtering episodes by status (New, Continue, Short).
- **Auto-Sync**: Background synchronization of feeds via `WorkManager`.

## User Interface Requirements
- **Flux Aesthetics**: Vibrant, dark-mode focused design with glassmorphism and subtle gradients.
- **Micro-interactions**: Hover effects, smooth transitions between the library and player.
- **Responsive Layout**: Designed for modern Android devices with edge-to-edge support.

## Technical Requirements
- **Platform**: Android (Kotlin/Compose).
- **Storage**: Room Database for podcast metadata and episode tracking.
- **Networking**: OkHttp with Proxy support for restricted feed access.
- **Concurrency**: Kotlin Coroutines and Flow for reactive data updates.
- **Dependency Injection**: Hilt for modular and testable code.
- **Media**: Media3/ExoPlayer for optimized streaming and local playback.
