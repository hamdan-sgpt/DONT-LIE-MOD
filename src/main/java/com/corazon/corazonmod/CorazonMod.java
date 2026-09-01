package com.corazon.corazonmod;

import com.corazon.corazonmod.command.DontLieCommands;
import com.corazon.corazonmod.game.DontLieGame;
import com.corazon.corazonmod.init.ModBlocks;
import com.corazon.corazonmod.init.ModCreativeTabs;
import com.corazon.corazonmod.init.ModItems;
import com.corazon.corazonmod.network.ModMessages;
import com.mojang.logging.LogUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CorazonMod.MOD_ID)
public class CorazonMod {
    public static final String MOD_ID = "corazonmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CorazonMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register Mod Deferred Registers
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        com.corazon.corazonmod.init.ModEntities.register(modEventBus);

        // Register Setup Listener
        modEventBus.addListener(this::commonSetup);

        // Register Mod to Forge Event Bus
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Register Network Packets
        event.enqueueWork(ModMessages::register);
        LOGGER.info("Corazon Mod (GOING SEVENTEEN: Don't Lie Edition) initialized!");
        LOGGER.info("Network packets registered successfully.");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        DontLieCommands.register(event.getDispatcher());
        LOGGER.info("Don't Lie commands registered: /dontlie, /mafia, /doctor, /police");
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            DontLieGame.getInstance().tick(event.getServer());
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("═══════════════════════════════════");
        LOGGER.info("  Corazon Mod - Don't Lie Edition  ");
        LOGGER.info("  Type /dontlie help for commands   ");
        LOGGER.info("═══════════════════════════════════");
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Corazon Mod Client Setup - GUI Screens & HUD Overlay registered!");
        }

        @SubscribeEvent
        public static void registerRenderers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(com.corazon.corazonmod.init.ModEntities.MONEY_POUCH.get(), com.corazon.corazonmod.client.renderer.MoneyPouchRenderer::new);
        }
    }
}
