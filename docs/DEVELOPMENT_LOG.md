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
