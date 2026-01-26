# Contributing to Alakey

## Philosophy
Alakey follows the **Transcendental Protocol**:
1. **Simplicity is Reliability**: Avoid complected code. Pure functions > Classes.
2. **Visual Purity**: The UI must reflect the Flux state. Use `GlassSystem` primitives.
3. **Data Oriented**: The database is the source of truth. The UI is a view of the database.

## Architecture
- **Language**: Kotlin (Strict mode)
- **Framework**: Jetpack Compose (Flux/MVI pattern)
- **Database**: Room (Single source of truth)

## Pull Request Process
1. Ensure `gradlew lintDebug` passes with zero errors.
2. Verify visual aesthetics match the `Flux` design system.
3. Keep changes atomic and de-complected.

## Tooling
- **Build**: Gradle 8.13+
- **JDK**: OpenJDK 17
- **Style**: "Rich Hickey" (Think first, code second)
