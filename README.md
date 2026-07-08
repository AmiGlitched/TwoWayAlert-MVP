# Two-Way Alert

A personal safety Android app that detects falls automatically and lets you trigger an SOS manually — via a button, a shake, a volume-button triple-press, or a power-button triple-press. When triggered, it texts your emergency contacts your live location, optionally calls them (or 112), and can sound a siren + strobe the camera flash to draw attention.

Built with Kotlin + Jetpack Compose, Firebase (Auth + Firestore), and a TensorFlow Lite model for on-device fall detection.

## Features

- **Automatic fall detection** — a TFLite model classifies accelerometer data in the background and fires a 10-second cancellable countdown before alerting contacts.
- **Manual SOS** — big red button with preset emergency types (Medical, Theft, General).
- **Alternate triggers** — shake the phone, triple-press either volume key, or triple-press power (screen off 3x) to fire an alert without opening the app.
- **Dynamic contact list** — add/remove as many emergency contacts as you want, synced to Firestore.
- **SOS History** — every trigger is logged locally with timestamp, type, and a location link.
- **Loud / Silent mode** — Loud mode sounds a siren and strobes the flashlight; Silent mode skips both.
- **Periodic tracking SMS** — after an alert, sends a follow-up location text every N seconds for up to 10 minutes.
- **Custom SOS message template** — personalize what your contacts receive.
- **Dial 112 option** — call emergency services instead of your primary contact.

## Tech Stack

- Kotlin, Jetpack Compose, Material 3
- Firebase Authentication + Cloud Firestore
- TensorFlow Lite (on-device fall classification)
- Google Play Services (Fused Location Provider)
- Android `SensorManager` (accelerometer), `CameraManager` (torch strobe), `ToneGenerator` (siren)

## Project Structure

```
app/src/main/java/com/myapplication/app/
├── MainActivity.kt              # Nav host + all screens (Auth, Profile, Contacts, Alert, History)
├── SosForegroundService.kt      # Background fall/shake/power-button detection
├── model/
│   ├── Contact.kt
│   └── SosHistoryEntry.kt
├── ml/
│   └── FallDetectionModel.kt    # TFLite interpreter wrapper
├── utils/
│   ├── ContactStore.kt          # Contact list persistence (SharedPreferences + Firestore)
│   ├── HistoryStore.kt          # SOS history persistence
│   ├── SirenTorch.kt            # Siren + torch strobe controller
│   └── Preprocessor.kt          # Feature normalization for the fall model
└── ui/theme/                    # Compose theme (Color.kt, Theme.kt, Type.kt)
```

## Setup

1. Clone the repo and open it in Android Studio.
2. Add your own `google-services.json` to `app/` (Firebase project with Auth + Firestore enabled).
3. Make sure `fall_verification.tflite` is present in `app/src/main/assets/`.
4. Sync Gradle — this pulls in the TensorFlow Lite dependency (`org.tensorflow:tensorflow-lite`) among others.
5. Run on a physical device for full sensor/SMS/call functionality (some features are limited or unavailable on emulators).

## Required Permissions

`SEND_SMS`, `CALL_PHONE`, `ACCESS_FINE_LOCATION`, `POST_NOTIFICATIONS`, `CAMERA` — all requested at first launch.

## Known Limitations

- Alternate triggers only fire location + contacts alerts; they don't currently share the 10-second cancel window that fall detection has.
- Periodic tracking SMS runs on a Handler loop in `MainActivity` and stops if the app process is killed outright (not just backgrounded).
- Power-button trigger detection is based on `ACTION_SCREEN_OFF` timing and can take a try or two to time correctly.

## Contributors

Built as a group assignment project.
