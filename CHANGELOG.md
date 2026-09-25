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
- The app now follows the system language. Previously a mix of hardcoded Russian,
  hardcoded English and resources produced a half-translated UI — the reported
  "half-Chinese/English start-up flow".
- Search no longer offers "Shutdown system" / "Restart system" / "Refresh data"
  results: the tap handler was empty, and triggering a shutdown from a search
  overlay with no confirmation is unsafe. These remain on the Home header, which
  asks first.
- The Settings → "Privacy" row (which only showed a "work in progress" toast)
  was removed.
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
