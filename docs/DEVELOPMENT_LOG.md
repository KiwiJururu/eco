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

## Infrastructure 0.2.0

In progress.

Goals:
- externalize project memory into repository
- permanent `development/stable` baseline
- remove duplicate CI executions
- cancel obsolete CI runs when newer commits arrive
- add scheduled full regression against stable development
- issue-driven family batches
- machine-readable 79-species QA matrix
