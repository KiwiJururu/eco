# Living Ecology — Canonical Design Decisions

This document is normative. It exists so implementation can continue without reconstructing design intent from chat history.

## Philosophy

- Real animals use gameplay-compressed ethology rather than scientific simulation for its own sake.
- Fantasy mobs preserve Minecraft identity and archetypes.
- Mobs are never omniscient: information must be perceived, remembered or socially transmitted.
- Difficulty should come from behavior, positioning, memory, cooperation and adaptation rather than large raw health/damage increases.
- Individuals of the same species can differ.
- Personality modifies tendencies but cannot grant behavior outside a species repertoire.
- Territorial advantage is behavioral/informational, not a direct combat-stat buff.
- The player is not given artificial debuffs merely for interacting with ecology; the environment reacts organically.
- Ecological systems must produce effects the player can perceive.

## Core attributes

Permanent/base attributes use 0–100 values:
1. Territory
2. Memory
3. Perception
4. Alert
5. Sociability
6. Instinct
7. Constitution
8. Personality — categorical modifiers
9. Adaptation — situational/calculated 0–100
10. Habits — diurnal/nocturnal/variable and species-specific rest

### Personalities

- Normal
- Lazy: Alert -10, Instinct -5
- Worried: Alert +15, Instinct +5
- Playful: Sociability +10, Perception +5
- Aggressive: Territory +10, Instinct +5, Constitution +5
- Weak: Constitution -20
- Curious: Perception +10, Memory +5
- Cautious: Alert +10, Memory +10, Instinct +5
- Bold: Constitution +10, Instinct +5, Alert -5
- Protective: Sociability +10, Territory +10
- Independent: Sociability -15, Instinct +10
- Stubborn: Territory +10, Constitution +5

## Temporary states

States stack from 0 to V:
- Fear
- Rage
- Stress
- Confidence
- Curiosity
- Fatigue
- Pain

State is not behavior. Species interpret state differently. Immediate state decays; memory can remain. Canonical rule: **State passes; memory remains.**

## Sleep / rest

Use vanilla model/pose capabilities where possible; do not require new texture/model libraries merely for sleep. Effective alert/perception/adaptation may be reduced while sleeping. Rest must never leave an entity permanently frozen when a valid high-priority event occurs.

## Unique and Boss mobs

### Unique Mob

Rare identity earned through survival/experience rather than forced visuals. Examples include Survivor, Vigilant, Nomad, Guardian, Cunning, Skittish, Friendly, Veteran. Species may have specific identities such as Cow Matriarch or Wolf Veteran.

### Boss Mob

Very rare where appropriate. Better attributes/adaptation/group influence, not a health sponge. Common mobs may become Unique and, only where sensible, Boss through survival/importance.

Special mobs can destabilize regions. Regional dominance is a calculated 0–100 ecological value.

## Ecological anomalies

A rare regional over-dominance can become an Ecological Anomaly: zombie infestation, huge spider colony, dominant wolf pack, bee collapse, etc. The ecosystem seeks stability, not forced equality. A cave fully dominated by Zombies can be a valid emergent result.

Killing a mobile Boss/core can reduce organization without magically deleting survivors.

## Territory

Territory is a persistent recognized region, not simply the entity's current position.

Possible ownership scales:
- individual
- group
- community
- structure

Zones:
- outer / influence
- inner / domain
- core / center

Cores can be dens, nests, hives, villages, Bastions, Monuments, homes, rest sites or an appropriate mobile boss/core.

### Territorial pressure

0–100, derived from inhabitants, territoriality, occupation time, core, cohesion, population and negative pressure from deaths/scarcity/threat. Territory can grow and shrink. Outer area is normally lost before the core. Abandoned territory may be remembered and reconquered.

### Territorial footprints

Physical marks should become more legible toward the core. Categories:
- traces
- modification
- infrastructure
- core

Examples: sparse Spider webs outside, denser webbing toward the nest. Species that do not plausibly modify terrain should not receive heavy footprints.

Footprints decay and can overlap during conquest/contested zones. Destruction of a core footprint may raise alert/rage/stress/negative memory when reasonably perceived.

### Griefing safety

Prefer natural blocks and protect player constructions/farms/machines. Conceptual configuration:
- `territorialGriefing = NONE | NATURAL_ONLY | FULL`
- default `NATURAL_ONLY`
- `protectPlayerStructures = true`

Physical changes use strict ecological budgets; never spam blocks or force-load chunks.

## Rivalry and affinity

Rivalry and affinity are separate 0–100 concepts.

### Rivalry
- natural
- acquired
- effective

It raises conflict probability; it is not a binary permission gate. It can be asymmetric where implementation supports it.

### Affinity
- natural
- acquired
- effective

Affinity is not the same as Sociability. High affinity can enable alerts, aid, shared defense/offense and alliances. Enemy-of-enemy history may build affinity. Alliances can decay.

### Territorial tension

Situational between territories. Inputs can include overlap, rivalry, territoriality, core proximity, resources, deaths, incursions and population pressure; reduced by tolerance, resources, distance and imbalance.

Escalation model:
coexistence → friction → contested border → incursions → conflict → conquest

### Cooperation

Situational 0–100 from affinity, sociability, history, shared threat, proximity, strength, memory and risk. Can represent mutualism, commensalism, protection or opportunism. No global diplomatic telepathy.

## Prototype anchor relationships

These are canonical anchors unless later explicitly changed:

- Spider ↔ Zombie: symbiotic/opportunistic cooperation. Zombies may learn corridors but are not immune to webs.
- Wolf ↔ Zombie: highly aggressive war.
- Wolf ↔ Spider: regulated coexistence / no-man's-land border; incidents can escalate.
- Cow: control species for herd/fear/memory/environment behavior.

## Environment

Each ecological region tracks a compact environmental situation:
- resources
- cover
- stability
- species-specific habitability

Disturbances may come from wildfire, deforestation, explosions, major water change, mass combat/deaths, core destruction or extreme construction.

Cause may be natural, player or mob. Ecology responds regardless; cognitive memory distinguishes cause only if the entity could perceive/learn it.

Recovery model:
Disturbed → Initial recovery → Recovery → Stable

Unloaded recovery is abstract/time-based. Physical recovery on natural/protected blocks is gradual. Players can help through ordinary actions rather than special debuffs/buffs.

## Population, migration and communication

- Unloaded population is represented abstractly.
- Carrying capacity depends on environment/habitat.
- Migration/dispersal can be driven by overcrowding, resources and threat as well as war.
- Information is individual: observed, remembered and optionally shared.
- Communities can have compact shared memory, but never global telepathy.
- Young can socially learn basic reactions, not inherit complete memories.
- Older individuals can accumulate more useful memory/experience rather than receiving a raw intelligence stat.

## Reproduction

Ecological reproduction complements vanilla breeding rather than replacing species identity. It must respect:
- species reproduction mode
- local carrying capacity/resources
- population pressure
- territory availability
- cooldowns

Birth can increase territory population and may contribute gradually to territorial growth, but territory must remain capped by species profile and environmental conditions.

## Performance architecture

### Loaded near players
Full entity AI/adaptive behavior.

### Unloaded generated regions
Compact persistent abstract territory/community/ecology state.

### Never-generated regions
Deterministic/procedural ecological history may be derived when discovered rather than simulated every tick beforehand.

Use logical game time and elapsed-time math. Never:
- assume 1 tick = 50 ms for ecological calculations;
- use `System.currentTimeMillis` for game simulation;
- use `Thread.sleep`;
- replay every missed tick after unload/offline time;
- force-load neighboring chunks to answer ecological queries.

Persist compact causes/state: territory ID, species, center/core, seed, population, pressure, maturity, last update, flags and small relation/environment deltas. Never save a per-block territory map.

Physical footprints are candidate/budget based. Cosmetic work may be deferred under load.

## Ecological timescale

Timescale accelerates abstract ecological time/observation; it must not multiply physical block-edit budgets per tick. Changing scale rebases relevant clocks so a new scale is not applied retroactively.

Testing may use high scales such as 10x/50x/100x. Supported prototype mobs can receive temporary test-oriented natural-despawn protection while accelerated observation is active; normal scale restores vanilla behavior.

## Human QA boundary

Automated tests own objective correctness. Human review owns:
- whether movement looks natural
- whether interactions feel believable/fun
- footprint visual density
- pacing
- whether the mod still feels like Minecraft

A technically green species may therefore remain `visual_qa=PENDING` until user review.
