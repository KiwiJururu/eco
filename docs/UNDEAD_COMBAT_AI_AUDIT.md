# Skeleton and undead-combat vanilla AI audit

Issue: #11
Validated gameplay commit: `5c13de68421321ed8cc55f20c10acdc051da1d3a`

These four species share combat pressure but not one safe movement controller. Skeleton and Stray
switch between bow strafing and melee goals according to equipment, Wither Skeleton owns melee
and Nether target goals, and Zombified Piglin owns neutral persistent anger plus group alerting.
Living Ecology therefore adds bounded memory, environment and territory context without selecting
targets, and only repairs melee navigation in states where vanilla has no working path.

| Species | Vanilla remains authoritative for | Living Ecology overlay | Persistent ecology |
| --- | --- | --- | --- |
| Skeleton | bow strafing, ranged attack, weapon-based melee fallback, sun avoidance and burning | local Skeleton/Stray target memory; no path order while holding a bow | mobile, no persistent territory; general habitat |
| Stray | bow strafing, slowness arrows, weapon-based melee fallback, sun avoidance and burning | local Skeleton/Stray target memory; no path order while holding a bow | mobile, no persistent territory; cold habitat |
| Wither Skeleton | melee attack, Wither effect and Nether target goals including Piglins | target observation, own-group memory and state-gated melee path recovery | Nether structure territory |
| Zombified Piglin | neutral behavior, persistent anger target/time, alert propagation and melee | anger observation, own-group memory and state-gated melee path recovery | Nether horde territory |

## Safety invariants

- the controller never calls `setTarget`; vanilla goal selectors and neutral anger remain the only
  sources of combat targets
- Creative/Spectator, dead and otherwise invalid targets are removed from active target and threat
  memory before any ecological processing
- Skeleton and Stray holding a bow never receive Living Ecology navigation recovery, preserving
  native strafing and ranged positioning
- melee recovery requires a valid target more than four blocks away, a grounded actor that is not
  mounted or leashed, and navigation that is either done or demonstrably stalled
- group knowledge is local to a 12-block loaded query and capped at eight informed receivers;
  shared memory never assigns the receiver a target
- Skeleton and Stray may share knowledge with one another, while Wither Skeleton and Zombified
  Piglin use separate knowledge groups
- Piglin/Wither Skeleton `WARLIKE` rivalry remains ecological context and never becomes an
  automatic target rule
- daylight exposure short-circuits optional idle ecology for uncovered Skeleton and Stray;
  vanilla remains authoritative for sun avoidance, burning and helmets
- idle home return applies only to persistent Nether territories, from outside the territory,
  with finished navigation and loaded safe destinations
- none of the four species creates a physical footprint

## Automated evidence

- JUnit checks exact four-species policy coverage, movement domains, weapon/daylight gates,
  melee recovery truth table, territory/footprint profiles, habitat differences and the
  Piglin/Wither Skeleton relation
- Forge GameTests cover bow-position ownership with Skeleton/Stray memory, Wither Skeleton rivalry
  without forced targeting plus melee recovery, Zombified Piglin anger/group memory, Creative
  target sanitization, and daylight fire/equipment/movement ownership
- Java 17 gate: `compileJava`, 43/43 JUnit tests, 41/41 Forge GameTests and `build` PASS

Human visual QA for bow strafing, melee pursuit, group response, daylight behavior and Minecraft
feel remains `PENDING` by policy.
