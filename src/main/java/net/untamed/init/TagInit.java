package net.untamed.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
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

    public static final TagKey<Item> LION_FOOD = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("lion_food"));
    public static final TagKey<Item> LIONESS_FOOD = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("lioness_food"));
    public static final TagKey<Item> RHINO_FOOD = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("rhino_food"));
    public static final TagKey<Item> CAPYBARA_FOOD = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("capybara_food"));
    public static final TagKey<Item> OCTOPUS_FOOD = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("octopus_food"));
    public static final TagKey<Item> KIWI_FOOD = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("kiwi_food"));
    public static final TagKey<Item> BLACK_BEAR_FOOD = TagKey.create(Registries.ITEM, UntamedMain.identifierOf("black_bear_food"));

    public static void init(){
    }
}
