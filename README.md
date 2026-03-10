# Delivery Service

## Переменные окружения

Конфигурация приложения находится в `src/main/resources/application.properties`.

### Обязательные переменные (без значения по умолчанию)

Перед запуском приложения задайте:

| Переменная | Используется в свойстве | Назначение |
| --- | --- | --- |
| `YANDEX_MAP_API_KEY` | `yandex.map.api.key` | API-ключ геокодера Яндекса |
| `SUGGEST_MAP_KEY` | `yandex.suggest.key` | API-ключ для подсказок адресов |
| `YANDEX_SUGGEST_URL` | `yandex.suggest.url` | URL сервиса подсказок |
| `SMS_RU_API` | `sms.ru.api.key` | API-ключ SMS-провайдера |
| `CLOUDPAYMENTS_PUBLIC_ID` | `cloud.payments.public.id` | Публичный идентификатор CloudPayments |
| `CLOUDPAYMENTS_SECRET` | `cloud.payments.secret` | Секретный ключ CloudPayments |
| `TELEGRAM_TOKEN` | `telegram.token` | Токен Telegram-бота |
| `TELEGRAM_BOT_NAME` | `telegram.bot_name` | Username Telegram-бота |
| `BASE_URL` | `base_url` | Публичный базовый URL сервиса |
| `S3_ACCESS_KEY` | `s3.access-key` | Ключ доступа к S3 |
| `S3_SECRET_KEY` | `s3.secret-key` | Секретный ключ S3 |
| `S3_BUCKET` | `s3.bucket` | Имя S3 bucket |

### Опциональные переменные (есть значения по умолчанию)

При необходимости можно переопределить:

| Переменная | Значение по умолчанию |
| --- | --- |
| `JWT_SECRET_BASE64` | `Zm9vZGJveC1kZWxpdmVyeS1qd3Qtc2VjcmV0LWtleS0zMi1ieXRlcw==` |
| `DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/delivery` |
| `DATASOURCE_LOGIN` | `postgres` |
| `DATASOURCE_PASSWORD` | `postgres` |
| `SERVER_PORT` | `8080` |
| `S3_ENDPOINT` | `https://storage.yandexcloud.net` |
| `S3_REGION` | `ru-central1` |
| `S3_PATH_STYLE_ACCESS` | `true` |

### Быстрый запуск

Пример для zsh/bash:

```bash
export IMAGES_DIR=./images
export YANDEX_MAP_API_KEY=your_key
export SUGGEST_MAP_KEY=your_key
export YANDEX_SUGGEST_URL=https://suggest-maps.yandex.ru/v1/suggest
export SMS_RU_API=your_key
export CLOUDPAYMENTS_PUBLIC_ID=your_public_id
export CLOUDPAYMENTS_SECRET=your_secret
export TELEGRAM_TOKEN=your_telegram_token
export TELEGRAM_BOT_NAME=your_bot_name
export BASE_URL=http://localhost:8080
export S3_ACCESS_KEY=your_s3_access_key
export S3_SECRET_KEY=your_s3_secret_key
export S3_BUCKET=your_bucket

./gradlew bootRun
```
