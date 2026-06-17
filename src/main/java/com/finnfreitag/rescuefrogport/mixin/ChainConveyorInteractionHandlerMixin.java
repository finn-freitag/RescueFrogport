package com.finnfreitag.rescuefrogport.mixin;

import com.finnfreitag.rescuefrogport.RescueFrogport;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorInteractionHandler;
import com.simibubi.create.content.logistics.packagePort.PackagePortTarget;
import com.simibubi.create.content.logistics.packagePort.PackagePortTargetSelectionHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChainConveyorInteractionHandler.class, remap = false)
public class ChainConveyorInteractionHandlerMixin {

    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true)
    private static void onIsActive(CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            ItemStack mainHandItem = mc.player.getMainHandItem();
            if (mainHandItem.is(RescueFrogport.RESCUE_FROGPORT_ITEM.get())) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private static void onOnUse(CallbackInfoReturnable<Boolean> cir) {
        if (ChainConveyorInteractionHandler.selectedLift == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            ItemStack mainHandItem = mc.player.getMainHandItem();
            if (mainHandItem.is(RescueFrogport.RESCUE_FROGPORT_ITEM.get())) {
                PackagePortTargetSelectionHandler.exactPositionOfTarget = ChainConveyorInteractionHandler.selectedBakedPosition;
                PackagePortTargetSelectionHandler.activePackageTarget = new PackagePortTarget.ChainConveyorFrogportTarget(
                        ChainConveyorInteractionHandler.selectedLift,
                        ChainConveyorInteractionHandler.selectedChainPosition,
                        ChainConveyorInteractionHandler.selectedConnection,
                        false
                );
                cir.setReturnValue(true);
            }
        }
    }
}
