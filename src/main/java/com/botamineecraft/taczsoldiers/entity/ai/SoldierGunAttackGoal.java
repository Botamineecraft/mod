package com.botamineecraft.taczsoldiers.entity.ai;

import com.botamineecraft.taczsoldiers.config.SoldiersConfig;
import com.botamineecraft.taczsoldiers.entity.SoldierEntity;
import com.botamineecraft.taczsoldiers.tacz.TaczBridge;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Ranged attack goal built around TACZ guns.
 * <p>
 * TACZ itself enforces fire rate, reload timing and ammo bookkeeping through
 * {@code IGunOperator}; this goal only aims, positions the soldier and pulls the trigger.
 */
public class SoldierGunAttackGoal extends Goal {
    private final SoldierEntity soldier;
    @Nullable private LivingEntity target;
    private int unseenTicks = 0;

    public SoldierGunAttackGoal(SoldierEntity soldier) {
        this.soldier = soldier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity candidate = this.soldier.getTarget();
        if (candidate == null || !candidate.isAlive() || !this.soldier.isArmed()) {
            return false;
        }
        this.target = candidate;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return this.canUse() || (this.target != null && this.target.isAlive() && this.unseenTicks < 80);
    }

    @Override
    public void start() {
        this.soldier.setAggressive(true);
    }

    @Override
    public void stop() {
        this.soldier.setAggressive(false);
        this.target = null;
        this.unseenTicks = 0;
        this.soldier.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.target == null || !this.target.isAlive()) {
            return;
        }
        double range = SoldiersConfig.ATTACK_RANGE.get();
        double distanceSq = this.soldier.distanceToSqr(this.target.getX(), this.target.getY(), this.target.getZ());
        boolean seen = this.soldier.getSensing().hasLineOfSight(this.target);
        this.unseenTicks = seen ? 0 : this.unseenTicks + 1;

        // Movement: keep a comfortable firing distance
        PathNavigation navigation = this.soldier.getNavigation();
        if (!seen && this.unseenTicks > 60) {
            navigation.moveTo(this.target, 1.1D);
        } else if (distanceSq > range * range) {
            navigation.moveTo(this.target, 1.1D);
        } else {
            double desired = range * 0.55D;
            if (distanceSq > desired * desired * 1.3D) {
                navigation.moveTo(this.target, 0.9D);
            } else if (distanceSq < desired * desired * 0.55D) {
                Vec3 away = this.soldier.position().subtract(this.target.position()).normalize().scale(5.0D);
                navigation.moveTo(this.soldier.getX() + away.x, this.soldier.getY(), this.soldier.getZ() + away.z, 0.9D);
            } else {
                navigation.stop();
            }
        }

        // Aim & fire
        if (seen && distanceSq <= range * range) {
            this.aimAt(this.target);
            float inaccuracy = this.getInaccuracy(distanceSq, range);
            if (TaczBridge.isTaczLoaded() && TaczBridge.isGunItem(this.soldier.getMainHandItem())) {
                String result = TaczBridge.shoot(this.soldier);
                if ("NO_AMMO".equals(result)) {
                    TaczBridge.reload(this.soldier);
                }
            } else if (!TaczBridge.isTaczLoaded()) {
                this.soldier.shootFallback(this.target, inaccuracy);
            }
        }
    }

    private void aimAt(LivingEntity target) {
        double dx = target.getX() - this.soldier.getX();
        double dy = (target.getY() + target.getEyeHeight() * 0.85D) - (this.soldier.getY() + this.soldier.getEyeHeight());
        double dz = target.getZ() - this.soldier.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Mth.atan2(dz, dx) * 180.0F / (float) Math.PI) - 90.0F;
        float pitch = (float) (-(Mth.atan2(dy, horizontal) * 180.0F / (float) Math.PI));

        float spread = this.getInaccuracy(this.soldier.distanceToSqr(target), SoldiersConfig.ATTACK_RANGE.get());
        yaw += (this.soldier.getRandom().nextFloat() - 0.5F) * 2.0F * spread;
        pitch += (this.soldier.getRandom().nextFloat() - 0.5F) * 2.0F * spread;

        this.soldier.setYRot(yaw);
        this.soldier.setYBodyRot(yaw);
        this.soldier.setYHeadRot(yaw);
        this.soldier.setXRot(Mth.clamp(pitch, -60.0F, 60.0F));
    }

    /** Aim error in degrees; bigger on easier difficulties and at longer range. */
    private float getInaccuracy(double distanceSq, double range) {
        float base = switch (this.soldier.level().getDifficulty()) {
            case PEACEFUL, EASY -> 7.0F;
            case HARD -> 2.0F;
            default -> 4.0F;
        };
        double distance = Math.sqrt(distanceSq);
        return base * (float) (0.5D + 0.5D * Math.min(1.0D, distance / range));
    }
}
