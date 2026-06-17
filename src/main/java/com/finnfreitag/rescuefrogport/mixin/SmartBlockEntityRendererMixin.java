package com.finnfreitag.rescuefrogport.mixin;

import com.finnfreitag.rescuefrogport.block.RescueFrogportBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SmartBlockEntityRenderer.class, remap = false)
public class SmartBlockEntityRendererMixin {

    @Inject(
            method = "renderNameplateOnHover(Lcom/simibubi/create/foundation/blockEntity/SmartBlockEntity;Lnet/minecraft/network/chat/Component;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void onRenderNameplateOnHover(SmartBlockEntity blockEntity, Component text, float alpha, PoseStack ms, MultiBufferSource buffer, int light, CallbackInfo ci) {
        if (blockEntity instanceof RescueFrogportBlockEntity) {
            ci.cancel();
        }
    }
}
