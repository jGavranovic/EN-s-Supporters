# EN-s-Supporters

![Line Coverage](.github/badges/line-coverage.svg)

## CI and Coverage

- GitHub Actions runs Android unit tests on every push to `main` via `.github/workflows/unit-tests-jacoco.yml`.
- The workflow runs from `TicketApp/` and executes `./gradlew testDebugUnitTest jacocoTestReport`.
- The workflow updates the repo-hosted line coverage badge at `.github/badges/line-coverage.svg`.
- Coverage is posted in the Actions job summary and the full HTML report is uploaded as the `jacoco-html-report` artifact.
- You can generate the same report locally from `TicketApp/` with:

```powershell
.\gradlew testDebugUnitTest jacocoTestReport
```

