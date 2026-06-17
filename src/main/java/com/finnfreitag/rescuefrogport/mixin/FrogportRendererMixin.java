package com.finnfreitag.rescuefrogport.mixin;

import com.finnfreitag.rescuefrogport.block.RescueFrogportBlockEntity;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = FrogportRenderer.class, remap = false)
public class FrogportRendererMixin {

    @Redirect(
            method = "renderSafe(Lcom/simibubi/create/content/logistics/packagePort/frogport/FrogportBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At(value = "INVOKE", target = "Ldev/engine_room/flywheel/api/visualization/VisualizationManager;supportsVisualization(Lnet/minecraft/world/level/LevelAccessor;)Z"),
            remap = false
    )
    private boolean redirectSupportsVisualization(LevelAccessor level, FrogportBlockEntity blockEntity) {
        if (blockEntity instanceof RescueFrogportBlockEntity) {
            return false; // Force standard BlockEntityRenderer for RescueFrogport
        }
        return VisualizationManager.supportsVisualization(level);
    }
}
