package com.github.entityinfo.modules;

import com.github.epsilon.events.bus.EventHandler;
import com.github.epsilon.events.impl.PacketEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.item.ItemStack;

@Environment(EnvType.CLIENT)
public final class XCarry extends Module {

    public static final XCarry INSTANCE = new XCarry();

    private XCarry() {
        super("XCarry", Category.PLAYER);
    }

    @EventHandler
    private void onPacketSend(PacketEvent.Send event) {
        if (!isEnabled() || !(event.getPacket() instanceof ServerboundContainerClosePacket closePacket)) {
            return;
        }

        if (!shouldKeepInventoryOpen()) {
            return;
        }

        if (mc.player.inventoryMenu == null || closePacket.getContainerId() != mc.player.inventoryMenu.containerId) {
            return;
        }

        event.setCancelled(true);
    }

    public boolean shouldCancelInventoryClose() {
        return isEnabled() && shouldKeepInventoryOpen();
    }

    private boolean shouldKeepInventoryOpen() {
        if (nullCheck() || !(mc.screen instanceof InventoryScreen) || mc.player == null) {
            return false;
        }

        if (!mc.player.isAlive() || mc.player.isRemoved()) {
            return false;
        }

        if (mc.player.inventoryMenu == null || mc.player.containerMenu != mc.player.inventoryMenu) {
            return false;
        }

        ItemStack carried = mc.player.containerMenu.getCarried();
        if (!carried.isEmpty()) {
            return false;
        }

        return !mc.player.inventoryMenu.getCraftSlots().isEmpty();
    }
}
