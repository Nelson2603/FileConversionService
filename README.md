# 📄 File Conversion Service

Микросервис для конвертации файлов в PDF, построенный на основе **Event-Driven Architecture**.

Сервис получает события из Kafka, скачивает файлы из MinIO, выполняет конвертацию в PDF и отправляет результат обратно через Kafka.

---

## 🚀 Возможности

* Конвертация текстовых файлов в PDF
* Конвертация изображений в PDF
* Обработка ZIP-архивов с объединением всех файлов в один PDF
* Асинхронная обработка через Kafka
* Идемпотентность обработки сообщений
* Хранение файлов в MinIO (S3-compatible storage)
* Отказоустойчивая событийная архитектура

---

## 📂 Поддерживаемые форматы

| Формат | Результат                            |
| ------ | ------------------------------------ |
| TXT    | PDF                                  |
| PNG    | PDF                                  |
| JPG    | PDF                                  |
| JPEG   | PDF                                  |
| ZIP    | PDF (объединение содержимого архива) |

---

# 🏗 Архитектура

## Используемые технологии

* Java 17
* Spring Boot 3.5.4
* Apache Kafka
* PostgreSQL
* MinIO
* Apache PDFBox
* Docker & Docker Compose
* Spring Data JPA
* Lombok

---

## Паттерны и подходы

### ✅ Strategy Pattern

Используется для выбора необходимого конвертера без больших `if-else` конструкций.

### ✅ Transactional Inbox Pattern

Обеспечивает идемпотентную обработку сообщений и защиту от повторной обработки событий.

### ✅ Event-Driven Architecture

Сервис взаимодействует исключительно через события Kafka и не использует REST API.

---

# 🔄 Схема работы

```text
┌────────────────────┐
│  External System   │
└─────────┬──────────┘
          │
          │ Upload file
          ▼
┌────────────────────┐
│       MinIO        │
│   input/file.txt   │
└─────────┬──────────┘
          │
          │ Send event
          ▼
┌────────────────────┐
│       Kafka        │
│ file-conversion-   │
│      requests      │
└─────────┬──────────┘
          │
          ▼
┌─────────────────────────────────┐
│    File Conversion Service      │
│                                 │
│ 1. Check idempotency            │
│ 2. Download file from MinIO     │
│ 3. Convert file to PDF          │
│ 4. Upload PDF to MinIO          │
│ 5. Send response to Kafka       │
└─────────┬───────────────────────┘
          │
          ▼
┌────────────────────┐
│       Kafka        │
│ file-conversion-   │
│     responses      │
└─────────┬──────────┘
          │
          ▼
┌────────────────────┐
│  External System   │
└────────────────────┘
```

---

# 📁 Структура проекта

```text
src/main/java/org/example/fileconversionservice
│
├── config
│   └── MinioConfig.java
│
├── converter
│   ├── FileConverter.java
│   ├── ConversionManager.java
│   ├── ImageToPdfConverter.java
│   ├── TextToPdfConverter.java
│   └── ZipToPdfConverter.java
│
├── dto
│   ├── FileConversionRequest.java
│   └── FileConversionResponse.java
│
├── entity
│   └── InboxMessage.java
│
├── exception
│   └── StorageException.java
│
├── kafka
│   └── FileConversionConsumer.java
│
├── repository
│   └── InboxRepository.java
│
├── service
│   ├── IdempotencyService.java
│   └── MinioService.java
│
└── FileConversionServiceApplication.java
```

---

# 🐳 Запуск проекта

## 1. Запуск инфраструктуры

```bash
docker-compose up -d
```

Будут подняты следующие сервисы:

| Сервис        | Порт |
| ------------- | ---- |
| PostgreSQL    | 5432 |
| Kafka         | 9092 |
| Zookeeper     | 2181 |
| MinIO API     | 9000 |
| MinIO Console | 9001 |
| Kafka UI      | 8080 |

---

## 2. Запуск приложения

Через IntelliJ IDEA:

```text
Run -> FileConversionServiceApplication
```

или через терминал:

```bash
./gradlew bootRun
```

При первом запуске приложение автоматически создаст бакет:

```text
conversion-files
```

---

# 🧪 Тестирование

## Шаг 1. Загрузить файл в MinIO

Открой:

```text
http://localhost:9001
```

Логин:

```text
minioadmin
```

Пароль:

```text
minioadmin
```

Создай папку:

```text
input
```

и загрузи туда файл:

```text
input/test.txt
```

---

## Шаг 2. Отправить сообщение в Kafka

Открой:

```text
http://localhost:8080
```

Перейди в топик:

```text
file-conversion-requests
```

Нажми:

```text
Produce Message
```

Отправь сообщение:

```json
{
  "messageId": "unique-id-123",
  "filePath": "input/test.txt"
}
```

---

## Шаг 3. Проверить результат

В логах приложения:

```text
Received message from Kafka
Downloading file from MinIO
Converting file to PDF
Uploading converted PDF
Successfully processed message
```

В MinIO появится файл:

```text
output/test.pdf
```

В топике `file-conversion-responses`:

```json
{
  "messageId": "unique-id-123",
  "filePath": "output/test.pdf"
}
```

---

# 🔁 Проверка идемпотентности

Отправьте сообщение повторно с тем же `messageId`:

```json
{
  "messageId": "unique-id-123",
  "filePath": "input/test.txt"
}
```

В логах:

```text
Duplicate message detected. Skipping processing for ID: unique-id-123
```

Повторная конвертация выполняться не будет.

---

# 📌 Основные преимущества проекта

* Event-Driven Architecture
* Асинхронная обработка файлов
* Strategy Pattern
* Transactional Inbox Pattern
* Работа с Kafka и MinIO
* Docker-инфраструктура
* Идемпотентность обработки сообщений
* Расширяемая архитектура для поддержки новых форматов файлов

---

# 👨‍💻 Автор

**Наиль**

Java Backend Developer

Стек:

`Java` `Spring Boot` `Kafka` `PostgreSQL` `Docker` `MinIO` `JPA` `PDFBox`
