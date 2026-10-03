# Changelog

All notable changes to TrueNasMobile are listed here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
versioning follows [SemVer](https://semver.org/spec/v2.0.0.html).

## [1.0.6] — 2026-10-03

UI rework from the owner's feedback on 1.0.5. Plan and status:
`docs/UI-PLAN-2026-10-03.md`.

### Fixed
- **The Details screen printed garbage instead of the load.** CPU showed
  `1753567890123%` and the temperature a value in the billions, because the row
  read the *first* column of a `system.report` point - a Unix timestamp in
  milliseconds - as if it were the value. CPU is now the average across cores
  clamped to 0..100, memory and temperature read the value column. The read
  lives in one function now (`LatestMetric.latestValue`), so the timestamp
  cannot be mistaken for a reading a second time.
- **The Tasks tab header moved between tabs.** The Apps/Containers/VMs
  switcher sat above the header, so the header was not where the other tabs
  put it. It now belongs to `TasksScreen` and is drawn once above the switcher.
- **The filter row did not fit.** The checkmark on the selected chip ate ~24dp
  per chip, which pushed "New" off the end on a narrow screen.
- **Share details lost both the header and the bottom bar.** Storage stays
  highlighted there now.

### Changed
- **Header adapts to the device.** With a camera cutout at the top, the brand
  line steps below it and stays small; without one it grows. On a narrow
  screen the action row compacts and scrolls rather than pushing icons out.
- **The profile chip is an avatar now**: a circle with the first letter of the
  username. Tap opens the account page, long press opens a quick switcher that
  lists the saved accounts and switches in one tap.
- **Bottom bar labels are always visible**, and the Apps tab is gone - it
  duplicated the Tasks sub-tab. Removing it from the destination enum also
  migrates anyone who had it saved, because unknown names are dropped on load.
- **An empty share group is one quiet line** instead of a card with an icon;
  with nothing shared at all, a single sentence replaces three cards.
- **Tapping a saved account opens the editor** (login, password, server
  nickname) instead of switching silently; switching moved to its own button.
  The password field starts empty - it never reads the stored one back, and an
  empty field keeps the current password.

### Added
- **A figures-only widget at the top of Details**: disks by type and count, the
  RAID level from the vdev topology, total/used/free with a usage bar, and live
  CPU, memory (as a share of `physmem`, since the series reports bytes) and
  temperature.
- **A file browser at the bottom of a share** (`filesystem.listdir`, verified
  on a 26.0 stand): folders first, a parent-folder step, and "open elsewhere",
  which hands the path to a third-party app as an `smb://` link - the files
  live on the NAS, so a local file to pass does not exist.

## [1.0.5] — 2026-10-02

### Added
- **Tab header template.** Every tab now shares one header: a brand line with
  the app name on the left and the author on the right, then the tab title
  with instance settings (box + gear), search, profile chip (list + plus,
  opens the account switcher), notifications bell, power (shutdown/restart
  dialog lives in the header on every tab) and app settings (robot + gear,
  opens the mobile app settings so theme/profile/about stay reachable).
- **"New" action on the tab screens.** A real add action, not a filter:
  Applications → Marketplace; Containers and VMs → an honest "coming soon"
  toast (no create UI exists yet).
- **Storage pool details keep the bottom menu.** The pool screen shows the
  bottom bar with Home highlighted, per the storage-pool spec (top and
  bottom menu).
- **Container API migrated to TrueNAS 26.0.** The old `container.*` namespace
  no longer exists on 26.0; the screen speaks the new `container.*` API
  surface with all 18 methods.
- **"Сведения" (Details) rework.** The Statistics tab was renamed to
  Сведения and lost its placeholder Graphs sub-tab; CPU/Memory/Temperature
  are now expandable sections with real charts.
- **Storage tab = shared resources.** SMB/NFS/webshare via
  `sharing.webshare.query`; the old dead storage screen is gone.

### Changed
- **First run no longer begs for notification permission.**
  `POST_NOTIFICATIONS` is not auto-requested at first launch anymore; the
  setup bottom sheet still opens automatically on first run.
- **Biometric prompt fixed.** When the lock is off, the app no longer asks
  for the fingerprint at startup; Back from a tab returns to the tab you
  came from instead of dropping you on Home.

## [1.0.4] — 2026-09-29

### Fixed
- **The app would have crashed on Android 10 and 11.** Material You wallpaper
  colours (`dynamicLightColorScheme` / `dynamicDarkColorScheme`) need API 31, and
  `ic_launcher_background` pointed at `@android:color/system_accent1_100`, which
  also only exists from API 31. Both were introduced by lowering `minSdk` to 29 in
  1.0.2 and shipped in 1.0.3, because **only `lintVital` had ever run**: it is
  part of `assembleRelease`, and it did not fail on `NewApi` here, while the full
  `lintGithubRelease` has been reporting them since the floor moved. The CI status
  of `main` was not being looked at — it had been red since 27 September. The
  theme now falls back to the app's own palette below API 31, the icon background
  is a plain colour, `NewApi` stays fatal, and lint now runs in the release
  workflow as well as in CI.
- 40 `LocalContextGetResourceValueCall` findings, all pre-existing, are reported
  as warnings with a justification rather than silenced or refactored blind.
  Tracked as TODO 32.

## [1.0.3] — 2026-09-29

> **Superseded by 1.0.4**, which fixes a crash on Android 10 and 11 that 1.0.3
> shipped. The navigation fix below is in both.

### Added
- **A test suite that can see the bugs 1.0.2 shipped.** 39 local JVM tests, of
  which the interesting ones are new:
  - `NavHostOwnershipRuntimeTest` renders real `NavHost`s under Robolectric and
    calls the real `dashboardViewModelOwner`, so the graph-ownership rule is
    exercised rather than merely asserted about. It also fails if the inner
    graph ever *stops* rejecting the `main` entry, so the negative case keeps
    proving the check is sensitive.
  - `RouteRegistrationTest` fails when the app navigates to a route no NavHost
    registers — the whole-app version of the search bug, which crashed on tap.
  - `ReleaseArtifactTest` checks every ZIP entry's CRC, recomputes the dex
    Adler-32 and SHA-1 the way ART does, compares the dex size in the header with
    the archive, and prints a SHA-256. It is how a truncated download becomes
    distinguishable from a broken build without reading a stack trace.
  - `AppCacheLifecycleTest` and `AccountSwitchingTest` from the previous work,
    plus the existing localisation and navigation-bar guards.
- `./gradlew :app:verifyRelease` — one command that builds the release APKs, runs
  the whole test suite against them and runs lintVital, in that order.
- `scripts/smoke-device.sh` — installs an APK on a connected device, launches it,
  and fails on a fatal. It also recognises the silent-death signature: the
  process gone with no `FATAL EXCEPTION` logged, which is what 1.0.2 did.
- `docs/TESTING.md` — what is covered, what is not, the three Robolectric traps
  hit while writing the first Robolectric test here (the app's own Application
  refuses to start under test because it schedules WorkManager, and
  `navigation-testing` collides with Compose's navigator), and the rule that a
  new test must be made to fail before it is believed.

### Changed
- **The release workflow no longer publishes without checking anything.** It now
  runs the unit tests, then lint, then builds, then verifies the built artifacts,
  and only then signs and publishes. Before 27 September it went straight from
  tag push to published APK.

### Fixed
- **The app died silently right after a successful login.** Entering the main
  screen asked the *inner* NavHost for a back stack entry, but the `main`
  destination only exists in the *root* one — the two NavHosts are siblings, so
  neither graph sees the other's destinations. `getBackStackEntry` threw during
  composition, before the first frame, which is why there was no error to read:
  the app just closed. Fixed by asking the root controller, which is where
  `MainScreen` is hosted, and the lookup now lives in a named function
  (`dashboardViewModelOwner`) that both tests attack from both sides.
  A compile, a full lint pass, 27 unit tests and an APK integrity check all
  passed, because none of them run Compose navigation.
- A signed release that is merely *downloaded* badly is now diagnosable: the
  workflow publishes a `.sha256` per asset and repeats the sums in the release
  notes.

## [1.0.2] — 2026-09-27

> **Withdrawn.** This release crashed on every device immediately after login
> (see 1.0.3). It was never usable and has been replaced.

Localisation, security and dead-UI cleanup.

### Added
- **Instant profile switching, with the other profiles warmed in the background.**
  Every switch used to cost three round trips before anything could be drawn: a
  fresh WebSocket handshake, a full `auth.login`, and an `auth.generate_token` —
  repeated on every visit to the same NAS, because nothing was ever reused.
  `AccountSessionRegistry` now keeps one already-authenticated manager per
  account for the life of the process, and `MainViewModel.warmOtherProfiles`
  logs the remaining saved profiles in behind the user's back: one at a time,
  with a pause, a shorter connect timeout, and no side effects at all — warming a
  profile must not be able to make it the current one. An entry idle for more
  than three minutes is verified with a single `core.ping` before it is handed
  out, because a socket's local state does not prove the path is still alive;
  the ping costs far less than the login it avoids. Sessions are closed on
  sign-out and when a profile is deleted, and a successful ping of the active
  account keeps its entry fresh so returning to it is free too.
  `AccountSwitchingTest` guards the invariants.
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
- **Opening a list screen re-fetched a list the app had just fetched.** Apps,
  containers, VMs and services each seeded themselves from the cache and then
  asked the server anyway, even though a poller had refreshed the same rows
  seconds earlier. `AppCache` now records when each entry was written and
  exposes `isFresh(entry, ttl)`; a screen opening inside its own poll interval
  shows the cached rows and skips the request. A cold or stale cache still
  loads, and pull-to-refresh always goes to the network.
- **Six image call sites silently showed nothing.** Most TrueNAS app icons are
  SVG, and an `AsyncImage` with a bare url and no decoder fails the request
  rather than falling back — the marketplace tiles, app screenshots and the
  zoomable viewer all had it. `SvgDecoder` is now configured once on the
  app-wide `ImageLoader`, which covers those and every future call.
- **Widget icons bypassed the app's image cache.** `IconCache` built a private
  `ImageLoader` per download, so every home-screen icon missed the shared
  200 MB disk cache and came off the network again. It uses the app-wide loader
  now.
- **The search overlay kept its own copy of the marketplace catalogue**, which
  went stale for the life of its ViewModel; the one method that could refresh it
  had no callers. It reads the shared cache directly and writes its own fetch
  there, so the catalogue is fetched once per session instead of twice.
- Container and VM refresh were byte-for-byte second copies of the same RPC,
  cache write and state updates, and could only agree by accident. One code
  path each now serves the initial load, the poll, pull-to-refresh and the
  re-read after a start or stop.
- **Switching profiles left the app logged into the previous one.** The switcher
  wrote the session but never moved the "last used profile" pointer, which is
  what seventeen places read: app start, both widget activities, the AI app
  functions, the alert, job and sync workers, and session recovery. In practice a
  relaunch went back to the old NAS, the background workers polled its pools and
  sent its alerts, and an expired session recovered with another account's
  credentials — none of it visible on the switcher screen. The pointer now moves
  with the switch, and the shared `AppCache` is dropped with it so the previous
  server's pools and apps cannot be rendered for the new one.
- **The cached dashboard survived signing out.** `AppCache` is a process-wide
  object, and `clearAllCache()` had no call sites at all — so signing out of one
  TrueNAS and into another kept the first server's pools, disks, shares,
  services, apps and system info in memory for the dashboard and the search
  overlay to render. Every successful login now clears it (all three login paths
  funnel through one function) and both sign-out paths clear it too.
  `clearAllCache()` also skipped the marketplace catalogue and the service list,
  so even a call would have left those behind.
- **The performance screen reloaded the whole dashboard every 10 seconds.**
  Its poll went through the dashboard refresh, which re-ran all eight RPCs —
  `system.info`, `pool.query`, `disk.query`, both share queries, the update
  list, the version string and the graphs — to redraw three charts. Pools, disks,
  shares and the update list do not move on that cadence, so the poll now reads
  the graph data only.
- **The dashboard was fetched three times over.** Home, Storage and Performance
  each built their own `HomeViewModel`, so opening the app fired three identical
  eight-RPC batches and started three 30-second connectivity polls for one
  dataset. All three screens now share one instance, owned by the `Screen.Main`
  back stack entry so it is still discarded on sign-out.
- `AppCacheLifecycleTest` guards the invariants: the clear reaches every cache
  flow (a new field that forgets its reset fails the build), and both the login
  and the sign-out path call it.
- **Pulling a Docker image looked like it did nothing.** `app.image.pull` only
  returns a job id — the image appears in the list once the server finishes —
  but the screen reloaded the list immediately, so it kept showing the old
  contents. The job is now tracked through `JobTracker.pollJobStatus`: a
  progress row with the percentage and the server's own description appears
  above the search field, the pull button is disabled while it runs, and the
  list reloads when the job actually ends. If the job fails, the reason comes
  from the job record itself (`exception`/`error`) rather than a bare "pull
  failed" — a mistyped reference and a registry auth failure need different
  fixes. Timing out after five minutes says the pull is still running on the
  server instead of claiming failure.
- **Search threw on two of its own results, and the system back button broke
  the screen behind it.** The search overlay is drawn on top of the NavHost
  rather than routed through it, so nothing consumed the back gesture: pressing
  back popped the screen *underneath* while the search stayed on screen, and the
  next press left the app. It now has a `BackHandler`.
- **Closing search threw away the tab you opened it from.** `onCloseSearch`
  navigated to Home with `popUpTo(startDestination)`, so dismissing the search
  from Storage, Apps, Tasks or Performance dumped you on the dashboard. Closing
  now just reveals the screen that is still sitting in the back stack.
- **Tapping "Account" or "Change password" in the search crashed the app.** Both
  are registered in the root NavHost only, while `SearchResultNavigation` sent
  every result to the inner one — "Navigation destination that cannot be found in
  the NavController's graph". The controller is now resolved by asking both
  graphs which one knows the route. `SearchRouteTest` fails the build if the
  search index ever points at a screen no NavHost registers again.
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
