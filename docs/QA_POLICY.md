# Living Ecology — QA Policy

## Goal

Move fast without allowing silent regressions. "Green" means no known objective bug covered by current automated tests; it does not replace human visual/naturalness QA.

## Test layers

### Fast deterministic tests
Use JUnit for:
- attribute/state/adaptation bounds;
- timescale math and rebase rules;
- relation formulas;
- territory pressure/growth calculations;
- reproduction eligibility/capacity calculations;
- footprint budgets;
- serialization helpers that do not require a live server.

### Forge GameTests
Use GameTests for:
- real entity creation;
- target legality and target propagation;
- navigation orders and stuck recovery;
- species-family interactions;
- territory creation/inheritance;
- reproduction triggering and birth effects;
- biome/habitat-dependent runtime behavior where feasible;
- footprint block placement limits;
- persistence behavior requiring Minecraft data storage.

### Human QA
Reserve for:
- naturalness;
- pacing;
- visual density of footprints;
- whether emergent interactions are fun/readable;
- whether behavior still feels like Minecraft.

## Batch gate

A gameplay batch can fast-forward into `development/stable` only when:
1. compileJava passes;
2. JUnit passes;
3. all Forge GameTests pass;
4. build passes and produces a JAR;
5. no known objective bug from the batch is left open;
6. species status and project state are updated.

## Failure policy

When CI fails:
- do not advance to another family;
- inspect the first causal failure, not only the final Gradle error;
- fix production behavior when the test exposes a real bug;
- fix the test only when the setup/assertion does not represent a valid game state;
- rerun the full current regression before declaring the batch stable.

## Performance policy

Do not use brittle wall-clock assertions in CI. Test bounded work instead:
- maximum scan radius;
- maximum processed entities/items per pass;
- maximum footprint placements;
- no forced neighbor chunk loads;
- no per-missed-tick catch-up loops.

Use profiling separately when performance is suspect.

## Special mobs

Villagers, Piglins, Illagers, Guardians, Shulkers, Warden, Wither and Ender Dragon have important vanilla state machines. Prefer low-touch ecological overlays unless a dedicated controller is proven safe by targeted GameTests.

## Asset policy

Use vanilla textures/models/blocks whenever possible. Record `texture-needed` only when a behavior cannot be communicated adequately through existing assets, poses or blocks. A visual asset request must not silently block unrelated technical work.
