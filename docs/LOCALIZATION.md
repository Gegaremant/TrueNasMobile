# Localization

TrueNasMobile ships two locale dictionaries and nothing else:

| File | Role |
|---|---|
| `app/src/main/res/values/strings.xml` | English — the **base**. Every key lives here. |
| `app/src/main/res/values-ru/strings.xml` | Russian translation. Must cover every base key. |

There is no third language yet, and no per-feature split: one dictionary per
locale, so a new string is one edit in each of two files.

## The rule

**No user-visible text in Kotlin.** Not English, not Russian. Every label,
title, dialog, error message, toast, content description and empty-state message
comes from the dictionaries.

An earlier revision hardcoded Russian in `Homepage.kt`, `MainScreen.kt` and
`PerformanceScreen.kt`, which is how a Russian device ended up with a
half-translated start-up flow. The `LocalizationTest` unit test now fails the
build if a Cyrillic string literal reappears in `.kt` source.

### How to read a string

| Context | Use |
|---|---|
| Composable | `stringResource(R.string.example)` |
| Activity / non-composable with a `Context` | `context.getString(R.string.example)` |
| ViewModel, enum, data class | store `@StringRes Int` and resolve later |
| ViewModel with no `Context` | `ToastManager.resolveString(R.string.example)` |
| Glance widget | `context.getString(...)` |

`stringResource()` is a `@Composable`. It cannot be called from
`LaunchedEffect`, `semantics {}`, a `ifEmpty {}` lambda, or any other
non-composable lambda. Pre-resolve into a `val` or use `context.getString`.

Enums that map to a user-visible label carry `@StringRes val labelRes: Int` and
resolve it at render time — see `VmFilterCategory`, `SearchCategory`,
`TimeRange`.

## Adding a string

1. Add the key to `values/strings.xml` (English):

   ```xml
   <string name="pool_scrub_enabled">Scrub enabled</string>
   ```

2. Add the same key to `values-ru/strings.xml` (Russian):

   ```xml
   <string name="pool_scrub_enabled">Очистка включена</string>
   ```

3. Use it: `stringResource(R.string.pool_scrub_enabled)`.

4. Run `./gradlew :app:testGithubDebugUnitTest`.

Never let step 2 slide — a missing translation fails the parity test.

## Formatting and escaping

- Always use positional arguments, never implicit ones:
  `<string name="vm_start_message">Start \'%1$s\'?</string>`
- The same `%n$` placeholders must appear in both locales. A mismatch throws
  `IllegalFormatException` at runtime, and the test catches it at build time.
- Escape apostrophes as `\'`. A bare `'` fails aapt with
  `Invalid unicode escape sequence`.
- Escape `&` as `&amp;`, `<` as `&lt;`, `>` as `&gt;`.
- Never use `'` for Russian «ёлочки» in the RU file if you want typographic
  quality — prefer `«…»` for quotes.

### Plurals

English needs only `other`. Russian needs `one`, `few`, `many`, `other` or the
grammar breaks ("1 сохранённый аккаунт" vs "2 сохранённых аккаунта" vs
"5 сохранённых аккаунтов"):

```xml
<!-- values/strings.xml -->
<plurals name="account_saved_count">
    <item quantity="one">%1$d saved account</item>
    <item quantity="other">%1$d saved accounts</item>
</plurals>

<!-- values-ru/strings.xml -->
<plurals name="account_saved_count">
    <item quantity="one">%1$d сохранённый аккаунт</item>
    <item quantity="few">%1$d сохранённых аккаунта</item>
    <item quantity="many">%1$d сохранённых аккаунтов</item>
    <item quantity="other">%1$d сохранённых аккаунтов</item>
</plurals>
```

Read with `pluralStringResource(R.plurals.account_saved_count, count, count)`.

## What is deliberately *not* translated

Translating these would make them worse, not better:

- **Search keywords.** `SearchViewModel` matches English synonyms
  ("shutdown", "storage") because TrueNAS itself is English and users search in
  English. Keep them, and keep the localized label for display.
- **Route strings** in `Screen.kt` (`"app_details"`). They are navigation keys,
  never rendered.
- **Technical values**: UID/GID/SID, `RUNNING`, `smb`/`nfs`, `dangling`,
  `MMM dd, yyyy HH:mm`, `N/A`, `%1$s MB`.
- **Brand names**: Gegaremant Labs, TrueCommand, TrueNAS Connect.
- **Device locale** follows the system automatically — there is no in-app
  language switcher. Adding one means a `LocaleManager` in the manifest and a
  per-app locale preference.

## Checks

```bash
# the guard test
./gradlew :app:testGithubDebugUnitTest

# manual sweep for anything that slipped past the test
grep -rnE 'Text\((text = )?"[A-Za-zА-Яа-я]|contentDescription = "' app/src/main/java --include=*.kt \
  | grep -vE 'stringResource|getString|resolveString'
```

A green sweep is not "no text" — `label = "color"` on a Compose animation and
`Text("UID: $it")` are both fine. The test is there to catch the regression, the
sweep to find the specific leftovers.
