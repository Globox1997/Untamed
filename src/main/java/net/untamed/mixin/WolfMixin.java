package net.untamed.mixin; // Match your mod's mixin package

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.TamableAnimal;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.untamed.entity.GazelleEntity;
import net.untamed.entity.KiwiEntity;
import net.untamed.entity.RaccoonEntity;

@Mixin(Wolf.class)
public abstract class WolfMixin extends TamableAnimal {

    protected WolfMixin(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void untamed$addPreyTargets(CallbackInfo ci) {
        this.targetSelector.add(5, new NonTameRandomTargetGoal<>(
            this,
            Animal.class,
            false,
            entity -> entity instanceof RaccoonEntity
                   || entity instanceof KiwiEntity
                   || entity instanceof GazelleEntity
        ));
    }
}
