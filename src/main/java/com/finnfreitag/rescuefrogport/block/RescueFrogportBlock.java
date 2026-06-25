package com.finnfreitag.rescuefrogport.block;

import com.finnfreitag.rescuefrogport.RescueFrogport;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlock;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.Block;

public class RescueFrogportBlock extends FrogportBlock {

    public static final BooleanProperty FULL = BooleanProperty.create("full");

    public RescueFrogportBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FULL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FULL);
    }

    @Override
    public BlockEntityType<? extends FrogportBlockEntity> getBlockEntityType() {
        return RescueFrogport.RESCUE_FROGPORT_BE.get();
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (state.hasProperty(FULL) && state.getValue(FULL)) {
            if (random.nextFloat() < 0.8f) { // Increased amount to 80%
                double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.25;
                double y = pos.getY() + 1.15 + random.nextDouble() * 0.15; // Lowered start Y to rise through tubes
                double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.25;
                level.addParticle(ParticleTypes.MYCELIUM, x, y, z, 0.0, 0.04, 0.0);
            }
        }
    }
}
