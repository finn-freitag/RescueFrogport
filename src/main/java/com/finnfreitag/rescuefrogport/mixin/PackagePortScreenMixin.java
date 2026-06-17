package com.finnfreitag.rescuefrogport.mixin;

import com.finnfreitag.rescuefrogport.block.RescueFrogportBlockEntity;
import com.simibubi.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.simibubi.create.content.logistics.packagePort.PackagePortScreen;
import com.simibubi.create.content.logistics.packagePort.PackagePortTarget;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PackagePortScreen.class, remap = false)
public class PackagePortScreenMixin {

    @Shadow
    private EditBox addressBox;

    @Inject(method = "init()V", at = @At("TAIL"), remap = false)
    private void onInit(CallbackInfo ci) {
        PackagePortScreen screen = (PackagePortScreen) (Object) this;
        if (screen.getMenu().contentHolder instanceof RescueFrogportBlockEntity) {
            if (this.addressBox != null) {
                this.addressBox.setEditable(false);
                this.addressBox.active = false;
            }
        }
    }

    @Redirect(
            method = {"containerTick()V", "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V"},
            at = @At(value = "FIELD", target = "Lcom/simibubi/create/content/logistics/packagePort/PackagePortBlockEntity;target:Lcom/simibubi/create/content/logistics/packagePort/PackagePortTarget;"),
            remap = false
    )
    private PackagePortTarget redirectGetTarget(PackagePortBlockEntity blockEntity) {
        if (blockEntity instanceof RescueFrogportBlockEntity) {
            return null; // Make the target appear null to force simple GUI mode
        }
        return blockEntity.target;
    }

    @Redirect(
            method = "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V",
            at = @At(value = "INVOKE", target = "Lcom/simibubi/create/foundation/gui/AllGuiTextures;render(Lnet/minecraft/client/gui/GuiGraphics;II)V"),
            remap = false
    )
    private void redirectRenderBgTextures(AllGuiTextures texture, GuiGraphics graphics, int x, int y) {
        PackagePortScreen screen = (PackagePortScreen) (Object) this;
        if (texture == AllGuiTextures.FROGPORT_EDIT_NAME && screen.getMenu().contentHolder instanceof RescueFrogportBlockEntity) {
            return; // Hide the name edit pencil icon
        }
        texture.render(graphics, x, y);
    }
}
