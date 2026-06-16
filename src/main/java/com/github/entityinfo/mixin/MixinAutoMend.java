package com.github.entityinfo.mixin;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.Setting;
import com.github.epsilon.settings.impl.BoolSetting;
import com.github.epsilon.events.impl.PlayerTickEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(targets = "com.github.epsilon.modules.impl.combat.AutoMend")
public abstract class MixinAutoMend extends Module {

    @Unique
    private BoolSetting entityinfo$disableOnFullDurability;

    protected MixinAutoMend(String name, Category category) {
        super(name, category);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void entityinfo$injectSettings(CallbackInfo ci) {
        entityinfo$disableOnFullDurability = this.boolSetting("Disable On Full Durability", false);
        entityinfo$moveSettingAfter(entityinfo$disableOnFullDurability, entityinfo$findSetting("Swing Hand"));
    }

    @Inject(method = "onClientTick", at = @At("HEAD"), cancellable = true)
    private void entityinfo$disableWhenFullyRepaired(PlayerTickEvent.Pre event, CallbackInfo ci) {
        if (mc.player == null || entityinfo$disableOnFullDurability == null || !entityinfo$disableOnFullDurability.getValue()) {
            return;
        }

        if (!entityinfo$allArmorFullyRepaired()) {
            return;
        }

        toggle();
        ci.cancel();
    }

    @Unique
    private boolean entityinfo$allArmorFullyRepaired() {
        boolean hasRepairableArmor = false;
        EquipmentSlot[] armorSlots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

        for (EquipmentSlot slot : armorSlots) {
            ItemStack stack = mc.player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem()) {
                continue;
            }

            hasRepairableArmor = true;
            if (stack.getDamageValue() > 0) {
                return false;
            }
        }

        return hasRepairableArmor;
    }

    @Unique
    private void entityinfo$moveSettingAfter(Setting<?> setting, Setting<?> anchor) {
        if (setting == null || anchor == null || setting == anchor || !settings.remove(setting)) {
            return;
        }

        int anchorIndex = settings.indexOf(anchor);
        settings.add(anchorIndex < 0 ? settings.size() : anchorIndex + 1, setting);
    }

    @Unique
    private Setting<?> entityinfo$findSetting(String name) {
        for (Setting<?> setting : settings) {
            if (setting.getName().equals(name)) {
                return setting;
            }
        }

        return null;
    }
}
