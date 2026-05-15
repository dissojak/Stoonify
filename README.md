# Stoonify-App

A fully functional, end-to-end music streaming system for Android. Stoonify-App combines a modern, responsive mobile interface with a custom Node.js backend to provide a seamless audio experience featuring real-time streaming, intelligent disk caching, and dynamic UI layouts.

***
## Project Overview

Stoonify-App is built to handle the complexities of modern audio playback. It features a complete architecture from the database (MongoDB Atlas) to the server (Express.js) and finally the client (Android). Unlike static players, this app handles remote media streaming with the same reliability as a local library.

## Key Features

### 🎵 High-Performance Audio
- **Media3 ExoPlayer Engine**: Utilizes the industry-standard ExoPlayer for smooth, high-fidelity audio streaming.
- **Continuous Playback**: Music continues playing without interruption when navigating between the list view and detail screens.
- **Full Media Controls**: Includes Play/Pause, Skip Next, Skip Previous, and a precision Seek Bar for scrubbing through tracks.
- **Persistent Playback Bar**: A bottom-anchored control bar allows you to manage your music while browsing the song library.

### 📶 Intelligent Offline Mode
- **Disk Caching**: Automatically saves songs to a local 100MB disk cache during playback.
- **Instant Resume**: Cached songs start immediately even without an active internet connection.
- **Aggressive Buffering**: The system buffers up to 120 seconds ahead to ensure skip-free listening on unstable networks.
- **Partial Cache Support**: Supports range requests to resume downloads from where they left off.

### 🎨 Modern UI/UX
- **Dynamic View Modes**: Toggle instantly between a detailed **List View** and a modern **Grid View** (Material Cards) via the options menu.
- **Material Design 3**: Built using `MaterialCardView` and `RecyclerView` for a clean, professional aesthetic.
- **Visual Feedback**: A real-time **animating equalizer icon** indicates which song is currently active and whether it is playing or paused.
- **Efficient Image Loading**: High-resolution cover art is fetched and cached using the **Glide** library.

## System Architecture

### Backend
The backend lives in the `/backend` folder and is built on a modern Node.js stack.
- **Express.js API**: Handles song metadata and streams media via GridFS.
- **MongoDB Atlas**: Secure cloud storage for audio files and high-res images.
- **Range Support**: Custom streaming implementation that supports byte-range requests for efficient seeking and caching.

### Android Client
- **Retrofit 2**: Managed network communication with GSON for JSON parsing.
- **MusicPlayerManager**: A centralized singleton manager for the ExoPlayer instance.
- **AndroidX & Material Components**: Fully migrated to the latest Android libraries.

## Local Setup

### 1. Start the Backend
1. Navigate to the backend folder: `cd backend`
2. Install dependencies: `npm install`
3. Create a `.env` file with your `MONGO_URI`.
4. Start the server: `npm start`

### 2. Run the Android App
- **Target SDK**: 34 (Android 14)
- **Min SDK**: 24 (Android 7.0)
- **Networking**: By default, the app is configured to point to `http://10.0.2.2:3000` to reach the local server from the Android Emulator.

## Tech Stack
- **Android**: Java, Media3 ExoPlayer, Retrofit, Glide, RecyclerView.
- **Backend**: Node.js, Express, Mongoose, GridFS, MongoDB Atlas.
