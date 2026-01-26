# Testing & Coverage Guide

This project is configured for **JaCoCo** code coverage and **MCP-driven** testing.

## Prerequisites
1.  **OpenJDK 17+**: Required for Gradle. [(Install)](https://adoptium.net/)
2.  **Appium**: `npm install -g appium`
3.  **MCP Tools**: `pip install mcpandroidbuild`, `npm install -g @modelcontextprotocol/inspector`

## Running Tests

### Unit Tests with Coverage
Execute the following to run logic tests and generate a report:
```bash
./gradlew testDebugUnitTest jacocoTestReport
```

### Viewing Results
Open the generated HTML report:
`app/build/reports/jacoco/testDebugUnitTestReport/html/index.html`

## Architecture Compliance
*   **Pure Functions**: `RssParser` and `Data` classes should show 100% logic coverage.
*   **UI/Service**: `AppViewModel` and `AudioService` should be covered by integration tests (via Appium).
