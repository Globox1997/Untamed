package net.untamed.init;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.untamed.entity.*;

public class SpawnInit {

    public static void init() {
        setSpawnRestriction();
        addSpawnEntries();
    }

    // MONSTER tries to spawn often, CREATURE tries more rarely to spawn + in groups
    private static void addSpawnEntries() {
//        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(BiomeKeys.BASALT_DELTAS), SpawnGroup.MONSTER, EntityInit.MINI_BLACKSTONE_GOLEM, ConfigInit.CONFIG.mini_blackstone_golem_spawn_weight,
//                1, 1);
//        BiomeModifications.addSpawn(BiomeSelectors.foundInTheNether().and(BiomeSelectors.excludeByKey(BiomeKeys.BASALT_DELTAS)), SpawnGroup.MONSTER, EntityInit.BLAZE_GUARDIAN,
//                ConfigInit.CONFIG.blaze_guardian_spawn_weight, 1, 1);
//        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.NETHER_FOSSIL_HAS_STRUCTURE), SpawnGroup.MONSTER, EntityInit.SOUL_REAPER, ConfigInit.CONFIG.nightmare_spawn_weight, 1, 1);
//
//        BiomeModifications.addSpawn(BiomeSelectors.tag(TagInit.IS_MUSHROOM), SpawnGroup.CREATURE, EntityInit.RED_FUNGUS, ConfigInit.CONFIG.fungus_spawn_weight, 2, 3);
//        BiomeModifications.addSpawn(BiomeSelectors.tag(TagInit.IS_MUSHROOM), SpawnGroup.CREATURE, EntityInit.BROWN_FUNGUS, ConfigInit.CONFIG.fungus_spawn_weight, 2, 3);
//        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.VILLAGE_PLAINS_HAS_STRUCTURE), SpawnGroup.MONSTER, EntityInit.ORC, ConfigInit.CONFIG.orc_spawn_weight, 2, 4);
//        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IGLOO_HAS_STRUCTURE), SpawnGroup.CREATURE, EntityInit.MAMMOTH, ConfigInit.CONFIG.mammoth_spawn_weight, 2, 2);
//        BiomeModifications.addSpawn(BiomeSelectors.foundInTheEnd(), SpawnGroup.CREATURE, EntityInit.ENDER_WHALE, ConfigInit.CONFIG.ender_whale_spawn_weight, 1, 1);
//        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_BADLANDS), SpawnGroup.CREATURE, EntityInit.IGUANA, ConfigInit.CONFIG.iguana_spawn_weight, 1, 2);
//        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.DESERT_PYRAMID_HAS_STRUCTURE), SpawnGroup.MONSTER, EntityInit.DESERT_RHINO, ConfigInit.CONFIG.desert_rhino_spawn_weight, 1, 1);
//        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.SWAMP_HUT_HAS_STRUCTURE), SpawnGroup.MONSTER, EntityInit.SHAMAN, ConfigInit.CONFIG.shaman_spawn_weight, 1, 1);
//        BiomeModifications.addSpawn(BiomeSelectors.foundInTheEnd().and(BiomeSelectors.excludeByKey(BiomeKeys.THE_END, BiomeKeys.END_BARRENS)), SpawnGroup.MONSTER, EntityInit.ENDERWARTHOG,
//                ConfigInit.CONFIG.enderwarthog_spawn_weight, 1, 1);
//        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_FOREST), SpawnGroup.CREATURE, EntityInit.DEER, ConfigInit.CONFIG.deer_spawn_weight, 2, 3);

        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_SAVANNA), MobCategory.CREATURE, EntityInit.LION, ConfigInit.CONFIG.lionSpawnweight, 1, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_SAVANNA), MobCategory.CREATURE, EntityInit.LIONESS, ConfigInit.CONFIG.lionessSpawnWeight, 1, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_SAVANNA), MobCategory.CREATURE, EntityInit.RHINO, ConfigInit.CONFIG.rhinoSpawnWeight, 2, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_RIVER), MobCategory.CREATURE, EntityInit.CAPYBARA, ConfigInit.CONFIG.capybaraSpawnWeight, 2, 3);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_OCEAN), MobCategory.CREATURE, EntityInit.OCTOPUS, ConfigInit.CONFIG.octopusSpawnWeight, 2, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_TAIGA), MobCategory.CREATURE, EntityInit.KIWI, ConfigInit.CONFIG.kiwiSpawnWeight, 2, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_FOREST), MobCategory.CREATURE, EntityInit.BLACK_BEAR, ConfigInit.CONFIG.blackBearSpawnWeight, 2, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_SAVANNA), MobCategory.CREATURE, EntityInit.BUFFALO, ConfigInit.CONFIG.buffaloSpawnWeight, 2, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_SAVANNA), MobCategory.CREATURE, EntityInit.BISON, ConfigInit.CONFIG.bisonSpawnWeight, 2, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_SAVANNA), MobCategory.CREATURE, EntityInit.VULTURE, ConfigInit.CONFIG.vultureSpawnWeight, 2, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_SAVANNA), MobCategory.CREATURE, EntityInit.BLACK_PANTHER, ConfigInit.CONFIG.blackPantherSpawnWeight, 2, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_SAVANNA), MobCategory.CREATURE, EntityInit.HYENA, ConfigInit.CONFIG.hyenaSpawnWeight, 2, 2);
    }

    private static void setSpawnRestriction() {
        SpawnPlacements.register(EntityInit.LION, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, LionEntity::checkLionEntitySpawnRules);
        SpawnPlacements.register(EntityInit.LIONESS, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, LionessEntity::checkLionessEntitySpawnRules);
        SpawnPlacements.register(EntityInit.RHINO, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, RhinoEntity::checkRhinoEntitySpawnRules);
        SpawnPlacements.register(EntityInit.CAPYBARA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, CapybaraEntity::checkCapybaraEntitySpawnRules);
        SpawnPlacements.register(EntityInit.OCTOPUS, SpawnPlacementTypes.IN_WATER, Heightmap.Types.OCEAN_FLOOR, OctopusEntity::checkOctopusEntitySpawnRules);
        SpawnPlacements.register(EntityInit.KIWI, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, KiwiEntity::checkKiwiEntitySpawnRules);
        SpawnPlacements.register(EntityInit.BLACK_BEAR, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlackBearEntity::checkBlackBearEntitySpawnRules);
        SpawnPlacements.register(EntityInit.BUFFALO, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BuffaloEntity::checkBuffaloEntitySpawnRules);
        SpawnPlacements.register(EntityInit.BISON, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BisonEntity::checkBisonEntitySpawnRules);
        SpawnPlacements.register(EntityInit.VULTURE, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, VultureEntity::checkVultureEntitySpawnRules);
        SpawnPlacements.register(EntityInit.BLACK_PANTHER, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlackPantherEntity::checkBlackPantherEntitySpawnRules);
        SpawnPlacements.register(EntityInit.HYENA, SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, HyenaEntity::checkHyenaEntitySpawnRules);
    }

}
