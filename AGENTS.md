# AGENTS.md — MewBook Project Guide

## Project Snapshot

- MewBook（喵喵记账）是单模块 Android 应用，模块名为 `app`。
- 技术栈：Kotlin、Jetpack Compose、Material 3、Navigation Compose、Room、Hilt、DataStore、OkHttp、kotlinx-serialization。
- 当前构建事实来自 `app/build.gradle.kts`：`minSdk = 26`、`targetSdk = 34`、`versionCode = 9`、`versionName = "1.0.8"`。
- 当前 Room 数据库事实来自 `MewBookDatabase.kt`：数据库名 `mewbook.db`，版本 `4`，`exportSchema = false`。

## Standard Commands

Windows:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
.\gradlew.bat testDebugUnitTest :app:assembleDebug :app:lintDebug
```

Unix-like shells:

```bash
./gradlew testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
./gradlew testDebugUnitTest :app:assembleDebug :app:lintDebug
```

## Architecture Rules

- Keep UI in `ui/screens`, shared Compose elements in `ui/components`, navigation in `ui/navigation`, and visual system code in `ui/theme`.
- Keep durable business rules in `domain/policy` when they can be unit tested outside Compose.
- Keep repository interfaces in `domain/repository` and implementations in `data/repository`; bind implementations in `di/RepositoryModule.kt`.
- Keep Room entities and DAOs under `data/local`; update migrations, backup models, and migration tests whenever the schema changes.
- Reuse existing Compose patterns: `MewCompactTopAppBar`, shared period controls, `RecordItem`, clay theme helpers, and Hilt `@HiltViewModel`.

## Current Feature Surface

- Home: add/edit/delete records, quick entry, record details, inline search, day/week/month period controls, optional overview cards.
- Statistics: weekly/monthly/yearly charts, income/expense summaries, category breakdown, and category expense drilldown with record detail view, edit, and delete (transaction-safe via `database.withTransaction`).
- Assets and ledgers: account CRUD, ledger management, default ledger behavior, manual ordering, per-ledger default account initialization (`EnsureDefaultAccountForLedgerUseCase`).
- Categories and budgets: category management, total budgets, category budgets, multiple budget period types.
- Recurring templates: fixed income/expense templates with schedule policy and usage guidance.
- Migration and data exchange: local backup/restore, CSV/JSON export, external import preview, smart import.
- Sync and update: WebDAV sync/backup flows with server config dialog, manual DAV export with optional custom file names (auto-prefixed `manual_`), manual remote backup selection for restore, daily first-open DAV auto-backup with single retry and 60s prune timeout, import safety backup to local storage, GitHub Release update check with snooze/disable support, APK download, and installer handoff.

## Safety Notes

- Do not commit `keystore.properties`, `signing/`, local SDK settings, `.omx/`, `.planning/`, `.agents/`, or generated build outputs unless the user explicitly changes repository policy.
- `REQUEST_INSTALL_PACKAGES` exists for in-app APK update installation; keep related UX and permission handling explicit.
- Smart import stores API configuration through encrypted preferences when available; avoid logging API keys or raw sensitive input.
- WebDAV credentials and URLs should stay redacted in logs and docs. All `Log.d()` calls in `DavClient` and `DavSettingsViewModel` are guarded with `BuildConfig.DEBUG` to prevent URL/credential leakage in release builds.
- `DavConfig.isInsecure()` detects plain HTTP WebDAV URLs; the DAV settings UI shows a warning when HTTP is used without localhost.
- The user rule is active: use Context7 for library/API documentation, code generation, setup, or configuration work when docs are needed.

## Knowledge Files

- `README.md` is the human-facing quick-start and feature overview.
- `.planning/codebase/*.md` is the current codebase map for agents.
- `.planning/PROJECT.md`, `.planning/ROADMAP.md`, `.planning/REQUIREMENTS.md`, and `.planning/STATE.md` track GSD planning state.
- `docs/superpowers/specs/*.md` are historical UI/interaction specs from April 2026.
- `implementation_plan.md` is an archived historical implementation note, not the active task plan.
