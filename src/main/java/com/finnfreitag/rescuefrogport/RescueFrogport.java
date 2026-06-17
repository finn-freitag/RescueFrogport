package com.finnfreitag.rescuefrogport;

import com.finnfreitag.rescuefrogport.block.RescueFrogportBlock;
import com.finnfreitag.rescuefrogport.block.RescueFrogportBlockEntity;
import com.mojang.logging.LogUtils;
import com.simibubi.create.content.logistics.packagePort.PackagePortItem;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(RescueFrogport.MODID)
public class RescueFrogport {
    public static final String MODID = "rescuefrogport";
    private static final Logger LOGGER = LogUtils.getLogger();

    // --- Registries ---
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);

    // --- Block ---
    public static final DeferredBlock<RescueFrogportBlock> RESCUE_FROGPORT_BLOCK = BLOCKS.register(
            "rescue_frogport",
            () -> new RescueFrogportBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .strength(2.0f, 6.0f)
                    .noOcclusion()));

    // --- Block Item (registered as PackagePortItem) ---
    public static final DeferredItem<PackagePortItem> RESCUE_FROGPORT_ITEM =
            ITEMS.register("rescue_frogport",
                    () -> new PackagePortItem(RESCUE_FROGPORT_BLOCK.get(), new Item.Properties()));

    // --- Block Entity Type ---
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RescueFrogportBlockEntity>> RESCUE_FROGPORT_BE =
            BLOCK_ENTITY_TYPES.register("rescue_frogport",
                    () -> BlockEntityType.Builder.of(RescueFrogportBlockEntity::new,
                            RESCUE_FROGPORT_BLOCK.get()).build(null));

    // --- Create's creative tab key ---
    private static final ResourceKey<CreativeModeTab> CREATE_BASE_TAB =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                    ResourceLocation.fromNamespaceAndPath("create", "base"));

    public RescueFrogport(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("RescueFrogport initializing...");

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, RescueFrogportConfig.SPEC);

        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerRenderers);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        // Add to Create's base creative tab
        if (event.getTabKey() == CREATE_BASE_TAB) {
            event.accept(RESCUE_FROGPORT_ITEM.get());
        }
        // Also add to vanilla Functional Blocks as a fallback
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(RESCUE_FROGPORT_ITEM.get());
        }
    }

    private void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // Register Create's FrogportRenderer for our RescueFrogportBlockEntity type
        event.registerBlockEntityRenderer(RESCUE_FROGPORT_BE.get(),
                context -> (BlockEntityRenderer) new FrogportRenderer(context));
    }
}
