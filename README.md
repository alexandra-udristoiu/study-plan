# StudyPlan — Android notes & spaced-repetition flashcard app

Android study app for taking notes and reviewing them as
flashcards with spaced repetition, so you revisit each card right before you'd
forget it.

## Features

- **Notes** organised by topic — create, edit, filter, and browse.
- **Flashcards** generated from a note and reviewed in focused sessions.
- **AI flashcard generation** *(requires the companion backend)* — auto-generate
  flashcards from a note's title, summary, and content.
- **PDF text import** *(requires the companion backend)* — pick a PDF and pull its
  extracted text straight into a note's content.
- **Spaced repetition (SM-2)** — cards are scheduled with the SuperMemo-2
  algorithm: easy cards reappear less often, hard ones come back sooner.
- **AI summaries** *(optional, requires the companion backend)* — summarise a
  note and accept or discard the result.
- **Offline-first** — notes and schedules are stored on-device with Room.

## Screenshots

| Notes | Flashcard | Study note |
|:-----:|:---------:|:----------:|
| <img src="screenshots/notes.PNG" width="220" alt="Notes list organised by topic"> | <img src="screenshots/flashcard.PNG" width="220" alt="Flashcard review session"> | <img src="screenshots/studynote.PNG" width="220" alt="An individual study note"> |

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

The AI and PDF features call a small companion backend (`/api/summarize`,
`/api/generate-flashcard`, `/api/extract-pdf`). Without it, the rest of the app —
notes, manual flashcards, and spaced-repetition review — works fully offline.

## Roadmap

- [x] **AI flashcard generation** — auto-generate flashcards from a note.
- [x] **PDF text import** — pick a PDF and pull its extracted text into a note.
- [ ] Single immutable `UiState` exposed via `StateFlow` + `collectAsStateWithLifecycle()`.
- [ ] Reactive data layer — Room `Flow` → repository → `stateIn`.
- [ ] Migrate manual DI to Hilt.
- [ ] Unit tests for SM-2 scheduling; Compose UI tests for the review flow.
- [ ] Wire up and deploy the summarisation backend.
