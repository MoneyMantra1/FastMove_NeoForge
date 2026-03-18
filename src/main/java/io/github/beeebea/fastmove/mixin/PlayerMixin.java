package io.github.beeebea.fastmove.mixin;

import io.github.beeebea.fastmove.FastMove;
import io.github.beeebea.fastmove.IFastPlayer;
import io.github.beeebea.fastmove.MoveState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DeathMessageType;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.FoodData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity implements IFastPlayer {

    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Shadow protected abstract void updatePlayerPose();
    @Shadow @Final private Abilities abilities;
    @Shadow protected FoodData foodData;

    @Unique private MoveState moveState = MoveState.NONE;
    @Unique private MoveState lastMoveState = MoveState.NONE;
    @Unique private Vec3 bonusVelocity = Vec3.ZERO;
    @Unique private int rollTickCounter = 0;
    @Unique private int wallRunCounter = 0;
    @Unique private Vec3 lastWallDir = Vec3.ZERO;
    @Unique private boolean isWallLeft = false;
    @Unique private int slideCooldown = 0;
    @Unique private int diveCooldown = 0;
    @Unique private BlockPos lastBlockPos = null;
    @Unique private boolean fastmove_lastSprintingState = false;

    @Override
    public MoveState fastmove_getMoveState() {
        return moveState;
    }

    @Override
    public void fastmove_setMoveState(MoveState moveState) {
        this.moveState = moveState;
    }

    @Override
    public void fastmove_setJumpInput(boolean input) {}

    @Unique
    private Player fastmove$self() {
        return (Player) (Object) this;
    }

    @Unique
    private boolean fastmove_isMainPlayer() {
        return fastmove$self().isLocalPlayer();
    }

    @Unique
    private void updateCurrentMoveState() {
        if (lastMoveState != moveState) {
            lastMoveState = moveState;
            if (moveState == MoveState.ROLLING || moveState == MoveState.PRONE) {
                rollTickCounter = 0;
                setPose(Pose.SWIMMING);
            }
            if (fastmove_isMainPlayer()) {
                FastMove.moveStateUpdater.setMoveState(fastmove$self(), moveState);
            }
            FastMove.moveStateUpdater.setAnimationState(fastmove$self(), moveState);
            updatePlayerPose();
            refreshDimensions();
        }
    }

    @Unique
    private static Vec3 fastmove_movementInputToVelocity(Vec3 movementInput, double speed, float yaw) {
        double d = movementInput.lengthSqr();
        if (d < 1.0E-7) {
            return Vec3.ZERO;
        } else {
            Vec3 vec3 = (d > 1.0 ? movementInput.normalize() : movementInput).scale(speed);
            double f = Mth.sin(yaw * 0.017453292F);
            double g = Mth.cos(yaw * 0.017453292F);
            return new Vec3(vec3.x * g - vec3.z * f, vec3.y, vec3.z * g + vec3.x * f);
        }
    }

    @Unique
    private static Vec3 fastmove_velocityToMovementInput(Vec3 velocity, float yaw) {
        double d = velocity.lengthSqr();
        if (d < 1.0E-7) {
            return Vec3.ZERO;
        }
        float f = Mth.sin(yaw * 0.017453292F);
        float g = Mth.cos(yaw * 0.017453292F);
        Vec3 unrotated = new Vec3(velocity.x * g + velocity.z * f, velocity.y, -velocity.x * f + velocity.z * g);
        return (unrotated.lengthSqr() > 1.0 ? unrotated.normalize() : unrotated);
    }

    @Unique
    private void fastmove_WallRun() {
        var vel = getDeltaMovement();
        var hasWall = getWallDirection();

        if (moveState == MoveState.WALLRUNNING_LEFT || moveState == MoveState.WALLRUNNING_RIGHT) {
            if (!hasWall || onGround()) {
                wallRunCounter = 0;
                moveState = MoveState.NONE;
            } else {
                wallRunCounter++;
                setSprinting(true);

                var wallBlockPos = blockPosition().subtract(BlockPos.containing(lastWallDir));
                if (lastBlockPos == null || !lastBlockPos.equals(wallBlockPos)) {
                    lastBlockPos = wallBlockPos;
                    playStepSound(wallBlockPos, level().getBlockState(wallBlockPos));
                }

                var flatVel = vel.multiply(1, 0, 1);
                var wallVel = isWallLeft ? flatVel.normalize().yRot((float) Math.toRadians(90)) : flatVel.normalize().yRot((float) Math.toRadians(-90));
                moveState = !isWallLeft ? MoveState.WALLRUNNING_LEFT : MoveState.WALLRUNNING_RIGHT;
                if (fastmove_velocityToMovementInput(flatVel, getYRot()).dot(lastWallDir) < 0) {
                    addDeltaMovement(wallVel.multiply(-0.1, 0, -0.1));
                }
                addDeltaMovement(new Vec3(0, -vel.y * (1 - ((double) wallRunCounter / FastMove.getConfig().getWallRunDurationTicks())), 0));
                bonusVelocity = Vec3.ZERO;
                if (!FastMove.INPUT.ismoveUpKeyPressed()) {
                    double velocityMult = FastMove.getConfig().getWallRunSpeedBoostMultiplier();
                    addDeltaMovement(wallVel.multiply(0.3 * velocityMult, 0, 0.3 * velocityMult).add(new Vec3(0, 0.4 * velocityMult, 0)));
                    moveState = MoveState.NONE;
                }
            }
        } else {
            wallRunCounter = 0;
            if (!onGround() && FastMove.INPUT.ismoveUpKeyPressed() && hasWall && vel.y <= 0) {
                moveState = MoveState.WALLRUNNING_LEFT;
                foodData.addExhaustion(FastMove.getConfig().getWallRunStaminaCost());
            }
        }
    }

    @Unique
    private boolean getWallDirection() {
        var flat = getDeltaMovement().multiply(1, 0, 1);
        if (flat.lengthSqr() < 0.01) return false;
        flat = flat.normalize();
        var left = flat.yRot((float) Math.toRadians(-90)).multiply(0.5, 0, 0.5);
        var right = flat.yRot((float) Math.toRadians(90)).multiply(0.5, 0, 0.5);
        var lowerLeftHit = level().clip(new ClipContext(position().add(0, 0.2, 0), position().add(left), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (lowerLeftHit.getType() == HitResult.Type.BLOCK) {
            var upperLeftHit = level().clip(new ClipContext(position().add(0, 1.5, 0), position().add(left), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (upperLeftHit.getType() == HitResult.Type.BLOCK) {
                lastWallDir = blockPosition().getCenter().subtract(lowerLeftHit.getBlockPos().getCenter());
                isWallLeft = true;
                return true;
            }
        }
        var lowerRightHit = level().clip(new ClipContext(position().add(0, 0.2, 0), position().add(right), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (lowerRightHit.getType() == HitResult.Type.BLOCK) {
            var upperRightHit = level().clip(new ClipContext(position().add(0, 1.5, 0), position().add(right), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (upperRightHit.getType() == HitResult.Type.BLOCK) {
                lastWallDir = blockPosition().getCenter().subtract(lowerRightHit.getBlockPos().getCenter());
                isWallLeft = false;
                return true;
            }
        }
        lastWallDir = Vec3.ZERO;
        return false;
    }

    @Unique
    private boolean fastmove_isValidForMovement(boolean canSwim, boolean canElytra) {
        return !isSpectator() && (canElytra || !isFallFlying()) && (canSwim || !isInWater()) && !onClimbable() && !abilities.flying;
    }

    @Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
    private void fastmove_getDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        var currentState = fastmove_getMoveState();
        if (currentState != null && currentState != MoveState.NONE) {
            cir.setReturnValue(currentState.dimensions);
        }
    }

    @Inject(method = "getEyeHeight", at = @At("HEAD"), cancellable = true)
    private void fastmove_getEyeHeight(Pose pose, EntityDimensions dimensions, CallbackInfoReturnable<Float> cir) {
        var currentState = fastmove_getMoveState();
        if (currentState != null && currentState != MoveState.NONE) {
            cir.setReturnValue(currentState.dimensions.height() * 0.85f);
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void fastmove_tick(CallbackInfo info) {
        if (!FastMove.getConfig().enableFastMove) return;

        if (fastmove_isMainPlayer()) {
            if (abilities.flying || getControlledVehicle() != null) {
                moveState = MoveState.NONE;
                updateCurrentMoveState();
                return;
            }
            var bonusDecay = 0.9;
            if (moveState == MoveState.ROLLING) {
                rollTickCounter++;
                if (rollTickCounter >= 10) {
                    rollTickCounter = 0;
                    moveState = FastMove.INPUT.ismoveDownKeyPressed() ? MoveState.PRONE : MoveState.NONE;
                }
                bonusDecay = 0.98;
            }
            if (moveState == MoveState.SLIDING && !FastMove.INPUT.ismoveDownKeyPressed()) {
                moveState = MoveState.NONE;
            }

            if (FastMove.getConfig().wallRunEnabled) fastmove_WallRun();

            addDeltaMovement(bonusVelocity);
            bonusVelocity = bonusVelocity.multiply(bonusDecay, 0, bonusDecay);
        }

        updateCurrentMoveState();
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void fastmove_tick_tail(CallbackInfo info) {
        if (!FastMove.getConfig().enableFastMove) return;
        if (moveState == MoveState.PRONE || moveState == MoveState.ROLLING) setPose(Pose.SWIMMING);
        if (diveCooldown > 0) diveCooldown--;
        if (slideCooldown > 0) slideCooldown--;
    }

    @Inject(method = "travel", at = @At("HEAD"))
    private void fastmove_travel(Vec3 movementInput, CallbackInfo info) {
        if (!fastmove_isMainPlayer() || !FastMove.getConfig().enableFastMove || abilities.flying || getControlledVehicle() != null) return;
        fastmove_lastSprintingState = isSprinting();
        if (FastMove.INPUT.ismoveDownKeyPressed()) {
            if (!FastMove.INPUT.ismoveDownKeyPressedLastTick()) {
                var conf = FastMove.getConfig();
                if (diveCooldown == 0 && conf.diveRollEnabled && !onGround()
                        && getDeltaMovement().multiply(1, 0, 1).lengthSqr() > 0.05
                        && fastmove_isValidForMovement(conf.diveRollWhenSwimming, conf.diveRollWhenFlying)) {
                    diveCooldown = conf.getDiveRollCoolDown();
                    foodData.addExhaustion(conf.getDiveRollStaminaCost());
                    moveState = MoveState.ROLLING;
                    bonusVelocity = fastmove_movementInputToVelocity(new Vec3(0, 0, 1), 0.1f * conf.getDiveRollSpeedBoostMultiplier(), getYRot());
                    setSprinting(true);
                } else if (slideCooldown == 0 && conf.slideEnabled && fastmove_lastSprintingState && fastmove_isValidForMovement(false, false)) {
                    slideCooldown = conf.getSlideCoolDown();
                    foodData.addExhaustion(conf.getSlideStaminaCost());
                    moveState = MoveState.SLIDING;
                    bonusVelocity = fastmove_movementInputToVelocity(new Vec3(0, 0, 1), 0.2f * conf.getSlideSpeedBoostMultiplier(), getYRot());
                    setSprinting(true);
                }
            }
        } else {
            if (moveState == MoveState.PRONE) {
                moveState = MoveState.NONE;
            }
        }
    }

    @Inject(method = "maybeBackOffFromEdge", at = @At("HEAD"), cancellable = true)
    private void fastmove_adjustMovementForSneaking(Vec3 movement, MoverType type, CallbackInfoReturnable<Vec3> cir) {
        if (fastmove_isMainPlayer() && (moveState == MoveState.ROLLING || moveState == MoveState.SLIDING)) {
            cir.setReturnValue(movement);
        }
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void fastmove_jump(CallbackInfo info) {
        if (fastmove_isMainPlayer()) {
            setSprinting(fastmove_lastSprintingState);
            if (moveState == MoveState.SLIDING || moveState == MoveState.PRONE) {
                moveState = MoveState.NONE;
            }
            if (moveState == MoveState.ROLLING) {
                moveState = MoveState.PRONE;
            }
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void fastmove_damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (source.type().deathMessageType() == DeathMessageType.FALL_VARIANTS && moveState == MoveState.ROLLING) {
            cir.setReturnValue(false);
            cir.cancel();
        }
    }
}
