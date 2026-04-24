# Delivery Service Backend

Backend интернет-магазина на Kotlin + Spring Boot.

Проект сделан как модульный монолит с акцентом на реальную e-commerce предметную область: каталог, корзина, checkout, заказы, доставка (включая Яндекс), административная панель, медиа и интеграции.

## Что это за проект

Этот сервис решает полный цикл покупки:

- Публичное API для магазина: каталог, корзина, доставка, checkout, оформление заказа.
- Админское API: управление каталогом, зонами/тарифами доставки, статусами заказов, баннерами, юридическими документами.
- Интеграции: Yandex Geocoder, Yandex Delivery, S3-совместимое хранилище, Telegram-уведомления, FASHN Virtual Try-On.
- Поддержка гостевого и авторизованного пользователя в одном API-контуре.

На текущий момент OpenAPI содержит `95` path и `117` operations.

## Ключевой функционал

### Клиентский контур

- Аутентификация (`/api/v1/auth`): phone/email challenge flow, refresh, logout, Telegram/MAX completion.
- Каталог (`/api/v1/catalog`): категории, товары, товарные детали, варианты.
- Корзина (`/api/v1/cart`): добавление/обновление/удаление позиций, выбор доставки.
- Доставка (`/api/v1/delivery`): способы доставки, котировки, определение локации, ПВЗ Яндекс.
- Checkout (`/api/v1/checkout/options`): доступные комбинации доставки и оплаты.
- Заказы (`/api/v1/orders`): checkout для пользователя и гостя, просмотр текущих и исторических заказов.
- Оплаты (`/api/v1/payments`): список методов и создание платежной сущности.
- Виртуальная примерка (`/api/v1/virtual-try-on`): запуск сессии, синхронизация статуса, вебхук.

### Админский контур

- Админ-аутентификация (`/api/v1/admin/login|refresh|logout`).
- Управление каталогом, вариантами и модификаторами.
- Импорт каталога через CSV (`/api/v1/admin/catalog-import`), примеры в `docs/catalog-import/examples`.
- Управление доставкой: методы, зоны, тарифы, правила оплаты, ПВЗ.
- Управление заказами и workflow статусов (статусы, переходы, история).
- Hero banners и legal documents для витрины.
- Медиа-загрузка с presigned URL и post-processing изображений.
- Тестовые Telegram-уведомления из админского API.

## Архитектура

Проект организован как модульный монолит с явным разделением по слоям:

- `api` — REST-контроллеры, DTO, mapper-ы.
- `application` — use-case сервисы, команды, orchestrator-ы.
- `domain` — бизнес-модели и бизнес-правила.
- `infrastructure` — JPA entity/repository, внешние API-клиенты, адаптеры.

Базовый паттерн внутри модулей: Ports & Adapters.

- Примеры портов: `ObjectStoragePort`, `OrderPaymentPort`, `VirtualTryOnProviderGateway`.
- Примеры адаптеров: `S3ObjectStorageAdapter`, `OrderPaymentPortAdapter`, `FashnVirtualTryOnGateway`.

### Схема (упрощенно)

```text
Client / Admin UI
        |
     REST API (controllers)
        |
 Application services (use cases, transactions)
        |
  Domain models + rules + events
        |
Repositories / External gateways (ports)
        |
JPA(PostgreSQL/PostGIS), S3, Yandex APIs, Telegram, FASHN
```

### Событийная модель

- После коммита транзакции публикуются доменные события (например, `OrderCreatedEvent`, `OrderStatusChangedEvent`).
- Подписчики (например, Telegram listener) реагируют через `@TransactionalEventListener`.
- Для медиа-пайплайна используется фоновый worker (`@Scheduled`).

## Технологический стек

| Категория | Технологии |
| --- | --- |
| Язык и runtime | Kotlin 1.9, Java 17 |
| Framework | Spring Boot 3.4, Spring Web, Validation, Security, WebSocket |
| Данные | Spring Data JPA, PostgreSQL, PostGIS (`hibernate-spatial`, `jts-core`) |
| Безопасность | JWT (`jjwt`), stateless security filter chain, role-based access |
| Хранилище файлов | AWS SDK v2 S3 (S3-compatible endpoint) |
| Интеграции | Yandex Geocoder, Yandex Delivery, Telegram Bot API, FASHN API |
| Документация API | OpenAPI 3 (`openapi.yaml` + генерация public/admin спек) |
| Тесты | JUnit 5, Spring Boot Test, Spring Security Test, H2 |
| Сборка | Gradle Kotlin DSL |

## Структура проекта

```text
src/main/kotlin/ru/foodbox/delivery
├── common/                  # security, error handling, web infra
├── modules/
│   ├── auth/
│   ├── admin/auth/
│   ├── catalog/
│   ├── catalogimport/
│   ├── cart/
│   ├── checkout/
│   ├── delivery/
│   ├── orders/
│   ├── payments/
│   ├── media/
│   ├── herobanners/
│   ├── legal/
│   ├── notifications/
│   ├── user/
│   └── virtualtryon/
└── DeliveryApplication.kt

docs/
├── db/                      # SQL-скрипты изменения схемы
└── catalog-import/examples/ # Примеры CSV для импорта
```

## API и документация

- Основная спецификация: `openapi.yaml`.
- Генерация спеков:
  - `./gradlew generatePublicOpenApi`
  - `./gradlew generateAdminOpenApi`
  - `./gradlew generateOpenApiSpecs`
- Результат генерации: `build/openapi/`.

## Локальный запуск

### Требования

- JDK 17
- PostgreSQL 14+ с расширением PostGIS
- Gradle wrapper (`./gradlew`)

### 1. Поднять БД

Пример через Docker:

```bash
docker run --name delivery-postgres \
  -e POSTGRES_DB=delivery \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  -d postgis/postgis:16-3.4
```

### 2. Настроить переменные окружения

- Список параметров: `src/main/resources/application.properties`.
- Черновой пример: `.env.example`.
- Для локальной разработки можно запускать только с базовым набором и выключать внешние интеграции через флаги `*_ENABLED=false`.

Минимальный пример:

```bash
export DATASOURCE_URL=jdbc:postgresql://localhost:5432/delivery
export DATASOURCE_LOGIN=postgres
export DATASOURCE_PASSWORD=postgres
export JWT_SECRET_BASE64=Zm9vZGJveC1kZWxpdmVyeS1qd3Qtc2VjcmV0LWtleS0zMi1ieXRlcw==
export ADMIN_BOOTSTRAP_LOGIN=admin
export ADMIN_BOOTSTRAP_PASSWORD=password
export SERVER_PORT=8080
```

### 3. Запустить приложение

```bash
./gradlew bootRun
```

Если запускаете на пустой PostgreSQL-базе, сначала нужно создать схему. По умолчанию приложение
использует `JPA_DDL_AUTO=validate`, то есть Hibernate только проверяет таблицы и не создаёт их.
Для первого запуска на новом окружении можно временно выставить:

```bash
export JPA_DDL_AUTO=update
```

После успешного запуска и создания таблиц верните значение к `validate`.

### 4. Прогнать тесты

```bash
./gradlew test
```

## Конфигурация окружения

Ниже ключевые переменные, которые обычно важны при деплое:

### Core

- `DATASOURCE_URL`, `DATASOURCE_LOGIN`, `DATASOURCE_PASSWORD`
- `JWT_SECRET_BASE64`
- `ADMIN_BOOTSTRAP_LOGIN`, `ADMIN_BOOTSTRAP_PASSWORD`
- `BASE_URL`, `SERVER_PORT`

### Delivery/Yandex

- `YANDEX_GEOCODER_ENABLED`, `YANDEX_GEOCODER_API_KEY`
- `YANDEX_DELIVERY_ENABLED`, `YANDEX_DELIVERY_TOKEN`, `YANDEX_DELIVERY_SOURCE_STATION_ID`
- `SUGGEST_MAP_KEY`, `YANDEX_SUGGEST_URL`

### Storage/Media

- `S3_ENDPOINT`, `S3_REGION`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`, `S3_BUCKET`
- `MEDIA_UPLOAD_*`
- `MEDIA_PROCESSING_*`

### Notifications

- `TELEGRAM_ENABLED`, `TELEGRAM_TOKEN`, `TELEGRAM_BOT_NAME`, `TELEGRAM_DEFAULT_CHAT_IDS`

### Virtual Try-On

- `VIRTUAL_TRY_ON_FASHN_ENABLED`
- `FASHN_API_KEY`, `FASHN_API_BASE_URL`
- `VIRTUAL_TRY_ON_WEBHOOK_BASE_URL`, `VIRTUAL_TRY_ON_WEBHOOK_SECRET`

## Качество и инженерные практики

- Разделение предметной области на 16 модулей.
- Тесты: `35` test-файлов (unit + integration).
- Транзакционная публикация доменных событий.
- Централизованный обработчик ошибок и trace-id filter.
- SQL-скрипты эволюции БД в `docs/db`.

## Дорожная карта развития

### Ближайший этап

- [ ] Подключить production-реализации auth-провайдеров вместо stub (SMS/email/phone call, внешние OAuth/IDP).
- [ ] Добавить полноценный online payment processor + webhook flow (CloudPayments и аналоги).
- [ ] Вынести миграции БД на Flyway/Liquibase и включить автопрогон при старте.

### Следующий этап

- [ ] Ввести cache-слой (Redis) для горячих read-path (каталог, delivery quote presets).
- [ ] Улучшить observability: метрики, structured logging, трассировка интеграций.
- [ ] Добавить Docker Compose для полного локального стенда (API + DB + supporting services).

### Продуктовый этап

- [ ] Расширить промо-механику (скидки, купоны, dynamic pricing rules).
- [ ] Добавить SLA-мониторинг внешних интеграций (Yandex/FASHN/S3) и fallback-стратегии.
- [ ] Подготовить CI/CD pipeline с quality gates (tests + static analysis + contract checks).

## Что этот проект показывает в профиле backend-разработчика

- Умение моделировать бизнес-процессы e-commerce end-to-end.
- Проектирование модульного монолита с clean boundaries.
- Работа с интеграциями и отказоустойчивыми сценариями.
- Практика с геоданными (PostGIS), асинхронными процессами и real-time обновлениями (WebSocket/STOMP).
- Инженерная дисциплина: API-контракты, тесты, декомпозиция на модули и слои.
