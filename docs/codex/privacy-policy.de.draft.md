# Datenschutzerklärung für die Hanmaum-DN-App

**Arbeitsentwurf, zuletzt bearbeitet am 27. Oktober 2026 — nicht zur Veröffentlichung freigegeben.**

Dieser Text ist auf den geprüften App-Code zugeschnitten. Angaben in
**[OFFEN: …]** müssen vor Veröffentlichung ergänzt und die Rechtsgrundlagen durch
die verantwortliche Gemeinde geprüft werden. Funktionen aus Issue #290 sind als
geplant gekennzeichnet. Die Erklärung selbst ist keine Einwilligungserklärung.

## 1. Wer ist verantwortlich?

프랑크푸르트 한마음교회 (한마음교회)<br>
Leonhard-Heißwolf-Str. 5<br>
65936 Frankfurt am Main<br>
Deutschland

Kircheninterne Datenschutzverantwortung: **이찬규**, leitender Pastor (담임목사).
Kontakt- und Bearbeitungsstelle für Datenschutzanfragen, Beschwerden und
Auskunftsersuchen: **이충만 목사**, Pastor.

Telefon: **+49 69 3399 9677** (Kirchenbüro).
App-bezogener Datenschutzkontakt: **hanmaum.dev@gmail.com** (Dev-Team der Gemeinde).

**[OFFEN: Rechtsform, rechtlichen Träger und vertretungsberechtigte Person
bestätigen. Die kircheninternen Zuständigkeiten stammen aus der vom Betreiber
übermittelten Kirchenrichtlinie. Sie bestätigen weder eine rechtliche
Vertretungsbefugnis noch eine formelle Bestellung als Datenschutzbeauftragter
nach dem anwendbaren Recht. Prüfen, ob eine solche Bestellung vorliegt oder
erforderlich ist; gegebenenfalls den gesonderten Kontakt ergänzen.]**

## 2. Für welche Nutzung gilt diese Erklärung?

Die App unterstützt die Organisation und Information unserer Gemeinde. Dazu
gehören das persönliche Mitgliedskonto, Gemeindemitteilungen, Termine, Alben,
Anwesenheitsmeldungen sowie Anmeldungen zu Veranstaltungen und Kursen.

Die kirchenweiten Vorgaben sehen eine Verarbeitung nur für die angegebenen
Zwecke vor. Bei einer Zweckänderung werden die erforderlichen
Datenschutzmaßnahmen geprüft und, soweit erforderlich, eine neue Einwilligung
eingeholt. Zur Bearbeitung von Anfragen und Beschwerden gehören die Prüfung der
Identität und des Anliegens, notwendige Rückfragen sowie die Mitteilung des
Ergebnisses.

Die folgenden Angaben betreffen die Android- und iOS-App und die zugehörigen
Gemeindedienste. Beim Öffnen externer Angebote werden Daten außerdem durch deren
Betreiber verarbeitet. Der Besuch der Website, auf der diese Erklärung später
steht, benötigt zusätzlich passende Angaben zum Websitebetrieb.

**[OFFEN: Prüfen, ob für den Träger die DSGVO oder kirchliches Datenschutzrecht
gilt. Die nachfolgenden Verweise verwenden vorläufig die DSGVO. Eine kirchliche
Zugehörigkeit allein klärt den Anwendungsbereich nicht. Bei abweichendem Recht
sind Rechtsgrundlagen, Rechte und Aufsicht einheitlich anzupassen.]**

## 3. Welche Daten verarbeiten wir wofür?

### Registrierung, Anmeldung und Mitgliedskonto

Bei der Registrierung werden Vor- und Nachname, E-Mail-Adresse, Passwort,
Geburtsdatum, Wohnort und ein Statusmerkmal zur persönlichen Lebenssituation
(`identityStatus`) an den Gemeindeserver übermittelt. Das Formular kann weitere
Angaben wie Telefonnummer und Anschrift erfassen. Weitere Profilangaben umfassen
gegebenenfalls Schule oder Arbeitgeber und eine Profilbild-Adresse.

Diese Angaben dienen der Einrichtung und Zuordnung des Kontos, der
Identifikation und Anmeldung, der Mitgliederverwaltung, dem Schutz vor
Missbrauch, notwendigen Benachrichtigungen und der Bereitstellung der jeweiligen
App-Funktionen. Der Zugang kann eine Freigabe durch die Gemeinde voraussetzen.

Die Anmeldung erfolgt über unseren Keycloak-Anmeldedienst im Systembrowser. Bei
der App-Registrierung wird das eingegebene Passwort zunächst an den
Registrierungsdienst übertragen; der Browser-Login ist davon zu unterscheiden.
Nach der Anmeldung nutzt die App Zugangstokens. Die Funktion „angemeldet bleiben“
kann eine erneute Passworteingabe vermeiden.

**[OFFEN: Pflichtfelder des veröffentlichten Formulars mit Server und Version
abgleichen; Notwendigkeit insbesondere von Geburtsdatum und Statusmerkmal
begründen. Nicht notwendige Angaben freiwillig gestalten. Hosting und Betrieb
von Keycloak sowie E-Mail-Versand für Kontovorgänge ergänzen.]**

### Gemeindedaten und religiöse Bezüge

Zum Konto können Gemeindegruppe, Bereich, Dienste, Kurszugehörigkeit sowie
Anwesenheits- und Teilnahmedaten gehören. Kursformulare können Angaben zu Taufe,
bisherigen Kursen und weiteren für den jeweiligen Kurs abgefragten Umständen
enthalten. Solche Angaben können religiöse Überzeugungen erkennen lassen und
benötigen daher eine gesondert geprüfte Grundlage für besondere Datenkategorien.

**[OFFEN: Pro Zweck sowohl die Grundlage nach Art. 6 als auch eine einschlägige
Ausnahme nach Art. 9 Abs. 2 DSGVO festlegen. Ausdrückliche Einwilligung ist eine
mögliche Grundlage; Art. 9 Abs. 2 Buchst. d kann bei Vorliegen seiner
Voraussetzungen für bestimmte interne religiöse Tätigkeiten einschlägig sein.
Dies ist keine pauschale Erlaubnis für beliebige Weitergaben oder Funktionen.]**

### Anwesenheit und Standort

Beim aktiven Check-in kann die App den aktuellen Breiten- und Längengrad sowie
die ermittelte Standortgenauigkeit an den Gemeindeserver senden. Dies dient der
Prüfung, ob die Anwesenheitsmeldung am vorgesehenen Ort erfolgen kann. Der Server
liefert außerdem Ihre Anwesenheitshistorie und Zusammenfassungen an die App.

Davon getrennt kann die App mit einer Standortberechtigung das Betreten des
Kirchenbereichs erkennen und eine lokale Erinnerung anzeigen. Die geprüfte
Geofence-Logik registriert dafür einen Bereich beim Betriebssystem; sie sendet
selbst keine fortlaufende Standortspur an den Gemeindeserver.

Die Standortfreigabe lässt sich in den App- beziehungsweise Geräteeinstellungen
ändern. Ohne Berechtigung stehen die betreffenden ortsabhängigen Funktionen
gegebenenfalls nicht zur Verfügung.

**[OFFEN: Produktionsseitige Speicherung, Protokollierung und Löschung der beim
Check-in übertragenen Koordinaten prüfen. Keine Aussage „Standort bleibt nur auf
dem Gerät“ veröffentlichen. Rechtsgrundlage, notwendiger Berechtigungsumfang und
eine Alternative zur Standortprüfung festlegen.]**

### Veranstaltungen, Dienste und Kursanmeldungen

Wir verarbeiten Ihre Auswahl zur Veranstaltungsteilnahme, Zuordnungen zu Diensten
und die Angaben, die Sie für einen Kurs einreichen. Bei Kursen können neben
Kontaktdaten beispielsweise Taufinformationen, Kursvorgeschichte, Fotos und
Freitext anfallen. Einzelne Formulare können auch Familienangaben wie Namen und
Alter von Kindern abfragen. Die konkreten Felder sind im jeweiligen Formular
erkennbar.

Kursanmeldungen gehen an den Gemeindeserver und können an das angebundene
Kursverwaltungsportal der Gemeinde unter application.hanmaum.de weitergegeben
werden.

**[OFFEN: Hosting und Vertragsrollen des kirchlichen Kursportals, konkrete
Empfänger, erforderliche Felder, Zweck und Aufbewahrung ergänzen. Die technische
Anbindung an application.hanmaum.de ist im Servercode erkennbar; sie bestätigt
keinen Verarbeitungsort. Angaben über Kinder oder andere Personen nur erheben,
soweit erforderlich; Hinweise für diese Personen und Minderjährige ergänzen.
Eine eigene Kurseinwilligung aus #290 ist noch nicht angebunden.]**

### Push-Mitteilungen

Die App verwendet Firebase Cloud Messaging; unter iOS wird zusätzlich Apple Push
Notification service verwendet. Dafür werden technische Installations- und
Push-Kennungen sowie Zustellinformationen verarbeitet. Push-Kennungen können
beim Gemeindeserver mit dem angemeldeten Konto verbunden werden, um Nachrichten
zuzustellen. Nachrichten können je nach Geräteeinstellung auf dem Sperrbildschirm
erscheinen.

Sie können die Anzeige von Push-Mitteilungen in den Geräteeinstellungen und die
angebotenen Benachrichtigungen in der App steuern. Das Ausschalten der Anzeige
bedeutet nicht automatisch, dass eine technische Installationskennung gelöscht
oder jede Kommunikation des SDKs beendet wird. Im geprüften iOS-Code wird
Firebase beim App-Start initialisiert, sofern die Konfiguration vorhanden ist.

**[OFFEN: Rechtsgrundlage, Initialisierung vor einer Auswahl, konkrete
Nachrichteninhalte, Token-Löschung und Anbieter-Verträge prüfen. Nicht
versprechen, dass Firebase erst nach einer Push-Zustimmung startet.]**

### Kalender, Alben und externe Inhalte

Der Gemeindekalender wird direkt über Google Calendar abgerufen. Beim Aufruf
werden technisch insbesondere Ihre IP-Adresse und die für den Kalenderabruf
benötigten Anfrageinformationen an Google übertragen. Es handelt sich um den
öffentlichen Gemeindekalender, nicht um einen Zugriff auf Ihren persönlichen
Google-Kalender.

Die geprüfte Albumfunktion ruft Verzeichnisse und Bilder direkt über pCloud ab.
Dabei erhält der jeweilige Auslieferungsdienst technisch unter anderem Ihre
IP-Adresse und Informationen zum angefragten Inhalt. Ein sichtbares Album ist
nicht automatisch ein nur für Mitglieder geschütztes Album; Zugriffsregeln und
Freigabelinks müssen unabhängig davon geprüft werden.

**[OFFEN: Anbieter und Regionen von Google, pCloud und den tatsächlich genutzten
Bild-/Mediendiensten bestätigen. Profilbild-URLs und externe Links gesondert
prüfen. Noch geplanten S3-Upload nicht als bereits aktive Funktion beschreiben.
Rechtsgrundlagen und gegebenenfalls eine vorgeschaltete Auswahl festlegen.]**

### Gerätespeicher und biometrische Anmeldung

Die App speichert Einstellungen, zum Beispiel Sprache, Darstellung und
Berechtigungspräferenzen, sowie je nach Funktion zwischengespeicherte Inhalte.
Zugangstokens werden im geschützten Speicher des jeweiligen Betriebssystems
abgelegt.

Bei aktivierter biometrischer Anmeldung gibt das Betriebssystem ein geschütztes
Anmeldetoken nach erfolgreicher Prüfung frei. Die App erhält dabei keine
Gesichtsaufnahme und keinen Fingerabdruck aus der biometrischen Prüfung.

**[OFFEN: Cache-Löschung bei Kontowechsel, Logout und Deinstallation sowie
Geräte-/Cloud-Backups prüfen. Logout nicht mit vollständiger Datenlöschung
gleichsetzen. Erforderlichkeit von Speicherzugriffen nach § 25 TDDDG prüfen.]**

### Technischer Betrieb und Kontakt

Beim Zugriff auf Server und Dienste fallen technisch Verbindungsdaten an,
insbesondere IP-Adresse, Zeitpunkt und angefragte Ressource. Welche dieser Daten
protokolliert werden, hängt von den eingesetzten Servern und Diensten ab.
Wenn Sie uns kontaktieren, verarbeiten wir Ihre Nachricht und Kontaktangaben zur
Bearbeitung des Anliegens. Die genannte Kontaktadresse wird bei Gmail betrieben;
die konkrete Vertrags- und Kontokonfiguration ist zu prüfen.

### Schutz Ihrer Daten

Die kirchenweiten Datenschutzvorgaben umfassen interne Verwaltungsregeln und
regelmäßige Schulungen, die Verwaltung von Zugriffsrechten und Zugriffskontrollen,
Verschlüsselung schutzbedürftiger Identifikationsdaten und Schutzsoftware sowie
die Begrenzung des Zugangs zu Räumen und Aufbewahrungsorten für Unterlagen und
IT-Systeme. Diese Vorgaben werden auch der App zugrunde gelegt.

**[OFFEN: Tatsächliche Umsetzung und Zuständigkeit für diese Schutzmaßnahmen
einschließlich des Hostingbetriebs prüfen. Logs, Diagnose-/Testdienste,
E-Mail-Anbieterrolle und Fristen ergänzen. Keine pauschale Zusicherung „kein
Tracking“ oder „keine Drittdienste“ ohne gesonderte Prüfung des ausgelieferten
Builds abgeben.]**

## 4. Rechtsgrundlagen und Freiwilligkeit

**[OFFEN — Freigabe erforderlich: Für jeden oben beschriebenen Zweck eine
konkrete Rechtsgrundlage und bei Art. 6 Abs. 1 Buchst. f das konkrete Interesse
eintragen. Vertragsnotwendigkeit oder berechtigte Interessen nicht pauschal
unterstellen. Optionale Verarbeitung von notwendigen Kontofunktionen trennen.]**

Soweit wir eine Verarbeitung auf Ihre Einwilligung stützen, erläutern wir Zweck,
betroffene Daten und Empfänger vor Ihrer Entscheidung. Eine Einwilligung erteilen
Sie durch eine aktive, nicht vorausgewählte Auswahl. Die bloße Kenntnisnahme
dieser Datenschutzerklärung erteilt keine Einwilligung.

**[OFFEN: Für jede freiwillige Funktion die Folgen einer Ablehnung nennen. Ein
allgemeines „App nur gegen Zustimmung zu allem“ nicht ungeprüft übernehmen.
Eine Betriebssystemberechtigung ersetzt die Prüfung der Rechtsgrundlage nicht.]**

## 5. Wer erhält Daten und wo werden sie verarbeitet?

### Betriebsumgebungen: ST und Prod

Für die ST-Umgebung ist Hosting bei **Hetzner in Deutschland** bestätigt.
Auch die **ST-Datenbank und der ST-Keycloak-Dienst werden in Deutschland
betrieben**. Diese Angaben wurden vom Betreiber am 24. Oktober 2026 mitgeteilt;
eine Prüfung der Providerverträge und der tatsächlichen Infrastruktur steht aus.

Die **Prod-Umgebung verwendet einen anderen Hostinganbieter als ST**. Der
Prod-Anbieter und die Betriebsorte von Prod-Server, Datenbank und Keycloak sind
noch zu ergänzen. Die Angaben zu Hetzner und Deutschland gelten damit ausdrücklich
nur für ST; sie belegen keine entsprechenden Standorte für Prod.

**[OFFEN: Angaben für die konkret ausgelieferte Umgebung fertigstellen. Das
Prod-Setup wird in hanmaum-dn-server#269 verfolgt. Vertragsgesellschaften,
Backup-/Log-Standorte, Unterauftragnehmer und weitere externe Dienste getrennt
bestätigen. Vor Veröffentlichung den für Nutzer maßgeblichen Umgebungsbezug klar
darstellen; ST- und Prod-Angaben dürfen sich nicht widersprechen.]**

### Empfänger

Innerhalb der Gemeinde sollen nur die Personen auf Daten zugreifen, die diese
für ihre Aufgabe benötigen. Der konkrete Empfängerkreis muss mit den tatsächlich
umgesetzten Rollen und Sichtbarkeiten übereinstimmen.

| Dienst / Empfängerkreis | Zweck | Noch vor Veröffentlichung zu ergänzen |
| --- | --- | --- |
| Gemeinde- und App-Administration, zuständige Gruppen-/Kursverantwortliche | Konto-, Teilnahme- und Gemeindeverwaltung | Konkrete Rollen, sichtbare Felder und weitere Mitglieder als Empfänger, falls vorhanden |
| Hosting, Datenbank und Keycloak | Betrieb und Anmeldung | ST: Hetzner-Hosting in Deutschland, DB und Keycloak ebenfalls Deutschland; Vertragsgesellschaften und Rollen prüfen. Prod: anderer Anbieter, Name und Betriebsorte offen |
| Backups, Logs und E-Mail-Dienst | Sicherung, technischer Betrieb und Kontokommunikation | Anbieter, Standorte, Unterauftragnehmer, Vertragsrollen und Löschfristen je Umgebung gesondert bestätigen |
| Google / Firebase; unter iOS Apple | Push-Zustellung | Verantwortliche Gesellschaften, Datenflüsse und vertragliche Grundlage |
| Google Calendar | Kalenderanzeige | Gesellschaft, Verarbeitungsorte und Rechtsgrundlage |
| pCloud und verwendete Bild-/Mediendienste | Album- und Bildanzeige | Gesellschaften, Region, Freigaben und Empfängerrolle |
| Kirchliches Kursportal application.hanmaum.de | Kursanmeldung | Hostinganbieter, Verarbeitungsorte und Vertrags-/Verantwortungsverteilung ergänzen |

**[OFFEN: Werden Daten außerhalb des EWR verarbeitet, Land und konkrete Grundlage
nach Kapitel V DSGVO sowie Zugang zu den verwendeten Garantien angeben. Weder
„alles in Deutschland“ noch „EU-Hosting verhindert jede Drittlandverarbeitung“
behaupten. Eine etwaige Angemessenheit/Zertifizierung muss für den tatsächlichen
Empfänger und Dienst überprüft werden.]**

## 6. Wie lange werden Daten gespeichert?

Für die Konto- und Profilverwaltung gilt nach kirchlicher Vorgabe grundsätzlich
die Dauer bis zur Beendigung des jeweiligen Mitgliedskontos. Daten werden
gelöscht, sobald der Zweck entfällt oder die maßgebliche Aufbewahrungsdauer
abläuft. Soweit eine weitere Speicherung rechtlich erforderlich und zulässig ist,
werden die Daten getrennt aufbewahrt. Dies kann beispielsweise bei laufenden
Ermittlungen oder Prüfungen wegen Rechtsverstößen bis zu deren Abschluss gelten.

**[OFFEN — konkrete Fristen und Ausnahmen für die noch offenen Datenarten sowie
die tatsächliche technische Umsetzung des Löschkonzepts ergänzen:]**

| Daten | Festzulegende Frist oder nachvollziehbares Kriterium |
| --- | --- |
| Konto und Profil | Grundsätzlich bis zur Kontobeendigung; frühere Zweckerreichung und rechtlich erforderliche, zulässige Ausnahmen berücksichtigen |
| Anwesenheit, RSVP und Kurse | Jeweilige Zweck-/Dokumentationsdauer; Anonymisierung getrennt prüfen |
| Standortdaten und technische Logs | Tatsächliche Verarbeitung und kurze konkrete Fristen je System |
| Push-Kennungen, Geräte-Caches und Backups | Ablauf, Abmeldung, Kontowechsel und Backup-Rotation je Dienst |
| Einwilligungsnachweise und Anfragen | Notwendige Nachweisdauer mit geprüfter Grundlage und Löschregel |

Für die Löschung sieht die kirchliche Richtlinie die Auswahl der zu löschenden
Daten und die Freigabe durch die kircheninterne Datenschutzverantwortung vor.
Elektronische Daten sollen mit geeigneten Verfahren nicht wiederherstellbar
gelöscht werden; Papierunterlagen werden durch Schreddern oder Verbrennen
vernichtet. Gesetzlich weiter aufzubewahrende Unterlagen werden in einer
gesonderten Datenbank oder an einem getrennten Aufbewahrungsort gesichert.

Das Deinstallieren der App beendet nicht automatisch Ihr Gemeindekonto und
löscht keine serverseitigen Daten. Für eine Löschanfrage erreichen Sie uns unter
**hanmaum.dev@gmail.com**. Welche Daten anschließend noch aus rechtlichen Gründen
aufbewahrt werden müssen, ist im konkreten Fall zu prüfen und mitzuteilen.

## 7. Einwilligung widerrufen und Rechte ausüben

Sie können eine Einwilligung jederzeit mit Wirkung für die Zukunft widerrufen.
Die Rechtmäßigkeit der zuvor auf dieser Grundlage erfolgten Verarbeitung bleibt
unberührt. Anfragen können schriftlich an die oben genannte Anschrift,
telefonisch an das Kirchenbüro unter **+49 69 3399 9677** oder per E-Mail an
**hanmaum.dev@gmail.com** gestellt werden. Die kirchliche Annahme- und
Bearbeitungsstelle ist **이충만 목사**. Wir bearbeiten die Anliegen ohne
unangemessene Verzögerung.

Anfragen können auch durch eine gesetzliche Vertretung oder eine bevollmächtigte
Person gestellt werden; wir können einen geeigneten Nachweis der
Vertretungsbefugnis verlangen. Nach der kirchlichen Bearbeitungsregel werden
Daten, auf die sich ein Berichtigungs- oder Löschungsersuchen bezieht, bis zur
Erledigung nicht weiter genutzt oder weitergegeben. Die praktische Umsetzung
dieser Regel für die App ist noch zu überprüfen. Wenn Sie Angaben über andere
Personen machen, beachten Sie bitte deren Rechte und Privatsphäre.

**[OFFEN: Vor Einführung der App-Einwilligung muss ein ebenso einfacher
Widerrufsweg tatsächlich bereitstehen. Der geplante Backend-Endpoint allein
genügt nicht. Die Folgen für Konto, Kurse und optionale Funktionen sowie die
Bearbeitung und erforderliche Einschränkung bei Auskunfts-, Berichtigungs- und
Löschanfragen technisch und organisatorisch prüfen; Widerruf ist nicht
automatisch Kontolöschung.]**

Nach den jeweils geltenden Voraussetzungen haben Sie Rechte auf Auskunft,
Berichtigung, Löschung, Einschränkung und Datenübertragbarkeit. Soweit eine
Verarbeitung auf berechtigten Interessen beruht, können Sie aus Gründen Ihrer
besonderen Situation widersprechen. Sie können sich außerdem bei einer
Datenschutzaufsichtsbehörde beschweren. Für Anfragen nutzen Sie den oben genannten
Datenschutzkontakt.

**[OFFEN: Zuständige Aufsicht bestätigen. Falls die staatliche Aufsicht für den
Träger in Hessen zuständig ist, kommt der Hessische Beauftragte für Datenschutz
und Informationsfreiheit in Betracht; bei kirchlichem Datenschutzrecht kann
eine kirchliche Aufsicht zuständig sein. Erst nach Klärung eindeutig benennen.]**

## 8. Altersgruppe und automatisierte Entscheidungen

### Altersgruppe

Unsere App richtet sich an Personen, die älter als 16 Jahre sind. Eine Nutzung
durch Personen bis einschließlich 16 Jahre ist nicht vorgesehen. Für dieses
Nutzungskonzept wird keine elterliche Zustimmung eingeholt.

**[OFFEN: Umsetzung der Zielgruppenregel und Umgang mit 17-jährigen Nutzenden
prüfen; diese sind noch minderjährig. Die Aussage zur elterlichen Zustimmung
beschreibt den vom Betreiber vorgesehenen Ablauf, keine pauschale rechtliche
Bewertung aller Verarbeitungen. Keine technische Altersprüfung behaupten,
solange sie nicht umgesetzt und geprüft wurde.]**

### Automatisierte Entscheidungen

Über die Freigabe von Mitgliedskonten und die Zulassung zu Kursen entscheidet
jeweils eine verantwortliche Person. Diese Entscheidungen werden nicht
ausschließlich durch automatisierte Verarbeitung getroffen.

**[OFFEN: Weitere automatische Verarbeitungen wie Matching, Prüfung von
Kursvoraussetzungen und Standortabgleich sowie etwaiges Profiling gesondert auf
ihre Zwecke und Auswirkungen prüfen.]**

## 9. Änderungen dieser Erklärung

Wenn sich Zwecke, Dienste oder Datenflüsse ändern, passen wir diese Informationen
an. Eine nötige neue Einwilligung darf nicht durch ein bloßes Versionsupdate
ersetzt werden. Die für Einwilligungen geplante Speicherung von Zweck, Version,
Zeitpunkt und Herkunft ist Teil von #290 und noch nicht ausgeliefert.

**[OFFEN: Nach Freigabe tatsächliches Gültigkeitsdatum, unveränderliche
Versionskennung und öffentliche HTTPS-Adresse einsetzen. Alte Fassungen für die
Nachweisbarkeit aufbewahren. Deutsche, koreanische und englische Arbeitsfassungen
liegen parallel vor; Änderungen und die abschließende Freigabe in allen drei
Sprachen inhaltlich gleichwertig halten.]**

---

## Quellen und Prüfhinweise — Redaktionsteil, nicht Teil der Veröffentlichung

Dies ist ein individueller Entwurf, keine übernommene fremde Datenschutzerklärung
und keine rechtliche Freigabe. Vor Veröffentlichung müssen alle OFFEN-Felder
aufgelöst und die tatsächliche Produktionskonfiguration bestätigt sein.

- [Informationspflichten — Sächsische Datenschutz- und Transparenzbeauftragte](https://www.datenschutz.sachsen.de/informationspflichten.html): Struktur der Informationen nach Art. 13/14 DSGVO.
- [EDSA-Leitlinien 05/2020 zur Einwilligung](https://www.edpb.europa.eu/system/files/documents/files/file1/edpb_guidelines_202005_consent_en.pdf): Freiwilligkeit, Zwecktrennung, Nachweis und Widerruf.
- [Betroffenenrechte — LfD Niedersachsen](https://www.lfd.niedersachsen.de/dsgvo/betroffenenrechte/): Voraussetzungen der Rechte betroffener Personen.
- [DSG-EKD, insbesondere Anwendungsbereich](https://kirchenrecht-ekd.de/document/58309): Kirchliche Zuständigkeit nicht aus der Bezeichnung „Kirche“ ableiten.
- [Handbuch zum europäischen Datenschutzrecht — EGMR/FRA](https://www.echr.coe.int/documents/d/echr/Handbook_data_protection_DEU): Voraussetzungen für die Verarbeitung sensibler Daten durch religiöse Organisationen ohne Gewinnerzielungsabsicht.
- [§ 25 TDDDG](https://www.gesetze-im-internet.juris.de/ttdsg/__25.html): Speicherung und Zugriff auf Endgeräten gesondert prüfen.
- [Firebase: Privacy and Security](https://firebase.google.com/support/privacy): Installationskennungen und mögliche globale Verarbeitung; tatsächlichen Vertrag prüfen.
- [Hessen: Beschwerde übermitteln](https://datenschutz.hessen.de/service/beschwerde-uebermitteln): mögliche staatliche Anlaufstelle nach Klärung des Trägers.
- [Kirchliche Datenschutzvorgaben auf application.hanmaum.de](https://application.hanmaum.de/): vom Betreiber am 27. Oktober 2026 als kirchenweit geltend bestätigt; Quelltext mit Geltungsbeginn 1. Mai 2020. Übernommen werden organisatorische Zuständigkeiten und passende Regeln, nicht automatisch koreanische Gesetzesverweise, Behörden, Cookie-Aussagen oder pauschale Nichtweitergabe.

Die Übernahme und ihre Grenzen sind in `privacy-policy-church-source-2026-10-27.md`
dokumentiert. Der historische Geltungsbeginn der Quelle ist kein Gültigkeitsdatum
dieser App-Erklärung. Organisatorische Vorgaben sind von nachgewiesener technischer
Umsetzung zu unterscheiden.

Technischer Prüfstand: Mobile `origin/main` bei `7f58123`; Registrierung und Login
(`RegisterRequest`, `AuthRepositoryImpl`), Profil (`MemberResponse`), Standort
(`AttendanceRepositoryImpl`, `GeofenceCoordinator`), Kalender
(`CalendarRepositoryImpl`), Alben (`AlbumDetailRepositoryImpl`), Kurse
(`TrainingRepositoryImpl`), Push (`AndroidPushManager`, `IosPushManager`,
`iOSApp.swift`) und lokale Anmeldung (`TokenStorageImpl`, `BiometricVault`).
Dies ist eine Quellcodeprüfung, keine Prüfung des Produktionsbetriebs.
