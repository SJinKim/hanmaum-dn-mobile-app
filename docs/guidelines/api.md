# API-Richtlinien

Diese Abschnitte sind stabile Linkziele für Feature-Designs und Reviews. Die
Schichtengrenzen und Teststrategie stehen in [kmp.md](kmp.md).

## API-Vertrag

Backend-Aufrufe der Mobile-App dürfen nur Endpunkte verwenden, die im zentralen
Vertrag [`hanmaum-dn-ops/api/openapi.yaml`](https://github.com/SJinKim/hanmaum-dn-ops/blob/main/api/openapi.yaml)
stehen. Vor einer Implementierung Methode, Pfad, Parameter, Request-/Response-Schemas,
Authentifizierung und Statuscodes dort prüfen. Das gilt auch bei Änderungen bestehender
Aufrufe; ein erreichbarer Server-Endpunkt allein ersetzt keinen Vertrag.

Fehlt ein benötigter Endpunkt oder widerspricht der Vertrag dem Server, ist der
Vertrag veraltet: Abweichung im Ticket mit konkreter Operation dokumentieren und die
Aktualisierung im Ops-/Server-Kontext klären, bevor der neue Aufruf implementiert wird.
Keine Pfade oder Felder aus UI-Wünschen ableiten und keinen undokumentierten Ersatzaufruf
bauen. Ein neuer Serververtrag oder eine Schemaänderung wird nicht beiläufig Teil einer
Mobile-Story. Bestehende Abweichungen gezielt nachverfolgen, statt sie als Muster zu kopieren.

Der Vertrag beschreibt den Hanmaum-Backend-Service. Die bereits vorhandenen direkten
Integrationen mit Keycloak, Google Calendar und pCloud sind externe APIs und folgen
ihrem jeweiligen Vertrag; das ist keine Freigabe für neue Direktintegrationen.

## Client und Authentifizierung

Backend-Repositories verwenden den injizierten gemeinsamen `HttpClient` aus
[`NetworkClient.kt`](../../composeApp/src/commonMain/kotlin/com/hanmaum/dn/mobile/core/network/NetworkClient.kt).
Relative Pfade werden dort mit `BuildKonfig.BACKEND_URL` und `/api/v1/` ergänzt;
Feature-Code dupliziert weder Präfix noch Umgebungs-URLs. Keycloak-/externe Aufrufe
verwenden absolute URLs und dürfen keine Backend-Base-Path-Injektion erhalten.

Bearer-Tokens dürfen ausschließlich an den konfigurierten Backend-Host und nicht an
Registrierungs-/OpenID-Connect-Pfade gehen. Der gemeinsame Client schützt sowohl den
initialen Aufruf als auch Challenges und Wiederholungen; diese Schutzlogik nicht im
Feature umgehen. Externe Hosts dürfen niemals Keycloak-Bearer erhalten. Tokenzugriffe
laufen über `TokenStorage`, nicht über direkte Settings-Zugriffe in der Präsentation.
Ein isolierter zusätzlicher Client braucht eine explizite Begründung und Prüfung des
Auth-Leak-Risikos. Secrets und Tokens gehören weder in Code noch in Logs oder PR-Beispiele.

## DTOs und Vertragstests

Transporttypen liegen in `data/model`, Mapping und HTTP-Fehlerbehandlung in
`data/repository`. JSON-Schlüssel exakt aus dem Vertrag übernehmen; bei Unklarheit
zusätzlich den passenden Server-DTO prüfen und den Vertragswiderspruch klären.
Keine globale Umbenennungsstrategie vermuten. Bei partiellen Updates unterscheiden,
ob ein ausgelassenes Feld, `null` und ein leerer Wert unterschiedliche Bedeutung haben.

Repository-Tests mit Ktor `MockEngine` sichern Methode, aufgelöste URL, Parameter,
exakte serialisierte Request-Schlüssel und relevante Antwort-/Fehlerfälle ab.
Optionalität und Response-Hüllen gemäß Schema behandeln; keine erfolgreiche Antwort
allein aus einem deserialisierbaren Body ableiten. Abbruch und Fehlerabbildung folgen
[kmp.md – Fehlerbehandlung und Datenzugriff](kmp.md#fehlerbehandlung-und-datenzugriff).
