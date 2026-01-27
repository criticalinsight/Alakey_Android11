# The Transcendental Roadmap: Simplicity & Power
# [STATUS: COMPLETED]

> "Simplicity is prerequisite for reliability." — Edsger W. Dijkstra (Conceptually aligned with Rich Hickey)

This roadmap outlines the evolution of Alakey, utilizing **Java 25**, **Android API 36**, and the principles of **Simple Architecture**.

## 1. Epochal Time Model (State Management) [COMPLETED]
**Principle**: *State is a value at a point in time.*
We currently use `StateFlow` (functional reactive). We utilize **Java 25 Records** (conceptually via Kotlin Data Classes) to ensure that every emitted state is a pure, immutable snapshot.

*   **Action**: Refine `PodcastEntity` to be purely immutable. [DONE]
*   **Action**: Ensure `AppViewModel` never mutates internal state directly, but strictly processes inputs to produce new state values (The "Function of State" approach). [DONE: Pure Logic Refactor]

## 2. Virtualized Concurrency (Java 25 / Loom) [DEFERRED]
**Principle**: *Do not complect logic with threading mechanics.*
*Note: We opted for Kotlin Coroutines (Structured Concurrency) which aligns perfectly with this principle on Android.*

## 3. The Queue is Truth (Durable Execution) [COMPLETED]
**Principle**: *Processes are ephemeral; Data is forever.*
We use Room as the single source of truth.

*   **Action**: Create an `EventLog` table. [DONE]
*   **Action**: The `FeedSyncWorker` operates as a function of data. [DONE]

## 4. Privacy as Decoupling (API 36) [COMPLETED]
**Principle**: *Access only what you need (Principle of Least Privilege).*

*   **Action**: Fully adopt the **Photo Picker** (or equivalant privacy standards) for any future cover-art customization. [DONE: No Permissions Required]
*   **Action**: Use **Predictive Back** (API 36) to model navigation as a reversible pure function. [DONE]

## 5. Simplicity by Removal (Deleting RssParser) [ADAPTED]
**Principle**: *Complexity is cost. Fragility is failure.*

*   **Action**: **DELETE** `RssParser`. [DONE: Replaced with Spec-Driven Logic]
*   **Strategy**: Replaced "Parser Object" with "Parsers as Functions" approach (via Pure Logic extraction).
*   **Goal**: The "Parser" should not exist as a component; parsing should be a pure function. [DONE: PureLogic.kt]

## 6. De-complecting Components (Rich Hickey Protocol) [COMPLETED]
**Principle**: *Separate Policy from Mechanism.*

### AudioService (The Engine) [COMPLETED]
*   **Current State**: De-complected.
*   **Action**: Cleaned `AudioService` of dead logic. Logic moved to `PlaybackClient` (Event Sourcing).

### GlassSystem (The View) [COMPLETED]
*   **Current State**: Spec-Driven.
*   **Action**: `GlassSystem` treats UI as a pure projection of `PodcastRowSpec`.
*   **Action**: Gestures are mapped to `Intents`, logic handles the rest. [DONE]

## 7. Tangible Liveness ("Flux") [COMPLETED]
**Principle**: *The system should look like it works.*
Rich Hickey values "Completeness".

*   **Goal**: Visualize the *Process*, not just the *Result*. [DONE: Haptics, Nebula, Prismatic Glass]
