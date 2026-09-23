# TrueNasMobile Push Relay

Минимальный self-hosted релей, доставляющий webhook-предупреждения TrueNAS в
Android-приложение TrueNasMobile в виде push-уведомлений со сквозным
шифрованием.

```
TrueNAS ──webhook──▶ relay ──шифрованный envelope──▶ ntfy ящик ◀──websocket── приложение TrueNasMobile
                        │
                        └── каждое устройство получает свой E2E-enabled envelope
```

Рели — это один процесс Kotlin/JVM без внешних зависимостей, построенный на
`com.sun.net.httpserver` из JDK. Он никогда не видит открытый текст предупреждений:
устройства регистрируют EC P-256 **открытый** ключ, а каждый payload шифруется
на этот ключ с помощью эфемерной ключевой пары (forward secrecy). Даже если
релей (или публичный ntfy-ящик) будет полностью скомпрометирован, содержимое
предупреждений останется запечатанным.

## Быстрый старт

### Docker (рекомендуется)

```bash
docker compose up -d --build
```

### Локальный JVM

```bash
./gradlew installDist
build/install/truenasmobile-relay/bin/truenasmobile-relay
```

Порт по умолчанию — `8080`.

## Конфигурация (переменные окружения)

| Переменная                | По умолчанию        | Назначение                                                                 |
|---------------------------|---------------------|----------------------------------------------------------------------------|
| `PORT`                    | `8080`              | HTTP-порт прослушивания                                                    |
| `RELAY_TOKENS`            | *пусто (dev-режим)* | Токены рели через запятую. Запросы требуют `Authorization: Bearer <token>`. Пусто = принимается любой токен (только для dev!) |
| `RELAY_DATA_FILE`         | `data/devices.json` | JSON-файл с реестром устройств                                              |
| `NTFY_URL`                | `https://ntfy.sh`   | ntfy-совместимый сервер, используемый как ящик шифротекста для устройств `ntfy`. Подходит и self-hosted ntfy |
| `FCM_SERVICE_ACCOUNT_FILE`| *не задано*         | Путь к JSON сервисного аккаунта Firebase. Задайте, чтобы включить транспорт `fcm` |
| `FCM_PROJECT_ID`          | *из JSON*           | Необязательное переопределение id проекта Firebase                        |

## HTTP API

Все эндпоинты возвращают JSON. Аутентификация — `Authorization: Bearer <relayToken>`.

### `GET /healthz`
Проверка доступности, используемая кнопкой «Тест» в приложении.

```json
{ "ok": true }
```

### `POST /api/device/register`
Регистрирует (или обновляет) устройство для текущего токена. Повторная
регистрация того же `pushToken` заменяет предыдущую запись — приложение
перерегистрируется при каждом запуске сервиса, поэтому открытые ключи
устройств восстанавливаются автоматически после сброса.

```json
{
  "name": "Pixel 9",
  "pubkey": "BBase64(0x04||X||Y)",     // сырая точка EC P-256 без сжатия, 65 байт
  "transport": "ntfy",
  "pushToken": "th-4f1a7c3d9e"         // тема ntfy (или FCM-токен позже)
}
```

```json
{ "ok": true, "deviceId": "6f3d…" }
```

### `POST /api/send`
Эндпоинт webhook-предупреждений TrueNAS. Тело обрабатывается как непрозрачные
байты и пересылается каждому зарегистрированному устройству в виде отдельного
шифрованного envelope.

```json
{ "ok": true, "delivered": 1 }
```

`502` с `"delivered": N`, если часть транспортов не сработала.

## Настройка TrueNAS

Два способа — простой:

1. В TrueNasMobile: **Настройки → Push-уведомления** → укажите URL рели и токен →
   **Зарегистрировать и включить** → **Создать webhook на TrueNAS**. Готово.
2. Вручную в веб-интерфейсе TrueNAS: **Alerts → Alert Services → Add** →
   тип **Webhook**, укажите `https://your-relay/api/send` с заголовком:

```
Authorization: Bearer <token>
```

Выберите уровень **WARNING** и выше, чтобы на телефон приходили только
важные предупреждения.

## Безопасность и конфиденциальность

- Приватные ключи устройств хранятся в Android Keystore и никогда не покидают телефон.
- Релей хранит только открытые ключи и шифротекст; envelope каждого устройства
  шифруется индивидуально (на ключ конкретного устройства, а не групповой ключ).
- Тема ntfy содержит только шифротекст, так что даже `curl https://ntfy.sh/<topic>`
  ничего не раскрывает.
- Разместите релей за TLS (Caddy/nginx). `PORT` слушает все интерфейсы.

## Транспорт FCM (Firebase)

Рели содержит полноценный транспорт [HTTP v1](https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages):
он сам получает OAuth2-токены из сервисного аккаунта Firebase
(самоподписанный RS256 JWT + токен-эндпоинт) и отправляет тот же шифрованный
envelope как `data`-сообщение — релей по-прежнему не видит открытый текст.

### 1. Включение рели (серверная сторона)

1. [Консоль Firebase](https://console.firebase.google.com) → ваш проект →
   **Project settings → Service accounts** → **Generate new private key**
   (скачивается `serviceAccount.json`).
2. (Опционально) Создайте приложение в консоли Firebase, если его ещё нет.
3. Запустите релей:

```bash
export FCM_SERVICE_ACCOUNT_FILE=/path/to/serviceAccount.json
docker compose up -d --build
```

В строке health теперь должно быть `fcm -> configured`. Устройства,
регистрирующиеся с `"transport": "fcm"`, будут получать push через Firebase.

### 2. Включение клиента (Android, чек-лист)

FCM требует реальную запись приложения в Firebase, поэтому клиентская часть —
небольшая задача на потом, когда появится `google-services.json`. На это
уйдёт минут 15:

1. В консоли Firebase добавьте **Android-приложение** с именем пакета
   `com.gegaremant.truenasmobile` (applicationId, используемый сборкой), затем
   скачайте `google-services.json` в `app/`.
2. Пропишите Gradle-сторону (только после появления файла, чтобы сборки без
   него продолжали работать):
   - корневой `build.gradle.kts`: `id("com.google.gms.google-services") version "4.4.2" apply false`,
   - `app/build.gradle.kts`: `if (project.file("google-services.json").exists()) apply(plugin = "com.google.gms.google-services")` плюс `implementation(libs.firebase.messaging)`.
3. Добавьте `FirebaseMessagingService`:
   - `onNewToken`: зарегистрируйте токен в релее — используйте
     `RelayClient().register(...)` с `transport = "fcm"` и
     `pushToken = token`.
   - `onMessageReceived`: читайте `remoteMessage.data["envelope"]`, парсите как
     `PushEnvelope`, расшифруйте через `PushE2E` и отобразите через
     `PushNotificationRenderer` (тот же конвейер, что и в `PushService`).
4. Опубликуйте сборку. FCM работает только на устройствах с Google Play
   services; устройства без GMS продолжают использовать ntfy, выбрав `ntfy`
   при регистрации.

Пока шаги 1–3 не выполнены, Android-приложение полностью работает через ntfy;
транспорт `fcm` просто сообщает понятную ошибку на релеях без переменной окружения.

## Структура

```
relay/
├── Dockerfile, docker-compose.yml
└── src/main/kotlin/com/gegaremant/truenasmobile/relay/
    ├── Main.kt                  # точка входа
    ├── Config.kt                # конфигурация из env
    ├── crypto/E2E.kt            # шифрование + envelope
    ├── model/Api.kt             # DTO
    ├── store/DeviceStore.kt     # JSON-реестр устройств
    ├── transport/               # PushRouter, NtfyTransport, FcmTransport (stub)
    └── web/RelayServer.kt       # HTTP-маршруты
```