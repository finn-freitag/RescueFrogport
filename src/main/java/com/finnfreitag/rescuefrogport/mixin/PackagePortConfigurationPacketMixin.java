package com.finnfreitag.rescuefrogport.mixin;

import com.finnfreitag.rescuefrogport.block.RescueFrogportBlockEntity;
import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.simibubi.create.content.logistics.packagePort.PackagePortConfigurationPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PackagePortConfigurationPacket.class, remap = false)
public class PackagePortConfigurationPacketMixin {

    @Inject(
            method = "applySettings(Lnet/minecraft/server/level/ServerPlayer;Lcom/simibubi/create/content/logistics/packagePort/PackagePortBlockEntity;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void onApplySettings(ServerPlayer player, PackagePortBlockEntity be, CallbackInfo ci) {
        if (be instanceof RescueFrogportBlockEntity) {
            ci.cancel(); // Block setting updates (address filter / acceptsPackages) on RescueFrogport
        }
    }
}
