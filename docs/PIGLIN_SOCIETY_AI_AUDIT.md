# Piglin Society and Bastion Ecology — AI Audit

Issue: #15 — Piglin society and Bastion ecology

Validated gameplay commit: `3d47dfc6432cf1a3058300d02f97f9b70e7fe246`

## Overview

Piglin and Piglin Brute now use a dedicated low-touch Living Ecology overlay instead of the generic hostile-society controller. Their vanilla Brain remains authoritative for bartering, gold admiration, fear/avoidance, hunting, combat, Bastion HOME state and movement. Zombified Piglin remains deliberately separate under the validated undead-combat controller.

The ecological layer records local environment/territory context and may transmit bounded threat memory between nearby living Piglins. It never assigns a combat target, `ATTACK_TARGET`, `AVOID_TARGET` or navigation path.

## Species ownership matrix

| Species | Vanilla ownership preserved | Ecology overlay | Persistent ecology |
| --- | --- | --- | --- |
| Piglin | Brain bartering/gold admiration, fear/avoid, hunting, combat and movement | territory/context + bounded living-society threat memory | yes, species-scoped community |
| Piglin Brute | Brain HOME, Bastion guard/combat role, ATTACK_TARGET and movement | territory/context + bounded living-society threat memory | yes, species-scoped community |
| Zombified Piglin | persistent neutral anger and undead alert/combat behavior | separate `UndeadCombatBehavior`; excluded from living-society relay | yes, undead horde profile |

## Safety invariants

- Piglin and Piglin Brute are no longer routed through `GenericBehavior`, preventing ecological rivalry from becoming an independent target selector or generic society path order.
- Piglin/Brute cooperation uses the existing symmetric natural relation (rivalry 5, affinity 90, `SYMBIOTIC`) plus bounded local knowledge transfer.
- Piglin versus Wither Skeleton remains `WARLIKE` context (rivalry 85, affinity 0) without Living Ecology inventing a target or path.
- Living Piglin threat reports reach at most eight receivers within 16 blocks and require source visibility/proximity plus receiver perception of the threat.
- Zombified Piglins are in a separate knowledge group and do not receive living Piglin society reports.
- Creative/Spectator targets and matching threat memory are sanitized without changing gold/barter state or issuing navigation.
- Piglin, Piglin Brute and Zombified Piglin retain zero ecological physical footprints and no ecological reproduction.
- Territory formation remains same-species, preserving Piglin and Piglin Brute identity while `RelationService` supplies cross-species cooperation.
- No forced chunk loading or unbounded per-tick scans were introduced.

## Automated evidence

### JUnit

`PiglinSocietyEcologyTest` validates:

- exact issue-species policy coverage;
- living versus zombified knowledge separation;
- Piglin/Brute symbiotic affinity and Wither Skeleton rivalry;
- Nether/LAND/community persistence profiles;
- distinct Zombified Piglin undead ecology;
- no ecological footprints or reproduction for the batch.

### Forge GameTests

Seven targeted GameTests validate:

1. Piglin admiration, hunt, avoid target, gold/offhand state, zombification lifecycle and active navigation remain vanilla-owned.
2. Piglin Brute HOME, Brain `ATTACK_TARGET`, zombification lifecycle and active navigation remain vanilla-owned.
3. Piglin and Piglin Brute form species-scoped persistent communities while retaining strong relation-based cooperation.
4. A living Piglin hurt report reaches a nearby Brute but not a Zombified Piglin and assigns no target/path.
5. Wither Skeleton rivalry does not invent a Piglin target or path.
6. Local living-society threat memory is capped at exactly eight receivers.
7. Creative target/threat memory is sanitized without changing Piglin gold state or navigation.

The final pre-documentation CI run on Java 17 passed `compileJava`, JUnit, all **71/71 Forge GameTests**, `build`, re-obfuscation and JAR artifact upload. Workflow run: `31665114231`. Artifact ID: `9167654779`; ZIP SHA-256: `77c2814a184d8ffde6f68fabb00f3a898b32e7463c19f3ea1a6ef11eb0204f98`.

## Test-fixture corrections during validation

Two test-only assumptions were corrected without weakening production behavior:

- the protected Piglin zombification getter was replaced with before/after NBT persistence comparison;
- Piglin Brute combat ownership is Brain-driven, so the fixture now validates `MemoryModuleType.ATTACK_TARGET` instead of the generic `Mob#setTarget` field.

These corrections made the tests represent valid vanilla state rather than adapting production code to artificial fixtures.

## Human visual QA pending

Automated technical QA does not determine naturalness. User review should still inspect:

- Piglin crowd movement and spacing in/around a Bastion-like environment;
- whether gold/bartering and fear/hunt transitions still look vanilla;
- Piglin Brute guard/response timing;
- local Piglin/Brute group response readability without synchronized telepathy;
- Wither Skeleton encounters and living-versus-zombified separation;
- overall pacing and Minecraft feel.

No new textures, models or other visual assets are required by this batch.
