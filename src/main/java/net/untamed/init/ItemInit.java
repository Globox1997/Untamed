package net.untamed.init;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.untamed.UntamedMain;

public class ItemInit {

    // Item Group
    public static final ResourceKey<CreativeModeTab> UNTAMED_ITEM_GROUP = ResourceKey.create(Registries.CREATIVE_MODE_TAB, UntamedMain.identifierOf("item_group"));

    public static final Item RAW_LION = register("raw_lion", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build())));
    public static final Item COOKED_LION = register("cooked_lion", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.8F).build())));

    public static final Item RAW_RHINO = register("raw_rhino", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build())));
    public static final Item COOKED_RHINO = register("cooked_rhino", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.8F).build())));

    public static final Item RAW_CAPYBARA = register("raw_capybara", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build())));
    public static final Item COOKED_CAPYBARA = register("cooked_capybara", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.8F).build())));

    public static final Item RAW_OCTOPUS = register("raw_octopus", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build())));
    public static final Item COOKED_OCTOPUS = register("cooked_octopus", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.8F).build())));

    public static final Item RAW_KIWI = register("raw_kiwi", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build())));
    public static final Item COOKED_KIWI = register("cooked_kiwi", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6F).build())));

    public static final Item RAW_BEAR = register("raw_bear", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build())));
    public static final Item COOKED_BEAR = register("cooked_bear", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6F).build())));

    public static final Item RAW_BUFFALO = register("raw_buffalo", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build())));
    public static final Item COOKED_BUFFALO = register("cooked_buffalo", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6F).build())));

    public static final Item RAW_BISON = register("raw_bison", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build())));
    public static final Item COOKED_BISON = register("cooked_bison", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6F).build())));

    public static final Item RAW_VULTURE = register("raw_vulture", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build())));
    public static final Item COOKED_VULTURE = register("cooked_vulture", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6F).build())));

    public static final Item RAW_PANTHER = register("raw_panther", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build())));
    public static final Item COOKED_PANTHER = register("cooked_panther", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.6F).build())));

    private static Item register(String id, Item item) {
        return register(UntamedMain.identifierOf(id), item);
    }

    private static Item register(ResourceLocation id, Item item) {
        ItemGroupEvents.modifyEntriesEvent(UNTAMED_ITEM_GROUP).register(entries -> entries.prepend(item));
        return Registry.register(BuiltInRegistries.ITEM, id, item);
    }

    public static void init() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, UNTAMED_ITEM_GROUP,
                FabricItemGroup.builder().icon(() -> new ItemStack(Items.ACACIA_BOAT)).title(Component.translatable("item.untamed.item_group")).build());
    }
}
