package com.finnfreitag.rescuefrogport.mixin;

import com.finnfreitag.rescuefrogport.block.RescueFrogportBlockEntity;
import com.simibubi.create.content.logistics.packagePort.PackagePortTarget;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PackagePortTarget.ChainConveyorFrogportTarget.class, remap = false)
public class ChainConveyorFrogportTargetMixin {

    @Inject(method = "canSupport", at = @At("HEAD"), cancellable = true)
    private void onCanSupport(BlockEntity be, CallbackInfoReturnable<Boolean> cir) {
        if (be instanceof RescueFrogportBlockEntity) {
            cir.setReturnValue(true);
        }
    }
}
