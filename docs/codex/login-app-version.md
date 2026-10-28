# App-Version im Login

Auftrag vom 28.10.2026: Die installierte App-Version im Format `x.y.z` unten
im Login anzeigen. Sie ist unabhängig von der Versionskennung einer
Datenschutzerklärung.

## Umsetzung

- `LoginScreen.kt` zeigt eine feste, zentrierte Versionszeile unter dem
  scrollbar bleibenden Login-Inhalt. DN-Typografie, Textfarbe und Abstände
  werden für beide Farbmodi wiederverwendet; keine zusätzlichen Screen-Insets.
- `core/platform/AppVersion.kt` stellt den gemeinsamen Plattformadapter und
  die Formatierung bereit. Die Android-Implementierung liest
  `BuildConfig.VERSION_NAME`, iOS `CFBundleShortVersionString` aus dem
  tatsächlich installierten Bundle. Kein Netzwerkabruf und kein fest
  eingetragener Releasewert in der UI.
- Eine zweistellige lokale Apple-Version wird mit Patch `0` dargestellt;
  Vorab-/Buildzusätze werden für die gewünschte numerische Releasezeile
  entfernt. Fehlende oder ungültige Metadaten erzeugen keine erfundene Version.
- `AppStrings.kt` liefert `Version …` und `버전 …`.
  `AppVersionTest.kt` prüft Releaseformate, ungültige Metadaten und beide Sprachen.

## Verifikation

Am 28.10.2026 gegen den finalen Code geprüft:

| Befehl | Ergebnis |
| --- | --- |
| `./gradlew :composeApp:testDevDebugUnitTest --tests "com.hanmaum.dn.mobile.core.platform.AppVersionTest"` | 3 Tests bestanden |
| `./gradlew :composeApp:testDevDebugUnitTest` | 708 Tests, 0 Fehler/Fehlschläge/übersprungene Tests |
| `./gradlew lint` | 0 Fehler im XML-Bericht |
| `./gradlew :composeApp:assembleDevDebug` | BUILD SUCCESSFUL |
| `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer ./gradlew :composeApp:iosSimulatorArm64Test` | 703 Tests, 0 Fehler/Fehlschläge/übersprungene Tests; BUILD SUCCESSFUL |

Der unreferenzierte TODO-Check und `git diff --check` sind sauber. Ein beim
ersten Lint-Lauf gemeldeter nullable `remember`-Rückgabetyp wurde durch explizite
Typargumente behoben, ohne Unterdrückung oder Baseline. Der anschließende
vollständige JVM-/Lint-/Android-/iOS-Lauf bestand.

Es wurde keine von Swift aufgerufene Kotlin-Deklaration verändert; Swift ruft
weiterhin nur den unveränderten `MainViewController` auf. Deshalb ist das
bedingte zusätzliche Swift-Interop-Build für diese Änderung nicht erforderlich.
Ein Simulator wurde nicht gestartet. Die tatsächliche Darstellung auf Geräten
einschließlich großer Schrift, kleiner Bildschirmhöhe und Hell/Dunkel bleibt
in TestFlight durch den Betreiber zu prüfen; diese Dokumentation behauptet
keine visuelle Geräteabnahme oder bereits erfolgte Auslieferung.
