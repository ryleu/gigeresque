package mods.cybercat.gigeresque.common.entity.ai.tasks.movement;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mod.azure.azurelib.common.api.common.animatable.GeoEntity;
import mod.azure.azurelib.sblforked.api.core.behaviour.ExtendedBehaviour;
import mod.azure.azurelib.sblforked.util.BrainUtils;
import mod.azure.azurelib.sblforked.util.RandomUtil;
import mods.cybercat.gigeresque.common.block.GigBlocks;
import mods.cybercat.gigeresque.common.entity.ai.GigMemoryTypes;
import mods.cybercat.gigeresque.interfacing.AbstractAlien;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class EggmorpthTargetTask<E extends PathfinderMob & AbstractAlien & GeoEntity> extends ExtendedBehaviour<E> {
    private static final List<Pair<MemoryModuleType<?>, MemoryStatus>> MEMORY_REQUIREMENTS = ObjectArrayList.of(
            Pair.of(GigMemoryTypes.NEARBY_NEST_BLOCKS.get(), MemoryStatus.VALUE_PRESENT));

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return MEMORY_REQUIREMENTS;
    }

    @Override
    protected void start(E entity) {
        entity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    @Override
    protected boolean canStillUse(@NotNull ServerLevel level, E entity, long gameTime) {
        return !entity.isFleeing();
    }

    @Override
    protected void tick(@NotNull ServerLevel level, E entity, long gameTime) {
        var nestBlockLocation = entity.getBrain().getMemory(GigMemoryTypes.NEARBY_NEST_BLOCKS.get()).orElse(null);
        var test = RandomUtil.getRandomPositionWithinRange(entity.blockPosition(), 3, 1, 3, false, level);
        var passenger = entity.getFirstPassenger();
        if (nestBlockLocation != null && nestBlockLocation.stream().findAny().isPresent()) {
            var blockPos = nestBlockLocation.stream().findAny().get().getFirst();
            // Check if the block is within the entity's view direction and reachable via pathfinding
            if (isBlockInViewAndReachable(entity, blockPos)) {
                this.startMovingToTarget(entity, blockPos);

                var blockCenter = Vec3.atCenterOf(blockPos);
                var entityPos = entity.position();
                // Calculate the squared distance between the entity and the block
                var distanceSquared = blockCenter.distanceToSqr(entityPos);

                // Check if the distance is within 3 blocks (3 blocks squared is 9.0)
                if (distanceSquared <= 9.0)
                    for (BlockPos testPos : BlockPos.betweenClosed(test, test.above(2)))
                        if (level.getBlockState(test).isAir() && level.getBlockState(
                                test.below()).isSolid() && level.getEntitiesOfClass(LivingEntity.class,
                                new AABB(test)).stream().noneMatch(Objects::isNull)) {
                            BrainUtils.clearMemory(entity, MemoryModuleType.WALK_TARGET);
                            if (passenger != null) {
                                passenger.setPos(Vec3.atBottomCenterOf(testPos));
                                passenger.removeVehicle();
                                entity.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                                level.setBlockAndUpdate(testPos, GigBlocks.NEST_RESIN_WEB_CROSS.get().defaultBlockState());
                                level.setBlockAndUpdate(testPos.above(),
                                        GigBlocks.NEST_RESIN_WEB_CROSS.get().defaultBlockState());
                            }
                        }
            }
        }
    }

    private boolean isBlockInViewAndReachable(E entity, BlockPos blockPos) {
        var blockCenter = Vec3.atCenterOf(blockPos);
        var entityPos = entity.position();
        // Calculate the squared distance between the entity and the block
        var distanceSquared = blockCenter.distanceToSqr(entityPos);

        // Check if the distance is less than or equal to one block
        if (distanceSquared <= 1.0) {
            // Don't start moving towards the target if already within one block distance
            return false;
        }

        // Check if the block is reachable via pathfinding
        var path = entity.getNavigation().createPath(blockPos, 0);
        return path != null && !path.isDone();
    }

    private void startMovingToTarget(E alien, BlockPos targetPos) {
        BrainUtils.setMemory(alien, MemoryModuleType.WALK_TARGET, new WalkTarget(targetPos, 1.5F, 0));
    }

}
