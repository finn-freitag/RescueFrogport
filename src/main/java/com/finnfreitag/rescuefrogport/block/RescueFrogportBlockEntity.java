package com.finnfreitag.rescuefrogport.block;

import com.finnfreitag.rescuefrogport.RescueFrogport;
import com.finnfreitag.rescuefrogport.RescueFrogportConfig;
import com.finnfreitag.rescuefrogport.network.ChainConveyorNetworkScanner;
import com.finnfreitag.rescuefrogport.network.PackageRescueHandler;
import com.mojang.logging.LogUtils;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.Random;

public class RescueFrogportBlockEntity extends FrogportBlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CHARSET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int ADDRESS_RANDOM_LENGTH = 8;

    private int tickCounter = 0;

    public RescueFrogportBlockEntity(BlockPos pos, BlockState state) {
        super(RescueFrogport.RESCUE_FROGPORT_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            if (this.addressFilter == null || this.addressFilter.isEmpty()) {
                generateRescueAddress();
            }
        }
    }

    public void generateRescueAddress() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder("rescue-");
        for (int i = 0; i < ADDRESS_RANDOM_LENGTH; i++) {
            sb.append(CHARSET.charAt(random.nextInt(CHARSET.length())));
        }
        this.addressFilter = sb.toString();
        setChanged();
        sendData();
        LOGGER.info("RescueFrogport at {} generated address: {}", worldPosition, addressFilter);
    }

    public String getRescueAddress() {
        if (this.addressFilter == null || this.addressFilter.isEmpty()) {
            generateRescueAddress();
        }
        return this.addressFilter;
    }

    @Override
    public void tick() {
        super.tick(); // Run normal frogport animations, catching, pushing/pulling, etc.

        if (level == null || level.isClientSide()) return;

        tickCounter++;
        if (tickCounter < RescueFrogportConfig.scanIntervalTicks) return;
        tickCounter = 0;

        if (this.addressFilter == null || this.addressFilter.isEmpty()) return;
        if (this.target == null) return;

        BlockPos targetChainPos = this.worldPosition.offset(this.target.relativePos);
        if (!(level.getBlockEntity(targetChainPos) instanceof ChainConveyorBlockEntity)) {
            return;
        }

        try {
            ChainConveyorNetworkScanner scanner =
                    new ChainConveyorNetworkScanner(level, targetChainPos);
            PackageRescueHandler.processNetwork(level, scanner,
                    RescueFrogportConfig.congestionThreshold);
        } catch (Exception e) {
            LOGGER.warn("RescueFrogport at {} error during scan: {}",
                    worldPosition, e.getMessage());
        }
    }
}
