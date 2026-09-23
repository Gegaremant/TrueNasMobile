**[Русский](README.md) | [English](README.en.md)**

# TrueNasMobile

**A native Android client for managing TrueNAS. Simple, fast, right from your phone.**

**⬇️ [Download the latest release](https://github.com/Gegaremant/TrueNasMobile/releases/latest)** · Android 14+

TrueNAS is a great system for storage and home servers. But there is one big injustice: it never got a proper native app. Keeping an eye on your server through a browser on a small screen is awkward, and the two existing client versions (NasDesk and TrueHub) simply don't cover all the capabilities.

So we decided to build our own app — in-house, inspired by those two projects. TrueNasMobile is built from scratch as a modern native application with everything we were missing: quick access to containers, honest performance graphs, and full NAS management without a browser.

This project is inspired by NasDesk and TrueHub.

## Features

- **Apps**
  - Install from catalog (including additional instances)
  - Start / Stop / Restart
  - Update and roll back versions
  - View details and edit configuration
  - Uninstall apps
- **Containers**
  - Start / Stop
  - View status and details
- **Virtual machines**
  - Start / Stop / Shut down
  - View status and details
- **Storage**
  - Browse pools and datasets
  - Monitor usage
- **Performance**
  - Real-time load graphs (CPU, memory, network, disks)
  - Overall system dashboard
- **Notifications**
  - Push notifications about important NAS events
  - Alerts and system warnings feed
- **Search**
  - Instant search across your whole server, from anywhere in the app
- **Home screen widgets**
  - Quick launch for apps
  - Pools summary
  - App updates
- **Security**
  - Login with API key, password and 2FA
  - Biometric app lock
- **Multiple servers**
  - Manage several TrueNAS instances at once

## Build from source

Requirements:

- JDK 17+
- Android SDK (compileSdk 37 / targetSdk 37)
- Minimum Android: 14 (minSdk 33)

Build:

```bash
git clone https://github.com/Gegaremant/TrueNasMobile.git
cd TrueNasMobile
./gradlew assembleGithubRelease
```

APKs will be placed in `app/build/outputs/apk/github/release/`.

## Author

Built by [Gegaremant](https://github.com/Gegaremant). Check the profile for other projects by the author.

## License

This project is licensed under [GPL-3.0](app/src/main/res/raw/gpl_v3.txt). It is an unofficial app and is not affiliated with iXsystems.