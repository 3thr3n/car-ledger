# Privacy policy: a draft for operators

A starting point for the privacy policy (Art. 13 GDPR) of a CarLedger installation that other people use. It lists
what CarLedger itself processes; your own setup (hoster, identity provider, mail, map, backups) adds to it.
**This is not legal advice**: fill in the `[brackets]`, delete what your installation doesn't use (e.g. GPS tracking
without trackers, e-mail without an SMTP server) and have the text checked. A German version is in
[PRIVACY_TEMPLATE.de.md](PRIVACY_TEMPLATE.de.md). Host the finished page anywhere and link it with
`carLedger.legal.privacyUrl` ([Legal pages](DEPLOYMENT.md#legal-pages)).

Only the owner (or family and friends who know you) logging in? Then you usually need neither this page nor an
Impressum: the GDPR doesn't apply to purely personal or household activities (Art. 2(2)(c)).

---

## Privacy policy of [name of the installation, e.g. CarLedger of Jane Doe]

### Controller

[Name]
[Address]
[E-mail address]

### What is processed, and why

CarLedger records the costs, papers and trips of your cars. Everything below is processed to provide this service
to you (Art. 6(1)(b) GDPR, performance of the contract of use); without it the service can't be used. Nothing is used
for advertising or profiling, and no decisions are made automatically.

- **Login**: the login goes through [the identity provider, e.g. Keycloak operated by us at keycloak.example.com].
  CarLedger receives your user name, your name, your e-mail address (and whether it is confirmed) and your roles. A
  session cookie keeps you logged in.
- **Account**: your settings (language, currency, VAT country, units, color), when the account was created and last
  used, and your notification settings.
- **Cars and entries**: what you enter: cars (name, plate, VIN, year, purchase date and price, picture), refuels,
  charges, maintenance, other and recurring costs, modifications, inspections (HU/AU), reminders, and the files you
  upload with them (receipts, photos, documents) [stored in the database / in an S3 storage of [provider]].
- **Trip log**: trips with their times, odometers, destination, purpose and business partner as you enter them, and
  who drove.
- **GPS tracking** (only for cars with a tracker): the positions the tracker sends with time, speed, altitude,
  accuracy and g-forces, the names you give places, and the last status of the tracker (firmware, storage, WLAN
  connection). [The positions are snapped to the roads by a routing service operated by us; they aren't sent to third
  parties.] The map of the tracks is loaded by your browser from [map provider, e.g. OpenFreeMap / MapTiler]: it gets
  your IP address and the map area shown, but no positions of the tracks.
- **Sharing**: when a car is shared, the invited people see its data according to their role; an invitation names
  their e-mail address. Entries and trips show who created them.
- **Car requests**: requests for more cars with their reason, and the decision of the administrators.
- **Notifications**: an inbox in the app (kept for 90 days). [By e-mail through [SMTP provider].] [As push messages to
  the browsers you turn them on for: the address of the push service of your browser (e.g. Google, Mozilla, Apple,
  Microsoft) is stored; the messages are encrypted for your browser, the push service only forwards them.]
- **Server logs**: errors and events of the server, some with your user name or the id of your account, [and the
  logs of the web server with IP addresses], kept for [x days], for the operation and security of the service
  (Art. 6(1)(f) GDPR).
- **In your browser**: CarLedger keeps in the storage of your browser only what the app needs: the page to return to
  after the login, the column mapping of a CSV import, the last opened car, the dismissed install hint, whether the
  sidebar is hidden, the color, the address of the push subscription, data for offline use (the account, the cars,
  entries not sent yet) and the files of the app, and documents you keep offline. No tracking, no cookies besides the
  session, no content of third parties besides the map; therefore no consent banner (§ 25(2) TDDDG).

### Recipients

- [Hoster of the server, e.g. Example GmbH, under a data processing agreement (Art. 28 GDPR)]
- [Identity provider, if not operated by us]
- [SMTP provider, under a data processing agreement]
- [S3 storage provider, under a data processing agreement]
- [Map provider: your IP address, when the map is shown]
- [Push services of the browsers: encrypted messages; Google, Apple and Microsoft are located in the USA, a transfer
  under the EU-US Data Privacy Framework (Art. 45 GDPR)]
- People you share a car with: its data, according to their role

### How long

Your data is kept until you delete it or your account. You delete your account yourself in the settings: it deletes
your cars with all their entries, files and tracks; trips you drove in cars of others stay in those cars, without
your name. Track points are kept as long as set for the car (forever by default, or 7 days to 2 years). Notifications
of the inbox are deleted after 90 days. [Server logs after x days, backups of the database after x days.]

### Your rights

You have the right to access (Art. 15 GDPR), rectification (Art. 16), erasure (Art. 17), restriction of processing
(Art. 18), data portability (Art. 20: in the settings you download all of your data as a ZIP) and to object to
processing based on Art. 6(1)(f) (Art. 21). Write to [e-mail address]. You may lodge a complaint with a supervisory
authority (Art. 77 GDPR), e.g. [the one of your state].

Last updated: [date]
