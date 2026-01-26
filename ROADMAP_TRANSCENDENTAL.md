# The Transcendental Roadmap: Simplicity & Power

> "Simplicity is prerequisite for reliability." — Edsger W. Dijkstra (Conceptually aligned with Rich Hickey)

This roadmap outlines the evolution of Alakey, leveraging **Java 25**, **Android API 36**, and the principles of **Simple Architecture**.

## 1. Epochal Time Model (State Management)
**Principle**: *State is a value at a point in time.*
We currently use `StateFlow` (functional reactive). We will double down on this by utilizing **Java 25 Records** (where possible in the hierarchy) or Kotlin Value Classes to ensure that every emitted state is a pure, immutable snapshot.

*   **Action**: Refine `PodcastEntity` to be purely immutable.
*   **Action**: Ensure `AppViewModel` never mutates internal state directly, but strictly processes inputs to produce new state values (The "Function of State" approach).

## 2. Virtualized Concurrency (Java 25 / Loom)
**Principle**: *Do not complect logic with threading mechanics.*
With **Java 25**, we have access to **Virtual Threads** (Project Loom). While Kotlin Coroutines are powerful, Virtual Threads offer a "Simple" generic way to handle blocking IO without the "color" of suspend functions in the pure Java layers.

*   **Objective**: Investigate replacing the `OkHttp` thread pool in `UniversalRepository` with a Virtual Thread Executor. This makes network calls simple, blocking code (easy to reason about) that behaves asynchronously (efficient).

## 3. The Queue is Truth (Durable Execution)
**Principle**: *Processes are ephemeral; Data is forever.*
We use Room as the single source of truth. We will enhance this by treating every user intent (Download, Subscribe, Play) as a **Durable Event** stored in the database before it is processed.

*   **Action**: Create an `EventLog` table.
*   **Action**: The `FeedSyncWorker` becomes a pure function: `EventLog -> Network -> Database Update`.

## 4. Privacy as Decoupling (API 36)
**Principle**: *Access only what you need (Principle of Least Privilege).*
API 36 enforces privacy. We embrace this not as a constraint, but as a simplification. By removing broad storage permissions, we decouple our app from the global file system state.

*   **Action**: Fully adopt the **Photo Picker** for any future cover-art customization (Zero Permissions).
*   **Action**: Use **Predictive Back** (API 36) to model navigation as a reversible pure function.

## 5. Simplicity by Removal (Deleting RssParser)
**Principle**: *Complexity is cost. Fragility is failure.*
The current `RssParser` is a custom, fragile state machine. It is "complected" with the specifics of XML.

*   **Action**: **DELETE** `RssParser`.
*   **Strategy**: Replace with a rigorous, standard library (like ROME or a focused version of XmlPullParser wrapped in a function) that returns immutable Records directly.
*   **Goal**: The "Parser" should not exist as a component; parsing should be a pure function: `String -> List<PodcastEntity>`.

## 6. De-complecting Components (Rich Hickey Protocol)
**Principle**: *Separate Policy from Mechanism.*

### AudioService (The Engine)
*   **Current State**: Complected. Mixes foreground service notification logic, media session handling, and playback state.
*   **Future**: Decompose into:
    *   `NotificationPolicy` (Pure logic: State -> Notification)
    *   `PlaybackMechanism` (ExoPlayer wrapper)
    *   `SessionManager` (Media3 glue)

### GlassSystem (The View)
*   **Current State**: Complected. Mixes rendering (Shaders) with physics (Gestures).
*   **Future**:
    *   Treat `GlassSystem` as a pure projection of `UiState`.
    *   Gestures produce `Events`, they do not mutate View State directly.

## 5. Tangible Liveness ("Flux")
**Principle**: *The system should look like it works.*
Rich Hickey values "Completeness". In UI, this means the feedback loop is complete. New API 36 graphics capabilities (if available) or Canvas 2D updates will be used to make the "Flux" background reactive to audio amplitude.

*   **Goal**: Visualize the *Process*, not just the *Result*.
