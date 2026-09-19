# Выпуск APK

Пакет и namespace: `app.offlinecounter`. Минимальная версия Android: 10.

## Проверки и сборка

Для текущей ветки требуются JDK 21, Android SDK 36.1 и Build Tools 36.0.0.
Для воспроизведения старого выпуска используйте соответствующий git-тег.

```bash
./gradlew clean testDebugUnitTest testReleaseUnitTest lintDebug lintRelease assembleRelease
```

CI сохраняет неподписанный release APK, отчёты, R8 mapping и инструменты
подписи. Приватных ключей в CI нет. Release использует R8, сокращение
ресурсов и отключённую отладку.

## Подпись

Используйте постоянный ключ из приватной резервной копии.
Не создавайте новый ключ для очередного обновления.

```bash
export APKSIGNER_JAR="$ANDROID_HOME/build-tools/36.0.0/lib/apksigner.jar"
bash scripts/sign-release.sh \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  Offline-Counter-release.apk \
  /absolute/path/to/signing-private
```

Каталог ключа содержит `offline-counter.p12` и `keystore.password`.
Храните две защищённые резервные копии вне репозитория. Не прикладывайте
ключ или пароль к публичному релизу. Публичный сертификат можно распространять.
Увеличивайте versionCode, проверяйте APK через apksigner, сверяйте отпечаток
сертификата и сохраняйте mapping именно этой сборки. Не изменяйте APK после подписи.

Первый release подписан другим ключом, чем предыдущие debug-сборки.
Android не допускает обновление с несовместимой подписью.
Перед переходом экспортируйте нужные результаты и сохраните файлы вне приложения.
Последующие release-обновления используют один и тот же ключ.

## Проверка на Samsung перед публичным распространением

- Подсчитать один список пять раз и сверить результат.
- Проверить обе темы, сворачивание и раскрытие панели у обоих краёв.
- Проверить очистку, скрытие действий и блокировку закрытия во время подсчёта.
- Сохранить, открыть и передать XLSX, проверить сообщения без приложения Excel.
- Проверить установку с включённым Play Protect и обновление предыдущего release.

Автотесты не заменяют эти проверки на устройстве с One UI.

## Play Protect

Подпись подтверждает автора обновлений, но не гарантирует одобрение Play Protect.
Сервис проверяет приложения независимо от источника установки.
При ложном срабатывании подавайте апелляцию для конкретного подписанного APK:
https://support.google.com/googleplay/android-developer/contact/protectappeals

Документация:
https://developer.android.com/studio/publish/app-signing
https://developers.google.com/android/play-protect

Для распространения вне Google Play доступна отдельная верификация разработчика:
https://developer.android.com/developer-verification
Это отдельная процедура, не замена проверке безопасности APK.
