# StudyPlan

Android study app for taking notes and reviewing them as
flashcards with spaced repetition, so you revisit each card right before you'd
forget it.

## Features

- **Notes** organised by topic — create, edit, filter, and browse.
- **Flashcards** generated from a note and reviewed in focused sessions.
- **Spaced repetition (SM-2)** — cards are scheduled with the SuperMemo-2
  algorithm: easy cards reappear less often, hard ones come back sooner.
- **AI summaries** *(optional, requires the companion backend)* — summarise a
  note and accept or discard the result.
- **Offline-first** — notes and schedules are stored on-device with Room.

## Tech stack

Kotlin · Jetpack Compose · Material 3 · Navigation-Compose · Coroutines · Room (KSP)
· Retrofit/OkHttp · MVVM + repository pattern

## Architecture

```
ui/        Compose screens + ViewModels
domain/    Business logic — spaced-repetition scheduling, summarisation
data/      Repositories, Room entities/DAOs, remote API
```

Scheduling is decoupled from any one algorithm: `CardSchedule` defines when a card
is due, `Sm2CardSchedule` implements SuperMemo-2, and a `CardScheduleFactory`
rebuilds it from a stored payload. Each card persists an opaque state payload plus
a promoted `due` date — so "which cards are due today?" stays a simple query while
the algorithm can change without a database migration.

## Build & run

Requires Android Studio (recent stable) and a device/emulator on **API 26+**.

```bash
git clone <your-repo-url>
cd StudyPlan
./gradlew installDebug   # or open in Android Studio and Run
```

The AI-summary feature calls a small companion backend (`POST /api/summarize`).
Without it, the rest of the app works fully offline.

## Roadmap

- [ ] **AI flashcard generation & PDF import** — upload a PDF, extract its text, and
  auto-generate flashcards (and notes) from the content.
- [ ] Single immutable `UiState` exposed via `StateFlow` + `collectAsStateWithLifecycle()`.
- [ ] Reactive data layer — Room `Flow` → repository → `stateIn`.
- [ ] Migrate manual DI to Hilt.
- [ ] Unit tests for SM-2 scheduling; Compose UI tests for the review flow.
- [ ] Wire up and deploy the summarisation backend.
