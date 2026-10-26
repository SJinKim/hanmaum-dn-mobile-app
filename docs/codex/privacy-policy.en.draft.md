# Privacy notice for the Hanmaum DN app

**Working draft, last edited on 26 October 2026 — not approved for publication.**

This text is tailored to the reviewed app code. Items marked **[OPEN: …]** must
be completed before publication, and the responsible church must review the
legal bases. Features from issue #290 are identified as planned. This notice
is not itself a declaration of consent.

## 1. Who is responsible?

한마음교회<br>
Leonhard-Heißwolf-Str. 5<br>
65936 Frankfurt am Main<br>
Germany

Privacy contact: **hanmaum.dev@gmail.com** (the church's development team).

**[OPEN: Confirm the controller's full legal name, legal form and authorised
representative. Only the church name has been supplied so far. Check whether a
data protection officer has been appointed or must be appointed; add their
contact details where applicable. The development team is identified here as a
contact point, not as a formally appointed data protection officer.]**

## 2. What does this notice cover?

The app supports our church's organisation and communication. Its functions
include personal member accounts, church announcements, dates and events,
albums, attendance check-ins, and event and course applications.

The following information concerns the Android and iOS app and the associated
church services. When you open external services, their operators also process
data. The website on which this notice will eventually be published requires
appropriate additional information about the operation of that website.

**[OPEN: Determine whether the controller is subject to the GDPR or church data
protection law. The references below provisionally use the GDPR. Church
affiliation alone does not establish which law applies. If a different law
applies, update the legal bases, rights and supervisory authority consistently.]**

## 3. What data do we process, and why?

### Registration, sign-in and member accounts

During registration, your first and last name, email address, password, date of
birth, place of residence and an attribute describing your personal circumstances
(`identityStatus`) are sent to the church server. The form may collect additional
information such as your phone number and address. Other profile details may
include your school or employer and a profile picture URL.

These details are used to create and assign your account, administer membership
and provide the relevant app functions. Access may require approval by the church.

Sign-in takes place through our Keycloak authentication service in the system
browser. During registration in the app, the entered password is first sent to
the registration service; this is distinct from browser-based sign-in. After
sign-in, the app uses access tokens. The “stay signed in” feature may avoid the
need to enter your password again.

**[OPEN: Check the published form's mandatory fields against the server and
version; justify the necessity of the date of birth and personal-status field in
particular. Make non-essential information optional. Add details of Keycloak
hosting and operation and of email delivery for account-related processes.]**

### Church information and religious aspects

Your account may include your church group, area, ministries, course membership,
attendance and participation data. Course forms may ask about baptism, previous
courses and other circumstances relevant to the course. Such information may
reveal religious beliefs and therefore requires a separately reviewed basis for
processing special categories of personal data.

**[OPEN: For each purpose, establish both an Article 6 GDPR legal basis and an
applicable exception under Article 9(2) GDPR. Explicit consent is one possible
basis; Article 9(2)(d) may apply to certain internal religious activities if its
conditions are met. It is not a general permission for any disclosure or feature.]**

### Attendance and location

When you actively check in, the app may send your current latitude, longitude
and estimated location accuracy to the church server. This is used to check
whether the attendance check-in can take place at the designated location. The
server also supplies your attendance history and summaries to the app.

Separately, if location permission has been granted, the app may detect entry
into the church area and display a local reminder. The reviewed geofencing logic
registers a region with the operating system; it does not itself send a continuous
location trail to the church server.

You can change location permission in the app or device settings. Without that
permission, the relevant location-dependent features may not be available.

**[OPEN: Review production-side storage, logging and deletion of the coordinates
transmitted during check-in. Do not publish a claim that location stays only on
the device. Establish the legal basis, necessary permission scope and an
alternative to location verification.]**

### Events, ministries and course applications

We process your event participation choices, ministry assignments and the
information you submit for a course. Course applications may include baptism
information, course history and free text in addition to contact details. The
specific fields are shown in the relevant form.

Course applications are sent to the church server and may be forwarded to the
connected course administration system.

**[OPEN: Identify the course administration system's operator, recipients,
required fields, purpose and retention. Collect information about children or
other people only where necessary; add notices for those people and minors.
The separate course consent from #290 is not yet connected.]**

### Push notifications

The app uses Firebase Cloud Messaging and, on iOS, also Apple Push Notification
service. This involves processing technical installation identifiers, push
identifiers and delivery information. Push identifiers may be linked to your
signed-in account on the church server to deliver messages. Depending on device
settings, messages may appear on the lock screen.

You can control the display of push notifications in your device settings and
the offered notifications in the app. Turning off their display does not
automatically delete a technical installation identifier or stop all SDK
communication. In the reviewed iOS code, Firebase is initialised when the app
starts if the configuration is present.

**[OPEN: Review the legal basis, initialisation before a user choice, actual
message content, token deletion and provider agreements. Do not promise that
Firebase starts only after permission for push notifications has been given.]**

### Calendar, albums and external content

The church calendar is retrieved directly through Google Calendar. In particular,
your IP address and the request information needed to retrieve the calendar are
technically transmitted to Google. This is the public church calendar, not access
to your personal Google Calendar.

The reviewed album feature retrieves directories and images directly through
pCloud. The relevant delivery service thereby technically receives information
including your IP address and the requested content. A visible album is not
automatically an album restricted to members; access rules and sharing links
must be checked separately.

**[OPEN: Confirm the providers and regions for Google, pCloud and the image/media
services actually used. Review profile picture URLs and external links
separately. Do not describe the still-planned S3 upload as an active feature.
Establish legal bases and, where necessary, a prior user choice.]**

### Device storage and biometric sign-in

The app stores settings such as language, appearance and permission preferences,
as well as cached content depending on the feature. Access tokens are kept in
the protected storage of the relevant operating system.

When biometric sign-in is enabled, the operating system releases a protected
sign-in token after a successful check. The app does not receive a facial image
or fingerprint from that biometric check.

**[OPEN: Review cache deletion on account changes, sign-out and uninstallation,
and device/cloud backups. Do not equate sign-out with complete data deletion.
Review whether storage access is necessary under section 25 TDDDG.]**

### Technical operation and contact

Access to servers and services technically involves connection data, in
particular the IP address, time and requested resource. Which of these data are
logged depends on the servers and services used. If you contact us, we process
your message and contact details to handle your request. The stated contact
address is operated through Gmail; the actual contractual and account
configuration must be reviewed.

**[OPEN: Add the actual logs, diagnostic/testing services, email provider's role
and retention periods. Do not give general assurances of “no tracking” or “no
third-party services” without separately reviewing the distributed build.]**

## 4. Legal bases and voluntary choices

**[OPEN — approval required: Specify a concrete legal basis for each purpose
above and, for Article 6(1)(f), the particular legitimate interest. Do not assume
contractual necessity or legitimate interests across the board. Separate
optional processing from necessary account functions.]**

Where we rely on consent for processing, we explain the purpose, affected data
and recipients before you make your choice. You give consent through an active
selection that is not preselected. Merely reading or acknowledging this notice
does not give consent.

**[OPEN: State the consequences of refusal for each optional feature. Do not
adopt a blanket “use the app only if you consent to everything” approach without
review. An operating system permission does not replace the assessment of the
legal basis.]**

## 5. Who receives data, and where are they processed?

### Operating environments: ST and production

Hosting with **Hetzner in Germany** has been confirmed for the ST environment.
The **ST database and ST Keycloak service are also operated in Germany**. The
operator supplied this information on 24 October 2026; provider agreements and
the actual infrastructure have not yet been audited.

The **production environment uses a different hosting provider from ST**. The
production provider and the locations of the production server, database and
Keycloak still need to be added. The Hetzner and Germany information therefore
expressly applies only to ST; it does not establish corresponding locations for
production.

**[OPEN: Complete the information for the environment actually distributed.
Production setup is tracked in hanmaum-dn-server#269. Separately confirm the
contracting companies, backup/log locations, subprocessors and other external
services. Before publication, make the environment relevant to users clear; ST
and production information must not contradict each other.]**

### Recipients

Within the church, access to data should be limited to people who need it for
their tasks. The actual recipients must match the roles and visibility rules
implemented in practice.

| Service / recipients | Purpose | Information to complete before publication |
| --- | --- | --- |
| Church and app administration, responsible group/course leaders | Account, participation and church administration | Specific roles, visible fields and other members as recipients, if applicable |
| Hosting, database and Keycloak | Operation and sign-in | ST: Hetzner hosting in Germany, database and Keycloak also in Germany; verify contracting companies and roles. Production: different provider, name and operating locations still open |
| Backups, logs and email service | Backup, technical operation and account communication | Confirm providers, locations, subprocessors, contractual roles and deletion periods separately for each environment |
| Google / Firebase; Apple on iOS | Push delivery | Responsible companies, data flows and contractual basis |
| Google Calendar | Calendar display | Company, processing locations and legal basis |
| pCloud and the image/media services used | Album and image display | Companies, region, sharing arrangements and recipient roles |
| Course administration system operator | Course application | Legal operator, location and allocation of responsibilities |

**[OPEN: If data are processed outside the EEA, specify the country, the concrete
basis under Chapter V GDPR and how the safeguards used can be accessed. Do not
claim that everything is in Germany or that EU hosting prevents all third-country
processing. Any adequacy decision/certification must be checked for the actual
recipient and service.]**

## 6. How long are data kept?

**[OPEN — complete with the actual deletion policy:]**

| Data | Retention period or understandable criterion to establish |
| --- | --- |
| Account and profile | Duration of the purpose; process for leaving, suspension and permanent deletion |
| Attendance, RSVP and courses | Relevant purpose/documentation period; assess anonymisation separately |
| Location data and technical logs | Actual processing and short, specific periods for each system |
| Push identifiers, device caches and backups | Expiry, sign-out, account changes and backup rotation for each service |
| Consent records and requests | Necessary evidence period with a reviewed legal basis and deletion rule |

Uninstalling the app does not automatically close your church account or delete
server-side data. You can request deletion at **hanmaum.dev@gmail.com**. Any data
that must subsequently be kept for legal reasons must be assessed in the
individual case and explained to you.

## 7. Withdrawing consent and exercising your rights

You can withdraw consent at any time with effect for the future. This does not
affect the lawfulness of processing previously carried out on that basis.
Contact: **hanmaum.dev@gmail.com**.

**[OPEN: Before introducing app consent, an equally easy withdrawal route must
actually be available. The planned backend endpoint alone is not enough.
Specify the consequences for the account, courses and optional features;
withdrawal is not automatically account deletion.]**

Subject to the applicable conditions, you have rights of access, rectification,
erasure, restriction and data portability. Where processing relies on legitimate
interests, you can object on grounds relating to your particular situation.
You can also lodge a complaint with a data protection supervisory authority.
Use the privacy contact above for requests.

**[OPEN: Confirm the competent supervisory authority. If the controller falls
under the state supervisory authority in Hesse, the Hessian Commissioner for
Data Protection and Freedom of Information may be relevant; under church data
protection law a church authority may be competent. Name one unambiguously only
after this has been clarified.]**

## 8. Age group and automated decisions

### Age group

Our app is intended for people aged 17 and above. Use by people aged 16 or
younger is not intended. Parental consent is not obtained under this intended
usage model.

**[OPEN: Review implementation of the intended age-group rule and the handling
of 17-year-old users, who are still minors. The statement on parental consent
describes the operator's intended process, not a general legal assessment of all
processing. Do not claim a technical age check unless it has been implemented
and verified.]**

### Automated decisions

A responsible person decides whether member accounts are approved and whether
applicants are admitted to courses. These decisions are not made solely through
automated processing.

**[OPEN: Separately review the purposes and effects of other automated
processing, such as matching, course prerequisite checks and location checks,
and of any profiling.]**

## 9. Changes to this notice

We update this information when purposes, services or data flows change. A
necessary new consent must not be replaced by a mere version update. The planned
recording of consent purpose, version, time and source is part of #290 and has
not yet been released.

**[OPEN: After approval, insert the actual effective date, immutable version
identifier and public HTTPS address. Retain previous versions for evidence.
German, Korean and English working drafts are available in parallel; changes and
the final approved version must be kept equivalent across all three languages.]**

---

## Sources and review notes — editorial material, not part of publication

This is an individually prepared draft, not another organisation's copied
privacy notice or a legal approval. All OPEN items must be resolved and the
actual production configuration confirmed before publication.

- [Information obligations — Saxon Data Protection and Transparency Commissioner](https://www.datenschutz.sachsen.de/informationspflichten.html): structure of the information under Articles 13/14 GDPR.
- [EDPB Guidelines 05/2020 on consent](https://www.edpb.europa.eu/system/files/documents/files/file1/edpb_guidelines_202005_consent_en.pdf): voluntary choices, separate purposes, evidence and withdrawal.
- [Data subject rights — Lower Saxony Data Protection Commissioner](https://www.lfd.niedersachsen.de/dsgvo/betroffenenrechte/): conditions for exercising data subject rights.
- [DSG-EKD, in particular its scope](https://kirchenrecht-ekd.de/document/58309): do not infer church-law jurisdiction from the name “church”.
- [Handbook on European data protection law — ECtHR/FRA](https://www.echr.coe.int/documents/d/echr/Handbook_data_protection_DEU): conditions for processing sensitive data by non-profit religious organisations.
- [Section 25 TDDDG](https://www.gesetze-im-internet.juris.de/ttdsg/__25.html): separately review storage and access on devices.
- [Firebase: Privacy and Security](https://firebase.google.com/support/privacy): installation identifiers and possible global processing; check the actual agreement.
- [Hesse: submitting a complaint](https://datenschutz.hessen.de/service/beschwerde-uebermitteln): possible state contact after the controller's status is clarified.

Technical review baseline: mobile `origin/main` at `7f58123`; registration and
sign-in (`RegisterRequest`, `AuthRepositoryImpl`), profile (`MemberResponse`),
location (`AttendanceRepositoryImpl`, `GeofenceCoordinator`), calendar
(`CalendarRepositoryImpl`), albums (`AlbumDetailRepositoryImpl`), courses
(`TrainingRepositoryImpl`), push (`AndroidPushManager`, `IosPushManager`,
`iOSApp.swift`) and local sign-in (`TokenStorageImpl`, `BiometricVault`).
This is a source-code review, not an audit of production operations.
