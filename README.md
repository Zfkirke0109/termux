# Termux beta.3 + PR #5357 — mobile edition

This repository contains an **unofficial custom build**, not a Termux project release.

## Sources

- Base: [termux/termux-app v0.119.0-beta.3](https://github.com/termux/termux-app/releases/tag/v0.119.0-beta.3), commit `e634d8f981f48b6b89202cf0e04533f0889e03b3`.
- Backport: [PR #5357](https://github.com/termux/termux-app/pull/5357), commit `7dd6e3fb1baee13641b08cf129b0f949c0d0e3d7`, by matthematics1137.
- PR #5357 is backported; later upstream master changes are not included. `patches/mobile-friendly.patch` adds the mobile actions and JNI correction described below.
- Source and upstream licenses are preserved under `termux-app/`.

## Downloads

`downloads/termux-app_v0.119.0-beta.3+apt-android-7-github-debug_arm64-v8a.apk` is the **unchanged upstream release APK**. It does **not** contain PR #5357.

Upstream SHA-256: `3bb969df2400d884ccb929b7d79cc76861f291c98942c401e12408d734a460f3`.

The root **Build beta.3 with PR 5357** Actions workflow fetches pinned source, runs emulator and app unit tests, checks APK signing certificates, and uploads APKs, checksums and certificate reports. After all checks pass it commits the source, unchanged original APK and custom arm64 APK to this branch under `termux-app/` and `downloads/`. Its version name is `0.119.0-beta.3+pr5357.mobile.<commit>`; the package ID (`com.termux`) and version code (`1022`) are unchanged.

## Mobile changes

Long-press the terminal and open **More**:

- **Search recent output**: literal, case-insensitive search, newest rows first. It searches up to 2,000 recent visual rows / 250,000 characters and returns up to 100 matching rows. Wrapped rows are searched separately. Changed output is checked before jumping to a result.
- **Copy visible output**: copy the viewport without dragging selection handles; preserves leading indentation and joins soft-wrapped rows.
- **Jump to latest output**: return to the live screen.
- **Stable scrollback reading**: scrolling upward no longer snaps back to the bottom whenever fresh output arrives. Typing or jumping to latest returns to the live screen, subject to the pre-existing manual auto-scroll toggle.

Search follows upstream request [#4701](https://github.com/termux/termux-app/issues/4701); stable reading addresses requests [#1242](https://github.com/termux/termux-app/issues/1242) and [#684](https://github.com/termux/termux-app/issues/684). These are concrete upstream requests, not a claim that this is a complete ranking of community demand. Copy-visible and jump-latest are supporting mobile conveniences.

The JNI working-directory UTF string is now released with its matching `cwd` Java reference rather than `cmd`. See [Android 17 review](docs/ANDROID17_REVIEW.md) for limits and remaining device tests.

## Signing and compatibility

Debug builds retain upstream `app/testkey_untrusted.jks`. Expected signing certificate SHA-256:

`B6DA01480EEFD5FBF2CD3771B8D1021EC791304BDD6C4BF41D3FAABAD48EE5E1`

This is Termux's **publicly shared, untrusted GitHub test key**, not an official private developer key. Anyone can sign with it; a matching certificate alone does not establish authenticity. Verify provenance and file checksums. This custom build must not be presented as an official Termux release.

The APK is intended for existing **GitHub-source** Termux installations. F-Droid and Play Store signing identities differ. Do not uninstall Termux or its plugins merely to try this APK: uninstallation can delete your terminal data. Back up first, and confirm the installed certificate before attempting an in-place update.

## Local build

Use Java 11, Android SDK platform 30/build-tools 30.0.3 and NDK 22.1.7171670:

```sh
cd termux-app
export TERMUX_PACKAGE_VARIANT=apt-android-7
export TERMUX_APP_VERSION_NAME=0.119.0-beta.3+pr5357.local
export TERMUX_APK_VERSION_TAG=v0.119.0-beta.3+pr5357.local-apt-android-7-github-debug
./gradlew --no-daemon :terminal-emulator:testDebugUnitTest :app:testDebugUnitTest assembleDebug
```

No rooting or device installation is performed by this repository.
