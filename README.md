# FirstClub Membership Service

Backend for a tiered membership program. Users pick a plan (Monthly, Quarterly, Yearly) and a tier (Silver, Gold, Platinum), can upgrade, downgrade or cancel, and can see their current membership and expiry. Tiers are earned through order count, monthly order value or cohort membership.

**In scope:** plan/tier/benefit catalog, eligibility rules, the membership lifecycle, and correctness under concurrent requests.
**Out of scope:** payments, auto-renewal, authentication, and computing order stats (the order system would own that; here it is pushed in through a profile endpoint).

## Running

Requires Java 17. The Maven wrapper is included.

```bash
./mvnw spring-boot:run
./mvnw test
```

If your default `java` is not 17, point `JAVA_HOME` at a JDK 17 first, e.g. `export JAVA_HOME=$(/usr/libexec/java_home -v 17)` on macOS.

The service listens on `http://localhost:8080`. All data is in memory and resets on restart.

**Stack:** Spring Boot 3.5 (web, validation), Lombok, JUnit 5 / MockMvc.

## Design

```
com.firstclub.membership
├── eligibility   rules and the member profile they are evaluated against
├── catalog       plans, tiers, benefits (loaded from application.yml)
├── membership    the Membership entity, repository, service and API
└── common        Clock, error codes, exception handler
```

Dependencies point one way: `membership → catalog → eligibility`.

| Class | Responsibility |
|---|---|
| `MembershipPlan` | Price and duration (`Period`, e.g. `P1M`). |
| `MembershipTier` | Rank, benefits and the `EligibilityRule` a user must satisfy. |
| `Benefit` | Perk type, description and optional value (e.g. discount percentage). |
| `Catalog` | Builds plans and tiers from config, validates them at startup, and finds a user's highest eligible tier. |
| `EligibilityRule` | `OrderCountAtLeastRule`, `MonthlyOrderValueAtLeastRule`, `CohortRule`, and the composites `AllOfRule` / `AnyOfRule`. |
| `MemberProfile` | Order count, monthly order value and cohorts for a user. |
| `Membership` | Immutable entity: user, plan, tier, price paid, start, expiry, cancellation time, version. |
| `MembershipService` | Subscribe, change tier, cancel, re-evaluate tier. |

Plans and tiers are separate on purpose: the plan decides how long and how much, the tier decides which perks. Changing tier never changes the plan, price paid or expiry.

`Membership` is immutable. `withTier` and `cancel` return a new instance with `version + 1`. Status is not stored; it is derived from timestamps and the injected `Clock`:

- `CANCELLED` if `cancelledAt` is set
- `ACTIVE` before `expiresAt`
- `EXPIRED` from `expiresAt` onwards

`expiresAt` is computed once at subscription as `startedAt + plan duration` in the business time zone (Asia/Kolkata), so a monthly plan started on 31 Jan expires on 28/29 Feb. No scheduler is needed to expire memberships.

## Catalog configuration

Plans, tiers and benefits live in [`application.yml`](src/main/resources/application.yml) under `membership.catalog`:

```yaml
tiers:
  - code: GOLD
    name: Gold
    rank: 2
    eligibility:
      match: ALL                     # ALL (default) or ANY
      min-order-count: 5
      min-monthly-order-value: 10000
    benefits:
      - type: EXTRA_DISCOUNT
        description: Extra discount on selected categories
        value: 5
```

Each configured condition becomes one rule, combined with `match`. A tier without `eligibility` is open to everyone. The catalog is validated at startup: unique plan codes, tier codes and ranks, and the lowest-ranked tier must accept a user with no history. A bad catalog stops the app from starting.

Seeded tiers:

| Tier | Rank | Eligibility |
|---|---|---|
| SILVER | 1 | everyone |
| GOLD | 2 | at least 5 orders **and** monthly order value ≥ 10000 |
| PLATINUM | 3 | monthly order value ≥ 30000 **or** cohort `VIP` |

## Eligibility: the ceiling model

A user's **ceiling** is the highest-ranked tier whose rule they satisfy. They may hold the ceiling tier or anything below it.

- Subscribe and tier change both require `target rank ≤ ceiling rank` (422 otherwise).
- Downgrades within the ceiling are always allowed.
- `POST .../tier-evaluation` moves the user to their ceiling, up or down. In production this would be triggered by a job or order events rather than an API call.

The ceiling matters because tier rules are not necessarily nested. A VIP with no orders qualifies for Platinum but not Gold's own rule; under the ceiling model they can still downgrade from Platinum to Gold.

## API

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/plans` | List plans |
| GET | `/api/tiers?userId=` | List tiers; `userId` (optional) adds an `eligible` flag per tier |
| PUT / GET | `/api/users/{userId}/profile` | Set or read the user's order stats and cohorts |
| POST | `/api/users/{userId}/membership` | Subscribe |
| GET | `/api/users/{userId}/membership` | Latest membership with effective status and expiry |
| PUT | `/api/users/{userId}/membership/tier` | Upgrade or downgrade |
| POST | `/api/users/{userId}/membership/cancel` | Cancel |
| POST | `/api/users/{userId}/membership/tier-evaluation` | Move to the highest eligible tier |

Set a profile, then subscribe:

```bash
curl -X PUT localhost:8080/api/users/u1/profile -H 'Content-Type: application/json' \
  -d '{"orderCount": 6, "monthlyOrderValue": 12000, "cohorts": ["EARLY_ADOPTER"]}'

curl -X POST localhost:8080/api/users/u1/membership -H 'Content-Type: application/json' \
  -d '{"planCode": "QUARTERLY", "tierCode": "SILVER"}'
```

```json
{
  "id": "58023bd5-b0a1-438c-8625-43c6e1a1f745",
  "userId": "u1",
  "status": "ACTIVE",
  "planCode": "QUARTERLY",
  "planName": "Quarterly",
  "tierCode": "SILVER",
  "tierName": "Silver",
  "pricePaid": { "amount": 799, "currency": "INR" },
  "startedAt": "2026-10-05T09:25:47.068Z",
  "expiresAt": "2027-01-05T09:25:47.068Z",
  "cancelledAt": null,
  "benefits": [{ "type": "FREE_DELIVERY", "description": "Free delivery on eligible orders" }],
  "version": 0
}
```

Upgrade, then try a tier above the ceiling:

```bash
curl -X PUT localhost:8080/api/users/u1/membership/tier -H 'Content-Type: application/json' -d '{"tierCode": "GOLD"}'
# 200, tierCode GOLD, same expiresAt and pricePaid, version 1

curl -X PUT localhost:8080/api/users/u1/membership/tier -H 'Content-Type: application/json' -d '{"tierCode": "PLATINUM"}'
```

```json
{
  "type": "about:blank",
  "title": "Unprocessable Entity",
  "status": 422,
  "detail": "Tier PLATINUM is above the highest tier user u1 qualifies for (GOLD)",
  "instance": "/api/users/u1/membership/tier",
  "code": "TIER_NOT_ELIGIBLE"
}
```

Cancel:

```bash
curl -X POST localhost:8080/api/users/u1/membership/cancel
# 200, status CANCELLED, cancelledAt set, benefits []
```

`GET .../membership` returns the latest membership whatever its status, so an expired or cancelled one still shows when it ended. Benefits are only listed while the membership is `ACTIVE`.

Errors use Spring's `ProblemDetail` with a `code` field:

| Status | Codes |
|---|---|
| 400 | `INVALID_REQUEST` (with `fieldErrors`) |
| 404 | `PLAN_NOT_FOUND`, `TIER_NOT_FOUND`, `MEMBERSHIP_NOT_FOUND` |
| 409 | `ACTIVE_MEMBERSHIP_EXISTS`, `MEMBERSHIP_NOT_ACTIVE`, `CONCURRENT_MODIFICATION` |
| 422 | `TIER_NOT_ELIGIBLE` |

## Concurrency

The invariants are: at most one active membership per user, and no lost updates. Both are enforced in the repository, which keeps each user's latest membership in a `ConcurrentHashMap` and does every write through `compute()`, so the check and the write are atomic for that user.

- **Subscribe** uses `insertIfNoActive`: inside `compute()`, if the existing membership is still active it throws `ACTIVE_MEMBERSHIP_EXISTS`, otherwise it stores the new one. Concurrent subscribes for the same user produce exactly one success.
- **Tier change, cancel, re-evaluation** use optimistic locking: the service reads the membership, builds the new version, and calls `update(changed, expectedVersion)`. The write only succeeds if the stored membership has the same id and is still at the expected version; otherwise the client gets 409 `CONCURRENT_MODIFICATION` and can reload and retry.

Rule evaluation and catalog lookups happen before the atomic section, so `compute()` only does the compare-and-swap. There is no global lock; contention is per user key. Catalog and domain objects are immutable, so reads need no locking. Profile updates are last-write-wins.

There are no retries; the service reports the conflict and lets the caller decide.

## Assumptions

- A user with no stored profile is treated as having 0 orders, 0 order value and no cohorts, which qualifies them for the base tier only.
- Order count eligibility is "at least X" (`>=`). The brief says "more than X", which is configured as X + 1.
- Cohort matching is exact and case-sensitive.
- One active membership per user. A new subscription is allowed once the previous one has expired or been cancelled.
- Cancellation is immediate. There is no refund or cancel-at-period-end.
- Tiers do not change the price; the plan sets the price.
- Changing plan means cancelling and subscribing again.
- `userId` is taken from the path; authentication is out of scope.

## Tests

37 tests, run with `./mvnw test`:

- Unit tests for eligibility rules, catalog validation and ceiling selection, membership expiry and state transitions, and the repository's atomic insert and version checks.
- API tests that run the full Spring context against the real `application.yml`, with a mocked `Clock` to cover expiry.
- Concurrency tests that start 16 threads at once to show only one subscription succeeds, that stale-version updates are rejected, and that concurrent cancels take effect once.

## Production evolution

- **Persistence.** Replace the in-memory repositories with a relational database (e.g. Postgres) behind the same interfaces. The service and controllers do not change.
- **Optimistic locking.** Map `Membership.version` to JPA `@Version`, so updates become `UPDATE ... WHERE id = ? AND version = ?` and a stale write fails the same way it does now.
- **One active membership per user.** Expiry depends on the current time, so it cannot be a static index predicate. Instead, keep a `current_membership` row per user (`user_id` primary key, pointing to the membership). Subscribing runs in one transaction: lock or version-check that row, confirm the referenced membership is no longer active, insert the new membership and repoint the row. The primary key makes concurrent first-time subscribes collide in the database.
- **Membership history.** Keep every membership and tier change as rows instead of only the latest one, for support and auditing.
- **Idempotency.** Once subscribing involves payment, accept an `Idempotency-Key` on subscribe so a retried request does not charge or subscribe twice.
- **Tier re-evaluation.** Drive it from order events or a scheduled job instead of the API endpoint, and feed the profile from the order and CRM systems.
- **Observability.** Structured logs with user and membership ids, metrics for subscriptions, tier changes and 409 conflicts, and request tracing.
- **Catalog.** Move plans and tiers to an admin-managed store if they need to change without a deploy. Existing memberships already keep the price they paid.
