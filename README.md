# MoneyBear (Rahakaru)

Offline-first personal expense tracker for Android built with Kotlin, Jetpack Compose, and Firebase.

## Getting Started

1. Install Android Studio Ladybug (or newer) with JDK 21.
2. Place your Firebase `google-services.json` under `app/`.
3. Sync Gradle and run the `app` configuration on an API 26+ device.

### Build Config

- `compileSdk`: 36
- `targetSdk`: 36
- `minSdk`: 26
- Kotlin 2.0.21, AGP 8.7.0

## Architecture Overview

- `data/local` – Room database (`Transaction`, DAO, converters, `AppDb`).
- `data/store` – `SettingsStore` backed by DataStore for language, currency, default categories, sync metadata.
- `data/remote` – Firestore stubs (`RemoteService`, `TransactionDTO`, `FirestoreService`).
- `domain` – Business logic (`TransactionRepository`, `SyncRepository`, `IdUtils`, `TimeUtils`).
- `ui` – Jetpack Compose navigation/screens (Home, Add/Edit, Settings, Paywall; Login untouched).
- `worker` – WorkManager scheduling (`SyncWorker`, `SyncScheduler`).
- `billing` – Stubbed Google Play Billing integration.

## Firebase Data Structure

Every authenticated Google user owns a separate document in Firestore. The structure is created and kept up to date when the app syncs:

```
users/{uid}
  uid: string
  email: string?
  displayName: string?
  photoUrl: string?
  createdAt: server_timestamp
  updatedAt: server_timestamp
  lastSeenAt: server_timestamp
  lastSyncAt: epoch_millis
  transactions/{transactionId}
    id, uid, amount, currency, dateUtcMillis, monthKey, category, note?,
    planned, deleted, type, updatedAtServer, savingsGoalId?, savingsImpact
  settings/{...}          # reserved for future synced settings
  savingsGoals/{...}      # reserved for future synced savings data
  auditLog/{...}          # reserved for future forensic events
```

The Firebase Auth UID attached to the Google account is the authoritative key; logging in from another device automatically resolves the same document tree.

## Current Behaviour

- Google Sign-In via Firebase (existing implementation reused).
- Sync bootstrap ensures a per-user Firestore document exists with profile metadata and the latest sync timestamps before any remote read/write.
- Transactions track a `TxType` (expense or income); Add/Edit offers quick tabs with per-type category defaults.
- Home shows monthly net totals, a stacked category breakdown, a four-month net cashflow forecast, and the transaction list.
- Settings exposes language/currency toggles, editable default categories, manual sync trigger, and sign-out.
- WorkManager schedules a daily unmetered sync and supports manual one-shot syncs; remote/billing flows remain stubbed.

## TODO / Next Steps

1. Extend Firestore syncing to cover DataStore-backed preferences and savings goals.
2. Add Cloud Functions (monthly summaries, purchase verification) once backend is ready.
3. Replace the billing stub with the real Google Play Billing flow and entitlement handling.

## Notes

- Login-related files and `google-services.json` remain untouched as requested.
- Strings are localized for English and Estonian; a simple restart accommodates language changes.
- Keep secrets (keystores, passwords) outside the codebase — use environment variables.
