# Moonlight Android

## Xbox-style UI fork

This fork restyles Moonlight with an Xbox dashboard look and a friendlier touch interface:

- **Dark dashboard theme** with the Xbox green accent, a top bar (title, clock, large round
  buttons for Add PC / Help / Settings) and section headings.
- **Tiles instead of icons**: rounded PC tiles (green when the PC is paired and ready, with a
  status line such as "Ready to play" / "Offline" / "Tap to pair") and rounded box-art game tiles
  with a "running" badge.
- **Controller/remote focus** behaves like the Xbox dashboard: the selected tile grows and gets a
  white focus ring. **Touch** gives a press-shrink and ripple, and the game list has an on-screen
  back button.
- **On-screen controller** redrawn in Xbox style: filled translucent buttons, A/B/X/Y in green,
  red, blue and yellow, pill-shaped bumpers/triggers, MENU/VIEW labels, a solid D-pad cross and
  thumbstick caps. Pressed elements light up green. Custom colors and opacity are still honored.
- **Live wallpaper** ("Moonlight Dashboard"): a dark animated background with drifting green
  glows, flowing light ribbons and rising particles; tap it for a green pulse. It only animates
  while visible (about 30 FPS) and follows launcher page scrolling with a slight parallax.
- **Android home screen**: "Moonlight Home" can be chosen as the device launcher. It shows your
  PCs over the wallpaper, never closes on Back, and has a **My apps** drawer (grid button in the
  header) with all installed Android apps as Xbox-style tiles (long-press for App info).

To set them up, open **My apps** and tap the **Personalize** (palette) button, then choose
*Set Xbox-style live wallpaper* or *Use Moonlight as home screen*. Both can also be picked from
Android's own wallpaper and default home app settings. Moonlight's normal app icon keeps working
as before.


[![AppVeyor Build Status](https://ci.appveyor.com/api/projects/status/232a8tadrrn8jv0k/branch/master?svg=true)](https://ci.appveyor.com/project/cgutman/moonlight-android/branch/master)
[![Translation Status](https://hosted.weblate.org/widgets/moonlight/-/moonlight-android/svg-badge.svg)](https://hosted.weblate.org/projects/moonlight/moonlight-android/)

[Moonlight for Android](https://moonlight-stream.org) is an open source client for NVIDIA GameStream and [Sunshine](https://github.com/LizardByte/Sunshine).

Moonlight for Android will allow you to stream your full collection of games from your Windows PC to your Android device,
whether in your own home or over the internet.

Moonlight also has a [PC client](https://github.com/moonlight-stream/moonlight-qt) and [iOS/tvOS client](https://github.com/moonlight-stream/moonlight-ios).

You can follow development on our [Discord server](https://moonlight-stream.org/discord) and help translate Moonlight into your language on [Weblate](https://hosted.weblate.org/projects/moonlight/moonlight-android/).

## Downloads
* [Google Play Store](https://play.google.com/store/apps/details?id=com.limelight)
* [Amazon App Store](https://www.amazon.com/gp/product/B00JK4MFN2)
* [F-Droid](https://f-droid.org/packages/com.limelight)
* [APK](https://github.com/moonlight-stream/moonlight-android/releases)

## Building
* Install Android Studio and the Android NDK
* Run ‘git submodule update --init --recursive’ from within moonlight-android/
* In moonlight-android/, create a file called ‘local.properties’. Add an ‘ndk.dir=’ property to the local.properties file and set it equal to your NDK directory.
* Build the APK using Android Studio or gradle

## Authors

* [Cameron Gutman](https://github.com/cgutman)  
* [Diego Waxemberg](https://github.com/dwaxemberg)  
* [Aaron Neyer](https://github.com/Aaronneyer)  
* [Andrew Hennessy](https://github.com/yetanothername)

Moonlight is the work of students at [Case Western](http://case.edu) and was
started as a project at [MHacks](http://mhacks.org).
