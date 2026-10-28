# KMP-Richtlinien

Diese Richtlinien beschreiben die bestehenden Grenzen und Muster der Mobile-App und
sind der Bezugspunkt für Feature-Designs und Reviews. Verbindliche Arbeits- und
Prüfbefehle stehen in [AGENTS.md](../../AGENTS.md); die UI-Vorgaben in
[DESIGN.md](../../designs/dn_app/DESIGN.md). Die Richtlinien führen keine neuen Module,
Abhängigkeiten oder Architekturtests ein.

Die Abschnittsüberschriften bleiben als Linkziele stabil. Ein Feature-Design verweist
zum Beispiel auf `docs/guidelines/kmp.md#mvvm-und-ui-state` und beschreibt nur seine
konkreten Ergänzungen oder genehmigten Abweichungen.

## Module und Schichten

**Ist-Stand:** [settings.gradle.kts](../../settings.gradle.kts) bindet genau ein
Gradle-Modul `:composeApp` ein. Die Features sind Pakete innerhalb dieses Moduls;
`iosApp/` ist der native Xcode-/Swift-Einstieg, kein weiteres Gradle-Modul.

Unter `composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/` gilt:

| Pfad | Verantwortung |
|---|---|
| `features/<feature>/domain/model/` | Gemeinsame Fachmodelle ohne Plattform- oder Transportdetails |
| `features/<feature>/domain/repository/` | Repository-Verträge für die Präsentationsschicht |
| `features/<feature>/data/model/` | Serialisierbare DTOs und deren Abbildung auf Fachmodelle |
| `features/<feature>/data/repository/` | Netzwerk-, Settings- und Cache-Zugriffe hinter den Verträgen |
| `features/<feature>/presentation/` | ViewModel, UI-State, Screens und Feature-Komponenten |
| `core/` | Gemeinsam genutzte Infrastruktur, Navigation, Lokalisierung, Theme und Komponenten |
| `di/` | Zusammensetzung der Implementierungen mit Koin |

Neue Arbeit folgt dieser Struktur; einzelne ältere Pakete weichen noch ab.
Präsentation hängt von Repository-Interfaces und Fachmodellen ab, die Datenschicht
implementiert die Interfaces. DTOs und ihre Abbildung bleiben in der Datenschicht.
Composables und ViewModels führen keine direkten HTTP-, Settings-, Firebase- oder
GPS-Zugriffe aus. Plattformdienste werden hinter gemeinsamen Abstraktionen injiziert.
Ein zusätzliches Gradle-Modul oder eine breite Umstrukturierung braucht ein separates,
von Jin freigegebenes Design.

## MVVM und UI-State

**Ist-Stand und Referenz:**
[BulletinViewModel.kt](../../composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/bulletin/presentation/BulletinViewModel.kt)
verwendet einen unveränderlichen `BulletinUiState`, einen privaten `MutableStateFlow`
und einen öffentlich nur lesbaren Flow über `asStateFlow()`.

- Das ViewModel besitzt Laden, Wiederholen, Auswahl und Fehlerzustände; die UI rendert
  den State und meldet Benutzeraktionen als Methodenaufrufe zurück.
- Coroutines für Bildschirmarbeit laufen in `viewModelScope`. Wiederholte Aufrufe,
  Abbruch und überholte Antworten dürfen keinen veralteten State zurückschreiben.
- Lade-, Leer-, Erfolgs- und Fehlerzustände werden explizit modelliert. Cache-Inhalt
  und dessen Alter dürfen nicht als frischer Online-Erfolg erscheinen.
- Berechnung, Sortierung und Datenaufbereitung gehören ins ViewModel oder in die
  Fach-/Datenschicht. UI-spezifische Berechnungen werden bei Bedarf mit `remember`
  gehalten; Lazy-Listen erhalten stabile Schlüssel.
- Screens verwenden `AppScreen` und die Tokens unter `core/presentation/theme/`,
  insbesondere `AppSpacing`. Abweichungen vom gemeinsamen Screen-Rahmen brauchen
  eine begründete Ausnahme gemäß DESIGN.md.

## Shared- und Plattform-Code

**Ist-Stand:** [composeApp/build.gradle.kts](../../composeApp/build.gradle.kts)
konfiguriert `androidTarget`, `iosArm64` und `iosSimulatorArm64`. Gemeinsame Logik,
Repositories, ViewModels und Compose-UI liegen in `commonMain`; gemeinsame Tests in
`commonTest`.

- `androidMain` enthält Android-APIs, Context, Play Services und Firebase-Integration.
  `iosMain` enthält die Implementierungen mit Apple-APIs und den Compose-Einstieg.
- `commonMain` importiert keine Android-, JVM- oder Apple-APIs. Plattformverhalten
  erhält einen gemeinsamen Vertrag und Implementierungen in den Plattform-Source-Sets.
- Das Repo verwendet sowohl Interfaces mit plattformspezifischen Klassen (etwa
  `GeofenceManager`) als auch `expect`/`actual` (etwa `platformModule`). Bestehende
  Abstraktionen erweitern, statt Plattformabfragen im ViewModel einzubauen.
- Betriebssystemberechtigungen und Lebenszyklus bleiben bei der Plattformintegration;
  deren Ergebnis wird als State oder Ergebnis über den gemeinsamen Vertrag weitergegeben.
- Lifecycle, Navigation Compose und Koin sind auf iOS ABI-gekoppelt. Keine isolierten
  Versionssprünge; Änderungen gemeinsam prüfen und vor riskanten Upgrades freigeben lassen.

## Navigation

**Ist-Stand:**
[Routes.kt](../../composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/navigation/Routes.kt)
definiert `@Serializable`-Routenobjekte und Datenklassen;
[App.kt](../../composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/App.kt)
verdrahtet sie mit `NavHost` und `composable<Route>`.

Neue Ziele dort typisiert ergänzen; keine freien String-Routen einführen. Argumente
enthalten die zum Auflösen nötigen IDs oder kleinen Werte, keine Token oder kompletten
Serverantworten. UI-Texte werden lokalisiert; beispielsweise enthält `LoginRoute.notice`
einen Meldungscode statt eines übersetzten Satzes.

ViewModels erhalten keinen `NavController`. Der bestehende plattformunabhängige
[NavRoute](../../composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/domain/model/NavRoute.kt)
beschreibt Auth-/Start-Navigationsabsichten, die die UI auf konkrete Routen abbildet;
Feature-Screens verwenden außerdem Navigations-Callbacks. Bei neuen Übergängen
Backstack und einmalige Verarbeitung der Absicht ausdrücklich berücksichtigen.

## Dependency Injection

**Ist-Stand:**
[AppModule.kt](../../composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/di/AppModule.kt)
registriert gemeinsame Repositories, Storage, den gemeinsamen `HttpClient` als
`single` und ViewModels mit `viewModel`. `di/PlatformModule.kt` deklariert
`expect val platformModule`; die jeweiligen `PlatformModule.android.kt` und
`PlatformModule.ios.kt` liefern die Plattformdienste.

[KoinInit.kt](../../composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/di/KoinInit.kt)
lädt beide Module. Android startet Koin in `DnChurchApp` mit `androidContext`,
iOS startet ihn im Swift-App-Einstieg über `doInitKoinIos()`.

Abhängigkeiten werden über Konstruktoren injiziert und am zentralen Zusammensetzungspunkt
registriert. Kein Service-Locator im Fachcode und keine selbst erzeugten Clients oder
Storage-Instanzen in ViewModels. Parametergebundene Detail-ViewModels folgen den
vorhandenen Koin-Definitionen. Tests können so Repositories, Zeitgeber und Dienste
mit Fakes ersetzen.

## Fehlerbehandlung und Datenzugriff

**Ist-Stand und Referenz:**
[BulletinRepositoryImpl.kt](../../composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/features/bulletin/data/repository/BulletinRepositoryImpl.kt)
trennt erfolgreiche Leere, Zugriffsverweigerung, temporäre Ausfälle und Cache-Rückgriff.

- Repository-Verträge verwenden `Result<T>` oder das vorhandene Feature-Fehlermuster.
  Erwartete Netzwerk-/Fachfehler werden an der Repository-Grenze abgebildet, statt
  unkontrolliert bis in die UI zu werfen.
- Bei breiten Exception-Handlern `CancellationException` wieder werfen, damit Abbruch
  nicht zum Fehlerzustand oder Cache-Erfolg wird.
- HTTP-Status und gegebenenfalls `ApiResponse.success`/`data` prüfen. Eine leere Antwort,
  ein abgelaufener Login und ein Verbindungsfehler sind unterschiedliche Zustände.
  Statusbedeutungen folgen dem [API-Vertrag](api.md#api-vertrag).
- Die UI zeigt lokalisierte Meldungen und sinnvolle Wiederholaktionen; keine ungefilterten
  Exception-Texte, Tokens oder personenbezogenen Serverdaten in Logs oder Fehlermeldungen.
- Ein Cache braucht Gültigkeitsdauer, Invalidierung und Benutzer-/Umgebungsgrenzen.
  Berechtigungsfehler dürfen nicht über veraltete Daten umgangen werden. Regeln wie
  Veröffentlichungsgrenzen gelten auch offline; Zeitgeber für Grenztests injizieren.
- HTTP-Client, Base-URL und Bearer-Schutz folgen [api.md](api.md#client-und-authentifizierung).

## Tests und Verifikation

**Ist-Stand:** `commonTest` verwendet `kotlin.test`, `kotlinx.coroutines.test`, Ktor
`MockEngine` und `MapSettings`. Es gibt noch keinen konfigurierten Architekturtest
und kein gemeinsames automatisiertes E2E-Gate in `.team/config.yml`.

| Ebene | Erwartete Prüfung | Bestehende Referenz unter `composeApp/src/commonTest/kotlin/com/hanmaum/dn/mobile/` |
|---|---|---|
| Fachlogik | Gültige Werte, Grenzen, ungültige Eingaben | `features/login/domain/model/BirthDateInputTest.kt` |
| ViewModel | State-Wechsel, Fehler, Retry, konkurrierende Aufrufe mit Fake-Repository und kontrolliertem Scheduler | `features/bulletin/BulletinViewModelTest.kt` |
| Repository | Methode, URL, Query, serialisierte Schlüssel, Mapping, HTTP-/Transportfehler mit `MockEngine` | `features/bulletin/BulletinRepositoryImplTest.kt` |
| Sicherheit | Auth-Hostgrenze einschließlich Challenge/Retry und Sessionwechsel | `core/network/NetworkAuthTest.kt` |

Für Verhaltensänderungen Tests ergänzen, bei Fehlern einen Regressionstest. Main-Dispatcher
in ViewModel-Tests setzen und anschließend zurücksetzen; keine echten Netzwerkzugriffe
oder Wartezeiten. Neue Nutzerflüsse brauchen zusätzlich einen nachvollziehbaren
Ende-zu-Ende-Nachweis; ein fehlendes automatisiertes Gate ist keine Abnahme.

Die verbindlichen Befehle und bedingten Gates stehen in
[AGENTS.md – Build, Test, and Verification Commands](../../AGENTS.md#build-test-and-verification-commands)
und der dortigen Definition of Done. Kleinsten relevanten Test zuerst ausführen,
danach die JVM-Tests und Lint; bei gemeinsamem/iOS-erreichbarem Code auch native Tests,
bei geänderten Swift-Aufrufen den Swift-Interop-Build. Keine flavor-mehrdeutigen
Gradle-Tasks verwenden. Nicht ausgeführte Gates und verbleibendes Risiko im PR nennen.
iOS-Laufzeit prüft Jin auf TestFlight; kein routinemäßiger App-Start im Simulator und
kein eigenständiger TestFlight-Dispatch.
