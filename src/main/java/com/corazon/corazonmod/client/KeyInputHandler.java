package com.corazon.corazonmod.client;

import com.corazon.corazonmod.CorazonMod;
import com.corazon.corazonmod.network.AdminActionPacket;
import com.corazon.corazonmod.network.ModMessages;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * KeyInputHandler — Registers Key 'G' to open the Don't Lie Admin & Game GUI Menu.
 */
@Mod.EventBusSubscriber(modid = CorazonMod.MOD_ID, value = Dist.CLIENT)
public class KeyInputHandler {

    public static final KeyMapping DONT_LIE_MENU_KEY = new KeyMapping(
            "key.corazonmod.dontlie_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.corazonmod"
    );

    @Mod.EventBusSubscriber(modid = CorazonMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModClientEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(DONT_LIE_MENU_KEY);
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (DONT_LIE_MENU_KEY.consumeClick()) {
            if (Minecraft.getInstance().player != null) {
                // Send request to server to open admin control panel GUI
                ModMessages.sendToServer(new AdminActionPacket("REQUEST_OPEN_MENU"));
            }
        }
    }
}
