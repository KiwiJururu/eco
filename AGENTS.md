# Living Ecology — Agent Operating Guide

This repository is the source of truth for the Living Ecology / adaptive ecology mod for Minecraft Java 1.20.1 + Forge.

## Start here

Before making changes, read in this order:
1. `docs/PROJECT_STATE.yaml`
2. `docs/ROADMAP.md`
3. `docs/QA_POLICY.md`
4. `docs/SPECIES_STATUS.csv`
5. the active GitHub issue/PR for the current family

Do not require chat history to reconstruct project state.

## Autonomous operating rule

Continue implementing, testing and fixing the active batch without asking for approval on ordinary technical decisions. Stop only when a decision changes gameplay philosophy, requires a new visual asset, risks player-world destruction/data loss, or has multiple materially different design outcomes that cannot be resolved from the existing design rules.

## Branch model

- `main`: release snapshots only. Do not develop directly here.
- `development/stable`: latest automatically validated development baseline.
- `work/<family-or-system>-<version>`: active implementation branches.

A work branch may advance `development/stable` only after all required CI checks for its scope are green and there are no known objective bugs in that batch.

## Definition of done for a family/batch

A batch is not done merely because code exists. It must:
- compile on Java 17 / Forge 1.20.1;
- pass JUnit/core invariant tests;
- pass relevant Forge GameTests;
- pass the current global regression suite;
- preserve important vanilla mechanics for special/complex mobs;
- avoid Creative/Spectator becoming hostile targets or threat memory;
- avoid forced chunk loading and unbounded per-tick scans;
- respect movement domain and habitat;
- respect persistent territory rules when the species forms territory;
- use vanilla assets unless `texture-needed` is explicitly recorded;
- update `docs/PROJECT_STATE.yaml` and `docs/SPECIES_STATUS.csv` when status changes.

## Project design invariants

- No omniscience. Information must be perceived, remembered or socially transmitted.
- State is temporary; memory persists longer.
- Difficulty comes primarily from behavior, positioning, cooperation and memory rather than raw stat inflation.
- Territory advantage is behavioral/informational, not a direct combat buff.
- Real animals use gameplay-compressed ethology; fantasy mobs preserve Minecraft identity.
- Same-species individuals may vary, but personalities cannot grant behaviors outside the species repertoire.
- Player structures should be protected by default from ecological footprint/griefing systems.
- Save compact causes/state, never per-block territory maps.
- Unloaded ecology is abstract and timestamp-based. Never replay every missed tick.
- Use logical game time; never assume wall-clock tick duration.
- Do not add an external runtime dependency unless profiling or implementation clearly justifies it.

## Testing hierarchy

1. Pure/JUnit tests for deterministic calculations and invariants.
2. Forge GameTests for entity interaction, navigation, target selection, territory, reproduction and persistence behavior.
3. Human visual QA for naturalness, pacing, aesthetics and "does this still feel like Minecraft?".

Automated green status means "no known objective bug covered by current tests", not "human visual QA complete".

## Reporting

Record important fixes, limitations and visual checks in the active PR/issue. The final project report will be generated from repository state rather than reconstructed from chat history.
