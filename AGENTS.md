# Night Sky & Wings

- Start with the issue and the relevant files under `app/src/`. Read `HANDOFF.md` when you need setup, build, or known-issue details.
- Search for specific symbols before scanning whole files. Reuse findings from this session and keep command output focused on relevant matches and failures.
- Keep edits within the requested scope. Preserve stated constraints and flag assumptions that affect the result.
- Start with a focused JVM test. In Windows PowerShell, run `.\gradlew.bat :app:testDebugUnitTest --tests '<fully-qualified-test-class>'`. On Linux or macOS, use `./gradlew` with the same arguments. Run `:app:testDebugUnitTest` without the filter when the change warrants broader coverage.
- Keep updates brief. Report the change, checks run, and anything material that remains unverified.
