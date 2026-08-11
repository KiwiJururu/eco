package com.livingecology.event;

import com.livingecology.LivingEcology;
import com.livingecology.ai.AdaptiveAiManager;
import com.livingecology.ai.BehaviorUtil;
import com.livingecology.command.ModCommands;
import com.livingecology.data.*;
import com.livingecology.environment.EnvironmentManager;
import com.livingecology.territory.TerritoryManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
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
        AdaptiveAiManager.tickLevel(level);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        // EntityJoinLevelEvent may happen before its chunk is FULL. Initialization therefore never scans/changes the world.
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Mob mob)) return;
        if (MobMindData.supports(mob)) MobMindData.initialize(mob, level);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        if (!(event.getEntity() instanceof Mob victim) || !MobMindData.supports(victim)) return;
        MobMindData.initialize(victim, level);
        MobMindData.setResting(victim, false);

        int pain = event.getAmount() >= 6.0F ? 2 : 1;
        MobMindData.addState(victim, StateType.PAIN, pain);

        Entity sourceEntity = event.getSource().getEntity();
        LivingEntity attacker = sourceEntity instanceof LivingEntity living ? living : null;
        SpeciesType victimSpecies = SpeciesType.from(victim).orElse(null);
        if (attacker != null) {
            int threatStrength = Math.min(35, 8 + Math.round(event.getAmount() * 3.0F));
            MobMindData.rememberThreat(victim, attacker, threatStrength, level);
            if (attacker instanceof Player) MobMindData.addState(victim, StateType.TRUST, event.getAmount() >= 6.0F ? -3 : -2);
            switch (victimSpecies) {
                case COW -> {
                    MobMindData.addState(victim, StateType.FEAR, 2);
                    MobMindData.addState(victim, StateType.STRESS, 1);
                }
                case WOLF -> {
                    MobMindData.addState(victim, StateType.RAGE, 1);
                    if (event.getAmount() >= victim.getMaxHealth() * 0.25F) MobMindData.addState(victim, StateType.FEAR, 1);
                }
                case SPIDER -> MobMindData.addState(victim, StateType.RAGE, 2);
                case ZOMBIE -> MobMindData.addState(victim, StateType.RAGE, 1);
            }
            TerritoryManager.recordAggression(victim, attacker, level, event.getAmount() >= 6.0F ? 3 : 1);
            propagateLocalAlarm(victim, attacker, level);
            propagateSymbioticAlarm(victim, attacker, level);
        } else if (event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_FIRE)) {
            MobMindData.addState(victim, StateType.FEAR, victimSpecies == SpeciesType.ZOMBIE ? 0 : 2);
            MobMindData.addState(victim, StateType.STRESS, 1);
        }
    }

    private static void propagateLocalAlarm(Mob victim, LivingEntity attacker, ServerLevel level) {
        int social = MobMindData.getAttribute(victim, AttributeType.SOCIABILITY);
        double radius = 6.0D + social * 0.12D;
        SpeciesType species = SpeciesType.from(victim).orElse(null);
        if (species == null) return;

        for (Mob ally : BehaviorUtil.nearbySameSpecies(victim, level, radius)) {
            MobMindData.initialize(ally, level);
            if (!ally.hasLineOfSight(victim) && ally.distanceToSqr(victim) > 36.0D) continue;
            MobMindData.rememberThreat(ally, attacker, Math.max(4, social / 8), level);
            switch (species) {
                case COW -> {
                    MobMindData.addState(ally, StateType.FEAR, 1);
                    MobMindData.addState(ally, StateType.STRESS, 1);
                }
                case WOLF, SPIDER, ZOMBIE -> MobMindData.addState(ally, StateType.RAGE, 1);
            }
        }
    }

    private static void propagateSymbioticAlarm(Mob victim, LivingEntity attacker, ServerLevel level) {
        SpeciesType species = SpeciesType.from(victim).orElse(null);
        SpeciesType partner = species == SpeciesType.SPIDER ? SpeciesType.ZOMBIE
                : species == SpeciesType.ZOMBIE ? SpeciesType.SPIDER : null;
        if (partner == null) return;
        for (Mob helper : BehaviorUtil.species(level, victim.blockPosition(), partner, 12.0D)) {
            MobMindData.initialize(helper, level);
            if (!helper.hasLineOfSight(victim) && helper.distanceToSqr(victim) > 49.0D) continue;
            MobMindData.rememberThreat(helper, attacker, 8, level);
            MobMindData.addState(helper, StateType.RAGE, 1);
            TerritoryManager.recordCooperation(helper, victim, level, 1);
        }
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
            for (Mob ally : BehaviorUtil.nearbySameSpecies(deadMob, level, 14.0D)) {
                MobMindData.initialize(ally, level);
                MobMindData.addState(ally, StateType.STRESS, 2);
                if (species == SpeciesType.COW || species == SpeciesType.WOLF) MobMindData.addState(ally, StateType.FEAR, 1);
                else MobMindData.addState(ally, StateType.RAGE, 1);
                if (source instanceof LivingEntity attacker) MobMindData.rememberThreat(ally, attacker, 20, level);
            }
        }
    }


    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getLevel() instanceof ServerLevel level) || event.getEntity().isSpectator()) return;
        if (event.getTarget() instanceof Cow cow && cow.isFood(event.getItemStack())) {
            MobMindData.initialize(cow, level);
            MobMindData.addState(cow, StateType.TRUST, 1);
            MobMindData.addState(cow, StateType.CURIOSITY, 1);
        }
    }

    @SubscribeEvent
    public static void onTame(AnimalTameEvent event) {
        if (!(event.getAnimal().level() instanceof ServerLevel level) || !(event.getAnimal() instanceof Wolf wolf)) return;
        MobMindData.initialize(wolf, level);
        MobMindData.setState(wolf, StateType.TRUST, 5);
        MobMindData.setState(wolf, StateType.FEAR, 0);
        MobMindData.setResting(wolf, false);
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
        BlockPos center = event.getAffectedBlocks().get(0);
        EnvironmentManager.onExplosion(level, center, event.getAffectedBlocks().size());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event);
    }
}
