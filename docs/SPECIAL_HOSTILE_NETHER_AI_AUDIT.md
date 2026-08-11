# Common hostile and Nether vanilla AI audit

Issue: #12
Validated gameplay commit: `dbd85b9a85240f164b796f394a651929b0c782c0`

These ten species use mutually incompatible movement and combat controllers. Creeper owns a fuse
state machine; Slime and Magma Cube own jump movement; four flyers use distinct float, charge,
anchor or phasing logic; Hoglin and Zoglin use Brain activities; and Strider combines lava walking
with temperature, saddle and boost state. Their Living Ecology controller is therefore
observation-only: it records perceived legal targets, habitat pressure and profiled territory
without selecting targets or issuing any movement order.

| Species | Vanilla remains authoritative for | Living Ecology overlay | Domain / persistent ecology |
| --- | --- | --- | --- |
| Creeper | swell, fuse, explosion, powered state, cat avoidance and target approach | fuse/rage observation and legal target memory | LAND; mobile and non-persistent |
| Slime | size, squish, split, jump timing and move control | habitat and legal target observation | LAND/swamp; non-persistent |
| Magma Cube | size, squish, split, lava jump and move control | habitat and legal target observation | LAND/Nether; non-persistent |
| Blaze | fireball volley, charged phase, height control, flight and water damage | water stress and legal target observation | AIR/Nether; structure territory |
| Ghast | random float, fireball charge and explosion power | charge/rage and legal target observation | AIR/Nether; non-persistent |
| Phantom | anchor, circle, swoop, size and daylight burning | habitat and legal target observation | AIR/aerial; non-persistent |
| Vex | owner, bound origin, charge, block phasing and limited life | charge/rage and legal target observation | AIR/aerial; non-persistent |
| Hoglin | Brain hunt/retreat, repellents, zombification, attack and breeding | conversion stress, legal target memory and group context | LAND/Nether; group territory; vanilla love |
| Zoglin | Brain universal hostility, attack animation/movement and baby state | habitat, legal target memory and group context | LAND/Nether; group territory |
| Strider | lava walking, cold state, saddle, rider, boost, temptation and breeding | cold/water stress and home-range context | LAVA; home range; vanilla love |

## Safety invariants

- the controller never calls `moveTo`, changes velocity, selects a valid target or mutates a
  defining species state; Creative/Spectator targets are still sanitized globally
- the generic ecological rivalry target selector, hostile path recovery and idle patrol code are
  unreachable for all ten species
- Strider is excluded from the broad `PASSIVE_HERD` social-alarm fallback, preventing an unaudited
  herd state from becoming a movement signal
- LAND, AIR and LAVA domains remain distinct; Magma Cube keeps its native lava-capable jump logic
  without ecological LAND path orders
- persistent territory follows profiles exactly: Blaze structure, Hoglin/Zoglin group and Strider
  home range; the other six are non-persistent
- no species in the batch creates a physical footprint
- only Hoglin and Strider use ecological reproduction, which still enters vanilla love mode and
  respects territory, capacity, habitat and actor-state gates
- `ReproductionManager.isEligibleBase` delegates to `Hoglin.canFallInLove`, so nearby repellent
  `PACIFIED` Brain memory cannot be bypassed by a direct ecological `setInLove` call
- ridden Striders are excluded from automatic reproduction and receive no ecological navigation
- observations and territory queries remain local/loaded; no chunk is force-loaded

## Automated evidence

- JUnit checks exact ten-species policy coverage, distinct observation modes, LAND/AIR/LAVA
  domains, persistent territory/footprint profiles, reproduction modes and habitat/hazard contracts
- Forge GameTests cover Creeper fuse/target, Slime and Magma Cube size/squish/jump motion, all four
  special flyers, Hoglin/Zoglin Brain/lifecycle/territory, ridden cold Strider state, Creative target
  sanitization, actual domain blocks, and Hoglin/Strider vanilla-love reproduction with PACIFIED
  Hoglin exclusion
- Java 17 gate: `compileJava`, 49/49 JUnit tests, 49/49 Forge GameTests and `build` PASS

Human visual QA for fuse timing, jump rhythm, flight paths, Brain pacing, group density and lava
riding remains `PENDING` by policy.
