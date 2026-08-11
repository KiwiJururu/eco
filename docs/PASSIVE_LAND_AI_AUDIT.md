# Common passive land vanilla AI audit

Issue: #9
Validated gameplay commit: `72cde05eb315dbffe93f6e996b2f8dbeeae34d32`

The twelve species in this batch share the `LAND` movement domain but have materially different
vanilla controllers. Living Ecology uses an explicit per-species ownership policy and issues
movement only while vanilla navigation is idle. Brain-driven and lifecycle-sensitive species are
observation-only.

| Species | Vanilla remains authoritative for | Living Ecology overlay | Persistent ecology |
| --- | --- | --- | --- |
| Sheep | wool, shearing and grass eating | idle herd cohesion, perceived-threat escape and local alarm | general home range and vanilla-love capacity |
| Pig | saddle, boost, riding and lightning conversion | idle herd cohesion and local alarm; no movement or reproduction while mounted | general home range and vanilla-love capacity |
| Chicken | egg timer, flap physics and jockey state | idle herd cohesion and local alarm outside jockey/navigation ownership | general home range and vanilla-love capacity |
| Rabbit | custom hop controls, garden raid and Killer Bunny combat | non-combat perceived-threat escape and distant home return | general home range, burrow marks and vanilla-love capacity |
| Horse | taming, owner, saddle, armor, riding and breeding | wild-only herd cohesion and distant home return | wild general home range and vanilla-love capacity |
| Donkey | taming, owner, chest, riding and horse hybrid breeding | wild-only herd cohesion and distant home return | wild general home range and vanilla-love capacity |
| Mule | taming, owner, chest and riding | wild-only herd cohesion and distant home return | wild general home range; sterile |
| Camel | `CamelAi`, sitting/pose transitions, dash and two-seat riding | panic observation only | desert home range and vanilla-love capacity |
| Goat | `GoatAi`, ram preparation/target, long jump and horns | ram-memory observation only | mountain home range and vanilla-love capacity |
| Llama | taming, owner, chest, caravan and spit target | wild, non-caravan idle cohesion/home return | mountain home range and vanilla-love capacity |
| Trader Llama | trader leash, defense and despawn lifecycle | observation only; no social-alarm relay | none |
| Sniffer | `SnifferAi`, sniff/search/dig and explored-position memory | panic observation only | general home range and vanilla-love capacity |

## Safety invariants

- all optional movement passes the land-domain safety check and never loads a missing chunk
- overlays run only while vanilla navigation is idle and respect passenger, leash, love and active
  species-specific states
- player-owned Horses, Donkeys, Mules and Llamas keep owner/mount semantics and never retain a
  wild persistent territory
- Camel is not treated as player-owned merely because vanilla `Camel.isTamed()` always returns
  true; its Brain still owns every movement decision
- mounted animals cannot enter automatic ecological love mode
- Killer Bunny combat, Llama caravan/spit, Goat ram and Sniffer search/dig states are never replaced
- Trader Llama remains non-territorial and non-reproductive throughout its trader lifecycle
- group scans are bounded to 12 members and local social alarms retain their existing bounded work
- Rabbit is the only species in this batch with a physical footprint, using the existing safe
  burrow-mark rules
- Bee territory association is capped at the same 48-block bound as hive recentering, preventing a
  stale distant colony record from trapping a new hive association

## Automated evidence

- JUnit checks exact twelve-species policy coverage, land domains, territory/reproduction profiles,
  footprints, habitat distinctions, social alarms and activity gates
- Forge GameTests cover Camel classification/sitting, tamed Donkey owner/chest state, ridden Pig
  mount/reproduction safety, Killer Bunny combat, Goat ram memory, Sniffer search, Llama caravan,
  Trader Llama lifecycle and Sheep vanilla-love capacity
- repeated SavedData regression validates that a Bee colony can still anchor after test layout
  changes without violating the bounded hive policy
- Java 17 gate: `compileJava`, 31/31 JUnit tests, 30/30 Forge GameTests and `build` PASS

Human visual QA for naturalness, land-herd pacing, burrow density and Minecraft feel remains
`PENDING` by policy.
