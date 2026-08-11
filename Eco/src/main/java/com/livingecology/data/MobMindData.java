package com.livingecology.data;

import com.livingecology.ai.BehaviorUtil;
import com.livingecology.territory.TerritoryContext;
import com.livingecology.territory.TerritoryManager;
import com.livingecology.util.HashNoise;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Compact per-entity state stored in Forge's persistent entity data.
 * No capability/network dependency is necessary for the server-side prototype.
 */
public final class MobMindData {
    private MobMindData() {}

    private static final String ROOT = "LivingEcologyMind";
    private static final String INITIALIZED = "initialized";
    private static final String TERRITORY_ID = "territoryId";
    private static final String LAST_DECAY = "lastDecay";
    private static final String LAST_TRUST_DECAY = "lastTrustDecay";
    private static final String LAST_THREAT_TIME = "lastThreatTime";
    private static final String LAST_THREAT_POS = "lastThreatPos";
    private static final String LAST_THREAT_UUID = "lastThreatUuid";
    private static final String THREAT_SCORE = "threatScore";
    private static final String PERSONALITY = "personality";
    private static final String UNIQUE_TRAIT = "uniqueTrait";
    private static final String BOSS = "boss";
    private static final String RESTING = "resting";
    private static final String VICTORIES = "victories";
    private static final String BIRTH_TIME = "birthGameTime";
    private static final String NEXT_TERRITORY_CHECK = "nextTerritoryCheck";
    private static final String NEXT_PATROL = "nextPatrol";
    private static final String LAST_MOVE_CHECK = "lastMoveCheck";
    private static final String LAST_MOVE_X = "lastMoveX";
    private static final String LAST_MOVE_Y = "lastMoveY";
    private static final String LAST_MOVE_Z = "lastMoveZ";
    private static final String STUCK_WINDOWS = "stuckWindows";
    private static final String NEXT_REPRODUCTION = "nextReproduction";

    public static boolean supports(Entity entity) {
        return SpeciesType.from(entity).isPresent();
    }

    public static CompoundTag get(Mob mob) {
        CompoundTag persistent = mob.getPersistentData();
        if (!persistent.contains(ROOT, Tag.TAG_COMPOUND)) {
            persistent.put(ROOT, new CompoundTag());
        }
        return persistent.getCompound(ROOT);
    }

    public static void initialize(Mob mob, ServerLevel level) {
        Optional<SpeciesType> maybeSpecies = SpeciesType.from(mob);
        if (maybeSpecies.isEmpty()) return;

        CompoundTag tag = get(mob);
        if (tag.getBoolean(INITIALIZED)) {
            // Old saves or entities copied by commands can still need their physical boss modifier.
            if (tag.getBoolean(BOSS) && !tag.getBoolean("bossPhysicalApplied")) {
                applyBossPhysical(mob, tag);
            }
            return;
        }

        SpeciesType species = maybeSpecies.get();
        SpeciesProfile profile = SpeciesProfile.of(species);
        long seed = HashNoise.mix64(mob.getUUID().getMostSignificantBits()
                ^ Long.rotateLeft(mob.getUUID().getLeastSignificantBits(), 17)
                ^ (species.ordinal() * 0x9e3779b97f4a7c15L));
        RandomSource random = RandomSource.create(seed);

        for (AttributeType type : AttributeType.values()) {
            SpeciesProfile.IntRange range = profile.range(type);
            int value = range.min() + random.nextInt(range.max() - range.min() + 1);
            setAttribute(tag, type, value);
        }

        Personality personality = profile.personalities().get(random.nextInt(profile.personalities().size()));
        tag.putString(PERSONALITY, personality.name());
        applyPersonality(tag, personality);

        // Deliberately uncommon, but not so rare that prototype testing becomes painful.
        boolean unique = random.nextDouble() < 0.015D;
        boolean boss = profile.bossEligible() && random.nextDouble() < 0.004D;
        UniqueTrait trait = unique || boss ? UniqueTrait.defaultFor(species) : UniqueTrait.NONE;
        tag.putString(UNIQUE_TRAIT, trait.name());
        tag.putBoolean(BOSS, boss);
        applyUniqueTrait(tag, trait);

        for (StateType state : StateType.values()) {
            tag.putByte("state_" + state.key(), (byte) 0);
        }

        tag.putLong(BIRTH_TIME, level.getGameTime());
        tag.putLong(LAST_DECAY, level.getGameTime());
        tag.putLong(LAST_TRUST_DECAY, level.getGameTime());
        tag.putLong(TERRITORY_ID, 0L);
        tag.putLong(NEXT_TERRITORY_CHECK, 0L);
        tag.putLong(NEXT_PATROL, level.getGameTime() + 60L + random.nextInt(100));
        tag.putLong(LAST_MOVE_CHECK, level.getGameTime());
        tag.putDouble(LAST_MOVE_X, mob.getX());
        tag.putDouble(LAST_MOVE_Y, mob.getY());
        tag.putDouble(LAST_MOVE_Z, mob.getZ());
        tag.putInt(STUCK_WINDOWS, 0);
        tag.putLong(NEXT_REPRODUCTION, level.getGameTime() + 1200L + random.nextInt(2400));
        tag.putBoolean(INITIALIZED, true);

        if (boss) applyBossPhysical(mob, tag);
    }

    private static void applyPersonality(CompoundTag tag, Personality personality) {
        for (Map.Entry<AttributeType, Integer> entry : personality.modifiers().entrySet()) {
            setAttribute(tag, entry.getKey(), getAttribute(tag, entry.getKey()) + entry.getValue());
        }
    }

    private static void applyUniqueTrait(CompoundTag tag, UniqueTrait trait) {
        switch (trait) {
            case SURVIVOR -> {
                addAttribute(tag, AttributeType.MEMORY, 25);
                addAttribute(tag, AttributeType.INSTINCT, 15);
            }
            case VIGILANT -> {
                addAttribute(tag, AttributeType.ALERT, 30);
                addAttribute(tag, AttributeType.PERCEPTION, 15);
            }
            case NOMAD -> {
                addAttribute(tag, AttributeType.TERRITORY, -40);
                addAttribute(tag, AttributeType.MEMORY, 15);
            }
            case GUARDIAN -> {
                addAttribute(tag, AttributeType.TERRITORY, 30);
                addAttribute(tag, AttributeType.CONSTITUTION, 15);
            }
            case ASTUTE -> {
                addAttribute(tag, AttributeType.MEMORY, 20);
                addAttribute(tag, AttributeType.INSTINCT, 15);
            }
            case SHY -> {
                addAttribute(tag, AttributeType.ALERT, 20);
                addAttribute(tag, AttributeType.INSTINCT, 15);
            }
            case FRIENDLY -> {
                addAttribute(tag, AttributeType.SOCIABILITY, 25);
                addAttribute(tag, AttributeType.TERRITORY, -10);
            }
            case VETERAN -> {
                addAttribute(tag, AttributeType.MEMORY, 20);
                addAttribute(tag, AttributeType.INSTINCT, 15);
            }
            case MATRIARCH -> {
                addAttribute(tag, AttributeType.SOCIABILITY, 10);
                addAttribute(tag, AttributeType.MEMORY, 10);
                addAttribute(tag, AttributeType.ALERT, 5);
            }
            case PACK_VETERAN -> {
                addAttribute(tag, AttributeType.MEMORY, 15);
                addAttribute(tag, AttributeType.INSTINCT, 10);
                addAttribute(tag, AttributeType.SOCIABILITY, 5);
            }
            case AMBUSHER -> {
                addAttribute(tag, AttributeType.PERCEPTION, 10);
                addAttribute(tag, AttributeType.INSTINCT, 10);
                addAttribute(tag, AttributeType.TERRITORY, 5);
            }
            case HORDE_BEARER -> {
                addAttribute(tag, AttributeType.SOCIABILITY, 15);
                addAttribute(tag, AttributeType.MEMORY, 10);
                addAttribute(tag, AttributeType.TERRITORY, 5);
            }
            case SENTINEL -> {
                addAttribute(tag, AttributeType.ALERT, 20);
                addAttribute(tag, AttributeType.PERCEPTION, 10);
                addAttribute(tag, AttributeType.TERRITORY, 10);
            }
            case COMMUNITY_ELDER -> {
                addAttribute(tag, AttributeType.MEMORY, 20);
                addAttribute(tag, AttributeType.SOCIABILITY, 15);
                addAttribute(tag, AttributeType.ALERT, 5);
            }
            case ROBUST -> addAttribute(tag, AttributeType.CONSTITUTION, 20);
            case NONE -> { }
        }
    }

    private static void applyBossPhysical(Mob mob, CompoundTag tag) {
        AttributeInstance maxHealth = mob.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(maxHealth.getBaseValue() * 1.30D);
            mob.setHealth(mob.getMaxHealth());
        }
        AttributeInstance attack = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attack != null) attack.setBaseValue(attack.getBaseValue() * 1.15D);
        addAttribute(tag, AttributeType.CONSTITUTION, 15);
        addAttribute(tag, AttributeType.MEMORY, 10);
        addAttribute(tag, AttributeType.ALERT, 10);
        addAttribute(tag, AttributeType.SOCIABILITY, 5);
        tag.putBoolean("bossPhysicalApplied", true);
    }

    private static void addAttribute(CompoundTag tag, AttributeType type, int delta) {
        setAttribute(tag, type, getAttribute(tag, type) + delta);
    }

    public static int getAttribute(Mob mob, AttributeType type) {
        return getAttribute(get(mob), type);
    }

    private static int getAttribute(CompoundTag tag, AttributeType type) {
        return tag.getByte("attr_" + type.key()) & 0xFF;
    }

    private static void setAttribute(CompoundTag tag, AttributeType type, int value) {
        tag.putByte("attr_" + type.key(), (byte) Mth.clamp(value, 0, 100));
    }

    public static int getState(Mob mob, StateType state) {
        return get(mob).getByte("state_" + state.key()) & 0xFF;
    }

    public static void setState(Mob mob, StateType state, int value) {
        get(mob).putByte("state_" + state.key(), (byte) Mth.clamp(value, 0, 5));
    }

    public static void addState(Mob mob, StateType state, int stacks) {
        setState(mob, state, getState(mob, state) + stacks);
    }

    public static void decayStates(Mob mob, ServerLevel level) {
        CompoundTag tag = get(mob);
        long now = level.getGameTime();
        long last = tag.getLong(LAST_DECAY);
        long elapsed = Math.max(0L, now - last);
        long steps = elapsed / 200L;
        if (steps > 0L) {
            int amount = (int) Math.min(5L, steps);
            for (StateType state : new StateType[]{StateType.FEAR, StateType.RAGE, StateType.STRESS,
                    StateType.CURIOSITY, StateType.FATIGUE, StateType.PAIN}) {
                setState(mob, state, getState(mob, state) - amount);
            }
            tag.putLong(LAST_DECAY, last + steps * 200L);
        }

        long trustLast = tag.getLong(LAST_TRUST_DECAY);
        long trustSteps = Math.max(0L, now - trustLast) / 2400L;
        if (trustSteps > 0L) {
            setState(mob, StateType.TRUST, getState(mob, StateType.TRUST) - (int) Math.min(5L, trustSteps));
            tag.putLong(LAST_TRUST_DECAY, trustLast + trustSteps * 2400L);
        }

        decayThreatMemory(mob, level);
    }

    private static void decayThreatMemory(Mob mob, ServerLevel level) {
        CompoundTag tag = get(mob);
        int score = tag.getInt(THREAT_SCORE);
        if (score <= 0) return;
        int memory = getAttribute(mob, AttributeType.MEMORY);
        long persistence = 400L + memory * 35L;
        long age = Math.max(0L, level.getGameTime() - tag.getLong(LAST_THREAT_TIME));
        if (age > persistence) {
            int decay = 1 + (int) ((age - persistence) / Math.max(200L, persistence / 4L));
            score = Math.max(0, score - decay * 10);
            tag.putInt(THREAT_SCORE, score);
            if (score == 0) tag.remove(LAST_THREAT_UUID);
        }
    }

    public static void rememberThreat(Mob mob, LivingEntity threat, int strength, ServerLevel level) {
        CompoundTag tag = get(mob);
        tag.putUUID(LAST_THREAT_UUID, threat.getUUID());
        tag.putLong(LAST_THREAT_POS, threat.blockPosition().asLong());
        tag.putLong(LAST_THREAT_TIME, level.getGameTime());
        tag.putInt(THREAT_SCORE, Mth.clamp(tag.getInt(THREAT_SCORE) + strength, 0, 100));
    }

    public static void rememberThreatPosition(Mob mob, BlockPos pos, int strength, ServerLevel level) {
        CompoundTag tag = get(mob);
        tag.putLong(LAST_THREAT_POS, pos.asLong());
        tag.putLong(LAST_THREAT_TIME, level.getGameTime());
        tag.putInt(THREAT_SCORE, Mth.clamp(tag.getInt(THREAT_SCORE) + strength, 0, 100));
    }

    public static Optional<LivingEntity> resolveThreat(Mob mob, ServerLevel level) {
        CompoundTag tag = get(mob);
        if (!tag.hasUUID(LAST_THREAT_UUID)) return Optional.empty();
        UUID id = tag.getUUID(LAST_THREAT_UUID);
        Entity entity = level.getEntity(id);
        if (entity instanceof LivingEntity living && BehaviorUtil.isValidCombatTarget(living)) return Optional.of(living);
        return Optional.empty();
    }

    /** Removes active threat memory when it points at a Creative/Spectator player. */
    public static void clearInvalidPlayerThreat(Mob mob, ServerLevel level) {
        CompoundTag tag = get(mob);
        if (!tag.hasUUID(LAST_THREAT_UUID)) return;
        Entity entity = level.getEntity(tag.getUUID(LAST_THREAT_UUID));
        if (entity instanceof net.minecraft.world.entity.player.Player player
                && (player.isCreative() || player.isSpectator())) {
            clearThreat(mob);
        }
    }

    public static void clearThreatIfMatches(Mob mob, UUID id) {
        CompoundTag tag = get(mob);
        if (tag.hasUUID(LAST_THREAT_UUID) && tag.getUUID(LAST_THREAT_UUID).equals(id)) clearThreat(mob);
    }

    public static void clearThreat(Mob mob) {
        CompoundTag tag = get(mob);
        tag.remove(LAST_THREAT_UUID);
        tag.remove(LAST_THREAT_POS);
        tag.putInt(THREAT_SCORE, 0);
        tag.putLong(LAST_THREAT_TIME, 0L);
    }

    public static Optional<BlockPos> lastThreatPos(Mob mob) {
        CompoundTag tag = get(mob);
        if (!tag.contains(LAST_THREAT_POS, Tag.TAG_LONG)) return Optional.empty();
        return Optional.of(BlockPos.of(tag.getLong(LAST_THREAT_POS)));
    }

    public static int threatScore(Mob mob) { return get(mob).getInt(THREAT_SCORE); }
    public static long lastThreatTime(Mob mob) { return get(mob).getLong(LAST_THREAT_TIME); }

    public static Personality personality(Mob mob) {
        String name = get(mob).getString(PERSONALITY);
        try { return Personality.valueOf(name); }
        catch (IllegalArgumentException ignored) { return Personality.NORMAL; }
    }

    public static UniqueTrait uniqueTrait(Mob mob) {
        String name = get(mob).getString(UNIQUE_TRAIT);
        try { return UniqueTrait.valueOf(name); }
        catch (IllegalArgumentException ignored) { return UniqueTrait.NONE; }
    }

    public static boolean isUnique(Mob mob) { return uniqueTrait(mob) != UniqueTrait.NONE; }
    public static boolean isBoss(Mob mob) { return get(mob).getBoolean(BOSS); }

    public static void setResting(Mob mob, boolean resting) { get(mob).putBoolean(RESTING, resting); }
    public static boolean isResting(Mob mob) { return get(mob).getBoolean(RESTING); }

    public static long territoryId(Mob mob) { return get(mob).getLong(TERRITORY_ID); }
    public static void setTerritoryId(Mob mob, long id) { get(mob).putLong(TERRITORY_ID, id); }

    public static long nextTerritoryCheck(Mob mob) { return get(mob).getLong(NEXT_TERRITORY_CHECK); }
    public static void setNextTerritoryCheck(Mob mob, long tick) { get(mob).putLong(NEXT_TERRITORY_CHECK, tick); }

    public static boolean patrolDue(Mob mob, ServerLevel level) {
        return level.getGameTime() >= get(mob).getLong(NEXT_PATROL);
    }

    public static void scheduleNextPatrol(Mob mob, ServerLevel level, int minTicks, int maxTicks) {
        int min = Math.max(1, minTicks);
        int max = Math.max(min, maxTicks);
        int extra = max == min ? 0 : mob.getRandom().nextInt(max - min + 1);
        get(mob).putLong(NEXT_PATROL, level.getGameTime() + min + extra);
    }

    /**
     * Small navigation watchdog. It samples displacement instead of iterating missed ticks, so it remains safe
     * when the server tick rate is changed. Two consecutive low-motion windows count as a stall.
     */
    public static boolean movementStalled(Mob mob, ServerLevel level, int sampleTicks, double minMovement) {
        CompoundTag tag = get(mob);
        long now = level.getGameTime();
        long last = tag.getLong(LAST_MOVE_CHECK);
        if (now - last < Math.max(5, sampleTicks)) return tag.getInt(STUCK_WINDOWS) >= 2;

        double dx = mob.getX() - tag.getDouble(LAST_MOVE_X);
        double dy = mob.getY() - tag.getDouble(LAST_MOVE_Y);
        double dz = mob.getZ() - tag.getDouble(LAST_MOVE_Z);
        double movedSqr = dx * dx + dy * dy + dz * dz;
        int windows = movedSqr < minMovement * minMovement && !mob.getNavigation().isDone()
                ? Math.min(5, tag.getInt(STUCK_WINDOWS) + 1) : 0;

        tag.putLong(LAST_MOVE_CHECK, now);
        tag.putDouble(LAST_MOVE_X, mob.getX());
        tag.putDouble(LAST_MOVE_Y, mob.getY());
        tag.putDouble(LAST_MOVE_Z, mob.getZ());
        tag.putInt(STUCK_WINDOWS, windows);
        return windows >= 2;
    }

    public static int stuckWindows(Mob mob) { return get(mob).getInt(STUCK_WINDOWS); }

    public static boolean reproductionDue(Mob mob, ServerLevel level) {
        return level.getGameTime() >= get(mob).getLong(NEXT_REPRODUCTION);
    }

    public static void scheduleNextReproduction(Mob mob, ServerLevel level, long baseTicks, double ecologicalScale) {
        double scale = Math.max(0.01D, Math.min(100.0D, ecologicalScale));
        long delay = Math.max(200L, (long) Math.ceil(Math.max(200L, baseTicks) / scale));
        long jitter = Math.max(1L, delay / 4L);
        delay += mob.getRandom().nextInt((int) Math.min(Integer.MAX_VALUE, jitter + 1L));
        get(mob).putLong(NEXT_REPRODUCTION, level.getGameTime() + delay);
    }

    public static long nextReproduction(Mob mob) { return get(mob).getLong(NEXT_REPRODUCTION); }

    public static int victories(Mob mob) { return get(mob).getInt(VICTORIES); }

    public static void recordVictory(Mob mob, ServerLevel level) {
        CompoundTag tag = get(mob);
        int wins = tag.getInt(VICTORIES) + 1;
        tag.putInt(VICTORIES, wins);
        SpeciesType species = SpeciesType.from(mob).orElse(null);
        if (species == null) return;

        if (!isUnique(mob) && wins >= 8 && mob.getRandom().nextFloat() < 0.10F) {
            UniqueTrait trait = UniqueTrait.defaultFor(species);
            tag.putString(UNIQUE_TRAIT, trait.name());
            applyUniqueTrait(tag, trait);
        }

        SpeciesProfile profile = SpeciesProfile.of(species);
        if (!isBoss(mob) && profile.bossEligible() && isUnique(mob) && wins >= 20
                && mob.getRandom().nextFloat() < 0.05F) {
            tag.putBoolean(BOSS, true);
            applyBossPhysical(mob, tag);
        }
    }

    public static long ageTicks(Mob mob, ServerLevel level) {
        return Math.max(0L, level.getGameTime() - get(mob).getLong(BIRTH_TIME));
    }

    public static int thinkInterval(Mob mob) {
        int alert = getAttribute(mob, AttributeType.ALERT);
        return Mth.clamp(18 - alert / 10, 6, 16);
    }

    public static int calculateAdaptation(Mob mob, ServerLevel level) {
        int memory = getAttribute(mob, AttributeType.MEMORY);
        int instinct = getAttribute(mob, AttributeType.INSTINCT);
        int perception = getAttribute(mob, AttributeType.PERCEPTION);
        int alert = getAttribute(mob, AttributeType.ALERT);
        int constitution = getAttribute(mob, AttributeType.CONSTITUTION);
        int social = getAttribute(mob, AttributeType.SOCIABILITY);

        double value = memory * 0.18D + instinct * 0.28D + perception * 0.12D
                + alert * 0.10D + constitution * 0.07D;

        TerritoryContext context = TerritoryManager.contextForMob(mob, level);
        value += switch (context.ownZone()) {
            case CORE -> 18.0D;
            case INNER -> 12.0D;
            case OUTER -> 5.0D;
            case OUTSIDE -> 0.0D;
        };

        int allies = BehaviorUtil.countNearbyAllies(mob, level, 14.0D);
        value += Math.min(15.0D, allies * (social / 100.0D) * 4.0D);
        if (threatScore(mob) >= 40) value += Math.min(8.0D, memory / 15.0D);

        value -= getState(mob, StateType.STRESS) * 6.0D;
        value -= getState(mob, StateType.FATIGUE) * 5.0D;
        value -= getState(mob, StateType.PAIN) * 3.0D;
        if (getState(mob, StateType.FEAR) >= 4) value -= 8.0D;
        if (context.noMansLand()) value -= 5.0D;

        return Mth.clamp((int) Math.round(value), 0, 100);
    }
}
