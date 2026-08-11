package com.livingecology.event;

import com.livingecology.LivingEcology;
import com.livingecology.ai.AdaptiveAiManager;
import com.livingecology.ai.BehaviorUtil;
import com.livingecology.command.ModCommands;
import com.livingecology.data.*;
import com.livingecology.environment.EnvironmentManager;
import com.livingecology.environment.ReproductionManager;
import com.livingecology.territory.RelationService;
import com.livingecology.territory.TerritoryManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = LivingEcology.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CommonForgeEvents {
    private CommonForgeEvents() {}

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        TerritoryManager.tickLevel(level);
        ReproductionManager.tickLevel(level);
        AdaptiveAiManager.tickLevel(level);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        // May fire before the chunk is FULL: initialization is data-only and performs no world scan/mutation.
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob mob)) return;
        if (MobMindData.supports(mob)) MobMindData.initialize(mob, level);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !MobMindData.supports(mob)) return;
        LivingEntity next = event.getNewTarget();
        // Canceling keeps the previous target; replacing with null actually clears it.
        if (next != null && !BehaviorUtil.isValidCombatTarget(next)) {
            event.setNewTarget(null);
            return;
        }
        if (next instanceof Mob other && MobMindData.supports(other)) {
            SpeciesType a = SpeciesType.from(mob).orElse(null);
            SpeciesType b = SpeciesType.from(other).orElse(null);
            if (a != null && b != null) {
                var relation = RelationService.natural(a, b);
                if (relation.affinity() >= 70 && relation.rivalry() <= 20) event.setNewTarget(null);
            }
        }
    }

    @SubscribeEvent
    public static void onAllowDespawn(MobSpawnEvent.AllowDespawn event) {
        Mob mob = event.getEntity();
        if (!MobMindData.supports(mob) || !(mob.level() instanceof ServerLevel level)) return;
        // Fast-forward is a laboratory tool. Protect nearby observed test actors from natural despawn only.
        if (TerritoryManager.simulationScale(level) <= 1.0D) return;
        boolean nearObserver = level.players().stream().anyMatch(player -> player.distanceToSqr(mob) <= 160.0D * 160.0D);
        if (nearObserver) event.setResult(Event.Result.DENY);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof Mob victim) || !MobMindData.supports(victim)) return;
        MobMindData.initialize(victim, level);
        MobMindData.setResting(victim, false);

        SpeciesType victimSpecies = SpeciesType.from(victim).orElse(null);
        if (victimSpecies == null) return;
        SpeciesProfile profile = SpeciesProfile.of(victimSpecies);
        int pain = event.getAmount() >= 6.0F ? 2 : 1;
        MobMindData.addState(victim, StateType.PAIN, pain);

        Entity sourceEntity = event.getSource().getEntity();
        LivingEntity attacker = sourceEntity instanceof LivingEntity living ? living : null;
        if (attacker != null) {
            boolean valid = BehaviorUtil.isValidCombatTarget(attacker);
            if (valid) {
                int threatStrength = Math.min(35, 8 + Math.round(event.getAmount() * 3.0F));
                MobMindData.rememberThreat(victim, attacker, threatStrength, level);
                if (attacker instanceof Player) MobMindData.addState(victim, StateType.TRUST, event.getAmount() >= 6.0F ? -3 : -2);
                applyFamilyHitState(victim, profile, event.getAmount());
                TerritoryManager.recordAggression(victim, attacker, level, event.getAmount() >= 6.0F ? 3 : 1);
                propagateLocalAlarm(victim, attacker, level, profile);
                propagateCooperativeAlarm(victim, attacker, level);
            }
        } else if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            if (profile.habitatClass() != HabitatClass.LAVA && victimSpecies != SpeciesType.BLAZE
                    && victimSpecies != SpeciesType.MAGMA_CUBE) {
                MobMindData.addState(victim, StateType.FEAR, 2);
                MobMindData.addState(victim, StateType.STRESS, 1);
            }
        }
    }

    private static void applyFamilyHitState(Mob victim, SpeciesProfile profile, float amount) {
        switch (profile.behaviorFamily()) {
            case PASSIVE_HERD, PASSIVE_WANDERER, AQUATIC, FLYING_PASSIVE, SOCIETY_PEACEFUL, SPECIAL_MOUNT -> {
                MobMindData.addState(victim, StateType.FEAR, 2);
                MobMindData.addState(victim, StateType.STRESS, 1);
            }
            case PREDATOR -> {
                MobMindData.addState(victim, StateType.RAGE, 1);
                if (amount >= victim.getMaxHealth() * 0.25F) MobMindData.addState(victim, StateType.FEAR, 1);
            }
            case COLONY, ARTHROPOD, UNDEAD_HORDE, UNDEAD_COMBAT, HOSTILE_MELEE, HOSTILE_RANGED,
                    FLYING_HOSTILE, EXPLOSIVE, SOCIETY_HOSTILE, GUARDIAN, BOSS ->
                    MobMindData.addState(victim, StateType.RAGE, amount >= 6.0F ? 2 : 1);
        }
    }

    private static void propagateLocalAlarm(Mob victim, LivingEntity attacker, ServerLevel level, SpeciesProfile profile) {
        int social = MobMindData.getAttribute(victim, AttributeType.SOCIABILITY);
        double radius = 6.0D + social * 0.12D;
        int inspected = 0;
        for (Mob ally : BehaviorUtil.nearbySameSpecies(victim, level, radius)) {
            if (inspected++ >= 12) break;
            MobMindData.initialize(ally, level);
            if (!ally.hasLineOfSight(victim) && ally.distanceToSqr(victim) > 36.0D) continue;
            MobMindData.rememberThreat(ally, attacker, Math.max(4, social / 8), level);
            switch (profile.behaviorFamily()) {
                case PASSIVE_HERD, PASSIVE_WANDERER, AQUATIC, FLYING_PASSIVE, SOCIETY_PEACEFUL, SPECIAL_MOUNT -> {
                    MobMindData.addState(ally, StateType.FEAR, 1);
                    MobMindData.addState(ally, StateType.STRESS, 1);
                }
                default -> MobMindData.addState(ally, StateType.RAGE, 1);
            }
        }
    }

    private static void propagateCooperativeAlarm(Mob victim, LivingEntity attacker, ServerLevel level) {
        SpeciesType victimSpecies = SpeciesType.from(victim).orElse(null);
        if (victimSpecies == null) return;
        for (Mob helper : level.getEntitiesOfClass(Mob.class, victim.getBoundingBox().inflate(13.0D),
                m -> m != victim && m.isAlive() && MobMindData.supports(m))) {
            SpeciesType helperSpecies = SpeciesType.from(helper).orElse(null);
            if (helperSpecies == null || helperSpecies == victimSpecies) continue;
            var relation = RelationService.natural(victimSpecies, helperSpecies);
            if (relation.affinity() < 65 || relation.rivalry() > 25) continue;
            if (!helper.hasLineOfSight(victim) && helper.distanceToSqr(victim) > 49.0D) continue;
            MobMindData.initialize(helper, level);
            MobMindData.rememberThreat(helper, attacker, 8, level);
            if (canFight(SpeciesProfile.of(helperSpecies).behaviorFamily()) && helper.getTarget() == null) helper.setTarget(attacker);
            TerritoryManager.recordCooperation(helper, victim, level, 1);
        }
    }

    private static boolean canFight(BehaviorFamily family) {
        return switch (family) {
            case PREDATOR, COLONY, ARTHROPOD, UNDEAD_HORDE, UNDEAD_COMBAT, HOSTILE_MELEE,
                    HOSTILE_RANGED, FLYING_HOSTILE, EXPLOSIVE, SOCIETY_HOSTILE, GUARDIAN, BOSS -> true;
            default -> false;
        };
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        LivingEntity dead = event.getEntity();
        Entity source = event.getSource().getEntity();

        if (source instanceof Mob killer && MobMindData.supports(killer)) {
            MobMindData.initialize(killer, level);
            MobMindData.recordVictory(killer, level);
            if (dead instanceof Mob deadMob && MobMindData.supports(deadMob)) {
                TerritoryManager.recordAggression(deadMob, killer, level, 6);
            }
        }

        if (dead instanceof Mob deadMob && MobMindData.supports(deadMob)) {
            SpeciesType species = SpeciesType.from(deadMob).orElse(null);
            if (species == null) return;
            BehaviorFamily family = SpeciesProfile.of(species).behaviorFamily();
            for (Mob ally : BehaviorUtil.nearbySameSpecies(deadMob, level, 14.0D)) {
                MobMindData.initialize(ally, level);
                MobMindData.addState(ally, StateType.STRESS, 2);
                if (switch (family) {
                    case PASSIVE_HERD, PASSIVE_WANDERER, AQUATIC, FLYING_PASSIVE, PREDATOR, SOCIETY_PEACEFUL, SPECIAL_MOUNT -> true;
                    default -> false;
                }) MobMindData.addState(ally, StateType.FEAR, 1);
                else MobMindData.addState(ally, StateType.RAGE, 1);
                if (source instanceof LivingEntity attacker && BehaviorUtil.isValidCombatTarget(attacker))
                    MobMindData.rememberThreat(ally, attacker, 20, level);
            }
        }
    }

    @SubscribeEvent
    public static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (event.getChild() == null || !(event.getParentA().level() instanceof ServerLevel level)) return;
        Mob parent = event.getParentA();
        if (!MobMindData.supports(parent) || !MobMindData.supports(event.getChild())) return;
        Mob child = event.getChild();
        MobMindData.initialize(parent, level);
        MobMindData.initialize(child, level);
        TerritoryManager.recordBirth(parent, child, level);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getEntity().isSpectator()) return;
        if (event.getTarget() instanceof Animal animal && MobMindData.supports(animal) && animal.isFood(event.getItemStack())) {
            MobMindData.initialize(animal, level);
            MobMindData.addState(animal, StateType.TRUST, 1);
            MobMindData.addState(animal, StateType.CURIOSITY, 1);
        }
    }

    @SubscribeEvent
    public static void onTame(AnimalTameEvent event) {
        if (!(event.getAnimal().level() instanceof ServerLevel level) || !MobMindData.supports(event.getAnimal())) return;
        Mob animal = event.getAnimal();
        MobMindData.initialize(animal, level);
        MobMindData.setState(animal, StateType.TRUST, 5);
        MobMindData.setState(animal, StateType.FEAR, 0);
        MobMindData.setResting(animal, false);
        MobMindData.setTerritoryId(animal, 0L);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) return;
        EnvironmentManager.onNaturalBlockBroken(level, event.getPos(), event.getState());
        if (event.getState().is(Blocks.COBWEB)) TerritoryManager.onCobwebBroken(level, event.getPos());
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getAffectedBlocks().isEmpty()) return;
        EnvironmentManager.onExplosion(level, event.getAffectedBlocks().get(0), event.getAffectedBlocks().size());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event);
    }
}
