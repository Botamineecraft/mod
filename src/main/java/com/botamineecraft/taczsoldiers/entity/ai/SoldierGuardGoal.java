package com.botamineecraft.taczsoldiers.entity.ai;

import com.botamineecraft.taczsoldiers.entity.SoldierEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * When guard mode is enabled (sneak + right-click with an empty hand),
 * the soldier walks back to the guarded position whenever it wanders too far.
 */
public class SoldierGuardGoal extends Goal {
    private static final double MAX_DISTANCE_SQ = 14.0D * 14.0D;
    private static final double ARRIVE_DISTANCE_SQ = 3.0D * 3.0D;

    private final SoldierEntity soldier;

    public SoldierGuardGoal(SoldierEntity soldier) {
        this.soldier = soldier;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (!this.soldier.isGuarding() || this.soldier.getTarget() != null) {
            return false;
        }
        BlockPos pos = this.soldier.getGuardPos();
        return pos != null && this.soldier.distanceToSqr(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D) > MAX_DISTANCE_SQ;
    }

    @Override
    public boolean canContinueToUse() {
        if (!this.soldier.isGuarding() || this.soldier.getTarget() != null) {
            return false;
        }
        BlockPos pos = this.soldier.getGuardPos();
        return pos != null && this.soldier.distanceToSqr(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D) > ARRIVE_DISTANCE_SQ;
    }

    @Override
    public void start() {
        BlockPos pos = this.soldier.getGuardPos();
        if (pos != null) {
            this.soldier.getNavigation().moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 1.0D);
        }
    }

    @Override
    public void stop() {
        this.soldier.getNavigation().stop();
    }
}
