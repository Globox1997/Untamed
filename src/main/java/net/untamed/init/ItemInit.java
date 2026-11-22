package net.untamed.init;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.untamed.UntamedMain;
import net.untamed.item.ClawArrowItem;

public class ItemInit {

    // ALSO MAYBE CHANGE SOUND VOLUMES AGAIN

    // Item Group
    public static final ResourceKey<CreativeModeTab> UNTAMED_ITEM_GROUP = ResourceKey.create(Registries.CREATIVE_MODE_TAB, UntamedMain.identifierOf("item_group"));

    public static final Item RAW_LION = register("raw_lion", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).effect(new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.3F).build())));
    public static final Item COOKED_LION = register("cooked_lion", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.6F).build())));

    public static final Item RAW_RHINO = register("raw_rhino", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).build())));
    public static final Item COOKED_RHINO = register("cooked_rhino", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(9).saturationModifier(0.8F).build())));

    public static final Item RAW_CAPYBARA = register("raw_capybara", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).build())));
    public static final Item COOKED_CAPYBARA = register("cooked_capybara", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.7F).build())));

    public static final Item RAW_OCTOPUS = register("raw_octopus", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build())));
    public static final Item COOKED_OCTOPUS = register("cooked_octopus", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8F).build())));

    public static final Item RAW_KIWI = register("raw_kiwi", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).effect(new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.3F).build())));
    public static final Item COOKED_KIWI = register("cooked_kiwi", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.6F).build())));

    public static final Item RAW_BEAR = register("raw_bear", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).effect(new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.3F).build())));
    public static final Item COOKED_BEAR = register("cooked_bear", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.8F).build())));

    public static final Item RAW_BUFFALO = register("raw_buffalo", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).build())));
    public static final Item COOKED_BUFFALO = register("cooked_buffalo", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8F).build())));

    public static final Item RAW_BISON = register("raw_bison", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).build())));
    public static final Item COOKED_BISON = register("cooked_bison", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8F).build())));

    public static final Item RAW_VULTURE = register("raw_vulture", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).effect(new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.8F).build())));
    public static final Item COOKED_VULTURE = register("cooked_vulture", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.4F).build())));

    public static final Item RAW_PANTHER = register("raw_panther", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).effect(new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.3F).build())));
    public static final Item COOKED_PANTHER = register("cooked_panther", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.6F).build())));

    public static final Item RAW_HYENA = register("raw_hyena", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.1F).effect(new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.5F).build())));
    public static final Item COOKED_HYENA = register("cooked_hyena", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.5F).build())));

    public static final Item RAW_GAZELLE = register("raw_gazelle", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).build())));
    public static final Item COOKED_GAZELLE = register("cooked_gazelle", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8F).build())));

    public static final Item RAW_RACCOON = register("raw_raccoon", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).effect(new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.3F).build())));
    public static final Item COOKED_RACCOON = register("cooked_raccoon", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.6F).build())));

    public static final Item RAW_LEOPARD = register("raw_leopard", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).effect(new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.3F).build())));
    public static final Item COOKED_LEOPARD = register("cooked_leopard", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.6F).build())));

    public static final Item RAW_BIRD = register("raw_bird", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.3F).effect(new MobEffectInstance(MobEffects.HUNGER, 600, 0), 0.3F).fast().build())));
    public static final Item COOKED_BIRD = register("cooked_bird", new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.6F).fast().build())));

    public static final Item BISON_FUR = register("bison_fur", new Item(new Item.Properties()));
    public static final Item BISON_HORN = register("bison_horn", new Item(new Item.Properties()));
    public static final Item BLACK_BEAR_PELT = register("black_bear_pelt", new Item(new Item.Properties()));
    public static final Item CAPYBARA_HIDE = register("capybara_hide", new Item(new Item.Properties()));
    public static final Item HEAVY_BONE = register("heavy_bone", new Item(new Item.Properties()));
    public static final Item LION_CLAW = register("lion_claw", new Item(new Item.Properties()));
    public static final Item LION_PELT = register("lion_pelt", new Item(new Item.Properties()));
    public static final Item PANTHER_CLAW = register("panther_claw", new Item(new Item.Properties()));
    public static final Item RACCOON_FUR = register("raccoon_fur", new Item(new Item.Properties()));
    public static final Item SNOW_LEOPARD_FUR = register("snow_leopard_fur", new Item(new Item.Properties()));
    public static final Item CLAW_ARROW = register("claw_arrow", new ClawArrowItem(new Item.Properties()));
    public static final Item SPOTTED_PELT = register("spotted_pelt", new Item(new Item.Properties()));

    private static Item register(String id, Item item) {
        return register(UntamedMain.identifierOf(id), item);
    }

    private static Item register(ResourceLocation id, Item item) {
        ItemGroupEvents.modifyEntriesEvent(UNTAMED_ITEM_GROUP).register(entries -> entries.prepend(item));
        return Registry.register(BuiltInRegistries.ITEM, id, item);
    }

    public static void init() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, UNTAMED_ITEM_GROUP,
                FabricItemGroup.builder().icon(() -> new ItemStack(RAW_OCTOPUS)).title(Component.translatable("item.untamed.item_group")).build());
    }
}
