Two-Way Alert

A personal safety app for Android that doesn't wait for you to open it.

Two-Way Alert watches for falls in the background, and lets you fire off an SOS a bunch of different ways — tap the button, shake your phone, triple-press a volume key, even triple-press the power button while the screen's locked. Once triggered, it texts your emergency contacts your live location, calls someone (or 112, if you'd rather), and can sound an alarm and flash your camera light to get attention nearby.

We built this as a college project, but tried to make it feel like something you'd actually want on your phone rather than a checklist of features bolted together.

What it actually does

Getting an alert out


Manual SOS with three quick presets (Medical, Theft/Robbery, General)
On-device fall detection using a TensorFlow Lite model reading the accelerometer — no cloud call, no lag
Shake-to-trigger, separate from the fall detector
Volume button triple-press and power button triple-press, both work from the lock screen
A 10-second "I'm okay" cancel window on anything that could go off by accident
Long-press the SOS button for quicker options — silent alarm, direct call to contacts


What happens when it fires


SMS to every saved contact with a live Google Maps link
A call to your primary contact, or straight to 112 if that's what you've set
Loud Mode: siren + camera flash strobe. Silent Mode: neither, if that's not the moment for noise
Follow-up location texts every so often after the initial alert, for as long as you set it to
Everything gets logged — you can see your own alert history later


Setup that actually matters


We ask for your name, age, sex, and why you need the app before you even sign in — because an elderly parent's app should behave differently than a college student walking home alone
If you're setting this up for someone over 60, Comfort Mode switches on automatically — bigger text, bigger buttons, fewer things to accidentally mess with. It's a toggle, not a locked mode
You can't finish setup without adding at least one emergency contact, unless you'd rather every alert just call 112 directly. One or the other, not neither


Built with


Kotlin + Jetpack Compose
Firebase Auth + Firestore
TensorFlow Lite (on-device, nothing leaves the phone for the fall detection itself)
Google Play Services location


Running it yourself

You'll need your own google-services.json dropped into app/ — Firebase Auth and Firestore both need to be turned on in that project. The fall detection model (fall_verification.tflite) needs to sit in app/src/main/assets/. After that it's a normal Gradle sync and run.

Test it on a real phone if you can. The sensor triggers, SMS, and calling don't really mean anything on an emulator.
