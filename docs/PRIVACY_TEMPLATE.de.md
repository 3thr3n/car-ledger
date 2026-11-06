# Datenschutzerklärung: ein Entwurf für Betreiber

Ein Ausgangspunkt für die Datenschutzerklärung (Art. 13 DSGVO) einer CarLedger-Installation, die andere Menschen
nutzen. Er listet, was CarLedger selbst verarbeitet; die eigene Umgebung (Hoster, Identity Provider, Mail, Karte,
Backups) kommt hinzu. **Keine Rechtsberatung**: die `[Klammern]` ausfüllen, streichen, was die Installation nicht nutzt
(z. B. GPS ohne Tracker, E-Mail ohne SMTP-Server), und den Text prüfen lassen. Die englische Fassung steht in
[PRIVACY_TEMPLATE.md](PRIVACY_TEMPLATE.md). Die fertige Seite irgendwo veröffentlichen und mit
`carLedger.legal.privacyUrl` verlinken ([Legal pages](DEPLOYMENT.md#legal-pages)).

Melden sich nur Sie selbst (oder Familie und Freunde, die Sie kennen) an? Dann braucht es meist weder diese Seite noch
ein Impressum: Die DSGVO gilt nicht für ausschließlich persönliche oder familiäre Tätigkeiten (Art. 2 Abs. 2 lit. c).

---

## Datenschutzerklärung von [Name der Installation, z. B. CarLedger von Max Mustermann]

### Verantwortlicher

[Name]
[Anschrift]
[E-Mail-Adresse]

### Was verarbeitet wird, und wozu

CarLedger erfasst die Kosten, Unterlagen und Fahrten Ihrer Fahrzeuge. Alles Folgende wird verarbeitet, um Ihnen diesen
Dienst bereitzustellen (Art. 6 Abs. 1 lit. b DSGVO, Erfüllung des Nutzungsvertrags); ohne diese Daten lässt sich der
Dienst nicht nutzen. Nichts davon dient der Werbung oder einem Profiling, es gibt keine automatisierten Entscheidungen.

- **Anmeldung**: Die Anmeldung läuft über [den Identity Provider, z. B. Keycloak, von uns betrieben unter
  keycloak.example.com]. CarLedger erhält Ihren Benutzernamen, Ihren Namen, Ihre E-Mail-Adresse (und ob sie bestätigt
  ist) und Ihre Rollen. Ein Session-Cookie hält Sie angemeldet.
- **Konto**: Ihre Einstellungen (Sprache, Währung, Land der Umsatzsteuer, Einheiten, Farbe), wann das Konto angelegt
  und zuletzt genutzt wurde, und Ihre Einstellungen der Benachrichtigungen.
- **Fahrzeuge und Einträge**: was Sie eingeben: Fahrzeuge (Name, Kennzeichen, FIN, Baujahr, Kaufdatum und -preis,
  Bild), Tankungen, Ladevorgänge, Wartungen, sonstige und wiederkehrende Kosten, Umbauten, HU/AU, Erinnerungen, und die
  Dateien, die Sie dazu hochladen (Belege, Fotos, Dokumente) [gespeichert in der Datenbank / in einem S3-Speicher von
  [Anbieter]].
- **Fahrtenbuch**: Fahrten mit Zeiten, Kilometerständen, Ziel, Zweck und Geschäftspartner, wie Sie sie eingeben, und
  wer gefahren ist.
- **GPS-Tracking** (nur bei Fahrzeugen mit Tracker): die Positionen, die der Tracker sendet, mit Zeit,
  Geschwindigkeit, Höhe, Genauigkeit und g-Kräften, die Namen, die Sie Orten geben, und der letzte Status des Trackers
  (Firmware, Speicher, WLAN-Verbindung). [Die Positionen werden von einem von uns betriebenen Routing-Dienst den
  Straßen zugeordnet; sie gehen nicht an Dritte.] Die Karte der Strecken lädt Ihr Browser von [Kartenanbieter, z. B.
  OpenFreeMap / MapTiler]: Er erhält Ihre IP-Adresse und den gezeigten Kartenausschnitt, aber keine Positionen der
  Strecken.
- **Teilen**: Wird ein Fahrzeug geteilt, sehen die Eingeladenen seine Daten entsprechend ihrer Rolle; eine Einladung
  nennt deren E-Mail-Adresse. Einträge und Fahrten zeigen, wer sie angelegt hat.
- **Fahrzeug-Anfragen**: Anfragen nach weiteren Fahrzeugen mit ihrer Begründung, und die Entscheidung der
  Administratoren.
- **Benachrichtigungen**: ein Posteingang in der App (90 Tage aufbewahrt). [Per E-Mail über [SMTP-Anbieter].] [Als
  Push-Nachrichten an die Browser, für die Sie sie einschalten: Gespeichert wird die Adresse beim Push-Dienst Ihres
  Browsers (z. B. Google, Mozilla, Apple, Microsoft); die Nachrichten sind für Ihren Browser verschlüsselt, der
  Push-Dienst leitet sie nur weiter.]
- **Server-Logs**: Fehler und Ereignisse des Servers, teils mit Ihrem Benutzernamen oder der Nummer Ihres Kontos, [und
  die Logs des Webservers mit IP-Adressen], aufbewahrt für [x Tage], für den Betrieb und die Sicherheit des Dienstes
  (Art. 6 Abs. 1 lit. f DSGVO).
- **In Ihrem Browser**: CarLedger legt im Speicher Ihres Browsers nur ab, was die App braucht: die Seite, zu der es
  nach der Anmeldung zurückgeht, die Spaltenzuordnung eines CSV-Imports, das zuletzt geöffnete Fahrzeug, den
  ausgeblendeten Installationshinweis, ob die Seitenleiste ausgeblendet ist, die Farbe, die Adresse des
  Push-Abonnements, Daten für die Offline-Nutzung (das Konto, die Fahrzeuge, noch nicht gesendete Einträge) und die
  Dateien der App, sowie Dokumente, die Sie offline behalten. Kein Tracking, keine Cookies außer der Session, keine
  Inhalte Dritter außer der Karte; daher kein Einwilligungsbanner (§ 25 Abs. 2 TDDDG).

### Empfänger

- [Hoster des Servers, z. B. Beispiel GmbH, mit einem Vertrag zur Auftragsverarbeitung (Art. 28 DSGVO)]
- [Identity Provider, falls nicht von uns betrieben]
- [SMTP-Anbieter, mit einem Vertrag zur Auftragsverarbeitung]
- [Anbieter des S3-Speichers, mit einem Vertrag zur Auftragsverarbeitung]
- [Kartenanbieter: Ihre IP-Adresse, wenn die Karte gezeigt wird]
- [Push-Dienste der Browser: verschlüsselte Nachrichten; Google, Apple und Microsoft sitzen in den USA, eine
  Übermittlung auf Grundlage des EU-US Data Privacy Framework (Art. 45 DSGVO)]
- Personen, mit denen Sie ein Fahrzeug teilen: dessen Daten, entsprechend ihrer Rolle

### Wie lange

Ihre Daten bleiben gespeichert, bis Sie sie oder Ihr Konto löschen. Ihr Konto löschen Sie selbst in den
Einstellungen: Das löscht Ihre Fahrzeuge mit allen Einträgen, Dateien und Strecken; Fahrten, die Sie in Fahrzeugen
anderer gefahren sind, bleiben dort, ohne Ihren Namen. GPS-Punkte bleiben so lange, wie für das Fahrzeug eingestellt
(standardmäßig unbegrenzt, oder 7 Tage bis 2 Jahre). Benachrichtigungen des Posteingangs werden nach 90 Tagen
gelöscht. [Server-Logs nach x Tagen, Backups der Datenbank nach x Tagen.]

### Ihre Rechte

Sie haben das Recht auf Auskunft (Art. 15 DSGVO), Berichtigung (Art. 16), Löschung (Art. 17), Einschränkung der
Verarbeitung (Art. 18), Datenübertragbarkeit (Art. 20: In den Einstellungen laden Sie alle Ihre Daten als ZIP
herunter) und auf Widerspruch gegen Verarbeitungen nach Art. 6 Abs. 1 lit. f (Art. 21). Schreiben Sie an
[E-Mail-Adresse]. Sie können sich bei einer Aufsichtsbehörde beschweren (Art. 77 DSGVO), z. B. [der Ihres
Bundeslandes].

Stand: [Datum]
