package com.finnfreitag.rescuefrogport.block;

import com.finnfreitag.rescuefrogport.RescueFrogport;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlock;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class RescueFrogportBlock extends FrogportBlock {

    public RescueFrogportBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends FrogportBlockEntity> getBlockEntityType() {
        return RescueFrogport.RESCUE_FROGPORT_BE.get();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        // This handles player advancements and orientation passiveYaw automatically
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof RescueFrogportBlockEntity be) {
            if (placer != null) {
                placer.sendSystemMessage(
                        Component.literal("Rescue Frogport address: " + be.getRescueAddress()));
            }
        }
    }
}
