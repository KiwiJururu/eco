# Living Ecology

Adaptive ecology / mob AI project for Minecraft Java 1.20.1 + Forge.

## Development source of truth

The repository, not chat history, is the project memory.

For development or agent handoff, start with:
- `AGENTS.md`
- `docs/PROJECT_STATE.yaml`
- `docs/DESIGN_DECISIONS.md`
- `docs/ROADMAP.md`
- `docs/QA_POLICY.md`
- `docs/SPECIES_STATUS.csv`
- the active GitHub issue listed in `PROJECT_STATE.yaml`

Supporting records:
- `docs/DEVELOPMENT_LOG.md`
- `docs/AUTOMATION.md`

## Branches

- `main` — release snapshots and repository-level scheduled automation
- `development/stable` — latest validated development baseline
- `work/*` — active gameplay/system batches

## Current status

- Minecraft 1.20.1 / Forge / Java 17
- 79 vanilla creatures registered in the Living Ecology catalogue
- automated JUnit + Forge GameTest + build pipeline
- nightly regression of `development/stable`
- human visual/naturalness QA intentionally separate from technical QA

See `docs/PROJECT_STATE.yaml` for the exact validated commit and active batch.
