# 🌐 DevBrowser — Mobile Chrome DevTools for Android

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**DevBrowser** — это мощный мобильный веб-браузер для Android со встроенной полноценной панелью разработчика **Chrome DevTools**. Создан для веб-разработчиков, тестировщиков и дизайнеров, которым необходим быстрый отладчик сайтов прямо на смартфоне или планшете.

---

## 🚀 Основные возможности

### 🔍 Inspector & Elements (Инспекция элементов)
- **Select Element on Page**: Инспекция любого HTML-элемента на странице нажатием пальца с точностью до отдельных тегов (`<tr>`, `<td>`, `button`, `div` и др.).
- **Интерактивное DOM-дерево**: Просмотр полной иерархии DOM (поддержка глубокой вложенности до 30+ уровней).
- **Подсветка инспектируемого элемента**: Выбранный на странице элемент автоматически подсвечивается рамочкой и акцентным фоном прямо в DOM-дереве с раскрытием всех родительских узлов.
- **Быстрое копирование**: Копирование тегов, `outerHTML`, `innerHTML` и CSS-селекторов в буфер обмена.

### 💬 Console (Консоль JavaScript)
- **Перехват логов**: Отображение всех типов логов страницы (`console.log`, `info`, `warn`, `error`).
- **Выполнение JavaScript**: Выполнение произвольных JS-скриптов в контексте текущей страницы в реальном времени.
- **Фильтрация и поиск**: Удобный фильтр логов по уровню предупреждений и поисковому запросу.

### 🌐 Network (Сетевой монитор)
- **Перехват HTTP/HTTPS запросов**: Логирование всех Fetch, XHR, скриптов, стилей и изображений.
- **Детальный разбор запросов**: Просмотр заголовков (Headers), статусов ответов, времени выполнения и тела запроса/ответа (JSON, HTML, Text).
- **Фильтрация**: Сортировка запросов по типам (`XHR/Fetch`, `JS`, `CSS`, `Img`, `Other`).

### 💾 Storage (Хранилище)
- **Cookies**: Просмотр и редактирование куки текущего домена.
- **LocalStorage & SessionStorage**: Инспекция ключей и значений локального хранилища веб-приложения.

### 📄 Sources (Исходный код)
- **Просмотр ресурсов**: Доступ к исходному коду загруженной страницы, встроенным скриптам и подключенным файлам.

### 📱 Device Emulation & Settings
- **Переключение User-Agent**: Эмуляция различных устройств (Desktop Chrome, Safari, Mobile Chrome, Custom User-Agent).
- **Настройки WebView**: Переключение JavaScript, режима инкогнито, темной темы и очистка кэша/куки.

### 📑 Функции браузера
- **Вкладки**: Полноценная многовкладочная система.
- **Закладки и история**: Сохранение избранных сайтов и история посещений.
- **Адресная строка**: Умный поиск и ввод URL-адресов.

---

## 🛠 Технологический стек

- **Язык**: Kotlin
- **UI Фреймворк**: Jetpack Compose (Material Design 3)
- **Архитектура**: MVVM (Model-View-ViewModel) + StateFlow
- **Компонент браузера**: Android WebKit WebView + JS Injections
- **Сборка**: Gradle (Kotlin DSL), Android Gradle Plugin (AGP)
- **CI/CD**: GitHub Actions (автоматическая сборка релизных APK и подпись keystore)

---

## 📦 Сборка и установка

### Предварительные требования
- Android Studio Ladybug / Jellyfish (или новее)
- JDK 17
- Android SDK 24+ (Android 7.0+)

### Инструкция по локальной сборке

1. Склонируйте репозиторий:
   ```bash
   git clone https://github.com/Fazer000/DevTools-Browser.git
   cd DevTools-Browser
   ```

2. Откройте проект в **Android Studio**.

3. Соберите и запустите проект на устройстве или эмуляторе:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 🔐 Подпись APK & CI/CD (GitHub Actions)

В проект встроен готовый Workflow для автоматической сборки релизных APK при каждом `push` в ветку `main` или `master`.

### Настройка единого Keystore в GitHub Secrets:
Если вы хотите собирать подписанные APK через GitHub Actions:

1. Зайдите в ваш репозиторий на GitHub: **Settings ➔ Secrets and variables ➔ Actions**.
2. Нажмите **New repository secret** и добавьте следующие секреты:
   - `RELEASE_KEYSTORE_BASE64` — base64-строка файла ключа (`release.keystore.base64`).
   - `RELEASE_STORE_PASSWORD` — `release_password`
   - `RELEASE_KEY_ALIAS` — `upload`
   - `RELEASE_KEY_PASSWORD` — `release_password`

3. При запуске Workflow `.github/workflows/release.yml` GitHub автоматически соберет и опубликует готовый `app-release.apk` в разделе **Releases**.

---

## 📄 Лицензия

Проект распространяется под лицензией [MIT](LICENSE).
