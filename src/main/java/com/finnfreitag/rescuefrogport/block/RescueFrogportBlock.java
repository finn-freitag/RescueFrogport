package com.finnfreitag.rescuefrogport.block;

import com.finnfreitag.rescuefrogport.RescueFrogport;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlock;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class RescueFrogportBlock extends FrogportBlock {

    public RescueFrogportBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends FrogportBlockEntity> getBlockEntityType() {
        return RescueFrogport.RESCUE_FROGPORT_BE.get();
    }


}
