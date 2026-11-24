package com.example.skystudio;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkyStudioClient implements ClientModInitializer {
    public static final String MOD_ID = "skystudio";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Shows up in Options > Controls > Key Binds under "Sky Studio", so the player can rebind it.
    public static KeyMapping OPEN_KEY;

    @Override
    public void onInitializeClient() {
        SkyConfig.load();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        OPEN_KEY = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.skystudio.open", InputConstants.Type.KEYSYM, InputConstants.KEY_K, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (OPEN_KEY.consumeClick()) {
                if (mc.screen == null && mc.level != null) mc.setScreen(new SkyScreen());
            }
        });
    }
}
