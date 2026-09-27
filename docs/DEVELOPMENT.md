# Разработка

## Окружение

Для текущей ветки `main` используются:

| Компонент | Версия |
| --- | --- |
| JDK | 21 |
| Android SDK | 36.1 |
| Target SDK | 36 |
| Build Tools | 36.0.0 |
| Gradle Wrapper | 8.13 |
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.3.0 |
| Минимальный Android | 10 / API 29 |

Версии закреплены в [корневом Gradle-файле](../build.gradle.kts), [конфигурации приложения](../app/build.gradle.kts) и [Gradle Wrapper](../gradle/wrapper/gradle-wrapper.properties).

Namespace и applicationId: `app.offlinecounter`.

## Сборка

Установите JDK и компоненты Android SDK из таблицы. Укажите путь к SDK через `ANDROID_HOME` или `sdk.dir` в локальном `local.properties`. Файл с локальными путями не добавляется в Git.

Из корня проекта:

```sh
./gradlew assembleRelease
```

Неподписанный APK: `app/build/outputs/apk/release/app-release-unsigned.apk`.

На Windows используйте `gradlew.bat`. Android Studio не обязательна для сборки из командной строки.

## Проверки

```sh
./gradlew testReleaseUnitTest lintRelease assembleRelease
```

Проверки также выполняет [Android CI](../.github/workflows/android.yml). Отчёты тестов и lint находятся в `app/build/reports/`. CI также сохраняет APK, результаты тестов и R8 mapping в артефакте `offline-counter-ci`.

Release APK в артефакте не подписан. Порядок подписи описан в [подготовке релиза](RELEASE.md).

Тесты охватывают разбор записей, управление подсчётом, экспорт и поведение панели. Проверки интерфейса через Robolectric не заменяют проверку службы специальных возможностей на устройстве.

## Структура

Исходники находятся в [`app/src/main/java/app/offlinecounter`](../app/src/main/java/app/offlinecounter), тесты — в [`app/src/test/java/app/offlinecounter`](../app/src/test/java/app/offlinecounter).

| Компонент | Назначение |
| --- | --- |
| `MainActivity` | Стартовый экран и переход к настройкам службы |
| `CounterAccessibilityService` | Чтение дерева доступности, прокрутка и жизненный цикл панели |
| `PersonParser` | Преобразование текста в записи |
| `CountingController` | Возврат к началу списка, сбор записей и завершение подсчёта |
| `OverlayPanel`, `OverlayPlacement` | Элементы панели и положение окна |
| `WorkbookBuilder`, `ExcelExporter` | Формирование XLSX и сохранение файла |

`CountingController` получает снимки списка и возвращает команды. Android-взаимодействия выполняет служба. Такое разделение позволяет проверять переходы состояния и устранение повторов без запущенного приложения-источника.

## Ветки и версии

`main` содержит текущую разработку. Выпускам 1.0.1, 1.0.2 и 1.0.3 соответствуют теги `v1.0.1`, `v1.0.2` и `v1.0.3`. Для воспроизведения опубликованного APK используйте соответствующий тег и требования к окружению из его исходников.
