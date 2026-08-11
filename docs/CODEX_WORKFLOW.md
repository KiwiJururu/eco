# Living Ecology — Codex Autonomous Workflow

This is the recommended high-autonomy execution model once `KiwiJururu/eco` is connected to a Codex environment.

## Source of truth

Codex must treat the repository as authoritative. Before coding, read:
1. `AGENTS.md`
2. `docs/PROJECT_STATE.yaml`
3. `docs/DESIGN_DECISIONS.md`
4. `docs/QA_POLICY.md`
5. the active GitHub issue listed by `PROJECT_STATE.yaml`

Do not reconstruct project intent from old chats.

## Roles

Use parallel agents only when their write scopes do not collide.

### 1. Primary implementation agent

Owns the active family issue and its production-code branch.

Prompt pattern:

> Continue Living Ecology from the active issue in `docs/PROJECT_STATE.yaml`. Work only on that issue. Audit the relevant vanilla mechanics first, implement the smallest safe family-level behavior plus necessary species exceptions, add objective tests, run the available QA commands, fix failures, and update repository status docs. Do not advance to the next issue while a known objective bug remains. Follow `AGENTS.md` and `docs/DESIGN_DECISIONS.md`. Use vanilla assets unless a new asset is genuinely required.

### 2. Independent QA / red-team agent

Does not own production implementation. Reviews the primary branch/PR and looks for missing cases.

Prompt pattern:

> Review the current Living Ecology work PR as an independent QA engineer. Do not redesign the mod and do not make broad production changes. Look for reproducible bugs, vanilla-behavior conflicts, invalid target propagation, movement-domain mistakes, forced chunk loads, persistence/timescale regressions, unbounded work, missing GameTests, and destructive terrain behavior. Report concrete findings with reproduction/test suggestions. If a small test-only change is needed, keep it isolated.

### 3. CI failure triage agent

Use only after a failing workflow. It should inspect the first causal error, not rewrite unrelated systems.

Prompt pattern:

> Triage the failing Living Ecology CI run for the active PR. Find the first causal compile/JUnit/GameTest/build failure, reproduce if possible, make the smallest safe correction, rerun relevant tests, then the full regression. Do not advance feature scope while CI is red.

## Parallelism rules

Safe parallel work:
- implementation vs independent review;
- deterministic unit-test expansion vs documentation/status updates;
- research/audit of a later special mob without modifying active production code.

Unsafe parallel work:
- two agents rewriting the same behavior controller;
- two agents changing `SpeciesProfile` or `TerritoryManager` independently;
- implementing two dependent families before the current family is green.

The goal is throughput without merge-conflict or architectural drift.

## Branch lifecycle

1. Start from `development/stable`.
2. Create `work/<issue>-<short-name>`.
3. Open a PR targeting `development/stable`.
4. Let the single PR CI run compile, JUnit, Forge GameTests and build.
5. Primary agent fixes failures; QA agent reviews independently.
6. Only after all objective gates are green, fast-forward/merge into `development/stable`.
7. Update `docs/PROJECT_STATE.yaml`, `docs/SPECIES_STATUS.csv`, `docs/DEVELOPMENT_LOG.md` and the issue.
8. Move to the next issue.

## Background watchdog

GitHub nightly regression independently retests `development/stable`. A failure opens/updates a GitHub issue automatically. Treat an open nightly regression issue as higher priority than new family work.

## User escalation policy

Do not interrupt the user for ordinary technical decisions. Escalate only when:
- a choice changes established gameplay philosophy;
- a new texture/model is genuinely required;
- a change risks destructive player-world/data behavior;
- two materially different gameplay outcomes cannot be resolved from canonical design docs.

Naturalness, pacing and aesthetics remain user visual QA after objective technical QA.

## Completion

After issue #19 is technically green, generate the final QA/development report and human visual checklist. The requested final PDF should be based on repository status/logs, not reconstructed chat history.
