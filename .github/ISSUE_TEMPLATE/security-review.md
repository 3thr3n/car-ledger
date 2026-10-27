---
name: Security review of a milestone
about: The last issue of every milestone, before its release
title: "Security review of the pull requests since <last release>, before <release>"
labels: ["security"]
---

### Scope
The pull requests merged into `develop` since the last release, e.g. `git log --oneline <last release>..origin/develop`
and `gh pr list --state merged --base develop --search "merged:>=<date of the last release>"`. Everything older was
reviewed with its milestone (see #481 for the full review before 0.9.0).

### Scans
- [ ] CodeQL and Dependabot: no open alerts
- [ ] `yarn audit` of the frontend
- [ ] Trivy on the image of `develop` (OS packages, JRE, jars)
- [ ] Semgrep (java, typescript, react, secrets, owasp-top-ten)
- [ ] Checkov on the rendered charts, if a chart changed
- [ ] ZAP API scan with `openapi.json` as alice, if the API changed: without `/api/auth/*` and the deletion of
      accounts, and check that the session is still valid afterwards

### Each pull request
- [ ] New or changed endpoints: the query goes through `CarAccess` with the right action; ids of another car in the
      path or body are not found; the admin API only for `admin`
- [ ] A driver still sees only the own entries; MANAGE stays with the owner
- [ ] Inputs: lengths of the texts, numbers and dates in range, NUL characters, uploads (type by content, size)
- [ ] Outputs: CSV cells through `CsvText`, file names of downloads, `Content-Type` / `nosniff`
- [ ] Outgoing requests (SSRF), new hosts in the CSP
- [ ] Data kept on the device (IndexedDB, Cache Storage): removed on the logout, for another account, when an access
      ends
- [ ] Logs and error answers without tokens, secrets or data of other users
- [ ] Charts: new secrets, values and permissions

### Done when
Every point is checked or has an issue of its own; the fixes are merged before the release, or moved to the next
milestone on purpose.
