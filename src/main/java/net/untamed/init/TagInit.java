package net.untamed.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.untamed.UntamedMain;

public class TagInit {

    public static final TagKey<Block> LIONS_SPAWNABLE_ON = TagKey.create(Registries.BLOCK, UntamedMain.identifierOf("lions_spawnable_on"));
    public static final TagKey<Block> RHINOS_SPAWNABLE_ON = TagKey.create(Registries.BLOCK, UntamedMain.identifierOf("rhinos_spawnable_on"));
    public static final TagKey<Block> CAPYBARAS_SPAWNABLE_ON = TagKey.create(Registries.BLOCK, UntamedMain.identifierOf("capybaras_spawnable_on"));
    public static final TagKey<Block> OCTOPUSES_SPAWNABLE_ON = TagKey.create(Registries.BLOCK, UntamedMain.identifierOf("octopuses_spawnable_on"));
    public static final TagKey<Block> KIWIS_SPAWNABLE_ON = TagKey.create(Registries.BLOCK, UntamedMain.identifierOf("kiwi_spawnable_on"));
    public static final TagKey<Block> BLACK_BEARS_SPAWNABLE_ON = TagKey.create(Registries.BLOCK, UntamedMain.identifierOf("black_bears_spawnable_on"));
    public static final TagKey<Block> BUFFALOS_SPAWNABLE_ON = TagKey.create(Registries.BLOCK, UntamedMain.identifierOf("buffalos_spawnable_on"));
    public static final TagKey<Block> BISONS_SPAWNABLE_ON = TagKey.create(Registries.BLOCK, UntamedMain.identifierOf("bisons_spawnable_on"));
    public static final TagKey<Block> VULTURES_SPAWNABLE_ON = TagKey.create(Registries.BLOCK, UntamedMain.identifierOf("vultures_spawnable_on"));
    public static final TagKey<Block> BLACK_PANTHERS_SPAWNABLE_ON = TagKey.create(Registries.BLOCK, UntamedMain.identifierOf("black_panthers_spawnable_on"));
    public static final TagKey<Block> HYENAS_SPAWNABLE_ON = TagKey.create(Registries.BLOCK, UntamedMain.identifierOf("hyenas_spawnable_on"));

    public static final TagKey<Block> HERD_GRAZEABLE = TagKey.create(Registries.BLOCK, UntamedMain.identifierOf("herd_grazeable"));
    public static final TagKey<Block> HYENA_COOLING_SPOTS = TagKey.create(Registries.BLOCK, UntamedMain.identifierOf("hyena_cooling_spots"));
    public static final TagKey<Block> KIWI_PROBE_BLOCKS = TagKey.create(Registries.BLOCK, UntamedMain.identifierOf("kiwi_probe_blocks"));
    public static final TagKey<Block> KIWI_COVER = TagKey.create(Registries.BLOCK, UntamedMain.identifierOf("kiwi_cover"));
    public static final TagKey<Block> RHINO_WALLOW_BLOCKS = TagKey.create(Registries.BLOCK, UntamedMain.identifierOf("rhino_wallow_blocks"));

    public static final TagKey<Item> LION_FOOD = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("lion_food"));
    public static final TagKey<Item> LIONESS_FOOD = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("lioness_food"));
    public static final TagKey<Item> RHINO_FOOD = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("rhino_food"));
    public static final TagKey<Item> CAPYBARA_FOOD = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("capybara_food"));
    public static final TagKey<Item> OCTOPUS_FOOD = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("octopus_food"));
    public static final TagKey<Item> KIWI_FOOD = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("kiwi_food"));
    public static final TagKey<Item> BLACK_BEAR_FOOD = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("black_bear_food"));
    public static final TagKey<Item> BLACK_BEAR_SCAVENGE = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("black_bear_scavenge"));
    public static final TagKey<Item> OCTOPUS_COLLECTIBLES = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("octopus_collectibles"));
    public static final TagKey<Item> BUFFALO_FOOD = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("buffalo_food"));
    public static final TagKey<Item> BISON_FOOD = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("bison_food"));
    public static final TagKey<Item> VULTURE_FOOD = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("vulture_food"));
    public static final TagKey<Item> BLACK_PANTHER_FOOD = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("black_panther_food"));
    public static final TagKey<Item> HYENA_FOOD = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("hyena_food"));

    public static final TagKey<EntityType<?>> CAPYBARA_PREDATORS = TagKey.create(Registries.ENTITY_TYPE, UntamedMain.identifierOf("capybara_predators"));
    public static final TagKey<EntityType<?>> BUFFALO_THREATS = TagKey.create(Registries.ENTITY_TYPE, UntamedMain.identifierOf("buffalo_threats"));
    public static final TagKey<EntityType<?>> LION_PREY = TagKey.create(Registries.ENTITY_TYPE, UntamedMain.identifierOf("lion_prey"));
    public static final TagKey<EntityType<?>> LION_LARGE_PREY = TagKey.create(Registries.ENTITY_TYPE, UntamedMain.identifierOf("lion_large_prey"));
    public static final TagKey<EntityType<?>> LION_LIVESTOCK_PREY = TagKey.create(Registries.ENTITY_TYPE, UntamedMain.identifierOf("lion_livestock_prey"));
    public static final TagKey<EntityType<?>> HYENA_PREY = TagKey.create(Registries.ENTITY_TYPE, UntamedMain.identifierOf("hyena_prey"));
    public static final TagKey<EntityType<?>> HYENA_LARGE_PREY = TagKey.create(Registries.ENTITY_TYPE, UntamedMain.identifierOf("hyena_large_prey"));
    public static final TagKey<EntityType<?>> HYENA_LIVESTOCK_PREY = TagKey.create(Registries.ENTITY_TYPE, UntamedMain.identifierOf("hyena_livestock_prey"));
    public static final TagKey<EntityType<?>> VULTURE_RIVALS = TagKey.create(Registries.ENTITY_TYPE, UntamedMain.identifierOf("vulture_rivals"));
    public static final TagKey<EntityType<?>> KIWI_PREDATORS = TagKey.create(Registries.ENTITY_TYPE, UntamedMain.identifierOf("kiwi_predators"));
    public static final TagKey<EntityType<?>> OCTOPUS_PREY = TagKey.create(Registries.ENTITY_TYPE, UntamedMain.identifierOf("octopus_prey"));
    public static final TagKey<EntityType<?>> BLACK_PANTHER_PREY = TagKey.create(Registries.ENTITY_TYPE, UntamedMain.identifierOf("black_panther_prey"));
    public static final TagKey<EntityType<?>> BLACK_PANTHER_LIVESTOCK_PREY = TagKey.create(Registries.ENTITY_TYPE, UntamedMain.identifierOf("black_panther_livestock_prey"));
    public static final TagKey<EntityType<?>> BLACK_PANTHER_FISH = TagKey.create(Registries.ENTITY_TYPE, UntamedMain.identifierOf("black_panther_fish"));

    public static void init(){
    }
}
