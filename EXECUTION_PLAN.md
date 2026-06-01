# CardDemo Modernization — Program Execution Plan

> Companion to [`MODERNIZATION_ROADMAP.md`](./MODERNIZATION_ROADMAP.md). The roadmap defines **what** to modernize and the P0–P3 prioritization; this document defines **how to run the program** — operating model, cadence, per‑phase activities with entry/exit gates, governance, risk controls, KPIs, and rollback strategy.

## 1. Guiding principles

1. **Strangler Fig** — legacy (COBOL/CICS/VSAM) and modern run in parallel behind a façade; cut over capability‑by‑capability, never big‑bang.
2. **Data before logic** — migrate data (Phase 2) and build the test safety net (Phase 0) *before* converting posting/interest logic (Phase 3).
3. **Prove value early, defer risk** — ship low‑risk, high‑value items first (read APIs, CI/CD, security); leave the highest‑risk financial programs until tests + modern DB exist.
4. **Every change is reversible** — legacy path remains the fallback until parity is proven in production‑shadow mode.

## 2. Operating model (team topology)

A thin shared platform plus vertically‑sliced domain squads, mirroring the repo's natural seams.

| Squad | Owns | Key CardDemo artifacts |
|---|---|---|
| **Platform / Enablement** | AWS landing zone, CI/CD, IaC, COBOL build/test harness, data‑replication backbone | `samples/m2`, JCL build procs |
| **Data** | VSAM→PostgreSQL schema, ETL, dual‑write/CDC, reconciliation | 10 VSAM KSDS, `CV*` copybooks |
| **Domain: Customer/Account/Card** | inquiry + maintenance services | `COACTVWC`, `COACTUPC`, `COCRDLIC/SLC/UPC`, `CVCUS01Y`, `CVACT0xY` |
| **Domain: Transactions/Posting** | txn capture + nightly financial batch | `COTRN0xC`, `CBTRN02C` (POSTTRAN), `CBACT04C` (INTCALC), `CVTRA0xY` |
| **Domain: Admin/Security/Reporting** | user mgmt, reports, bill pay | `COADM01C`, `COUSR0xC`, `CORPT00C`, `COBIL00C`, `CSUSR01Y` |

**Embedded SME pairing:** 1 mainframe SME (COBOL/CICS/JCL) per domain squad alongside cloud engineers. This is the single biggest de‑risker — the business rules in `COACTUPC` (4,236 LOC) and `CBTRN02C` are documented nowhere but the code.

## 3. Cadence & sequencing (overlapping waves, not waterfall)

Phase 0 fully precedes everything. Phases 1–2 overlap. Phase 3 starts **per‑domain** only once that domain's data is migrated and its golden‑master tests are green.

| Phase | Focus | Runs in parallel with |
|---|---|---|
| **0 — Assessment & harness** | discovery, golden‑master tests, AWS landing zone | — (must finish first) |
| **1 — Quick wins** | read‑only REST APIs, CI/CD, IaC, security, trivial batch | end of 0 / start of 2 |
| **2 — Data** | VSAM→PostgreSQL, dual‑write, CDC, reconciliation | 1 and 3 |
| **3 — Apps** | COBOL→JVM, program‑by‑program | per‑domain, after that domain's data cutover |
| **4 — Cloud‑native** | microservices, SQS/SNS, Step Functions, React UI, decommission | tail end |

## 4. Per‑phase execution detail

### Phase 0 — Foundation
- Re‑host on AWS Mainframe Modernization (M2) using existing `samples/m2` artifacts to get a runnable baseline to test against.
- Build the **golden‑master harness**: capture sample inputs (`app/data` VSAM/EBCDIC) and byte‑for‑byte outputs for `POSTTRAN`/`INTCALC` and the online flows as the regression oracle.
- Stand up AWS landing zone, accounts, networking, secrets, and observability baseline via IaC.
- **Exit gate:** baseline runs in M2; golden‑master tests reproduce known outputs; landing zone provisioned.

### Phase 1 — Quick wins
- Expose **read‑only REST APIs** over existing data (account view, card list/view, transaction list/view) — no core‑logic change.
- CI/CD pipeline + IaC for everything (none exists in the repo today); structured logging.
- **Security:** replace plaintext `CSUSR01Y`/`USRSEC` passwords + shared demo creds with hashed creds + RBAC; encrypt PII (`CUSTDAT` SSN/DOB/FICO, `CARDDAT` PAN).
- Convert trivial batch print programs (`CBACT01C/02C/03C`, `CBCUS01C`) as a low‑risk conversion proof‑of‑concept.
- **Exit gate:** APIs live behind façade; pipeline deploys to AWS; secrets/PII no longer plaintext.

### Phase 2 — Data
- Build PostgreSQL/Aurora schema from `CV*` copybooks (fixed‑point money → `NUMERIC`, **never float**; COMP‑3 parsing).
- ETL: REPRO → parse → load → validate; rebuild AIX (`CARDAIX`, `CXACAIX`) as indexes/FKs; lift already‑relational DB2 tables directly.
- Stand up **dual‑write + CDC** so legacy VSAM and Postgres stay in sync; run a daily **reconciliation** comparing balances/counts.
- **Exit gate:** reconciliation clean for N consecutive days; reads servable from Postgres.

### Phase 3 — Application
Convert program‑by‑program, **simplest→hardest**, each gated by the Phase 0 golden‑master tests:
1. `COTRN02C` (add txn), `COUSR0x` (user CRUD) — simple
2. `COCRDLIC`, `COACTVWC` — read/inquiry
3. `CBTRN02C` (POSTTRAN), `CBACT04C` (INTCALC) — core financial, highest risk
4. `COACTUPC` (4,236 LOC) — **decompose first** into smaller services before converting

Route each converted capability through the Strangler façade; legacy path stays as fallback until parity is proven.
- **Exit gate (per program):** golden‑master parity + dual‑run agreement in production‑shadow mode.

### Phase 4 — Cloud‑native
- Microservice decomposition along domain seams; MQ → SQS/SNS/EventBridge; IMS migration.
- Re‑implement the batch DAG (CLOSEFIL → file loads → POSTTRAN → INTCALC → statements) on **Step Functions or Spring Batch** with modern scheduling/monitoring.
- React/Angular UI replacing the 21 BMS screens, consuming the Phase 1+ APIs.
- Decommission CICS/VSAM once all traffic is off legacy.

## 5. Governance & risk gates

- **Stage gates** between phases with explicit entry/exit criteria (above); no domain advances to Phase 3 until its data reconciles.
- **Definition of Done** for any converted program: golden‑master parity, dual‑run agreement, observability in place, documented rollback path.
- **Active risk register** — top items carried from the roadmap:

| Risk | Mitigation |
|---|---|
| Financial‑precision drift | fixed‑point `NUMERIC`; golden‑master parity; dual‑run reconciliation |
| Multi‑file atomicity in posting | transaction‑coordinator design; verify all‑or‑nothing in tests |
| Hidden business rules | discovery phase + golden‑master tests capture behavior before conversion |
| PII / plaintext creds | hash creds + RBAC + encrypt PII in Phase 1 (pulled early) |
| Date handling | Y2K‑style audit + comprehensive date tests |
| Dual‑run consistency | monitoring, alerting, daily reconciliation breaks → zero |
| Scheduler fidelity | JCL→Step Functions mapping verified against batch DAG order |
| Missing tests | comprehensive golden‑master suite built in Phase 0 |

## 6. Success metrics (KPIs)

- % programs converted with passing parity tests; % data tables migrated with clean reconciliation.
- Dual‑run divergence / defect‑escape rate trending to zero.
- Lead time for change + deployment frequency (proving CI/CD value).
- Legacy MIPS / VSAM footprint retired over time (the cost‑savings signal).

## 7. Rollback strategy

- **Phase 1:** façade is additive; disable the API route to revert. Security changes are backward‑compatible (dual‑read hashed/plaintext during transition).
- **Phase 2:** legacy VSAM remains system of record until cutover; dual‑write means Postgres can be dropped without data loss. Reads flipped back via config flag.
- **Phase 3:** Strangler façade routes each capability; flip the route back to the legacy COBOL path on any parity break. No converted program goes live without a tested fallback.
- **Phase 4:** decommission only after a sustained clean dual‑run window; keep legacy recoverable until the window closes.

## 8. The one non‑negotiable

Never convert the posting/interest logic (`CBTRN02C`, `CBACT04C`) until **both** the golden‑master harness (Phase 0) and migrated data (Phase 2) are in place — that is where financial‑correctness risk concentrates.
