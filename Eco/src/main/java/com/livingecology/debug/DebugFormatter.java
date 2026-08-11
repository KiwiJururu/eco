package com.livingecology.debug;

import com.livingecology.data.*;
import com.livingecology.environment.EnvironmentManager;
import com.livingecology.environment.EnvironmentSnapshot;
import com.livingecology.territory.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

import java.util.Optional;

public final class DebugFormatter {
    private DebugFormatter() {}

    public static void sendMob(ServerPlayer player, Mob mob, ServerLevel level) {
        Optional<SpeciesType> maybeSpecies = SpeciesType.from(mob);
        if (maybeSpecies.isEmpty()) {
            line(player, ChatFormatting.RED, "[Ecologia] Esta entidade ainda não faz parte do protótipo.");
            return;
        }

        MobMindData.initialize(mob, level);
        TerritoryRecord territory = TerritoryManager.ensureTerritory(mob, level);
        TerritoryContext context = TerritoryManager.contextForMob(mob, level);
        SpeciesType species = maybeSpecies.get();
        EnvironmentSnapshot env = EnvironmentManager.snapshot(level, mob.blockPosition(), species);

        line(player, ChatFormatting.GOLD, "===== Living Ecology: " + species.displayName() + " =====");
        line(player, ChatFormatting.WHITE,
                "UUID: " + mob.getUUID() + " | Vida: " + one(mob.getHealth()) + "/" + one(mob.getMaxHealth()));
        line(player, ChatFormatting.AQUA,
                "Personalidade: " + MobMindData.personality(mob).displayName()
                        + " | Único: " + MobMindData.uniqueTrait(mob).displayName()
                        + " | Chefe: " + yesNo(MobMindData.isBoss(mob)));

        line(player, ChatFormatting.YELLOW, "Atributos (0-100)");
        line(player, ChatFormatting.GRAY,
                "T " + a(mob, AttributeType.TERRITORY)
                        + " | M " + a(mob, AttributeType.MEMORY)
                        + " | P " + a(mob, AttributeType.PERCEPTION)
                        + " | A " + a(mob, AttributeType.ALERT));
        line(player, ChatFormatting.GRAY,
                "S " + a(mob, AttributeType.SOCIABILITY)
                        + " | I " + a(mob, AttributeType.INSTINCT)
                        + " | C " + a(mob, AttributeType.CONSTITUTION)
                        + " | Adaptação " + MobMindData.calculateAdaptation(mob, level));

        line(player, ChatFormatting.YELLOW, "Estados (0-V)");
        line(player, ChatFormatting.GRAY,
                "Medo " + roman(s(mob, StateType.FEAR))
                        + " | Raiva " + roman(s(mob, StateType.RAGE))
                        + " | Estresse " + roman(s(mob, StateType.STRESS))
                        + " | Confiança " + roman(s(mob, StateType.TRUST)));
        line(player, ChatFormatting.GRAY,
                "Curiosidade " + roman(s(mob, StateType.CURIOSITY))
                        + " | Fadiga " + roman(s(mob, StateType.FATIGUE))
                        + " | Dor " + roman(s(mob, StateType.PAIN))
                        + " | Repouso: " + yesNo(MobMindData.isResting(mob)));

        line(player, ChatFormatting.LIGHT_PURPLE,
                "Memória: ameaça=" + MobMindData.threatScore(mob)
                        + " | vitórias=" + MobMindData.victories(mob)
                        + " | idade=" + MobMindData.ageTicks(mob, level) + " ticks");
        MobMindData.lastThreatPos(mob).ifPresent(pos ->
                line(player, ChatFormatting.DARK_PURPLE, "Última posição de ameaça: " + shortPos(pos)));

        if (territory == null) {
            line(player, ChatFormatting.GREEN, "Território: nenhum território consolidado.");
        } else {
            line(player, ChatFormatting.GREEN,
                    "Território #" + territory.id() + " | Zona: " + context.ownZone().name()
                            + " | Estado: " + territory.state().name());
            line(player, ChatFormatting.DARK_GREEN,
                    "Centro " + shortPos(territory.center())
                            + " | raio " + territory.radiusChunks() + " chunks"
                            + " | pressão " + territory.pressure()
                            + " | maturidade " + territory.maturity()
                            + " | população~ " + territory.population());
            line(player, ChatFormatting.DARK_GREEN,
                    "Pegada " + territory.footprintProgress() + "/" + territory.desiredFootprint()
                            + " | líder: " + (territory.leader() == null ? "não" : territory.leader().toString()));
        }

        line(player, context.contested() ? ChatFormatting.RED : ChatFormatting.BLUE,
                "Relações locais: tensão " + context.tension()
                        + " | afinidade " + context.affinity()
                        + " | contestado: " + yesNo(context.contested())
                        + " | terra de ninguém: " + yesNo(context.noMansLand()));
        if (context.strongest() != null) {
            line(player, ChatFormatting.BLUE,
                    "Influência dominante: " + context.strongest().species().displayName()
                            + " #" + context.strongest().id()
                            + (context.second() == null ? "" : " | 2ª: " + context.second().species().displayName() + " #" + context.second().id()));
        }

        line(player, ChatFormatting.DARK_AQUA,
                "Ambiente: recursos " + env.resources()
                        + " | cobertura " + env.coverage()
                        + " | estabilidade " + env.stability()
                        + " | habitabilidade " + env.habitability());
        line(player, ChatFormatting.DARK_GRAY,
                "Escala ecológica: x" + one(TerritoryManager.simulationScale(level)) + " (blocos continuam limitados por orçamento/tick)");
    }

    public static void sendLocation(ServerPlayer player, ServerLevel level, BlockPos pos) {
        TerritoryContext context = TerritoryManager.contextAt(level, pos, 0L);
        line(player, ChatFormatting.GOLD, "===== Living Ecology: local " + shortPos(pos) + " =====");
        if (context.strongest() == null) {
            line(player, ChatFormatting.GREEN, "Nenhuma influência territorial ativa registrada aqui.");
        } else {
            TerritoryRecord r = context.strongest();
            line(player, ChatFormatting.GREEN,
                    "Dominante: " + r.species().displayName() + " #" + r.id()
                            + " | pressão " + r.pressure() + " | maturidade " + r.maturity()
                            + " | estado " + r.state().name());
            if (context.second() != null) {
                line(player, ChatFormatting.BLUE,
                        "Segunda influência: " + context.second().species().displayName() + " #" + context.second().id());
            }
            line(player, context.contested() ? ChatFormatting.RED : ChatFormatting.BLUE,
                    "Tensão " + context.tension() + " | afinidade " + context.affinity()
                            + " | contestado " + yesNo(context.contested())
                            + " | terra de ninguém " + yesNo(context.noMansLand()));
        }
        for (SpeciesType species : SpeciesType.values()) {
            EnvironmentSnapshot env = EnvironmentManager.snapshot(level, pos, species);
            line(player, ChatFormatting.DARK_AQUA,
                    species.displayName() + " habitabilidade=" + env.habitability()
                            + " (R" + env.resources() + " C" + env.coverage() + " E" + env.stability() + ")");
        }
    }

    private static int a(Mob mob, AttributeType type) { return MobMindData.getAttribute(mob, type); }
    private static int s(Mob mob, StateType type) { return MobMindData.getState(mob, type); }
    private static String shortPos(BlockPos p) { return p.getX() + ", " + p.getY() + ", " + p.getZ(); }
    private static String yesNo(boolean value) { return value ? "sim" : "não"; }
    private static String one(double value) { return String.format(java.util.Locale.ROOT, "%.1f", value); }

    private static String roman(int value) {
        return switch (value) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> "0";
        };
    }

    private static void line(ServerPlayer player, ChatFormatting color, String text) {
        player.sendSystemMessage(Component.literal(text).withStyle(color));
    }
}
