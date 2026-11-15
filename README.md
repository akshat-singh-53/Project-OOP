a# I-ITSM — Intelligent IT Service Management & Incident Resolution System

CS F213 Object-Oriented Programming project, Semester I 2026-27.

A full-stack web application that manages the complete lifecycle of IT incidents —
**Report → Classify → Prioritise → Assign → Investigate → Resolve → Verify → Close** — with
three role-specific portals (Employee, Support Engineer, Administrator), SLA monitoring,
notifications, a full audit trail and seven intelligent features.

![Administrator dashboard](docs/screenshots/admin-dashboard.png)

---

## Features

### Users and portals

Three kinds of users, each with its own menu, dashboard and colour theme (Employee teal, Support Engineer indigo,
Administrator orange). Every page and API call checks the user's role.

**Employee / customer**
- Register an account or log in.
- Report an incident: choose the affected service, describe the problem, set impact, urgency and number of affected users.
  While typing, the system suggests a category and warns about similar open incidents.
- *My incidents*: view and search everything they reported, and follow its status, priority, SLA clock and timeline.
- Add comments, answer the technician's questions (*Provide info*), read the resolution.
- Confirm the fix (*Confirm it is fixed*) or reopen it (*Not fixed — reopen*), then rate the support from 1 to 5 stars.
- Dashboard: open incidents, incidents waiting for their reply, resolved incidents, average resolution time.
- Employees only ever see their own incidents and never see technicians' internal notes.

**Technician / support engineer** (levels L1, L2, Team Lead)
- *My assigned queue*, *Unassigned queue* (take an incident for yourself) and *All incidents*.
- Work an incident: *Acknowledge* → *Start work* → add public or internal investigation notes → *Request info* from the
  reporter → *Resolve* with resolution details.
- Re-prioritise (new impact/urgency with a reason), re-categorise, mark as a duplicate of another incident, close.
- See ranked technician recommendations with the reasons for each score; Team Leads can assign or reassign to anyone.
- Dashboard: own open incidents, unassigned queue, own incidents at SLA risk, team SLA compliance, workload.
- Set themselves *Away* on the profile page so automatic assignment skips them.
- *SLA risk & correlation* page, SLA policies and violations, and the same incident reporting form as employees.

**Administrator / IT manager**
- Organisation dashboard: totals, open, critical, unassigned, escalated, SLA compliance, average resolution time,
  average rating, 14-day trend, charts by status / priority / category, resolution time per priority,
  per-service analysis and technician workload.
- *Users & technicians*: create, edit, deactivate users and reset passwords; set a technician's level, experience,
  maximum workload, availability and skills; mark administrators as IT Manager (receives top-level escalations).
- *Services & categories*: add and edit IT services (business criticality 1–5, default category, owner team) and
  incident categories (with classification keywords). New categories work everywhere without code changes.
- *SLA policies & violations*: create and edit policies, run the SLA monitor on demand, list all violations.
- *Strategies & settings*: switch the assignment strategy and the priority strategy at runtime; turn automatic
  assignment, e-mail, SMS, escalation and the AI classifier on or off; set the auto-close time; send a test e-mail.
- *Audit log* (filter by incident, user, action) and *Notification outbox* (every message sent, by channel).
- Can perform every incident action, including assigning and closing.

Every user also has a *My profile* page (account details, change password) and a *Notifications* page with an
unread counter in the top bar.

### Incident lifecycle

```
NEW → CLASSIFIED → ASSIGNED → ACKNOWLEDGED → IN_PROGRESS ⇄ PENDING_INFO → RESOLVED → VERIFIED → CLOSED
                                                                              ↘ reopened → IN_PROGRESS
```

- Each status is its own class (State pattern) that allows only the legal next actions, and each action also checks
  who may do it (for example, only the assigned technician or an administrator can resolve; only the reporter can
  verify, reopen or give feedback). Illegal moves such as `NEW → CLOSED` are rejected with an error.
- The incident page only shows the buttons that are valid for the current status and the logged-in user.
- Each incident stores its ID (e.g. `INC1024`), reporter, service, category, impact, urgency, affected users,
  priority and score with an explanation, SLA targets and status, assigned technician and assignment method,
  notes, resolution, feedback, all timestamps and its escalation level.
- Resolved incidents the reporter does not confirm are closed automatically after 72 hours (configurable).
- Re-submitting the same incident title within 10 minutes is blocked as a duplicate.

### Priority

Priority is calculated automatically when an incident is reported, with a written explanation. Two strategies, chosen
in settings:

| Strategy | Formula | Bands |
|---|---|---|
| Impact × Urgency × Criticality (default) | impact (1–4) × urgency (1–4) × service criticality (1–5) = 1–80 | P1 ≥ 40, P2 ≥ 20, P3 ≥ 8, else P4 |
| Weighted scoring | 35 % impact + 30 % urgency + 20 % criticality + 15 % affected users (logarithmic) = 0–100 | P1 ≥ 75, P2 ≥ 55, P3 ≥ 35, else P4 |

Staff can re-prioritise later; the change and its reason go into the audit trail.

### Technician assignment

New incidents are assigned automatically (can be switched off). Four strategies, switchable at runtime:

| Strategy | How it chooses |
|---|---|
| Smart (default) | Score out of 100: skill match 40, spare capacity 25, experience 15, history with the same service 10, seniority for P1/P2 10 |
| Skill-based | Technicians skilled in the incident's category first, then by level and experience |
| Load-based | Lowest current workload relative to the technician's maximum |
| Round robin | Takes turns through the available technicians |

Unavailable technicians and those at their workload limit are never chosen. The *Assign* tab on an incident shows the
ranking with reasons (top 5, or all technicians), and any of them can be picked manually.

### SLA management

- Default policies per priority, plus optional service-specific policies (for example, a premium policy for the
  Payment Gateway). The most specific active policy is applied.

  | Priority | Response target | Resolution target |
  |---|---|---|
  | P1 | 5 min | 30 min |
  | P2 | 15 min | 2 h |
  | P3 | 1 h | 8 h |
  | P4 | 4 h | 24 h |

- Response and resolution deadlines are calculated and shown as a live clock on each incident.
- A background monitor runs every 30 seconds: it sends a warning when 75 % of the time is used (configurable per
  policy), records response and resolution violations, escalates, and auto-closes stale resolutions.
- SLA status (on track, at risk, breached, met) is shown on every incident, can be used as a search filter and appears on the dashboards.

### Notifications

Three channels: **in-app** (bell icon and notifications page), **e-mail** (real Gmail SMTP or simulated) and
**SMS** (third-party SMS API through an adapter, simulated). Each event goes to the right people:

| Event | Who is notified |
|---|---|
| Incident created | Reporter |
| Incident assigned / reassigned | Technician (all channels for P1), reporter in-app |
| Acknowledged, work started, priority or category changed | Reporter in-app |
| Information requested / provided | Reporter / technician |
| Resolved, closed, marked duplicate | Reporter |
| Reopened, reporter comment, feedback received | Technician |
| SLA approaching | Technician, Team Leads |
| SLA violated | Technician (SMS), administrators (e-mail) |
| Escalation | Level 1 Team Leads → level 2 administrators → level 3 IT Manager |
| Automatic assignment failed | Administrators |

Administrators can switch e-mail and SMS off and see every message in the notification outbox.

### Audit trail

Every important action is recorded with timestamp, user, action, incident ID, previous status, new status and a
description: creation, classification, priority calculation, SLA applied, assignment, every lifecycle step, notes,
SLA warnings, violations, escalations, logins (including failed ones) and administrator changes. Each incident page
shows its own timeline; administrators can search the whole log.

### Search, filtering and reporting

- Incident search with 12 criteria: incident ID, keyword (title/description), status, priority, category, service,
  technician, SLA status, created-from date, created-to date, open only, unassigned only.
- Role-specific dashboards with charts (see the portals above). Employees and technicians see their own figures;
  administrators see the whole organisation, per-service analysis and technician workload.

### Intelligent features

1. **Automatic categorisation**: suggests the category while the employee types, combining keyword matching (50 %),
   the categories of the most similar past incidents (25 %) and the service's usual category (25 %).
   Optionally uses an LLM through any OpenAI-compatible API.
2. **Duplicate detection**: compares the text with open incidents (word similarity with synonyms and stemming, boosted
   for the same service), warns the employee before submitting, and flags strong matches automatically.
3. **Incident correlation**: groups open incidents on the same service and category in a time window and highlights a
   suspected major incident (three or more incidents, or one containing a P1).
4. **Automatic escalation**: P1/P2 incidents not acknowledged within the response target escalate one level per missed
   window: Technician → Team Lead → System Administrator → IT Manager.
5. **Smart technician recommendation**: the multi-factor ranking above, with the reasons shown.
6. **SLA risk prediction**: compares the time left with the effort similar incidents needed in the past and ranks open
   incidents as high, medium or low risk.
7. **Suggested resolutions**: shows how the most similar solved incidents were fixed, ranked by similarity and rating.

### Error handling

Clear messages, never stack traces, for: invalid login, unauthorised operation, invalid input (with the error shown next
to each field), invalid incident or state transition, technician unavailable, technician workload exceeded, service not
found, missing SLA configuration, duplicate incident, database failure and external service failure (SMS, e-mail, AI).
An SMTP or SMS failure never stops the incident operation itself.

### Security

BCrypt password hashing, signed login tokens (JWT, 8-hour expiry), role-based authorisation on every endpoint,
employees limited to their own incidents, internal notes hidden from employees, server-side input validation,
failed logins recorded in the audit log, and credentials read only from environment variables.

### Persistence

All data (users, technicians, services, categories, incidents, notes, SLA policies, violations, audit records,
notifications and settings) is stored in an H2 relational database through JPA, and survives restarts.

---

## Quick start

Requirements: **JDK 21 or newer** (developed with Temurin JDK 25) and an internet connection for the first build.
You do **not** need to install Maven or Node.js — the Maven Wrapper downloads Maven, and the build downloads its own Node.js.

```powershell
cd backend
.\mvnw.cmd package          # builds React UI + Spring Boot, runs 36 tests  (Linux/macOS: ./mvnw package)
java -jar target\itsm-backend-1.0.0.jar
```

Open **http://localhost:8080** and click one of the demo accounts on the login page.

| Username | Password | Role |
|---|---|---|
| `amit`, `neha`, `karan`, `isha` | `pass123` | Employee |
| `rahul` | `pass123` | Technician — Team Lead |
| `priya`, `arjun`, `sneha`, `vikram`, `meera` | `pass123` | Technician |
| `admin` | `admin123` | Administrator — IT Manager |
| `sysadmin` | `pass123` | Administrator — System Administrator |

The first start creates an H2 database in `backend/data/` and seeds services, categories, SLA policies, users
and ~20 realistic incidents (including a payment-gateway outage burst for the duplicate/correlation demo).
Delete `backend/data/` to start again from fresh demo data.

### Development mode (hot reload)

```powershell
# terminal 1 – API on :8080 (skip rebuilding the UI)
cd backend;  .\mvnw.cmd spring-boot:run -DskipFrontend=true
# terminal 2 – React dev server on :5173 (proxies /api to :8080)
cd frontend; $env:PATH = "$PWD\node;$env:PATH"; npm run dev
```

### Optional: real e-mail through Gmail

E-mail notifications are simulated unless SMTP credentials are provided. To send real e-mail, create a Gmail account
for the project, turn on 2-Step Verification, create an **App Password** at
[myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords), then start the app with:

```powershell
cd backend
powershell -ExecutionPolicy Bypass -File .\run-with-email.ps1 -GmailAddress you@gmail.com -RedirectTo you@gmail.com
```

The script asks for the App Password (hidden input) and keeps it only in memory. Credentials are read from the
`ITSM_MAIL_USERNAME` / `ITSM_MAIL_PASSWORD` environment variables and are never stored in the code or the repository.
Seeded demo users have `.local` addresses, which are skipped; `-RedirectTo` sends every notification to one inbox
(the original recipient is shown in the subject). Mail is sent on a background thread, so a slow SMTP server never
delays an incident operation. Administrators can check the channel with *Strategies & settings → Send a test e-mail*.

### Optional: LLM-assisted categorisation

Set `ITSM_AI_API_KEY` (and optionally `ITSM_AI_BASE_URL`, `ITSM_AI_MODEL`) to any OpenAI-compatible API.
Without a key the built-in keyword + history classifier is used, so the app works fully offline.

---

## Technology stack

| Layer | Technology | Why |
|---|---|---|
| Language | Java 21 | Strong static typing, records, sealed/pattern-matching features, the course language |
| Backend | Spring Boot 3.5 (Web, Data JPA, Validation, Scheduling) | Dependency injection keeps classes small and testable; industry standard |
| Persistence | H2 (file mode) via JPA/Hibernate | Zero-install relational DB; switching to PostgreSQL/MySQL is a config change |
| Security | BCrypt password hashing, JWT (jjwt), role-based interceptor | Stateless API authentication with role checks on every endpoint |
| Frontend | React 18 + Vite 5, React Router, Recharts | Component model, fast builds, charts for dashboards |
| Build | Maven Wrapper + frontend-maven-plugin | One command builds UI + API into one runnable jar |
| Testing | JUnit 5, Spring MockMvc, Playwright (UI) | Unit, integration and end-to-end browser tests |

---

## Requirement coverage

| Requirement (MVP) | Where |
|---|---|
| 3 user types (Employee, Technician, Administrator) with different GUIs | `domain/user`, `frontend/src/components/Layout.jsx` (role menus + colour themes) |
| Incident create / view / update / assign / resolve / close | `IncidentService`, `IncidentController`, `pages/IncidentDetail.jsx` |
| Priority from impact, urgency, business criticality (+ affected users) | `strategy/priority` — 2 strategies, switchable at runtime |
| ≥ 2 assignment strategies | `strategy/assignment` — skill, load, round-robin, smart (4) |
| SLA calculation, deadline tracking, warnings, violation detection | `SlaService`, `SlaMonitorService` (`@Scheduled` every 30 s) |
| ≥ 2 notification mechanisms | In-app, e-mail (real Gmail SMTP or simulated), SMS (third-party API via Adapter) |
| Complete audit history | `AuditTrailListener`, `/api/audit`, incident timeline |
| ≥ 5 search criteria | ID, keyword, status, priority, category, service, technician, SLA status, date range, open/unassigned |
| ≥ 2 intelligent features | 7, listed in the table below |
| Persistence | H2 database through Spring Data repositories |

| Intelligent feature | Class | Technique |
|---|---|---|
| Automatic categorisation | `KeywordCategoryClassifier`, `LlmCategoryClassifier` | Weighted ensemble of keyword matching, k-nearest-neighbour on past incidents and the service's default category; optional LLM |
| Duplicate detection | `DuplicateDetector`, `TextAnalyzer` | Term-frequency cosine similarity with synonyms and stemming |
| Incident correlation | `CorrelationAnalyzer` | Grouping open incidents by service and category within a time window |
| Automatic escalation | `SlaMonitorService` | Scheduled check of unacknowledged P1/P2 incidents against the response target |
| Smart technician recommendation | `SmartAssignmentStrategy` | Multi-factor weighted score with explanations |
| SLA risk prediction | `SlaRiskPredictor` | Remaining time vs. expected effort learned from historical resolution times |
| Suggested resolutions | `ResolutionSuggester` | Similar solved incidents ranked by similarity and user rating |

---

## Object-oriented design

| Concept / pattern | Example in code |
|---|---|
| **State** | `domain/incident/state/*` — each lifecycle status is a class that allows only legal actions; `NEW → CLOSED` throws `InvalidStateTransitionException` |
| **Strategy** | `PriorityStrategy` (matrix, weighted) and `AssignmentStrategy` (skill, load, round-robin, smart), held in a generic `StrategyRegistry<T>` |
| **Observer** | `IncidentEventPublisher` → `AuditTrailListener`, `NotificationListener` |
| **Factory** | `UserFactory` (Employee/Technician/Administrator), `NotificationSenderFactory` |
| **Adapter** | `ThirdPartySmsAdapter` wraps a vendor SMS client; `LlmCategoryClassifier` wraps an OpenAI-compatible API |
| **Template Method** | `AbstractNotificationSender.send()` — fixed algorithm, channel-specific `deliver()` |
| **Repository** | Spring Data repositories in `repository/` |
| **Facade** | `IntelligenceService` hides the five intelligence components |
| **Flyweight** | `IncidentStates` shares one stateless instance per status |
| Inheritance | `User` ← `Employee`, `Technician`, `Administrator` (JPA single-table) |
| Composition | `Incident` owns its `IncidentNote`s (cascade + orphan removal) |
| Aggregation / association | `Technician` ↔ skill `Category`s; `Incident` → `ItService`, reporter, technician |
| Encapsulation | Entities expose intention-revealing methods (`incident.resolve(...)`) instead of public setters |
| Exceptions | `ItsmException` hierarchy (11 types) mapped to HTTP codes by `GlobalExceptionHandler` |

---

## Project structure

```
backend/                      Spring Boot application (Java)
  src/main/java/com/itsm/
    domain/                   entities, enums, incident state machine
    strategy/                 priority & assignment strategies
    event/                    observer: publisher + listeners
    notification/             senders, factory, SMS adapter
    intelligence/             classifier, duplicates, correlation, SLA risk, resolutions
    service/                  business logic / use cases
    security/                 password hashing, JWT, role interceptor
    web/                      REST controllers, DTOs, error handling
    config/                   web config, demo data seeder
  src/test/java/              36 automated tests (TC01–TC32)
frontend/                     React single-page app
  src/pages/                  role pages (employee, technician, admin)
  src/components/             layout, tables, badges
  e2e/                        Playwright browser tests
docs/                         screenshots
```

---

## Testing

```powershell
cd backend;  .\mvnw.cmd test -DskipFrontend=true      # 36 unit + integration tests
cd frontend; $env:PATH = "$PWD\node;$env:PATH"
npm run e2e              # every page for every role renders without errors (server must be running)
npm run e2e:lifecycle    # report -> assign -> acknowledge -> start -> resolve -> verify -> feedback in the browser
```

| ID | Test case | Type |
|---|---|---|
| TC01 | Payment outage (high impact, critical urgency, criticality 5) is P1 | Normal |
| TC02 | Single-user low-urgency problem on a minor service is P4 | Normal |
| TC03 | Priority boundary scores map to the correct band (parameterised) | Boundary |
| TC04 | Weighted strategy raises priority when many users are affected | Business rule |
| TC05 | Full lifecycle NEW → … → CLOSED succeeds and records timestamps | Normal |
| TC06 | NEW → CLOSED is rejected | Invalid transition |
| TC07 | Technician cannot resolve before acknowledging and starting work | Invalid transition |
| TC08 | Pending-info round trip and reopen after resolution | Business rule |
| TC09 | Closed incidents allow no further actions | Exception |
| TC10 | Each state exposes only its legal actions | Business rule |
| TC11 | Feedback rating outside 1..5 is rejected | Invalid input |
| TC12 | Skill-based strategy picks the technician skilled in the category | Normal |
| TC13 | Load-based strategy picks the least busy technician | Normal |
| TC14 | Round robin rotates through technicians | Normal |
| TC15 | Smart strategy balances skill, workload, experience and explains its choice | Business rule |
| TC16 | No candidates → no selection (TechnicianUnavailable) | Exception |
| TC17 | Text analyser folds synonyms and stems words | Normal |
| TC18 | Locked account is classified as Authentication | Intelligent |
| TC19 | Outlook crash → Software; Teams audio drop is not Network-only | Intelligent |
| TC20 | Duplicate detector links the three payment reports, ignores unrelated ones | Intelligent |
| TC21 | Suggested resolution comes from the most similar solved incident | Intelligent |
| TC22 | SLA risk: overdue incident scores 100, fresh low-priority incident scores low | Intelligent |
| TC23 | Three open incidents on one service → suspected major incident | Intelligent |
| TC24 | SMS adapter converts local numbers to the vendor's international format | Adapter |
| TC25 | Invalid login → 401 INVALID_LOGIN | Security |
| TC26 | No token → 401; employee on admin endpoint → 403 | Authorization |
| TC27 | Invalid incident input → 400 with field errors | Invalid input |
| TC28 | Workflow: report → auto classify → P1 → SLA → assigned → audited & notified | Integration |
| TC29 | Invalid transition via API → 409; duplicate re-submission blocked | Exception |
| TC30 | Employees cannot see incidents reported by someone else | Authorization |
| TC31 | SMTP e-mail: real addresses sent, demo addresses skipped, redirect applied, SMTP failure doesn't break the operation | External service |
| TC32 | Scheduled SLA monitor saves its results: an overdue incident's violation is reported once, not on every run | Regression |

---

## Example workflows

Three end-to-end scenarios to try after starting the app:

1. **Employee reports → system prioritises → technician assigned → notified** — log in as `amit`, *Report an incident*
   on *Payment Gateway*; watch the live category suggestion and duplicate warning; open the incident to see P1,
   the SLA clock, the smart assignment and the audit trail. Check the bell icon / *Notification outbox* as `admin`.
2. **Technician investigates → SLA monitored → resolved → user notified** — log in as the assigned technician,
   *Acknowledge*, *Start work*, add an internal note, *Resolve*; then as the employee *Confirm it is fixed* and rate it.
3. **Duplicates / correlation / escalation** — as `rahul` open *SLA risk & correlation*: the three payment incidents
   are flagged as a suspected major incident; as `admin` click *Run SLA monitor now* on the SLA page to see violations
   and escalations appear in the audit log.

## Security notes

Passwords are stored as BCrypt hashes; the API is stateless with signed JWTs (8 h expiry); every endpoint is checked
by `AuthInterceptor` + `@RequiresRole`; employees only see their own incidents and never see internal notes;
all input is validated with Bean Validation; errors never expose stack traces. For production, set
`ITSM_JWT_SECRET`, serve over HTTPS and switch to a server database.

## API overview

The React frontend talks to the backend only through this REST API (JSON over HTTP, with the login token sent in the
`Authorization: Bearer` header), so any other client — a mobile app, a script or another system — could use it too.
The main endpoints:

| Method & path | Purpose |
|---|---|
| `POST /api/auth/login`, `/register`; `GET /api/auth/me` | Authentication |
| `GET/POST /api/incidents` | Search (10 filters) / report |
| `POST /api/incidents/{id}/assign · take · acknowledge · start · notes · request-info · provide-info · resolve · verify · reopen · close · duplicate · feedback · reprioritise · recategorise` | Lifecycle |
| `GET /api/incidents/{id}/recommendations · suggested-resolutions · duplicates` | Intelligence per incident |
| `POST /api/intelligence/classify · duplicates`; `GET /correlations · sla-risks` | Intelligence |
| `GET/POST/PUT /api/users · categories · services · sla/policies · settings` | Administration |
| `GET /api/reports/overview · workload`, `/api/audit`, `/api/sla/violations`, `/api/notifications` | Reporting |
