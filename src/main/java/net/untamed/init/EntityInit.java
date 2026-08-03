package net.untamed.init;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.untamed.UntamedMain;
import net.untamed.entity.*;

@SuppressWarnings("unchecked")
public class EntityInit {

    public static final EntityType<LionEntity> LION = register(
            "lion", 12090190, 10703370, EntityType.Builder.of(LionEntity::new, MobCategory.CREATURE).sized(1.4F, 1.4F).clientTrackingRange(10).build());
    public static final EntityType<LionessEntity> LIONESS = register(
            "lioness", 12090190, 13070382, EntityType.Builder.of(LionessEntity::new, MobCategory.CREATURE).sized(1.4F, 1.4F).clientTrackingRange(10).build());
    public static final EntityType<RhinoEntity> RHINO = register(
            "rhino", 7233109, 9272431, EntityType.Builder.of(RhinoEntity::new, MobCategory.CREATURE).sized(1.4F, 1.4F).clientTrackingRange(10).build());
    public static final EntityType<CapybaraEntity> CAPYBARA = register(
            "capybara", 4204056, 7949103, EntityType.Builder.of(CapybaraEntity::new, MobCategory.CREATURE).sized(1.4F, 1.4F).clientTrackingRange(10).build());
    public static final EntityType<OctopusEntity> OCTOPUS = register(
            "octopus", 12893370, 11433813, EntityType.Builder.of(OctopusEntity::new, MobCategory.CREATURE).sized(1.4F, 1.4F).clientTrackingRange(10).build());
    public static final EntityType<KiwiEntity> KIWI = register(
            "kiwi", 7160104, 11895390, EntityType.Builder.of(KiwiEntity::new, MobCategory.CREATURE).sized(0.54F, 0.64F).clientTrackingRange(10).build());
    public static final EntityType<BlackBearEntity> BLACK_BEAR = register(
            "black_bear", 2434343, 4078651, EntityType.Builder.of(BlackBearEntity::new, MobCategory.CREATURE).sized(1.2F, 1.0F).clientTrackingRange(10).build());
    public static final EntityType<BuffaloEntity> BUFFALO = register(
            "buffalo", 5190172, 9523743, EntityType.Builder.of(BuffaloEntity::new, MobCategory.CREATURE).sized(1.4F, 1.7F).clientTrackingRange(10).build());

    private static <T extends Entity> EntityType<T> register(String id, int primaryColor, int secondaryColor, EntityType<T> entityType) {
        if (primaryColor != 0) {
            Item item = Registry.register(BuiltInRegistries.ITEM, UntamedMain.identifierOf(id + "_spawn_egg"),
                    new SpawnEggItem((EntityType<? extends Mob>) entityType, primaryColor, secondaryColor, new Item.Properties()));
            ItemGroupEvents.modifyEntriesEvent(ItemInit.UNTAMED_ITEM_GROUP).register(entries -> entries.prepend(item));
        }
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, UntamedMain.identifierOf(id), entityType);
    }

    public static void init() {
        // Attributes
        FabricDefaultAttributeRegistry.register(LION, LionEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(LIONESS, LionessEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(RHINO, RhinoEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(CAPYBARA, CapybaraEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(OCTOPUS, OctopusEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(KIWI, KiwiEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(BLACK_BEAR, BlackBearEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(BUFFALO, BuffaloEntity.createAttributes());
    }
}
