package com.bvanseg.gigeresque.common.entity.impl;

import com.bvanseg.gigeresque.Constants;
import com.bvanseg.gigeresque.common.Gigeresque;
import com.bvanseg.gigeresque.common.config.ConfigAccessor;
import com.bvanseg.gigeresque.common.entity.Entities;
import com.bvanseg.gigeresque.common.entity.Growable;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import software.bernie.geckolib3.core.IAnimatable;
import software.bernie.geckolib3.core.PlayState;
import software.bernie.geckolib3.core.builder.AnimationBuilder;
import software.bernie.geckolib3.core.controller.AnimationController;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;
import software.bernie.geckolib3.core.manager.AnimationData;
import software.bernie.geckolib3.core.manager.AnimationFactory;

public class RunnerbursterEntity extends ChestbursterEntity implements IAnimatable, Growable {
    public RunnerbursterEntity(EntityType<? extends RunnerbursterEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return LivingEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 15.0)
                .add(EntityAttributes.GENERIC_ARMOR, 2.0)
                .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 0.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23000000417232513)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0)
                .add(EntityAttributes.GENERIC_ATTACK_KNOCKBACK, 0.3);
    }

    private AnimationFactory animationFactory = new AnimationFactory(this);

    @Override
    protected int getAcidDiameter() {
        return 1;
    }

    /*
     * GROWTH
     */

    @Override
    public float getGrowthMultiplier() {
        return Gigeresque.config.miscellaneous.runnerbursterGrowthMultiplier;
    }

    @Override
    public float getMaxGrowth() {
        return Constants.TPD / 2.0f;
    }

    @Override
    public LivingEntity growInto() {
        if (hostId == null) {
            return new ClassicAlienEntity(Entities.ALIEN, world);
        }

        var variantId = ConfigAccessor.getReversedMorphMappings().get(hostId);
        if (variantId == null) {
            return new ClassicAlienEntity(Entities.ALIEN, world);
        }
        var identifier = new Identifier(variantId);
        var entityType = Registry.ENTITY_TYPE.getOrEmpty(identifier).orElse(null);
        if (entityType == null) {
            return new ClassicAlienEntity(Entities.ALIEN, world);
        }
        var entity = entityType.create(world);

        if (hasCustomName()) {
            if (entity != null) {
                entity.setCustomName(getCustomName());
            }
        }

        return (LivingEntity) entity;
    }

    /*
     * ANIMATIONS
     */

    private <E extends IAnimatable> PlayState predicate(AnimationEvent<E> event) {
        var velocityLength = this.getVelocity().horizontalLength();

        if (velocityLength > 0.0 && !this.isTouchingWater()) {
            if (this.isAttacking()) {
                event.getController().setAnimation(
                        new AnimationBuilder()
                                .addAnimation("moving_aggro", true)
                );
                return PlayState.CONTINUE;
            } else {
                event.getController().setAnimation(
                        new AnimationBuilder().addAnimation("moving_noaggro", true)
                );
                return PlayState.CONTINUE;
            }
        } else {
            event.getController().setAnimation(new AnimationBuilder().addAnimation("idle", true));
            return PlayState.CONTINUE;
        }
    }

    @Override
    public void registerControllers(AnimationData data) {
        data.addAnimationController(new AnimationController<>(this, "controller", 10f, this::predicate));
    }

    @Override
    public AnimationFactory getFactory() {
        return animationFactory;
    }
}
