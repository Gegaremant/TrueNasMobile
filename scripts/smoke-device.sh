#!/usr/bin/env bash
# Device smoke test: install, launch, and fail on any fatal.
#
# The gap this fills: 1.0.2 passed a compile, a full lint pass and every unit
# test, then closed silently on every device the moment a password was
# accepted. Nothing in the repository could see it, because nothing here runs
# the app. A smoke test that boots it and reads logcat is the cheapest thing
# that would have.
#
# Usage:
#   scripts/smoke-device.sh                        # build, install, launch
#   scripts/smoke-device.sh --apk path/to.apk     # use an existing artifact
#   scripts/smoke-device.sh --no-build             # just install and launch
#
# Requires a connected device or running emulator (`adb devices`).
set -uo pipefail

APK=""
DO_BUILD=1
TIMEOUT_SECONDS=45
PACKAGE="com.gegaremant.truenasmobile"
ACTIVITY="$PACKAGE/com.gegaremant.truenasmobile.MainActivity"

while [ $# -gt 0 ]; do
  case "$1" in
    --apk) APK="$2"; shift 2 ;;
    --no-build) DO_BUILD=0; shift ;;
    --timeout) TIMEOUT_SECONDS="$2"; shift 2 ;;
    -h|--help) sed -n '2,15p' "$0"; exit 0 ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done

cd "$(dirname "$0")/.." || exit 1

command -v adb >/dev/null 2>&1 || { echo "adb not found: install Android platform-tools" >&2; exit 3; }

if ! adb get-state >/dev/null 2>&1; then
  echo "no device: connect one or start an emulator, then check 'adb devices'" >&2
  adb devices 2>&1 | sed 's/^/  /'
  exit 3
fi

if [ "$DO_BUILD" -eq 1 ] && [ -z "$APK" ]; then
  echo "== building debug APK"
  ./gradlew :app:assembleGithubDebug -q || { echo "build failed" >&2; exit 1; }
  APK="app/build/outputs/apk/github/debug/app-github-debug.apk"
  [ -f "$APK" ] || APK=$(ls app/build/outputs/apk/github/debug/*.apk 2>/dev/null | head -1)
fi

[ -n "$APK" ] && [ -f "$APK" ] || { echo "APK not found: pass --apk <file>" >&2; exit 2; }

echo "== artifact"
echo "   file   : $APK"
echo "   size   : $(wc -c < "$APK") bytes"
echo "   sha256 : $(sha256sum "$APK" | cut -d' ' -f1)"

echo "== ZIP integrity"
if command -v unzip >/dev/null 2>&1; then
  unzip -t "$APK" >/dev/null 2>&1 && echo "   CRC ok" || { echo "   CRC FAILED - redownload, do not install" >&2; exit 1; }
else
  echo "   (unzip not available, skipped)"
fi

echo "== installing"
adb install -r -d "$APK" || { echo "install failed" >&2; exit 1; }

echo "== clearing logcat and launching"
adb logcat -c 2>/dev/null
adb shell am force-stop "$PACKAGE" 2>/dev/null
adb shell am start -W -n "$ACTIVITY" || { echo "launch command failed" >&2; exit 1; }

echo "== watching $TIMEOUT_SECONDS s for a crash"
# The composition of MainScreen is the part that silently killed 1.0.2, so give
# the app time to get past login. Nothing here drives the UI: a human has to log
# in, and this only asserts that the process stays alive and logs nothing fatal.
FOUND=0
for i in $(seq 1 "$TIMEOUT_SECONDS"); do
  sleep 1
  if adb logcat -d -s AndroidRuntime:E 2>/dev/null | grep -q "FATAL EXCEPTION"; then
    FOUND=1
    break
  fi
  ALIVE=$(adb shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r')
  if [ -z "$ALIVE" ]; then
    # The process is gone and no FATAL was logged: exactly the 1.0.2 signature -
    # an early failure that dies before the framework can report it.
    echo "   the process disappeared after ${i}s with no FATAL EXCEPTION logged."
    echo "   That is the silent-death signature: capture the full log next time with"
    echo "   adb logcat -d > crash.txt and send it in - the cause is suppressed"
    echo "   underneath the ClassNotFoundException."
    FOUND=2
    break
  fi
done

if [ "$FOUND" -eq 0 ]; then
  echo "   no crash, process alive"
  echo
  echo "PASS: the app launched and stayed up. Log in by hand and repeat if you"
  echo "     want to cover the authenticated screens."
  exit 0
fi

echo
echo "FAIL: crash detected after ${i:-?}s. Logcat tail:"
adb logcat -d -s AndroidRuntime:E 2>/dev/null | tail -60
exit 1
