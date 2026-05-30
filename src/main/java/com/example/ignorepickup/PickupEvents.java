package com.example.ignorepickup;

import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
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
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String idStr = id.toString();
        
    // Track items seen so they appear in the config list without immediate I/O
    Config.addKnownEphemeral(idStr);
        Set<String> active = Config.getIgnoredActive();

        if (active.contains(idStr)) {
            event.setCanPickup(TriState.FALSE);
            // todo: Debug Only
            // System.out.println("[IgnorePickup] Denied pickup: " + id);
        }
    }
}
