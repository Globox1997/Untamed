package net.untamed.init;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
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
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_SAVANNA), MobCategory.CREATURE, EntityInit.LION, ConfigInit.CONFIG.lionSpawnweight, 1, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_SAVANNA), MobCategory.CREATURE, EntityInit.LIONESS, ConfigInit.CONFIG.lionessSpawnWeight, 1, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_SAVANNA), MobCategory.CREATURE, EntityInit.RHINO, ConfigInit.CONFIG.rhinoSpawnWeight, 2, 4);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_RIVER), MobCategory.CREATURE, EntityInit.CAPYBARA, ConfigInit.CONFIG.capybaraSpawnWeight, 2, 3);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_OCEAN), MobCategory.CREATURE, EntityInit.OCTOPUS, ConfigInit.CONFIG.octopusSpawnWeight, 1, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_TAIGA), MobCategory.CREATURE, EntityInit.KIWI, ConfigInit.CONFIG.kiwiSpawnWeight, 2, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_FOREST), MobCategory.CREATURE, EntityInit.BLACK_BEAR, ConfigInit.CONFIG.blackBearSpawnWeight, 1, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_SAVANNA), MobCategory.CREATURE, EntityInit.BUFFALO, ConfigInit.CONFIG.buffaloSpawnWeight, 2, 4);
        BiomeModifications.addSpawn(BiomeSelectors.tag(ConventionalBiomeTags.IS_PLAINS), MobCategory.CREATURE, EntityInit.BISON, ConfigInit.CONFIG.bisonSpawnWeight, 2, 4);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_SAVANNA), MobCategory.CREATURE, EntityInit.VULTURE, ConfigInit.CONFIG.vultureSpawnWeight, 1, 3);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_JUNGLE), MobCategory.CREATURE, EntityInit.BLACK_PANTHER, ConfigInit.CONFIG.blackPantherSpawnWeight, 1, 2);
        BiomeModifications.addSpawn(BiomeSelectors.tag(BiomeTags.IS_SAVANNA), MobCategory.CREATURE, EntityInit.HYENA, ConfigInit.CONFIG.hyenaSpawnWeight, 2, 4);

//        ConventionalBiomeTags.IS_ICY Penguin
//        BiomeTags.IS_FOREST Racoon
//        BiomeTags.IS_SAVANNA Elephant
//        IS_MOUNTAIN Snow Leopard
        // Swamp and river alligator
//        ConventionalBiomeTags.IS_AQUATIC_ICY seal
        // IS_RIVER hippo
        // IS_SAVANNA Giraffe

//        Raccoon        minecraft:sweet_berries, minecraft:apple, minecraft:melon_slice, minecraft:cod
//        Elephant        minecraft:wheat, minecraft:hay_block, minecraft:apple
//        snow leopard    all meat
//        Penguin        minecraft:cod, minecraft:salmon, minecraft:tropical_fish
//        Alligator        minecraft:cod, minecraft:salmon, minecraft:chicken, minecraft:rabbit
//        Seal        minecraft:cod, minecraft:salmon
//        Hippo        minecraft:wheat, minecraft:hay_block, minecraft:melon_slice
//        Giraffe        minecraft:wheat, minecraft:hay_block, minecraft:apple
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
