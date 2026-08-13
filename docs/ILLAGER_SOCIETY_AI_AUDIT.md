# Illager Society, Raids and Regional Conflict — AI Audit

Issue: #16 — Illager society, raids and regional conflict

Validated gameplay/test tree: `767ea290813c88e7488f6ec893b02e6cfa6abf76`

## Overview

Pillager, Vindicator, Evoker, Witch, Ravager and Illusioner now use a dedicated low-touch Living Ecology society overlay instead of `GenericBehavior`. Vanilla patrol/raid machinery and role-specific combat remain authoritative. The ecological layer records territory/environment context and shares bounded local threat memory across Illager roles; it never assigns combat targets or navigation paths.

## Ownership matrix

| Species | Vanilla state explicitly covered |
| --- | --- |
| Pillager | crossbow charging, patrol/raid lifecycle, legal target |
| Vindicator | Johnny identity, axe combat, patrol/raid lifecycle, legal target |
| Evoker | Vex ownership, spell-combat target, raid lifecycle |
| Witch | potion-use state, legal target, raid lifecycle |
| Ravager | rider/passenger semantics, legal target, raid lifecycle |
| Illusioner | bow/ranged role, legal target, raid lifecycle |

All six remain LAND / COMMUNITY / persistent ecology with zero ecological footprint and no ecological reproduction.

## Social and regional behavior

- Every cross-role Illager pair keeps natural rivalry 5, affinity 85 and `SYMBIOTIC` relation.
- Every Illager versus Villager/Iron Golem pair keeps rivalry 95, affinity 0 and `WARLIKE` relation.
- A direct current target or recent direct attacker can become a local report.
- Reports are limited to 16 blocks and at most eight receivers, with LOS/proximity and receiver-perception checks.
- Receivers gain memory/RAGE only; no target or path is assigned.
- Real damage uses the existing `TerritoryManager.recordAggression` path, so repeated conflict increases acquired rivalry and territorial tension without scripted omniscient hostility.
- Overlapping Illager/village territories expose contested context through the existing territory relation system.

## Cross-cutting ownership bug fixed

During issue #16 audit, `CommonForgeEvents.propagateCooperativeAlarm` was found to assign `helper.setTarget(attacker)` to high-affinity cross-species allies. That could bypass the dedicated low-touch controllers already validated for Villager/Iron Golem and Piglin/Piglin Brute, and would also fight Illager raid ownership.

The cooperative alarm now remains memory/cooperation-only for all three audited societies. Target assignment is retained only for older non-audited combat families whose controller still owns that behavior. Regression GameTests cover Piglin → Piglin Brute and Villager → Iron Golem hurt alarms to ensure no forced target returns.

## Navigation fixture investigation

Initial role tests tried to prove active raid navigation by constructing a `PathNavigation` path inside the generic GameTest arena. Six tests failed only on the path assertion while every role-specific vanilla-state assertion passed.

A temporary diagnostic GameTest then checked the navigation immediately **before** `IllagerSocietyBehavior.tick` and proved the artificial Pillager path was already inactive. The diagnostic was removed. The final suite therefore tests the meaningful contract directly: the overlay and social-memory paths do not issue navigation, and Creative/memory-only scenarios remain path-free. Production code was not altered to satisfy an invalid fixture.

## Automated evidence

`IllagerSocietyEcologyTest` adds deterministic checks for:
- exact six-species ownership coverage;
- strong internal Illager affinity;
- symmetric Villager/Iron Golem hostility;
- persistent LAND/COMMUNITY profiles with no footprint/reproduction.

`IllagerSocietyEcologyGameTests` adds twelve objective GameTests covering:
- raid lifecycle across all six roles;
- Pillager, Vindicator, Evoker/Vex, Witch, Ravager/rider and Illusioner role state preservation;
- bounded cross-role memory without target/path assignment;
- real-conflict acquired rivalry/tension;
- territorial overlap conflict;
- cooperative-alarm target-ownership regressions for prior society batches;
- Creative target/memory sanitization without navigation.

The pre-documentation gameplay tree passed Java 17 compilation, JUnit and all **83/83 Forge GameTests**. Final documented-tree CI is required before merge.

## Human visual QA pending

User review should inspect raid formation/pacing, patrol movement, crossbow spacing, spell readability, Ravager rider behavior, cross-role response timing, village conflict escalation and whether memory sharing feels local rather than synchronized. No new visual assets are required.
