package net.untamed.init;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.untamed.entity.ai.CarcassTracker;

public class EventInit {

    public static void init() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> CarcassTracker.onDeath(entity));
    }
}
