package com.github.entityinfo.mixin;

import com.github.epsilon.events.impl.PacketEvent;
import com.github.epsilon.events.impl.PlayerTickEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import net.minecraft.core.component.DataComponents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(targets = "com.github.epsilon.modules.impl.player.PacketEat")
public abstract class MixinPacketEat extends Module {

    @Shadow
    private ItemStack item;

    protected MixinPacketEat(String name, Category category) {
        super(name, category);
    }

    @Inject(method = "onPostTick", at = @At("RETURN"))
    private void entityinfo$clearStaleUseStack(PlayerTickEvent.Post event, CallbackInfo ci) {
        if (mc.player == null || !mc.player.isUsingItem()) {
            item = ItemStack.EMPTY;
        }
    }

    @Inject(method = "onPacketSend", at = @At("HEAD"), cancellable = true)
    private void entityinfo$handleReleaseUseItem(PacketEvent.Send event, CallbackInfo ci) {
        if (!(event.getPacket() instanceof ServerboundPlayerActionPacket packet)
            || packet.getAction() != ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) {
            return;
        }

        if (entityinfo$isAlwaysEdibleFood(item)) {
            event.setCancelled(true);
            item = ItemStack.EMPTY;
            ci.cancel();
            return;
        }

        item = ItemStack.EMPTY;
    }

    @Inject(method = "onPacketSend", at = @At("RETURN"))
    private void entityinfo$clearCachedUseStackAfterRelease(PacketEvent.Send event, CallbackInfo ci) {
        if (event.getPacket() instanceof ServerboundPlayerActionPacket packet
            && packet.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM) {
            item = ItemStack.EMPTY;
        }
    }

    @Unique
    private static boolean entityinfo$isAlwaysEdibleFood(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        FoodProperties food = stack.get(DataComponents.FOOD);
        return food != null && food.canAlwaysEat();
    }
}
