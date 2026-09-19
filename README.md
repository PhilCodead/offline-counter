<div align="center">

<img src="docs/assets/icon.svg" width="112" alt="Иконка Оффлайн-счётчика">

# Оффлайн-счётчик

**Подсчёт списков и экспорт в Excel — прямо поверх открытого приложения.**

Kerimli Systems · Android · Kotlin · Без интернета

[![Android CI](https://github.com/PhilCodead/offline-counter/actions/workflows/android.yml/badge.svg)](https://github.com/PhilCodead/offline-counter/actions/workflows/android.yml)
[![Release 1.0.1](https://img.shields.io/badge/release-1.0.1-2563eb)](https://github.com/PhilCodead/offline-counter/releases/tag/v1.0.1)
![Android 10+](https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&logoColor=white)

[**Скачать APK**](https://github.com/PhilCodead/offline-counter/releases/latest) · [История изменений](CHANGELOG.md) · [Сообщить об ошибке](https://github.com/PhilCodead/offline-counter/issues/new/choose)

</div>

## Возможности

- Плавающая панель поверх исходного приложения.
- Возврат к началу списка, автоматическая прокрутка и устранение повторов.
- Общее количество записей и распределение по полу.
- XLSX с датой, временем и количеством записей в имени.
- Открытие Excel и передача файла через системное меню.
- Светлая и тёмная темы, свободное перемещение и привязка панели вблизи края.
- Защита от закрытия при подсчёте и подтверждение очистки.

## Установка и первый запуск

1. Скачайте `Offline-Counter-1.0.1-release.apk` из [Releases](https://github.com/PhilCodead/offline-counter/releases).
2. Установите APK и откройте «Оффлайн-счётчик».
3. Нажмите «Включить Accessibility». В настройках Samsung откройте
   «Установленные приложения» → «Оффлайн-счётчик» и включите службу.
4. Вернитесь к исходному списку и нажмите «Подсчёт» на плавающей панели.

Путь в системных настройках зависит от версии Android и One UI.
Разворачиваемая инструкция доступна на стартовом экране приложения.

## Подсчёт и экспорт

Приложение ожидает поддерживаемый формат ФИО, пола и даты рождения.
Оно не предназначено для подсчёта произвольного текста или любых списков.
Повторы определяются по нормализованному имени и дате рождения.

После обнаружения данных появляются кнопки экспорта и передачи.
Excel сохраняется в `Загрузки/OfflineCounter`. Для просмотра нужен
установленный обработчик XLSX. Перед передачей сначала сохраните файл.

При недоступности списка или достижении лимита прокрутки приложение
сообщает об ошибке: частичный результат не выдаётся за полный.

## Приватность и подпись

Обработка выполняется на устройстве. Нет разрешения INTERNET,
рекламы, аналитики и передачи данных разработчику.
[Подробнее о данных и Accessibility](docs/PRIVACY.md).

Release-версии подписаны постоянным ключом Kerimli Systems.
APK и контрольная сумма доступны в Releases. Обновление 1.0.1 устанавливается
поверх release 1.0.0. Для перехода с debug-сборки сначала экспортируйте
результаты: Android не допускает обновление с другой подписью.

Подпись не гарантирует отсутствие предупреждения Play Protect.
При блокировке приложите точный текст предупреждения к сообщению об ошибке,
предварительно удалив персональные данные.

## Разработка

Минимальная версия Android — 10 (API 29).
Namespace и applicationId — `app.offlinecounter`.
Актуальные версии SDK и зависимостей закреплены в Gradle-файлах.
Для текущей ветки нужны JDK 21, Android SDK 36.1 и Build Tools 36.0.0.
Опубликованный APK 1.0.1 соответствует тегу `v1.0.1`;
обновления SDK готовятся в линии `1.0.2-dev`.

```sh
./gradlew testDebugUnitTest testReleaseUnitTest lintDebug lintRelease assembleRelease
```

| Компонент | Ответственность |
| --- | --- |
| CountingController | Последовательность и состояние подсчёта |
| PersonParser | Разбор текста списка |
| CounterAccessibilityService | Android-события, прокрутка и жизненный цикл |
| OverlayPanel / OverlayPlacement | Интерфейс и положение панели |
| ExcelExporter / WorkbookBuilder | Публикация и содержимое XLSX |

[Участие в разработке](CONTRIBUTING.md) · [Выпуск и подпись](docs/RELEASE.md) · [Безопасность](SECURITY.md)

Проект не связан с Samsung; One UI используется как ориентир оформления.
