# 🏦 Система Управления Банковскими Картами

![Java](https://img.shields.io/badge/Java-17+-blue.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue.svg)
![Docker](https://img.shields.io/badge/Docker-ready-blue.svg)

Современное REST API для управления банковскими картами с полным циклом операций: от создания заявок до переводов между картами. Система построена с использованием современных технологий и лучших практик разработки.

## 🚀 Ключевые особенности

### 🔐 Безопасность
- **JWT аутентификация** с автоматическим refresh token
- **Шифрование номеров карт** с использованием AES-256
- **Маскирование чувствительных данных** в зависимости от роли пользователя
- **Device Fingerprinting** для дополнительной защиты сессий
- **Ролевая система доступа** (USER/ADMIN)

### 🏗️ Архитектура
- **Clean Architecture** с четким разделением слоев
- **Кастомные валидационные аннотации** с фабричным паттерном
- **AOP аспекты** для автоматического шифрования/расшифровки данных
- **Spring Data JPA** с оптимизированными запросами
- **Транзакционность** для критичных операций

### 📋 Аудит и отслеживание
- **Spring Data JPA Auditing** для автоматического отслеживания изменений
- **@CreatedDate / @LastModifiedDate** для временных меток
- **@CreatedBy / @LastModifiedBy** для отслеживания авторов изменений
- **UserAuditorAwareImpl** для автоматического заполнения аудиторских полей
- **Полная история операций** с картами и транзакциями

### 📖 Автоматическая документация
- **Автоматическая генерация OpenAPI спецификации** при запуске приложения
- **OpenApiFileGenerator** создает `docs/openapi.yaml` файл
- **Swagger UI** с интерактивным интерфейсом
- **Детализированные схемы** всех DTO и endpoint'ов
- **Примеры запросов и ответов** для всех операций

### 📊 Функциональность
- Управление пользователями и картами
- Заявки на создание карт с системой одобрения
- Запросы на блокировку карт
- Переводы между собственными картами
- История транзакций с фильтрацией
- Административная панель

### 🛠️ Технологии
- **Spring Boot 3.2.0** с Java 17+
- **Spring Security** для аутентификации и авторизации
- **PostgreSQL** в качестве основной БД
- **Liquibase** для миграций
- **Docker & Docker Compose** для контейнеризации
- **Swagger/OpenAPI** для документации API
- **JUnit & Mockito** для тестирования

## 📱 API Endpoints

### 🔑 Аутентификация
```http
POST /auth/sign-up      # Регистрация
POST /auth/sign-in      # Вход в систему
POST /auth/refresh      # Обновление токена
```

### 💳 Управление картами
```http
GET    /cards/my                    # Мои карты
GET    /cards/my/active            # Активные карты
GET    /cards/{id}                 # Карта по ID
GET    /cards/all                  # Все карты (админ)
POST   /cards/{id}/unblock         # Разблокировка (админ)
```

### 📝 Заявки на карты
```http
POST   /card-applications                    # Создать заявку
GET    /card-applications/my                # Мои заявки
POST   /card-applications/{id}/cancel       # Отменить заявку
GET    /card-applications/admin/all         # Все заявки (админ)
POST   /card-applications/{id}/approve      # Одобрить (админ)
POST   /card-applications/{id}/reject       # Отклонить (админ)
```

### 🔒 Запросы на блокировку
```http
POST   /card-block-requests                 # Создать запрос
GET    /card-block-requests/my             # Мои запросы
POST   /card-block-requests/{id}/cancel    # Отменить запрос
GET    /card-block-requests/admin/all      # Все запросы (админ)
POST   /card-block-requests/{id}/approve   # Одобрить (админ)
POST   /card-block-requests/{id}/reject    # Отклонить (админ)
```

### 💸 Транзакции
```http
POST   /transactions/transfer      # Перевод между картами
GET    /transactions/my           # История транзакций
GET    /transactions/{id}         # Транзакция по ID
GET    /transactions/all          # Все транзакции (админ)
```

### 👥 Управление пользователями (Админ)
```http
GET    /users                     # Список пользователей
GET    /users/{id}               # Пользователь по ID
POST   /users                    # Создать пользователя
POST   /users/{id}/toggle-status # Заблокировать/разблокировать
DELETE /users/{id}               # Удалить пользователя
```

## 🛠️ Запуск проекта

### 📋 Предварительные требования
- Java 17+
- Maven 3.8+
- Docker & Docker Compose (для контейнерного запуска)

### 🖥️ Локальный запуск

1. **Клонирование репозитория**
```bash
git clone https://github.com/your-username/bank-cards.git
cd bank-cards
```

2. **Настройка переменных окружения**
```bash
# Создайте .env файл в корне проекта
cp .env.example .env

# Отредактируйте .env файл с вашими настройками
```

3. **Запуск PostgreSQL**
```bash
docker run --name postgres-bank \
  -e POSTGRES_DB=bank_rest \
  -e POSTGRES_USER=root \
  -e POSTGRES_PASSWORD=qwerty \
  -p 6543:5432 \
  -d postgres:16
```

4. **Установка зависимостей и сборка**
```bash
mvn clean install -DskipTests
```

5. **Применение миграций**
```bash
mvn liquibase:update
```

6. **Запуск приложения**
```bash
mvn spring-boot:run
```

### 🐳 Запуск через Docker Compose

1. **Подготовка окружения**
```bash
# Создайте .env файл
cp .env.example .env
```

2. **Запуск всех сервисов**
```bash
docker-compose --env-file [PATH_TO_ENV] up -d --build
```

3. **Просмотр логов**
```bash
docker-compose logs -f app
```

4. **Остановка сервисов**
```bash
docker-compose down
```

### 📊 Мониторинг и отладка

**Проверка состояния контейнеров:**
```bash
docker-compose ps
```

**Подключение к базе данных:**
```bash
docker exec -it bank_cards_postgres psql -U root -d bank_rest
```

**Просмотр логов приложения:**
```bash
docker-compose logs -f app
```

## 📚 Документация API

### 🚀 Автоматическая генерация документации

Система автоматически генерирует OpenAPI документацию при каждом запуске приложения:

1. **OpenApiFileGenerator** подключается к запущенному приложению
2. Получает JSON спецификацию из `/v3/api-docs`
3. Конвертирует в YAML формат
4. Сохраняет в файл `docs/openapi.yaml`

После запуска приложения документация доступна по адресам:

- **Swagger UI**: http://localhost:8080/api/v1/swagger-ui/index.html
- **OpenAPI JSON**: http://localhost:8080/api/v1/v3/api-docs
- **OpenAPI YAML**: http://localhost:8080/api/v1/v3/api-docs.yaml
- **Локальный файл**: `docs/openapi.yaml`

### 📖 Особенности документации

- **Полное описание всех endpoint'ов** с примерами запросов
- **Схемы всех DTO** с валидационными правилами
- **Описание статусов ответов** и возможных ошибок
- **Информация о безопасности** и требуемых токенах
- **Интерактивное тестирование** прямо в Swagger UI

## 🔧 Конфигурация

### Переменные окружения для локального запуска (.env файл)

```bash
# База данных
DB_URL=jdbc:postgresql://localhost:6543/bank_rest
DB_USERNAME=root
DB_PASSWORD=qwerty

# Сервер
SERVER_PORT=8080
SERVER_CONTEXT_PATH=/api/v1
SPRING_PROFILES_ACTIVE=dev

# Безопасность
JWT_SECRET=your-super-secret-jwt-key-here
ENCRYPTION_KEY=your-encryption-key-here

# Конфигурации приложения
APP_EXPIRY_DATE=3
SERVER_PORT=8080
SERVER_CONTEXT_PATH=/api/v1

# Логирование
LOG_LEVEL_ROOT=ERROR
LOG_LEVEL_APP=DEBUG
LOG_FILE_PATH=logs/app.log
```
### Переменные окружения для запуска через докер компоуз(.env файл)

```bash
# База данных
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=9999

POSTGRES_DB=bank_cards_db
POSTGRES_USER=bank_user
POSTGRES_PASSWORD=StrongProductionPassword123!
POSTGRES_PORT=4321

DB_URL=jdbc:postgresql://postgres:5432/bank_cards_db
DB_USERNAME=bank_user
DB_PASSWORD=StrongProductionPassword123!


# Безопасность
JWT_SECRET=your-super-secret-jwt-key-here
ENCRYPTION_KEY=your-encryption-key-here

# Настройки карт
APP_EXPIRY_DATE=3

# Логирование
LOG_LEVEL_ROOT=WARN
LOG_LEVEL_APP=INFO
LOG_FILE_PATH=/app/logs/app-prod.log
```

### Тестовые пользователи

После применения миграций в системе доступны тестовые аккаунты:

| Роль  | Телефон        | Пароль | Описание           |
|-------|----------------|--------|--------------------|
| ADMIN | +7(900)1234567 | qwe    | Администратор      |
| USER  | +7(900)1234568 | qwe    | Обычный пользователь |

## 🧪 Тестирование

**Запуск всех тестов:**
```bash
mvn test
```

**Запуск определенного теста:**
```bash
mvn test -Dtest=AuthenticationServiceTest
```

**Отчет о покрытии:**
```bash
mvn jacoco:report
```

## 🏆 Архитектурные решения

### 🎭 AOP аспекты
- **CardDecryptionAspect**: Автоматическое расшифровка номеров карт при чтении из БД
- **CardMaskingAspect**: Маскирование номеров карт в DTO в зависимости от роли пользователя

### 📋 Система аудита

Полноценная система аудита с использованием Spring Data JPA Auditing:

```java
@EnableJpaAuditing  // Активация аудита в AppConfig

// Автоматическое заполнение временных меток
@CreatedDate
private Instant createdAt;

@LastModifiedDate  
private Instant updatedAt;

// Автоматическое заполнение пользователя
@CreatedBy
private User createdBy;

@LastModifiedBy
private User lastModifiedBy;
```

**UserAuditorAwareImpl** автоматически определяет текущего пользователя для аудита:
- Использует `AuthenticatedUserUtil` для получения текущего пользователя
- Обрабатывает случаи анонимного доступа
- Интегрируется с Spring Security контекстом

### ✅ Кастомные валидаторы
- **@ValidCardType**: Валидация типа карты с использованием enum фабрики
- **@ValidPhoneNumber**: Проверка формата и уникальности номера телефона
- **@ValidTransactionRequest**: Комплексная валидация запроса на перевод
- **@ValidBlockRequest**: Валидация запроса на блокировку карты

### 🔄 Статусные машины
Использование enum с интерфейсом `EnumInterface` для централизованного управления статусами:

```java
public interface EnumInterface {
    String getDescription();
    
    // Фабричные методы для работы со статусами
    static <E extends Enum<E> & EnumInterface> String toDescription(Class<E> enumClass, String value);
    static <E extends Enum<E> & EnumInterface> Boolean isExists(Class<E> enumClass, String value);
    static <E extends Enum<E> & EnumInterface> String getEnumDescription(Class<E> enumClass);
}
```

Статусы в системе:
- `CardStatus`: ACTIVE, BLOCKED, EXPIRED
- `TransactionStatus`: SUCCESS, CANCELLED, FAILED, REFUNDED
- `CardRequestStatus`: PENDING, APPROVED, REJECTED, CANCELLED

### 🔐 Система безопасности
- JWT токены с автоматическим обновлением
- Device fingerprinting для защиты от несанкционированного доступа
- Шифрование чувствительных данных с AES-256
- Ролевая система доступа с аннотациями `@PreAuthorize`

### 📊 Автоматизация документации

**OpenApiFileGenerator** работает как CommandLineRunner:
1. Ожидает запуска приложения (3 секунды)
2. Делает HTTP запрос к `/v3/api-docs`
3. Конвертирует JSON в YAML
4. Сохраняет в `docs/openapi.yaml`
5. Логирует результат и инструкции для ручного получения

## 📈 Производительность

- **Оптимизированные JPA запросы** с использованием индексов
- **Пагинация** для всех списочных API
- **Ленивая загрузка** связанных сущностей
- **Connection pooling** с HikariCP
- **@BatchSize** для оптимизации N+1 проблем
- **Scheduled задачи** для обновления истекших карт

## 🤝 Вклад в проект

1. Fork репозитория
2. Создайте feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit изменения (`git commit -m 'Add some AmazingFeature'`)
4. Push в branch (`git push origin feature/AmazingFeature`)
5. Откройте Pull Request

## 🔮 Дальнейшие улучшения
### 🎯 Паттерн Strategy для обработки статусов транзакций

**Предлагаемая реализация:**
```java
// Интерфейс стратегии
public interface TransactionStatusStrategy {
    TransactionDto processTransaction(TransferRequest request, Card fromCard, Card toCard);
    boolean canHandle(TransactionStatus status);
}

// Конкретные стратегии
@Component
public class SuccessTransactionStrategy implements TransactionStatusStrategy {
    @Override
    public TransactionDto processTransaction(TransferRequest request, Card fromCard, Card toCard) {
        // Логика успешной транзакции
    }
    
    @Override
    public boolean canHandle(TransactionStatus status) {
        return status == TransactionStatus.SUCCESS;
    }
}

@Component
public class FailedTransactionStrategy implements TransactionStatusStrategy {
    @Override
    public TransactionDto processTransaction(TransferRequest request, Card fromCard, Card toCard) {
        // Логика неуспешной транзакции - только создание записи без движения средств
    }
    
    @Override
    public boolean canHandle(TransactionStatus status) {
        return status == TransactionStatus.FAILED;
    }
}

// Контекст стратегии
@Component
public class TransactionProcessor {
    private final List<TransactionStatusStrategy> strategies;
    
    public TransactionDto processTransaction(TransferRequest request, TransactionStatus status) {
        TransactionStatusStrategy strategy = strategies.stream()
            .filter(s -> s.canHandle(status))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No strategy found for status: " + status));
        
        return strategy.processTransaction(request, fromCard, toCard);
    }
}
```

**Преимущества:**
- Легко добавлять новые типы обработки транзакций
- Инкапсуляция логики для каждого статуса
- Соблюдение принципа открытости/закрытости

## 📞 Контакты

**Автор**: Abu  
**Email**: [out1of1mind1exception@gmail.com]()  
**GitHub**: [DesBasito](https://github.com/DesBasito)

---

⭐ Если проект был полезен, поставьте звездочку!