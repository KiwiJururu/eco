# Living Ecology — Automation

## Per-batch CI

`.github/workflows/ci.yml` is the gate for work PRs targeting `development/stable`.

It runs:
1. `compileJava`
2. JUnit
3. Forge `runGameTestServer`
4. `build`
5. JAR upload on success / QA logs on failure

Work branches should use `work/*`. They do not trigger an extra push build; the PR triggers the single authoritative run. `development/stable` and `main` still run CI on direct updates.

The workflow uses a concurrency group with `cancel-in-progress: true`, so when a newer commit reaches the same PR, obsolete Minecraft/GameTest runs are cancelled instead of consuming time.

## Nightly stable regression

`.github/workflows/nightly-stable.yml` lives on the default branch so GitHub can schedule it. Every night it checks out `development/stable`, not `main`, and runs the full technical regression.

If nightly fails, the workflow opens or updates an issue titled `Nightly regression failure`, records the tested stable commit/run and uploads failure logs. When nightly becomes green again, the issue is resolved automatically.

This gives the project an external watchdog even when no chat session is active.

## Scaling rule

Do not shard GameTests prematurely because each Minecraft server startup has overhead. Keep one full GameTestServer while total CI time is reasonable. If the full suite consistently exceeds about 10 minutes or grows large enough that feedback becomes a bottleneck, split tests into stable logical suites only after measuring the startup/runtime tradeoff.

## Development handoff

A new session should not scan old chats. Read `AGENTS.md`, `docs/PROJECT_STATE.yaml`, and the active issue. The repository contains the state needed to continue.
