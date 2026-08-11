# Living Ecology — Development Log

This file records validated technical milestones so future work does not depend on chat history.

## Baseline 0.1.0 QA

Validated commit: `02543ed86054b2943fe5c0b938464352b878cf49`

- source materialized directly into repository
- compileJava PASS
- JUnit PASS
- Forge GameTests PASS (9/9)
- build PASS
- Wolf patrol pathfinding bug fixed
- Creative/Spectator target exclusion validated
- Cow reproduction / birth territory inheritance validated
- 79-species catalogue instantiation validated

## Mixed Zombie family 0.1.1

Validated commit: `c2aed050735cd566e6e6fac03658eef74113a202`

- Zombie, Zombie Villager, Husk and Drowned use dedicated horde controller
- target/threat knowledge shared across mixed variants
- Drowned idle drift respects amphibious movement domain
- mixed horde vs Wolf GameTest green

## Passive-herd social alarms 0.1.2

Validated commit: `b35db54124b724cf8c46be65a182280493bc6e8b`

- local same-species threat communication for PASSIVE_HERD
- line-of-sight and range constrained
- relay confidence decays across hops
- receivers gain Fear, memory and wake from rest
- Sheep GameTest added
- regression PASS (10/10 GameTests)

## Mooshroom bovine specialization 0.1.3

Validated commit: `d1903a31d608991d0453ed45571cfb6950964e6c`

- Mooshroom routed through specialized bovine behavior
- environment lookup remains species-aware
- calf/adult cohesion remains same-species despite Java inheritance from Cow
- Mooshroom territory identity GameTest added
- regression PASS (11/11 GameTests)

## Autonomous infrastructure 0.2.0

Implemented as the control plane for all later development:

- repository is the canonical project memory; chat history is no longer required
- permanent `development/stable` development baseline
- `AGENTS.md` autonomous operating rules
- machine-readable `PROJECT_STATE.yaml`
- canonical `DESIGN_DECISIONS.md`
- family roadmap, QA policy and 79-species status matrix
- issue-driven backlog (#7 through #19)
- duplicate development CI runs removed
- obsolete CI runs automatically cancelled when superseded
- nightly full regression of `development/stable`
- nightly failure automatically opens/updates a GitHub issue and resolves it when green again
- stale standalone-src README replaced by canonical documentation pointer
- minimal new-chat resume prompt added

After this milestone the active gameplay batch is issue #7: aquatic schools and amphibious ecology.

## Aquatic schools and amphibious ecology 0.2.1

Validated gameplay commit: `11facda3771d425104610e888ac1e62e3d5dd1c6`

- completed issue #7 scope for Cod, Salmon, Tropical Fish, Pufferfish, Squid, Glow Squid,
  Dolphin, Axolotl, Turtle, Tadpole and Frog
- added an explicit per-species vanilla ownership policy and `docs/AQUATIC_AI_AUDIT.md`
- retained vanilla fish schooling; strong escape orders require perceived/remembered danger
- extended local, line-of-sight social alarms to audited aquatic species with decaying confidence
  and a maximum of 12 processed senders per pass
- implemented idle Squid/Glow Squid cohesion with native movement vectors rather than path navigation
- preserved Pufferfish inflation, Dolphin air/treasure/item goals, Axolotl play-dead, Turtle
  home/egg travel and Frog/Tadpole Brain activities
- enforced `WATER` versus `AMPHIBIOUS` target domains without loading missing chunks
- validated Axolotl ecological home range and vanilla-love reproduction/capacity integration
- corrected the Squid GameTest setup to wait for Minecraft's first fluid-state update; production
  behavior was not weakened to accommodate the invalid same-tick setup
- compileJava PASS on Java 17
- JUnit PASS (19/19)
- Forge GameTests PASS (17/17)
- build and re-obfuscated JAR PASS (`livingecology-0.1.0.jar`)
- no new textures/assets required; human naturalness, density and pacing QA remains PENDING
- next planned gameplay batch: issue #8, flying passive/colony species

## Flying passive and colony ecology 0.2.2

Validated gameplay commit: `5a1dff521b2dbff993a62e3ec2e273005519008e`

- completed issue #8 scope for Bat, Parrot, Allay and Bee
- added an explicit per-species vanilla ownership policy and `docs/FLYING_COLONY_AI_AUDIT.md`
- preserved Bat custom flight/hanging without issuing path-navigation orders
- limited Parrot cohesion and perceived-threat escape to wild, active-period, idle-navigation cases;
  tame owner/sit/shoulder/jukebox semantics remain vanilla-owned
- kept Allay item pickup/delivery, liked player/note block, dancing, duplication and Brain movement
  observation-only
- anchored Bee territory center/core to a loaded valid hive within a 48-block bound and reindexed
  persistence without loading chunks
- shared Bee threat memory with at most 12 local same-territory receivers while leaving vanilla
  anger/target selection authoritative
- bounded immediate same-species hurt-alarm inspection to 12 entities
- corrected two GameTest fixture assumptions: ownership is persisted by UUID rather than mock-player
  identity, and territory members use the normal join path when SavedData survives a prior run
- compileJava PASS on Java 17
- JUnit PASS (25/25)
- Forge GameTests PASS (22/22)
- build and re-obfuscated JAR PASS (`livingecology-0.1.0.jar`)
- no new textures/assets required; human flight naturalness, colony density and pacing QA remains PENDING
- next planned gameplay batch: issue #9, common passive land species

## Common passive land ecology 0.2.3

Validated gameplay commit: `72cde05eb315dbffe93f6e996b2f8dbeeae34d32`

- completed issue #9 scope for Sheep, Pig, Chicken, Rabbit, Horse, Donkey, Mule, Camel, Goat,
  Llama, Trader Llama and Sniffer
- added an explicit per-species vanilla ownership policy and `docs/PASSIVE_LAND_AI_AUDIT.md`
- limited herd cohesion, perceived-threat escape and home return to idle vanilla navigation with
  land-domain, loaded-chunk and bounded twelve-member checks
- preserved Sheep grazing/shearing, Pig saddle/boost/riding, Chicken jockey/egg/flap, Rabbit custom
  hopping/garden/Killer Bunny, horse-family tame/owner/chest/riding, Llama caravan/spit, Camel and
  Goat Brain states, Trader Llama lifecycle and Sniffer search/dig behavior
- corrected vanilla Camel classification: `Camel.isTamed()` always returns true but does not express
  player ownership, so wild Camel territory and ecological reproduction now work without taking
  movement ownership from `CamelAi`
- made Trader Llama explicitly non-territorial and non-reproductive
- excluded ridden animals from automatic ecological reproduction while retaining mount control
- stabilized repeated GameTest runs by capping Bee colony association at the same 48-block bound as
  hive recentering; stale distant SavedData can no longer capture a new colony
- compileJava PASS on Java 17
- JUnit PASS (31/31)
- Forge GameTests PASS (30/30)
- build and re-obfuscated JAR PASS (`livingecology-0.1.0.jar`)
- no new textures/assets required; human land-herd naturalness, pacing and footprint QA remains PENDING
- next planned gameplay batch: issue #10, terrestrial predators

## Terrestrial predators and independent animals 0.2.4

Validated gameplay commit: `13d45b9b4f8f846cf0c9ee7964d72037c4c5436b`

- completed issue #10 scope for Fox, Ocelot, Cat, Polar Bear and Panda
- added an explicit per-species vanilla ownership policy and
  `docs/TERRESTRIAL_PREDATOR_AI_AUDIT.md`
- removed the batch from generic predator targeting/retreat behavior so ecology never invents
  aggression beyond vanilla prey, trusted defense, owner defense or neutral repertoires
- preserved Fox trusted UUIDs, held items, sleeping/stalking/pouncing; Ocelot trust/tempt/avoid;
  Cat tame/owner/sit/bed/collar; Polar Bear anger/standing/cub protection; and Panda genes plus
  sit/eat/sneeze/roll action states
- added low-health/fear retreat and distant home return only for idle, state-safe actors
- improved the shared safe-retreat planner with bounded 75% and 50% fallback arcs for confined
  loaded terrain without relaxing hazard, fluid or solid-ground checks
- validated bordered geometric predator overlap and acquired rivalry while proving that neither
  condition forces a combat target
- corrected two GameTest assumptions: ground pathfinding requires a pre-first-tick Ocelot fixture
  to be settled on the known floor, and `FakePlayer` is unsuitable as a neutral-mob target fixture
- compileJava PASS on Java 17
- JUnit PASS (37/37)
- Forge GameTests PASS (36/36)
- build and re-obfuscated JAR PASS (`livingecology-0.1.0.jar`)
- no new textures/assets required; human stalking, retreat and overlap pacing QA remains PENDING
- next planned gameplay batch: issue #11, skeleton and undead combat families
