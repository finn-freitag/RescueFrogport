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

    @org.spongepowered.asm.mixin.injection.Inject(
            method = "renderSafe(Lcom/simibubi/create/content/logistics/packagePort/frogport/FrogportBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @org.spongepowered.asm.mixin.injection.At("TAIL"),
            remap = false
    )
    private void onRenderSafe(FrogportBlockEntity blockEntity, float partialTicks, com.mojang.blaze3d.vertex.PoseStack ms, net.minecraft.client.renderer.MultiBufferSource buffer, int light, int overlay, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!(blockEntity instanceof RescueFrogportBlockEntity)) {
            return;
        }

        // 1. Recalculate head rotation yaw and pitch exactly as in original renderSafe
        float yaw = blockEntity.getYaw();
        float headPitch = 80.0f;
        float headPitchModifier = 1.0f;
        boolean hasTarget = blockEntity.target != null;
        boolean animating = blockEntity.isAnimationInProgress();
        boolean depositing = blockEntity.currentlyDepositing;
        net.minecraft.world.phys.Vec3 diff = net.minecraft.world.phys.Vec3.ZERO;

        if (hasTarget) {
            diff = blockEntity.target.getExactTargetLocation(blockEntity, (net.minecraft.world.level.LevelAccessor)blockEntity.getLevel(), blockEntity.getBlockPos()).subtract(0.0, animating && depositing ? 0.0 : 0.75, 0.0).subtract(net.minecraft.world.phys.Vec3.atCenterOf((net.minecraft.core.Vec3i)blockEntity.getBlockPos()));
            float tonguePitch = (float)net.minecraft.util.Mth.atan2((double)diff.y, (double)(diff.multiply(1.0, 0.0, 1.0).length() + 0.1875)) * 57.295776f;
            headPitch = net.minecraft.util.Mth.clamp((float)(tonguePitch * 2.0f), (float)60.0f, (float)100.0f);
        }

        if (animating) {
            float progress = blockEntity.animationProgress.getValue(partialTicks);
            if (depositing) {
                headPitchModifier = (float)Math.max(0.0, 1.0 - Math.pow((double)progress * 1.25 * 2.0 - 1.0, 4.0));
            } else {
                headPitchModifier = 1.0f - (float)Math.min(1.0, Math.max(0.0, (Math.pow((double)progress * 1.5, 2.0) - 0.5) * 2.0));
            }
        } else {
            float anticipation = blockEntity.anticipationProgress.getValue(partialTicks);
            headPitchModifier = anticipation > 0.0f ? (float)Math.max(0.0, 1.0 - Math.pow((double)anticipation * 1.25 * 2.0 - 1.0, 4.0)) : 0.0f;
        }

        headPitch *= headPitchModifier;
        headPitch = Math.max(headPitch, blockEntity.manualOpenAnimationProgress.getValue(partialTicks) * 60.0f);

        // 2. Fetch the Create electron tube item stack
        net.minecraft.world.item.ItemStack tubeStack = new net.minecraft.world.item.ItemStack(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                        net.minecraft.resources.ResourceLocation.parse("create:electron_tube")
                )
        );

        if (!tubeStack.isEmpty()) {
            ms.pushPose();

            // 1. Go to the center of the head's yaw rotation (0.5, 0.5, 0.5)
            ms.translate(0.5f, 0.5f, 0.5f);
            
            // 2. Rotate body yaw around Y-axis
            ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yaw));
            
            // 3. Go to the rotation pivot relative to the rotated center: (0.0, 0.125, 0.1875)
            ms.translate(0.0f, 0.125f, 0.1875f);
            
            // 4. Rotate head pitch around X-axis
            ms.mulPose(com.mojang.math.Axis.XP.rotationDegrees(headPitch));

            // 5. Go to the top center of the head in the rotated and tilted space: (0.0, 0.325, 0.0)
            ms.translate(0.0f, 0.325f, 0.0f);

            // 6. Scale down to be a fitting warn indicator (upscaled by 20% from 0.35f to 0.42f)
            ms.scale(0.42f, 0.42f, 0.42f);

            // First sprite (standing vertically)
            ms.pushPose();
            net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(
                    tubeStack,
                    net.minecraft.world.item.ItemDisplayContext.FIXED,
                    light,
                    overlay,
                    ms,
                    buffer,
                    blockEntity.getLevel(),
                    0
            );
            ms.popPose();

            // Second sprite (rotated 90 degrees around Y-axis for 3D cross model look)
            ms.pushPose();
            ms.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0f));
            net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(
                    tubeStack,
                    net.minecraft.world.item.ItemDisplayContext.FIXED,
                    light,
                    overlay,
                    ms,
                    buffer,
                    blockEntity.getLevel(),
                    0
            );
            ms.popPose();

            ms.popPose();
        }
    }
}
