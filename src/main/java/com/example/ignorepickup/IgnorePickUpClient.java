package com.example.ignorepickup;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(value = Dist.CLIENT, modid = IgnorePickUp.MODID)
public class IgnorePickUpClient {

    private static final net.minecraft.client.KeyMapping OPEN_CONFIG = new net.minecraft.client.KeyMapping(
            "key.ignorepickup.config",
            GLFW.GLFW_KEY_I,
            "key.categories.misc"
    );

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.register(ClientEvents.class);
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CONFIG);
    }

    public static class ClientEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (event.getAction() == GLFW.GLFW_PRESS && OPEN_CONFIG.isDown()) {
                Minecraft mc = Minecraft.getInstance();
                mc.setScreen(new IgnorePickUpConfigScreen());
            }
        }
    }
}
