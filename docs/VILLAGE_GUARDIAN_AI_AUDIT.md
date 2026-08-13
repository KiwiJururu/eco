# Village, trader and golem vanilla AI audit

Issue: #14
Validated gameplay commit: `b1faea5cb27ad69f683b8ed2ceb255343f14ad49`

Village actors combine a scheduled Brain, trading UIs, transient trader lifecycles and two distinct
guardian combat systems. Their Living Ecology controller therefore records community context and
bounded local defense knowledge without selecting targets, issuing paths or changing vanilla state.

| Species | Vanilla remains authoritative for | Living Ecology overlay | Persistent ecology |
| --- | --- | --- | --- |
| Villager | Brain schedule/POIs, profession, gossip, trading, restock, breeding and golem spawning | habitat, community and local hurt/defense memory | village community |
| Wandering Trader | offers/trading, potions, wander target, travel and despawn timer | habitat and isolated caravan memory | none; transient |
| Iron Golem | village defense targets, persistent anger, attack, creator flag, flower and repair | habitat, guardian community and local defense report | guardian community |
| Snow Golem | ranged target/attack, pumpkin/shearing, climate damage and vanilla snow trail | habitat, guardian community and local defense report | guardian community |
| Trader Llama | trader leash/defense, spit, caravan movement and despawn | habitat and isolated caravan memory | none; transient |

## Safety invariants

- the controller never calls `moveTo`, assigns a target, changes Brain memories, trades, profession,
  despawn delay, leash state, persistent anger, creator flags, pumpkin state or guardian animation
- active Villager Brain/navigation paths and all guardian/trader target-selection goals remain
  vanilla-owned
- only Villager, Iron Golem and Snow Golem profiles form persistent communities; Wandering Trader
  and Trader Llama clear or refuse territory association
- a current legal vanilla target or direct attacker from the previous 200 entity ticks may be
  reported locally; reports are not omniscient or permanent
- a pass informs at most eight loaded receivers within 16 blocks, requires visible/very-near source
  contact and keeps the threat within 1.7 times receiver perception
- reports only write threat memory and fear/rage state; receivers retain their existing target and
  navigation
- village and trader-caravan knowledge groups are isolated
- Villager/Iron Golem natural affinity remains symmetric at 100 and `SYMBIOTIC`
- all five profiles use `FootprintType.NONE`; ecological code cannot edit village or player-built
  structures
- no species in the batch enters ecological reproduction
- territory and memory queries remain local/loaded and never force-load chunks

## Automated evidence

- JUnit checks exact five-species policy coverage, affinity, persistent/transient roles, isolated
  knowledge groups, LAND domains and zero-footprint/reproduction contracts
- Forge GameTests preserve Villager Brain/profession/trade/path state, Wandering Trader and Trader
  Llama lifecycle, resident/guardian territories, local hurt reports without targets, bounded eight
  receiver work, Iron Golem anger/creator/flower, Snow Golem ranged/pumpkin state, player blocks and
  Creative target sanitization
- the full GameTest suite was rerun repeatedly on persisted world state to validate deterministic
  territory join fixtures
- Java 17 gate: `compileJava`, 61/61 JUnit tests, 64/64 Forge GameTests and `build` PASS

Human visual QA for village schedules/crowd flow, guardian response timing and trader caravan pacing
remains `PENDING` by policy.
