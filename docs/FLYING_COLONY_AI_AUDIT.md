# Flying-passive and colony vanilla AI audit

Issue: #8
Validated gameplay commit: `5a1dff521b2dbff993a62e3ec2e273005519008e`

The four species in this batch share the `AIR` movement domain but not a common movement
controller. Living Ecology therefore uses an explicit per-species ownership policy and adds only
bounded information/state overlays around vanilla AI.

| Species | Vanilla remains authoritative for | Living Ecology overlay | Persistent ecology |
| --- | --- | --- | --- |
| Bat | `customServerAiStep`, flight target and hanging/wake transitions | mirrors vanilla resting state for debug/memory; never uses `PathNavigation` | cave home range |
| Parrot | tame/owner, sit, shoulder, jukebox and wander goals | wild-only local alarm and idle air-domain cohesion; no orders at night or while vanilla navigation is active | wild forest home range; tamed Parrots are excluded |
| Allay | item pickup/delivery, liked player/note block, dance/duplication and `Brain` walk targets | observes panic for ecological state only; never issues navigation | none |
| Bee | hive/flower search, pollination, nectar, anger, sting and hive-entry goals | loaded valid hive anchors territory; target knowledge is shared locally with at most 12 colony receivers without assigning targets | hive-centered nest territory and vanilla-love reproduction |

## Safety invariants

- no overlay loads a missing chunk
- air targets pass `MovementDomain.AIR`; the flying controller never calls the land-only patrol helper
- valid existing hive cores are stable, so nearby hives cannot move one territory back and forth
- hive recentering is limited to 48 blocks and updates the saved spatial index
- direct same-species hurt alarms inspect at most 12 nearby entities
- tamed Parrots retain owner/sit semantics and never keep a wild territory
- Allay held items, dancing and Brain navigation remain untouched
- Bat hanging and Bee pollination/hive navigation remain vanilla-owned

## Automated evidence

- JUnit checks the exact four-species policy, air domains, territory/reproduction eligibility,
  habitat distinctions and day/night activity gates
- Forge GameTests cover Bat hanging, tamed Parrot commands, Allay held-item/dance state,
  hive-centered bounded Bee alarms and air-only patrol target selection
- Java 17 gate: `compileJava`, 25/25 JUnit tests, 22/22 Forge GameTests and `build` PASS

Human visual QA for naturalness, flight pacing and colony density remains `PENDING` by policy.
