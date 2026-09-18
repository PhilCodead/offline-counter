# Offline Counter 0.13

Основная кодовая база проекта переведена на Kotlin и обычную структуру Android Studio/Gradle.

## Архитектура

- `MainActivity` — стартовый экран и переход в настройки Accessibility.
- `CounterAccessibilityService` — state machine сбора, автопрокрутка, экспорт и Quick Share.
- `ScrollTracker` — границы списка по `TYPE_VIEW_SCROLLED` (`fromIndex/toIndex/itemCount`, `scrollY/maxScrollY`).
- `PersonParser` — извлечение ФИО, даты рождения, пола и статуса из Accessibility tree.
- `OverlayPanel` — Material 3 overlay с dynamic colors, ripple, haptic feedback и vector icons.
- `ExcelExporter` — локальное формирование XLSX на основе валидного шаблона.

## Сборка

Откройте каталог `android-studio` в Android Studio, дождитесь Gradle Sync и соберите `app` как обычный Android application module.

Требования проекта: JDK 17, Android SDK 35, minSdk 29.

В manifest отсутствует `android.permission.INTERNET`.
