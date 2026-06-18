package com.finnfreitag.rescuefrogport;

import com.simibubi.create.content.logistics.packagePort.frogport.FrogportRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class RescueFrogportClient {

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(RescueFrogport.RESCUE_FROGPORT_BE.get(), FrogportRenderer::new);
    }
}
