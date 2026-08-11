# Living Ecology — Aquatic/Amphibious Vanilla AI Audit

Issue: `#7` — aquatic schools and amphibious ecology
Minecraft/Forge baseline: Java 17, Minecraft 1.20.1, Forge 47.4.10, official mappings

This audit is normative for the aquatic controller. Living Ecology may add local memory,
perceived social warnings, habitat stress and carefully bounded cohesion, but it must not replace
the vanilla systems listed below.

| Species | Vanilla owner to preserve | Living Ecology overlay | Territory / reproduction |
| --- | --- | --- | --- |
| Cod | `AbstractSchoolingFish` leader/follower goals and fish navigation | Vanilla school retained; strong perceived threat may temporarily order a water-only escape | No persistent territory; no vanilla breeding |
| Salmon | `AbstractSchoolingFish` with Salmon school-size rules | Same as Cod; local visible school alarms enabled | No persistent territory; no vanilla breeding |
| Tropical Fish | `AbstractSchoolingFish` plus variant/group spawn rules | Same as Cod; variants are never collapsed into a shared identity | No persistent territory; no vanilla breeding |
| Pufferfish | `PufferfishPuffGoal`, puff/deflate timers and scary-mob detection | Observation/memory only while defense owns the response | No persistent territory; no vanilla breeding |
| Squid | `SquidRandomMovementGoal`, `SquidFleeGoal`, ink and movement vectors | Idle same-species cohesion uses a bounded movement vector; hurt/ink flee is never overwritten | No persistent territory; no vanilla breeding |
| Glow Squid | Squid vector controller plus Glow Squid dark-time behavior | Same safe vector overlay as Squid; no path-navigation takeover | No persistent territory; no vanilla breeding |
| Dolphin | Air/moistness handling, treasure, item play and swim-with-player goals | Idle-only cohesion; strong direct danger may interrupt only when air/item/treasure constraints are safe | Persistent home range; no vanilla breeding |
| Axolotl | Brain activities, play-dead, hunting, hydration and bucket state | Idle-only amphibious cohesion/escape; no movement during play-dead or critical Brain memories | Persistent home range; `VANILLA_LOVE` |
| Turtle | Home beach, travel, panic, water return, egg carrying and laying goals | Observation, memory, habitat and territory accounting only | Persistent home range; `VANILLA_LOVE` with vanilla egg lifecycle |
| Tadpole | Brain activities, fish navigation, age persistence and metamorphosis | Idle-only water cohesion/escape when no critical Brain task owns movement | No persistent territory; metamorphosis remains vanilla, not breeding |
| Frog | Brain swim/jump/tongue/lay-spawn activities and amphibious navigation | Observation, memory, habitat and territory accounting only | Persistent home range; `VANILLA_LOVE` with vanilla frogspawn lifecycle |

## Cross-cutting safety decisions

- Social warnings require a nearby same-species sender, line of sight to that sender, a legal
  loaded threat, plausible receiver range and decaying confidence. Creative/Spectator targets are
  rejected by the existing central target gate.
- Cohesion samples at most 12 loaded group members and never loads a missing chunk.
- `WATER` targets must contain water. `AMPHIBIOUS` targets must be water or safe land.
- Pufferfish, Turtle and Frog are explicit low-interference exceptions.
- No new blocks, textures, attributes, HP or damage modifiers are introduced by this batch.
- Human visual QA remains pending for school naturalness, density, pacing and Minecraft feel.
