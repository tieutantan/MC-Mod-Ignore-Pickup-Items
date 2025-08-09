package com.example.ignorepickup;

import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/**
 * Deny pickups for items present in Config.getIgnoredActive().
 */
public final class PickupEvents {
    public PickupEvents() {}

    @SubscribeEvent
    public void onItemPickupPre(ItemEntityPickupEvent.Pre event) {
        if (!Config.isEnabled()) return;

        ItemStack stack = event.getItemEntity().getItem();
        if (stack.isEmpty()) return;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
    // Track items seen so they appear in the config list without immediate I/O
    Config.addKnownEphemeral(id.toString());
        Set<String> active = Config.getIgnoredActive();

        if (active.contains(id.toString())) {
            event.setCanPickup(TriState.FALSE);
            System.out.println("[IgnorePickup] Denied pickup: " + id);
        }
    }
}
