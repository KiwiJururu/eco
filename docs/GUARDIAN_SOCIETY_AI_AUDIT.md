# Guardian Society and Monument Territory — AI Audit

Issue: #17 — Guardian society and monument territory

Validated gameplay commit: `170b509d1cf0683dba061a76364befbc8c4eb9c4`

## Overview

Guardian and Elder Guardian now use a dedicated WATER-domain low-touch overlay instead of `GenericBehavior`. Vanilla beam targeting, thorns, swimming/move-control and Elder Guardian mining-fatigue identity remain authoritative. Living Ecology adds structure territory context, bounded local monument memory and conservative water-only idle return.

## Ownership and territory

- Guardian: vanilla beam/thorns/swim/target ownership; Sentinel monument role.
- Elder Guardian: vanilla beam/thorns/mining-fatigue/swim/target ownership; Elder monument role.
- Both profiles remain `GUARDIAN`, `WATER`, `STRUCTURE`, `DEEP_WATER`, persistent, no ecological footprint and no ecological reproduction.
- Guardian and Elder territories remain species-scoped while natural relation stays rivalry 0 / affinity 95 / `SYMBIOTIC`.

## Bounded monument influence

A valid current target or recent direct attacker can be remembered locally. A normal Guardian can inform at most eight nearby Guardian/Elder receivers within 16 blocks. An Elder can inform at most twelve within 20 blocks. LOS/proximity and receiver perception are required. Receivers gain memory/RAGE only; no target is assigned.

The Elder therefore has stronger local informational reach rather than additional health/damage buffs. GameTests snapshot raw max-health and attack-damage attributes before/after the overlay.

## Navigation safety

Combat always returns control immediately to vanilla AI. Ecological monument return is permitted only when all of the following are true:

- no active/reportable threat;
- the mob is outside its recognized territory;
- vanilla navigation is idle;
- the mob is in water/bubble;
- the territory core chunk is already loaded;
- the core is a valid WATER-domain destination;
- distance exceeds 16 blocks.

This avoids forced chunk loading, dry-land path assumptions and beam/move-control interference.

## Automated evidence

`GuardianSocietyEcologyTest` adds four deterministic tests for exact policy coverage, Guardian/Elder affinity, WATER/STRUCTURE profile invariants and bounded Elder influence.

`GuardianSocietyEcologyGameTests` adds seven GameTests covering:

1. legal Guardian beam-combat target preservation with no ecological combat path;
2. Elder target ownership and no raw health/damage inflation;
3. species-scoped Guardian/Elder persistent territories plus symbiotic relation;
4. normal Guardian memory capped at exactly eight receivers;
5. Elder local influence capped at exactly twelve receivers with raw Guardian health unchanged;
6. dry idle Guardian receives no ecological navigation;
7. Creative target/threat sanitization without swim orders.

Initial Java 17 CI run `31667784634` passed compileJava, JUnit, all **90/90 Forge GameTests**, build and JAR upload. Artifact ID `9168609179`; SHA-256 `3990a34dda226958c849065520060d7827b31c688343dbf66411c499883d73d8`.

A final CI run over the documented tree is required before merge.

## Human visual QA pending

User review should inspect beam timing/animation, swim movement around monument geometry, idle return naturalness, Guardian/Elder spacing, whether Elder influence reads as stronger local organization without synchronized telepathy, and overall Monument pacing/Minecraft feel. No new visual assets are required.
