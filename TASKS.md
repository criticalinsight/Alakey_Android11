# Task Queue: Alakey Podcast App

## Documentation
- [x] Generate `PRD.md`
- [x] Generate `ARCHITECTURE.md`
- [x] Initialize `sly` workspace
- [x] Add `CONTRIBUTING.md`

## Infrastructure
- [ ] Configure `sly` for autonomous learning
- [ ] Set up CI/CD for Android builds

## Feature Roadmap
- [ ] Implement Smart Queue sorting
- [ ] Add support for password-protected feeds
- [ ] Enhance motion detection sensitivity settings
- [ ] Enhance motion detection sensitivity settings
- [ ] Implement episode transcription using AI

## Architectural Hygiene (De-complect)
- [x] Extract `PlaybackLiaison` from `AppViewModel`
    - [x] Move `MediaController` connection logic
    - [x] Move Sleep Timer logic
    - [x] Move Progress polling
- [/] Refactor `AlakeyUI` to consume discrete states
