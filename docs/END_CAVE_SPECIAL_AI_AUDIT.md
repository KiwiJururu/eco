# End, cave and special vanilla AI audit

Issue: #13
Validated gameplay commit: `7f55e95f218e6c666b385e7da8eabacdea3f9bea`

Enderman and Shulker have defining teleport/state machines, while Endermite and Silverfish have
very different lifecycle and community semantics despite both using small hostile ground-mob
goals. Their dedicated Living Ecology controller is therefore observation-only for movement and
target choice. Only Silverfish receives an additional ecological community action: bounded local
threat memory within its loaded nest, without assigning a target.

| Species | Vanilla remains authoritative for | Living Ecology overlay | Domain / persistent ecology |
| --- | --- | --- | --- |
| Enderman | stare acquisition, persistent anger, teleport, carried blocks, melee and Endermite target selection | water stress, legal target memory and relation context | LAND/End; mobile and non-persistent unless promoted boss |
| Endermite | lifetime/despawn, melee and native Enderman interaction | habitat and legal target observation | LAND/End; transient and non-persistent |
| Silverfish | infested-block hiding, wake-friends goal and melee | habitat, nest territory and bounded local threat memory | LAND/cave; persistent nest |
| Shulker | attachment, peek/armor, teleport, bullets, duplication and variant | habitat, legal target memory and structure context | STATIC/End; structure territory |

## Safety invariants

- the controller never calls `moveTo`, changes velocity, teleports an entity, selects a valid
  target or mutates carried block, anger, lifetime, attachment, peek, variant or vanilla goal state
- the generic ecological rivalry selector, melee recovery and idle patrol code are unreachable for
  all four species
- the Enderman/Endermite relation remains symmetric `WARLIKE` with rivalry 100, while the actual
  target selection remains the vanilla Enderman `NearestAttackableTargetGoal`
- ordinary Endermen remain mobile/non-persistent and Endermites never form territory
- only Silverfish form nest communities; local memory reaches at most eight loaded same-species
  receivers within 12 blocks and never sets their target
- Shulker uses the `STATIC` domain, for which the ecological domain gate yields no destination
- Creative/Spectator targets and corresponding threat memory are sanitized without taking over
  special state
- no species in the batch reproduces or creates a physical footprint
- observations, nest membership and structure territory use loaded local data and never force-load
  chunks

## Automated evidence

- JUnit checks exact four-species policy coverage, the symmetric natural war relation, community
  eligibility, movement domains, End/cave habitat responses, reproduction and footprint invariants
- Forge GameTests exercise native Enderman selection of Endermite, carried block/anger/movement,
  Endermite lifetime and non-territoriality, Silverfish nest memory without target propagation,
  Shulker attachment/peek/variant/combat ownership, Creative sanitization and LAND/STATIC gates
- Java 17 gate: `compileJava`, 55/55 JUnit tests, 56/56 Forge GameTests and `build` PASS

Human visual QA for Enderman teleport/block handling, Silverfish emergence and Shulker peek,
bullet and teleport pacing remains `PENDING` by policy.
