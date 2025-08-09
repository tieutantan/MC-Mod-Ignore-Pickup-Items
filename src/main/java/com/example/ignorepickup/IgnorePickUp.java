package com.example.ignorepickup;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

@Mod(IgnorePickUp.MODID)
public final class IgnorePickUp {
    public static final String MODID = "ignorepickupitems_tantn";
    public IgnorePickUp() {
        Config.load();
    NeoForge.EVENT_BUS.register(new PickupEvents());
    NeoForge.EVENT_BUS.addListener(IgnorePickupCommands::onRegisterCommands);
    }
}
