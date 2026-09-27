# Changelog

All notable changes to TrueNasMobile are listed here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
versioning follows [SemVer](https://semver.org/spec/v2.0.0.html).

## [1.0.2] — 2026-09-25

Localisation, security and dead-UI cleanup.

### Added
- English + Russian locale dictionaries with full key parity
  (`res/values/strings.xml` / `res/values-ru/strings.xml`).
- `LocalizationTest` — build-time guard: EN/RU key parity, no duplicate keys,
  matching format placeholders, valid plural quantities, and no hardcoded
  Cyrillic in Kotlin sources.
- `.env.example` documenting every secret the build reads.
- Release signing wired up: `KEYSTORE_PATH` or `KEYSTORE_BASE64` plus
  `KEYSTORE_ALIAS` / `KEYSTORE_PASSWORD`, resolved from the environment or the
  git-ignored root `.env`.
- GPL-3.0 `LICENSE` at the repository root.
- `LoadErrorContent` — shared error body with a retry action.
- `MissingSelectionScreen` — shown when a detail screen is opened without its
  backing data (e.g. after process death).
- `docs/RELEASE.md`, `docs/LOCALIZATION.md`.
- **User profile screen.** `ui/profile/ProfileScreen.kt` and its ViewModel were
  written (562 lines) and never wired to anything: no route, no call site, so
  the signed-in user, their roles and the account type had nowhere to show up.
  It is now a real destination — Settings → Account → Profile — with a back
  button added to its own header, which `UnifiedScreenHeader` does not provide.
- **Local administrator setup is reachable.** `Screen.LocalAdminSetupScreen`
  was registered as a destination while `UserListScreen` sat one level above it
  holding an `onNavigateToSetupAdmin` callback that `MainScreen` never passed.
  The FAB menu item "Set up a local administrator" therefore did nothing at all,
  and `setupLocalAdministratorWithResult` was dead code. Both are now live, and
  the menu item hides itself once `user.has_local_administrator_set_up` reports
  that one exists — the check that was also dead, since nothing called it.

### Fixed
- **Credentials were included in cloud backup and device transfer.**
  `data_extraction_rules.xml` and `backup_rules.xml` shipped as Android Studio
  boilerplate with the exclusions commented out, so the `secure_preferences`,
  `multi_account_preferences` and `push_preferences` DataStores — TrueNAS
  passwords, API keys and session tokens — were uploaded to the user's Google
  backup by default. Both files now exclude them.
- **Session recovery reported success on failure.**
  `performSessionRecovery()` returned `true` when `generateToken` failed, so the
  original request was retried with the dead session and the user got a generic
  "session expired" instead of a re-auth.
- **Dismissing or restoring an alert always reported success.** The real API
  result was discarded and `ApiResult.Success` was fabricated, so failures showed
  a success toast.
- **App upgrade via the AI AppFunction reported success without upgrading**
  when the API returned a `Loading` result.
- Detail screens no longer crash (`app!!` / `container!!` / `vm!!`) when reached
  without seeded data, e.g. from a restored back stack.
- Dataset explorer and pool details rendered a blank page on error and the
  dataset error banner could not be dismissed; both now show a real error with
  retry.

### Changed
- **`minSdk` lowered from 33 to 29 — the app now installs on Android 10 and
  newer.** Android 14 as a hard floor was the leading suspect behind reported
  "version incompatible" installs: a device below 14 could not install the
  release at all. Verified against `lintVitalRelease` and the built APK
  (`minSdkVersion=29`); the only API 35 calls in the tree go through
  `NotificationCompat`, which drops them on older platforms by itself. The
  release APK stays v3-signed, which is all the Android 10 floor requires.
- The app now follows the system language. Previously a mix of hardcoded Russian,
  hardcoded English and resources produced a half-translated UI — the reported
  "half-Chinese/English start-up flow".
- Search no longer offers "Shutdown system" / "Restart system" / "Refresh data"
  results: the tap handler was empty, and triggering a shutdown from a search
  overlay with no confirmation is unsafe. These remain on the Home header, which
  asks first.
- The Settings → "Privacy" row (which only showed a "work in progress" toast)
  was removed.
- **The bottom navigation bar is customizable and no longer hardcoded.** It was
  a fixed `listOf(Home, Storage, Tasks, Performance)` in `MainScreen`, while a
  full personalization subsystem around it — save, load, order, toggle — existed
  with no call sites at all. The theme screen now has a "Bottom navigation"
  editor: switch destinations on and off, reorder them, reset to defaults.
  Reordering actually reorders now; the resolver used to throw the user's order
  away and rebuild the bar from the catalog order. Home stays pinned first.
  Defaults are Home, Storage, Apps, Tasks, Performance — Apps is new to the bar
  because it was otherwise reachable only through the search overlay.
- `Compact navigation` setting now does something: it hides the labels under the
  bottom-bar icons to fit more destinations on screen. It was saved to disk and
  read by nothing.
- `True OLED black` is now reachable: the theme screen has the switch that was
  missing. The mode was already applied by `AdaptiveTheme` and advertised in two
  places in the UI, but no code path could ever turn it on.
- Licenses screen header now says "App License" — it shows this project's GPL-3.0
  licence, not a list of third-party licences.

### Removed
- `Screen.title` and `AppTheme.description`: never read, and both had drifted
  into hardcoded Russian.
- The commented-out Load Averages feature and the unused `HalfCircleGauge`
  component.

### Security
- API keys, passwords and session tokens are no longer eligible for backup.
- No secret is committed. The remote token lives only in `.git/config`; the
  release keystore lives outside the repository; `.env` is git-ignored and
  created with mode 600.

## [1.0.1]

Previous release. See the repository history for details.

## [1.0.0]

Initial public release.
