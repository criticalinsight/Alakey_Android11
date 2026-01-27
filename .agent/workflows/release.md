---
description: Push changes and create a GitHub Release
---

# Workflow: GitHub Release

This workflow stages all changes, commits them with a message, pushes to the master branch, and creates a formal GitHub release with the debug APK attached.

1. Stage all changes:
```bash
git add .
```

2. Commit changes (update the message as needed):
```bash
git commit -m "chore: cumulative updates and refinements"
```

3. Push to master:
```bash
git push origin master
```

4. Create the release (increment version as needed):
```bash
gh release create v2.1.x-sierra app/build/outputs/apk/debug/app-debug.apk --title "Release v2.1.x-sierra" --notes "Release notes here."
```
