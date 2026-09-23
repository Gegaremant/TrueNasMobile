# TrueNasMobile — руководство по размещению в F-Droid и IzzyOnDroid

Оба магазина тянут приложение из публичного GitHub-репозитория. После принятия
первоначальной записи обновления поступают автоматически при каждом теге нового
релиза с соответствующими статичными `versionName`/`versionCode` в
`app/build.gradle.kts` и прикреплёнными к GitHub-релизу подписанными APK.

Всё готово в репозитории:
- `fastlane/metadata/android/en-US/` — краткое/полное описание, название, иконка и
  скриншоты в `images/phoneScreenshots/`.

---
## 1) F-Droid (официальный) — сборка из исходников обязательна
Отправьте файл метаданных в репозиторий fdroiddata (GitLab) в виде merge request.

1. Убедитесь, что у вас есть аккаунт GitLab. Затем сделайте форк:
   https://gitlab.com/fdroid/fdroiddata
2. Создайте новую ветку, например `add-truenasmobile`.
3. Добавьте файл `metadata/com.gegaremant.truenasmobile.yml` с содержимым
   `fdroiddata-com.gegaremant.truenasmobile.yml` (рядом с этим файлом).
4. Запушьте ветку и откройте merge request в `master` fdroiddata.
5. Мейнтейнеры F-Droid проверяют исходники + рецепт сборки, затем принимают. Сборки и
   публикация происходят автоматически (обычно в течение нескольких дней после принятия;
   при первом запуске требуются ручные шаги с keystore).

Примечание: F-Droid собирает из исходников на своих серверах; подписанный APK от вас не нужен.

Альтернатива (без MR): отправьте запрос в очередь заявок приложения F-Droid на
https://f-droid.org → «submit an app», чтобы мейнтейнеры упаковали его (медленнее).

---
## 2) IzzyOnDroid — принимает ваш подписанный релизный APK
Репозиторий переехал на Codeberg. Запросите включение, открыв issue:

1. Заведите аккаунт Codeberg (или GitHub-аккаунт, раз issue в Codeberg позволяют).
2. Откройте новый issue на: https://codeberg.org/IzzyOnDroid/repo/issues
   (выберите шаблон «new app inclusion», если его предложат).
3. Заполните детали (см. предлагаемый текст ниже). Больше ничего не нужно —
   подписанные релизные APK и fastlane-метаданные подтягиваются автоматически из
   тегов GitHub-релизов.

Предлагаемая тема issue: `[New App] TrueNasMobile - Android client for TrueNAS SCALE`

Предлагаемый текст issue:
```
App Name: TrueNasMobile
Author: Gegaremant
Source: https://github.com/Gegaremant/TrueNasMobile
License: GPL-3.0-or-later

Description:
TrueNasMobile is an open-source, modern native Android client for managing a TrueNAS
SCALE server - monitor performance, manage apps, containers and VMs, and stay on
top of alerts, all from your phone.

It builds release-signed per-ABI APKs (~11 MB) attached to tagged GitHub releases.
Fastlane metadata (descriptions + icon + screenshots) are present in the repo at
fastlane/metadata/android/en-US/.

Link to latest release APKs:
https://github.com/Gegaremant/TrueNasMobile/releases/tag/v1.0.0
```

Инструменты Izzy подтянут APK с тегов GitHub и fastlane-метаданные, проверят и
(обычно в течение ~24 часов) добавят приложение в каталог.

---
## Советы
- Держите `versionName`/`versionCode` статичными в `app/build.gradle.kts` и
  вручную повышайте их в каждом релизе; тег должен совпадать (например
  `v1.0.1` -> `1.0.1`/`10001`).
- Прикрепляйте подписанные релизные APK к соответствующему GitHub-релизу (это делает CI).
- Добавляйте changelog в `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`
  для каждого релиза — так лучше отображается в каталоге.