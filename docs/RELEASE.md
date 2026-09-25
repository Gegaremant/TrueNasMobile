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
detects bundle tasks and turns the split off for them, but a single
`./gradlew assembleGithubRelease bundlePlaystoreRelease` invocation enables the
split and breaks the bundle.

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
```

Expected signer: `CN=TrueNasMobile, OU=Gegaremant Labs, O=Gegaremant Labs`.
At minSdk 33 the APK is signed with scheme v3 only; that is normal, AGP drops
v1/v2.

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
