package com.livingecology.ai;

import com.livingecology.data.MovementDomain;
import com.livingecology.data.SpeciesType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Explicit vanilla-ownership audit for issue #16's raid society. */
public record IllagerSocietySpeciesPolicy(VanillaOwner vanillaOwner,
                                          SocietyRole societyRole,
                                          KnowledgeGroup knowledgeGroup,
                                          MovementDomain expectedDomain,
                                          boolean persistentEcology) {
    public enum VanillaOwner {
        PILLAGER_CROSSBOW_PATROL_RAID_AND_COMBAT,
        VINDICATOR_AXE_JOHNNY_PATROL_RAID_AND_COMBAT,
        EVOKER_SPELLS_VEX_SUMMONING_RAID_AND_COMBAT,
        WITCH_POTION_DRINKING_THROWING_RAID_AND_COMBAT,
        RAVAGER_RIDER_ROAR_STUN_RAID_AND_COMBAT,
        ILLUSIONER_SPELLS_ILLUSIONS_RANGED_RAID_AND_COMBAT
    }

    public enum SocietyRole {
        RANGED_RAIDER,
        MELEE_RAIDER,
        SPELLCASTER,
        SUPPORT_CASTER,
        RAID_BEAST,
        ILLUSION_CASTER
    }

    public enum KnowledgeGroup {
        ILLAGERS
    }

    private static final EnumMap<SpeciesType, IllagerSocietySpeciesPolicy> POLICIES =
            new EnumMap<>(SpeciesType.class);

    static {
        put(SpeciesType.PILLAGER, VanillaOwner.PILLAGER_CROSSBOW_PATROL_RAID_AND_COMBAT,
                SocietyRole.RANGED_RAIDER);
        put(SpeciesType.VINDICATOR, VanillaOwner.VINDICATOR_AXE_JOHNNY_PATROL_RAID_AND_COMBAT,
                SocietyRole.MELEE_RAIDER);
        put(SpeciesType.EVOKER, VanillaOwner.EVOKER_SPELLS_VEX_SUMMONING_RAID_AND_COMBAT,
                SocietyRole.SPELLCASTER);
        put(SpeciesType.WITCH, VanillaOwner.WITCH_POTION_DRINKING_THROWING_RAID_AND_COMBAT,
                SocietyRole.SUPPORT_CASTER);
        put(SpeciesType.RAVAGER, VanillaOwner.RAVAGER_RIDER_ROAR_STUN_RAID_AND_COMBAT,
                SocietyRole.RAID_BEAST);
        put(SpeciesType.ILLUSIONER, VanillaOwner.ILLUSIONER_SPELLS_ILLUSIONS_RANGED_RAID_AND_COMBAT,
                SocietyRole.ILLUSION_CASTER);
    }

    public static Optional<IllagerSocietySpeciesPolicy> of(SpeciesType species) {
        return Optional.ofNullable(POLICIES.get(species));
    }

    public static IllagerSocietySpeciesPolicy require(SpeciesType species) {
        IllagerSocietySpeciesPolicy policy = POLICIES.get(species);
        if (policy == null) throw new IllegalArgumentException(
                "Species is outside Illager society batch: " + species);
        return policy;
    }

    public static Set<SpeciesType> species() {
        return Set.copyOf(POLICIES.keySet());
    }

    public static Map<SpeciesType, IllagerSocietySpeciesPolicy> all() {
        return Map.copyOf(POLICIES);
    }

    private static void put(SpeciesType species, VanillaOwner owner, SocietyRole role) {
        POLICIES.put(species, new IllagerSocietySpeciesPolicy(owner, role,
                KnowledgeGroup.ILLAGERS, MovementDomain.LAND, true));
    }
}
