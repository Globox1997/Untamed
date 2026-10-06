package net.untamed.entity.ai.hyena;

import net.minecraft.world.entity.LivingEntity;
import net.untamed.entity.HyenaEntity;

import java.util.ArrayList;
import java.util.List;

// Shared state of one clan hunt: a test run at the herd, then a long chase of the animal the leader picked out
public class HyenaHunt {

    private final HyenaEntity leader;
    private final List<HyenaEntity> hunters = new ArrayList<>();
    private LivingEntity prey;
    private boolean chasing;
    private boolean finished;

    public HyenaHunt(HyenaEntity leader, LivingEntity prey, List<HyenaEntity> hunters) {
        this.leader = leader;
        this.prey = prey;
        this.hunters.addAll(hunters);
    }

    public HyenaEntity getLeader() {
        return this.leader;
    }

    public List<HyenaEntity> getHunters() {
        return this.hunters;
    }

    public LivingEntity getPrey() {
        return this.prey;
    }

    public boolean isChasing() {
        return this.chasing;
    }

    public void startChase(LivingEntity markedPrey) {
        this.prey = markedPrey;
        this.chasing = true;
    }

    public void addHunter(HyenaEntity hunter) {
        if (!this.hunters.contains(hunter)) {
            this.hunters.add(hunter);
        }
        hunter.setHunt(this);
    }

    public boolean isFinished() {
        return this.finished || !this.prey.isAlive() || !this.leader.isAlive();
    }

    public void finish(int cooldown) {
        if (this.finished) {
            return;
        }
        this.finished = true;
        for (HyenaEntity hunter : this.hunters) {
            if (hunter.getHunt() == this) {
                hunter.setHunt(null);
                if (cooldown > 0) {
                    hunter.setHuntCooldown(cooldown);
                }
            }
        }
    }
}
