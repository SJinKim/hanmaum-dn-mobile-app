# Kirchenweite Datenschutzvorgaben — Übernahme vom 27.10.2026

## Auftrag und Plan

Jin hat die koreanische Datenschutzerklärung von `application.hanmaum.de`
übermittelt und bestätigt, dass die kirchenweit geltenden Angaben auch für die
App gelten. Die öffentlich erreichbare Seite wurde am 27.10.2026 gegengeprüft.
Der dort genannte Geltungsbeginn ist 01.05.2020; das ist kein Geltungsbeginn der
neuen App-Erklärung.

1. Organisatorische Fakten und allgemeine Kirchenregeln aus der Quelle
   übernehmen; Herkunft und Übertragungsgrenzen dokumentieren.
2. Deutsche, koreanische und englische Arbeitsfassung gleichwertig aktualisieren.
3. Vorschau und Kontaktkarten anpassen, Quellen-/Strukturabgleich und die
   bestehenden 15 Browserfälle prüfen; im vorhandenen Draft-PR speichern.
4. Keine Veröffentlichung, rechtliche Abschlussfreigabe oder Serveränderung
   aus der Quellenübernahme ableiten.

Quelle: [Kirchliches Anmeldeportal](https://application.hanmaum.de/), dort
„프랑크푸르트 한마음교회 개인정보 처리방침“. Die Übernahme beruht auf dem vom
Betreiber gelieferten Text und seiner ausdrücklichen Anwendung auf die Kirche.
Die öffentliche Seitenprüfung bestätigt den Text, nicht die tatsächliche
Durchführung sämtlicher dort beschriebenen Prozesse.

## Übernahmematrix

| Quellabschnitt | Übernahme in die App-Arbeitsfassungen | Grenze / verbleibende Prüfung |
| --- | --- | --- |
| Einleitung | Gemeindebezeichnung 프랑크푸르트 한마음교회; ergänzende Betreiberangaben: Koreanische Kirchengemeinde Hanmaum, Rechtsform e. V., D-U-N-S® 314469658 und rechtliche Vertretung durch 이찬규 목사 | Koreanische Gesetzesverweise nicht ungeprüft übernehmen; formelle Datenschutzbeauftragtenrolle gesondert prüfen |
| Art. 1: Zwecke | Konto, Identifikation/Anmeldung, Mitgliedsverwaltung, Missbrauchsschutz, Benachrichtigungen, Bearbeitung von Anfragen/Beschwerden; Zweckbindung | Versand, Zahlungsabwicklung, Inkasso und Unter-14-Regel sind kein Nachweis entsprechender App-Funktionen |
| Art. 2: Aufbewahrung | Konto-/Profilverwaltung bis zur Kontobeendigung; erforderliche und zulässige Aufbewahrung während laufender Ermittlungen/Prüfungen bis zu deren Ende | Keine pauschale Frist für Kurse, Anwesenheit, Koordinaten, Logs, Push, Backups oder Einwilligungsnachweise ableiten |
| Art. 3: Keine Drittweitergabe | Kein pauschales Versprechen in der App-Erklärung | Anbieter-/Empfängerrollen für Firebase, Apple, Google, pCloud und Kursverwaltung sowie Drittlandverarbeitung prüfen |
| Art. 4: Rechte | Auskunft, Berichtigung, Löschung, Verarbeitungseinschränkung; Anfragen schriftlich, telefonisch oder per E-Mail; Vertretung mit geeignetem Nachweis; kirchliche Vorgabe zur Aussetzung betroffener Verarbeitung während Berichtigung/Löschung | Keine koreanische Vollmachtsformularpflicht; Umsetzung der Bearbeitungssperre in der App prüfen; Fax nicht ohne erreichbare Faxnummer zusagen |
| Art. 5: Daten | Zwecke und Datenüberschneidungen für Identität und Mitgliedschaft als Referenz | Website-Pflichtfelder, Geschlecht und Cookies nicht automatisch als App-Pflichtfelder ausgeben |
| Art. 6: Löschung | Löschung bei Zweckerreichung/Ablauf; getrennte Aufbewahrung bei fortbestehender gesetzlicher Pflicht; Freigabe durch kircheninterne Datenschutzverantwortung; sichere elektronische und physische Vernichtung als Vorgabe | Keine ungeprüfte Low-Level-Formatierung von Cloud-/SSD-Speichern zusagen; konkrete Lösch-/Backup-Implementierung prüfen |
| Art. 7: Sicherheit | Interne Vorgaben/Schulung, Zugriffsrechte und -kontrollen, Verschlüsselung/Schutzsoftware, Schutz von Aufbewahrungsorten als kirchliche organisatorische Vorgabe | Tatsächliche Umsetzung und Zuständigkeit beim Hostinganbieter gesondert prüfen, keine unbestätigte Audit-Zusage |
| Art. 8: Cookies | Keine automatische Übernahme der personalisierten Website-Cookie-Nutzung | Native App, Keycloak-Browserlogin, Anmeldeportal und statische Privacy-Seite getrennt prüfen |
| Art. 9: Datenschutzverantwortung | 이찬규, 담임목사; kircheninterne Verantwortung. Aktuelle Kontakt-/Bearbeitungsstelle 이충만 목사 (Betreiberkorrektur vom 27.10.2026). Telefon +49 69 3399 9677, Kirchenbüro | Keine formelle Bestellung als Datenschutzbeauftragter nach Art. 37–39 DSGVO ableiten; rechtliche Vertretung beruht auf separater Betreiberbestätigung |
| Art. 10: Auskunft | 이충만 목사 / Kirchenbüro als Annahme- und Bearbeitungsstelle für Auskunftsanfragen | Technische App-Kontaktadresse hanmaum.dev@gmail.com bleibt ergänzend erhalten |
| Art. 11: Externe Stellen | Beschwerde- und Unterstützungswege sind zu benennen | Koreanische Behörden/Telefonnummern nicht als zuständige deutsche oder kirchliche Aufsicht darstellen; Zuständigkeit bleibt offen |
| Art. 12: Änderungen | Historischen Stand 01.05.2020 der Quelle dokumentieren | Neues Gültigkeitsdatum und neue Versionskennung nach App-Freigabe bestimmen |

## Korrektur der Kontaktperson

Jin hat am 27.10.2026 die Kontakt- und Bearbeitungsstelle ausdrücklich auf
**이충만 목사** geändert. Diese aktuelle Angabe gilt für alle drei Arbeitsfassungen
und die Vorschau. Der historische Quelltext nennt noch 김재희 목사; diese ältere
Angabe wird nur hier zur Nachvollziehbarkeit festgehalten. Kircheninterne
Datenschutzverantwortung (이찬규), Büronummer und App-E-Mail bleiben unverändert.

## Ergänzung zur Rechtsform

Jin hat am 27.10.2026 die Rechtsform **eingetragener Verein (e. V.)** bestätigt.
Diese Angabe ist in allen drei Sprachfassungen ergänzt. Anschließend hat Jin
den Namen **Koreanische Kirchengemeinde Hanmaum** und die **D-U-N-S®-Nummer
314469658** mitgeteilt. Der Name wird genau in dieser Schreibweise übernommen;
die bestätigte Rechtsform wird gesondert ausgewiesen. Es wird kein nicht
mitgeteilter Namenszusatz angehängt. Die D-U-N-S®-Nummer ist eine zusätzliche
Organisationskennung, keine Vereinsregisternummer. Eine unabhängige
Register-/Dun-&-Bradstreet-Prüfung wird nicht behauptet. Jin hat anschließend
**이찬규 목사** als rechtliche Vertretung bestätigt. Ein bestimmtes Vorstandsamt,
eine Einzelvertretungsbefugnis oder eine formelle Datenschutzbeauftragtenrolle
wird daraus nicht zusätzlich abgeleitet.

Jin hat außerdem bestätigt, dass die Gemeinde keinem Kirchen- oder Dachverband
angehört. Dies ist in allen drei Sprachfassungen vermerkt. Die rechtliche
Einordnung des Datenschutzrechts wird dadurch nicht als abgeschlossen ausgegeben.

## Zusätzlicher technischer Abgleich

Die Server-Referenz `origin/main` nennt `application.hanmaum.de` ausdrücklich als
angebundenes Kursanmeldungssystem (`CourseApplicationApiProperties`,
`CourseApplicationService`). Das entspricht Jins Bezeichnung als eigenes
Kirchenportal. Das Hosting und seine Vertragsrollen sind damit nicht geklärt.

Die öffentliche Formularseite zeigt je nach Formular außerdem Foto-, Tauf- und
Familienfelder, darunter Namen und Alter von Kindern. Daraus wird nur abgeleitet,
dass einzelne Kursformulare solche Angaben abfragen können; nicht, dass jedes
Feld für jeden Kurs oder für die App-Registrierung verpflichtend ist. Zweck,
Notwendigkeit und Hinweise für betroffene Dritte bleiben zu prüfen.

## Noch nicht durch diese Quelle geklärt

- Anwendbares Datenschutzrecht, formelle Datenschutzbeauftragtenrolle,
  zuständige Aufsicht und Rechtsgrundlagen
  je Zweck einschließlich religiöser Angaben.
- Prod-Anbieter/Regionen und vollständige Anbieter-/Vertrags-/Drittlandangaben,
  einschließlich Kontomails, Kursportal, Backups und Logs.
- Konkrete Aufbewahrungsfristen aller weiteren Datenarten und die tatsächliche
  Löschung, Einschränkung, Backup-Rotation und technische Schutzmaßnahmen.
- Notwendigkeit einzelner Pflichtfelder, Umgang mit Angaben über Dritte,
  Alternativen bei optionalen Berechtigungen und tatsächliche Alterskontrolle.
- Verfügbarkeit und Folgen des Widerrufs, weitere automatische Verarbeitung,
  abschließende Inhaltsfreigabe, Version und Veröffentlichung aller Sprachen.

## Verifikation dieser Übernahme

Die drei Arbeitsfassungen enthalten jeweils neun Hauptabschnitte, dreizehn
Unterabschnitte, zwei Tabellen und zwanzig verbleibende redaktionelle Prüfpunkte.
Die zusätzliche Unterüberschrift betrifft die aus der Kirchenrichtlinie
übernommenen Schutzmaßnahmen. Kontakte, Quellenlinks und technische Bezeichner
wurden zwischen den Sprachen abgeglichen.

Die aktualisierte lokale Vorschau hat alle 15 Browserfälle bestanden:
Deutsch/Koreanisch/Englisch, Desktop und Mobil in Hell/Dunkel sowie 320px Mobil.
Sprachwechsel und Sprunglinks funktionieren; kein globaler horizontaler Überlauf,
keine Skripte und keine externen Ressourcenabrufe. `git diff --check` ist sauber.
Es wurden nur Dokumente und die lokale Vorschau verändert. App-Logik,
Serverkonfiguration und öffentliche Veröffentlichung sind nicht betroffen;
Gradle-App-Prüfungen wurden deshalb für diesen Dokumentationsnachtrag nicht
erneut ausgeführt. Dies ist keine rechtliche oder unabhängige PR-Abnahme.
