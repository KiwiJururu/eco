# Living Ecology — Roadmap

The project advances by validated behavior families, not by adding all mobs at once. A family only moves to DONE when its objective tests and global regression suite are green.

## Phase 0 — Core / QA foundation — DONE

- 79-species catalogue and profile registration
- attributes, states, memory and adaptation bounds
- territory persistence core
- relation core
- environment core
- reproduction/capacity core
- footprint budgets
- Creative/Spectator target exclusion
- automated JUnit + Forge GameTest + build CI

## Phase 1 — Prototype anchors — DONE / VISUAL QA PENDING

- Cow
- Wolf
- Spider / Cave Spider
- Zombie

## Phase 2 — Closely related extensions — IN PROGRESS

Validated:
- Zombie Villager
- Husk
- Drowned
- Mooshroom
- PASSIVE_HERD social alarm infrastructure

Next batches:
1. aquatic schools and social movement: Cod, Salmon, Tropical Fish, Pufferfish, Squid, Glow Squid, Dolphin, Axolotl, Turtle, Tadpole, Frog
2. flying passive/social: Bat, Parrot, Allay, Bee
3. common passive land family: Sheep, Pig, Chicken, Rabbit, Horse, Donkey, Mule, Camel, Goat, Llama, Trader Llama, Sniffer
4. terrestrial predators/independent animals: Fox, Ocelot, Cat, Polar Bear, Panda

## Phase 3 — Common hostile families

- Skeleton / Stray / Wither Skeleton
- Creeper
- Slime / Magma Cube
- Enderman / Endermite
- Silverfish
- Phantom / Ghast / Vex
- Blaze
- Hoglin / Zoglin

## Phase 4 — Societies and structures

- Villager / Wandering Trader / Iron Golem / Snow Golem
- Piglin / Piglin Brute / Zombified Piglin
- Pillager / Vindicator / Evoker / Witch / Ravager / Illusioner
- Guardian / Elder Guardian
- Shulker

## Phase 5 — Special and boss-class entities

Treat these conservatively because vanilla state machines are gameplay-critical:
- Warden
- Wither
- Ender Dragon
- Giant
- Skeleton Horse
- Zombie Horse
- Strider

## Cross-cutting work required throughout

- habitat and biome behavior tests
- reproduction and carrying-capacity tests
- territory growth/shrinkage and migration
- footprints/territorial marks for appropriate species
- acquired rivalry/affinity and social information transfer
- persistence/save-load regression
- timescale regression
- unloaded abstract ecology
- performance budgets
- player-structure protection

## Final acceptance

The project reaches final technical QA only when:
- all 79 creatures initialize and operate without known objective bugs;
- every species has at least family-level behavior coverage and special cases have dedicated tests;
- full regression is green;
- persistence and timescale tests are green;
- performance budgets are acceptable;
- final human visual checklist is generated.

Human visual/naturalness QA remains intentionally separate from automated technical QA.
