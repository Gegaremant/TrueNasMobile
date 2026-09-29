# Release process

How a TrueNasMobile release is built, signed and shipped. Written for v1.0.2.

## 0. Before you start

| Requirement | Value |
|---|---|
| JDK | 17+ |
| Android SDK | compileSdk 37 / targetSdk 37 |
| Build tools | 37.0.0 (at `~/Projects/android-sdk/build-tools/37.0.0`) |
| Keystore | `~/keystores/truenasmobile-release.jks`, alias `truenasmobile` |
| Keystore password | `~/keystores/truenasmobile-release-password.txt` (mode 600) |

Verify the keystore is intact before you rely on it — there is no backup, and
losing it means you can never update the app on a store:

```bash
keytool -list -v -keystore ~/keystores/truenasmobile-release.jks \
  -storepass "$(cat ~/keystores/truenasmobile-release-password.txt)"
```

## 1. Secrets

Every secret lives in `.env` at the repository root, which is git-ignored.
`app/build.gradle.kts` reads the signing keys from, in order:

1. real environment variables (this is what CI injects),
2. Gradle properties (`~/.gradle/gradle.properties`, never committed),
3. the root `.env`.

`cp .env.example .env && chmod 600 .env` and fill it in. The keys:

| Key | Meaning |
|---|---|
| `KEYSTORE_PATH` | absolute path to the `.jks` on this machine |
| `KEYSTORE_ALIAS` | key alias, must match `keytool -list` |
| `KEYSTORE_PASSWORD` | store + key password |
| `KEYSTORE_BASE64` | alternative to `KEYSTORE_PATH` for CI: the whole keystore base64-encoded, decoded into the git-ignored build dir |

Local builds use `KEYSTORE_PATH`; CI uses `KEYSTORE_BASE64`.

If none of them are set, the release build **still runs** but logs a warning and
produces an **unsigned** APK. Always check the artifact name — unsigned builds end
in `-unsigned.apk`.

## 2. Bump the version

In `app/build.gradle.kts`, `defaultConfig`:

```kotlin
versionCode = 10002   // monotonically increasing, never reused
versionName = "1.0.2"
```

## 3. Verify before building

```bash
./gradlew :app:testGithubDebugUnitTest   # localization guard
./gradlew :app:compileGithubDebugKotlin  # fast compile check
```

The localization test is not optional: it fails the build on an EN/RU key
mismatch, a mismatched format placeholder, a duplicate key, or hardcoded
Cyrillic in Kotlin. See `docs/LOCALIZATION.md`.

## 4. Build

```bash
./gradlew :app:assembleGithubRelease     # F-Droid / GitHub / IzzyOnDroid
./gradlew :app:assemblePlaystoreRelease  # per-ABI APKs, playstore flavor
./gradlew :app:bundlePlaystoreRelease    # Google Play AAB
```

Output:

| Task | Artifact |
|---|---|
| `assembleGithubRelease` | `app/build/outputs/apk/github/release/app-github-<abi>-release.apk` |
| `assemblePlaystoreRelease` | `app/build/outputs/apk/playstore/release/app-playstore-<abi>-release.apk` |
| `bundlePlaystoreRelease` | `app/build/outputs/bundle/playstoreRelease/app-playstore-release.aab` |

**Build the bundle as a separate command.** A manual ABI split and an AAB cannot
coexist: AGP fails with *"Multiple shrunk-resources files found… Please disable
building multiple APKs when building an Android app bundle"*. `app/build.gradle.kts`
detects bundle tasks and turns the split off for them, so the two must not share
an invocation.

Note which way that fails, because the APK side is the quiet one. A single
`./gradlew assembleGithubRelease bundlePlaystoreRelease` enables the bundle
detection for *both* tasks, so `assembleGithubRelease` happily produces one
universal `app-github-release.apk` instead of the three per-ABI files, and the
bundle fails. The APK build does not error — it just hands you a different
artifact set, and step 5 then cannot find
`app-github-arm64-v8a-release.apk` at all. Always check the file list in step 4,
not just the exit code.

There is deliberately no universal APK: `isUniversalApk = false` in
`splits.abi`. Keep the `output:` list in the F-Droid recipe in sync with
`include("armeabi-v7a", "arm64-v8a", "x86_64")`.

## 5. Check the artifacts

Never ship an artifact you have not verified:

```bash
BT=~/Projects/android-sdk/build-tools/37.0.0
APK=app/build/outputs/apk/github/release/app-github-arm64-v8a-release.apk

$BT/apksigner verify --verbose --print-certs "$APK"   # must say "Verifies"
$BT/aapt2 dump badging "$APK" | head -3              # versionCode/versionName
$BT/aapt2 dump configurations "$APK" | grep -c ru     # the ru locale is present
unzip -t "$APK" >/dev/null && echo "zip intact"      # CRC of every entry
```

### What a broken download looks like

A truncated or mangled APK **installs successfully** and then dies on launch
with a `ClassNotFoundException` for the Application class — which is a red
herring. The real line is suppressed under it:

```
Failed to open dex files from base.apk: Bad checksum (745ca288, expected 41c63683)
```

The class loader could not open `classes.dex`, so of course it found no class.
Verify the dex the way ART does (Adler-32 from offset `0x0C`, SHA-1 from
`0x20`) before suspecting the build. That is also why the release publishes a
`.sha256` per asset and repeats the sums in the release notes: a user whose
download got cut can confirm it in one command instead of reading logcat.

```bash
sha256sum truenasmobile-v1.0.3-arm64.apk   # must equal the published .sha256
```

Expected signer: `CN=TrueNasMobile, OU=Gegaremant Labs, O=Gegaremant Labs`.
The APK is signed with scheme v3 only. That is correct for the Android 10
floor: API 24 and above verify v3 signatures, so AGP drops v1/v2 on its own.

Check the two things that decide whether existing users get an update instead of
a fresh install — a mismatch in either one means the release has to be
re-versioned before it ships:

```bash
# versionCode must be strictly greater than the published one, and the
# applicationId and signer must be unchanged
$BT/aapt2 dump badging "$APK" | head -1
$BT/apksigner verify --print-certs "$APK" | grep "SHA-256 digest"

# compare against the previous release's asset
curl -sSL -o prev.apk \
  "https://api.github.com/repos/<owner>/<repo>/releases/assets/<asset id>" \
  -H "Authorization: token $TOKEN" -H "Accept: application/octet-stream"
$BT/apksigner verify --print-certs prev.apk | grep "SHA-256 digest"
```

For 1.0.1 → 1.0.2 the reference values are: `versionCode` 10001 → 10002,
`applicationId com.gegaremant.truenasmobile` unchanged, signer SHA-256
`757ae97a85fc63689e5941f31884c3ac60ddcfca5059369c2b48053dc0f31bad`. Lowering
`minSdk` (33 → 29) is safe in this direction — it can only widen the set of
devices that accept the update.

## 6. Tag and publish

```bash
git tag -a v1.0.2 -m "TrueNasMobile 1.0.2"
git push origin main --tags
```

Then create the GitHub release from the signed APKs and update:

- `CHANGELOG.md`
- `CurrentVersion` / `CurrentVersionCode` / the `Builds` block in
  `docs/fdroid-submission/fdroiddata-com.gegaremant.truenasmobile.yml`

## 7. F-Droid

F-Droid builds from source on its own servers, so it needs no signed APK from
you — it needs the tag, the matching `versionName`/`versionCode` in
`app/build.gradle.kts`, and an `output:` list because the build produces
per-ABI APKs. Full instructions: `docs/fdroid-submission/README.md`.

## Troubleshooting

**`SigningConfig "release" is missing required property "keyAlias"`** — the
variable holding the alias was named `keyAlias`, so inside
`signingConfigs { create("release") { keyAlias = keyAlias } }` the right-hand
side resolved to the SigningConfig's own property. The variables are deliberately
named `releaseKeyAlias` / `releaseKeystorePassword` to avoid this.

**`Invalid unicode escape sequence` from aapt** — a bare apostrophe in a
`strings.xml` value. Write `\'`.

**`Found item String/x more than one time`** — a duplicate key in a resource
file. Check with:
`grep -oE 'name="[^"]+"' app/src/main/res/values/strings.xml | sort | uniq -d`

**Kotlin errors are invisible** — do not verify with `./gradlew -q ... | grep '^e:'`.
The quiet flag suppresses the `e:` prefix, so the grep finds nothing and a broken
build looks green. Use `--console=plain` without `-q` and look for `BUILD`.
