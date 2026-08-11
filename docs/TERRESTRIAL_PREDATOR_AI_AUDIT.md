# Terrestrial predator and independent-animal vanilla AI audit

Issue: #10
Validated gameplay commit: `13d45b9b4f8f846cf0c9ee7964d72037c4c5436b`

The five species in this batch do not share a safe generic predator controller. Fox and Ocelot
have trust mechanics, Cat is tamable, Polar Bear is a context-sensitive neutral mob, and Panda
uses genes plus multi-tick action states. Living Ecology therefore never invents combat targets
for this batch and only adds bounded retreat or distant home-range return while vanilla navigation
is idle.

| Species | Vanilla remains authoritative for | Living Ecology overlay | Persistent ecology |
| --- | --- | --- | --- |
| Fox | trusted UUIDs/defense, held items, sleep/sit, stalking, crouch and pounce, prey goals and breeding | low-health/fear retreat and nocturnal distant home return only outside atomic states | wild forest home range and vanilla-love capacity |
| Ocelot | trust, player avoidance, fish temptation, prey and breeding | low-health/fear retreat and nocturnal distant home return while idle | wild forest home range and vanilla-love capacity |
| Cat | tame/owner, commands, collar, owner bed/gift behavior and prey | untamed-only retreat and distant village home return | wild village home range; tamed Cats are excluded |
| Polar Bear | neutral anger, persistent anger target, standing attack and adult-with-cub protection | anger observation; retreat/home return only when calm, cub-free and idle | cold home range; no automatic reproduction |
| Panda | genes, bamboo conditions, breeding and sit/eat/sneeze/roll/on-back/unhappy sequences | gene-aware retreat and distant home return only between vanilla actions | forest home range; no automatic reproduction |

## Safety invariants

- none of these controllers calls `setTarget`; prey, trusted defense, owner defense and neutral
  aggression remain limited to vanilla repertoires
- active targets are never cleared for ecological retreat
- trusted Fox/Ocelot state and tamed Cat owner/command state are persisted unchanged
- player-owned Cats never receive a wild territory or ecological navigation order
- Polar Bears with anger, a combat target, standing attack or a nearby cub receive no movement overlay
- Panda genes and atomic action states block all ecological movement
- all retreat/home targets require loaded chunks, land-domain safety and idle vanilla navigation
- the shared retreat planner tries bounded 75% and 50% arcs when confined terrain has no safe point
  at full range; hazard and solid-ground requirements remain unchanged
- overlapping predator territories use the existing `BORDERED` relationship and acquired rivalry;
  rivalry history never creates a combat target by itself
- all five species deliberately use no physical footprint

## Automated evidence

- JUnit checks exact five-species policy coverage, land domains, territory/footprint/reproduction
  profiles, forest/village/cold habitat response, activity gates and bordered predator relations
- Forge GameTests cover Fox trust/sleep/item state, trusting Ocelot retreat without aggression,
  tamed Cat owner/sit/bed/collar state, Polar Bear cub/anger/standing ownership, Panda genes/actions,
  geometric predator overlap and acquired rivalry
- the Ocelot integration fixture is explicitly grounded before pathfinding because newly created
  GameTest entities have not received their first vanilla physics tick
- Java 17 gate: `compileJava`, 37/37 JUnit tests, 36/36 Forge GameTests and `build` PASS

Human visual QA for stalking, retreat pacing, territory overlap and Minecraft feel remains
`PENDING` by policy.
