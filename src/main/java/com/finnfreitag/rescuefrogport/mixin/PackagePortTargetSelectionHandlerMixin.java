package com.finnfreitag.rescuefrogport.mixin;

import com.finnfreitag.rescuefrogport.RescueFrogport;
import com.simibubi.create.content.logistics.packagePort.PackagePortTargetSelectionHandler;
import com.tterrag.registrate.util.entry.BlockEntry;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = PackagePortTargetSelectionHandler.class, remap = false)
public class PackagePortTargetSelectionHandlerMixin {

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lcom/tterrag/registrate/util/entry/BlockEntry;isIn(Lnet/minecraft/world/item/ItemStack;)Z"),
            remap = false
    )
    private static boolean redirectIsIn(BlockEntry<?> instance, ItemStack stack) {
        return instance.isIn(stack) || stack.is(RescueFrogport.RESCUE_FROGPORT_ITEM.get());
    }
}
