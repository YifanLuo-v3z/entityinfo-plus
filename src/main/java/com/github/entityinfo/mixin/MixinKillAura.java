package com.github.entityinfo.mixin;

import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import com.github.epsilon.settings.Setting;
import com.github.epsilon.settings.impl.BoolSetting;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.PiercingWeapon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(targets = "com.github.epsilon.modules.impl.combat.KillAura")
public abstract class MixinKillAura extends Module {

    @Shadow
    public LivingEntity target;

    @Shadow
    private int switchIndex;

    @Shadow
    private double attacks;

    @Unique
    private BoolSetting entityinfo$onlyWithWeapon;

    protected MixinKillAura(String name, Category category) {
        super(name, category);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void entityinfo$injectSettings(CallbackInfo ci) {
        entityinfo$onlyWithWeapon = this.boolSetting("Only With Weapon", false);
        entityinfo$moveSettingAfter(entityinfo$onlyWithWeapon, entityinfo$findSetting("SwingHand"));
    }

    @Inject(method = "onTick", at = @At("HEAD"), cancellable = true)
    private void entityinfo$blockTargetingWithoutWeapon(com.github.epsilon.events.impl.PlayerTickEvent.Pre event, CallbackInfo ci) {
        if (!entityinfo$shouldRestrictToWeapons()) {
            return;
        }

        target = null;
        switchIndex = 0;
        attacks = 0.0D;
        ci.cancel();
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void entityinfo$handleAttack(LivingEntity target, CallbackInfo ci) {
        if (entityinfo$shouldRestrictToWeapons()) {
            ci.cancel();
            return;
        }

        if (entityinfo$tryPiercingAttack()) {
            ci.cancel();
        }
    }

    @Unique
    private boolean entityinfo$isHoldingWeapon() {
        if (mc.player == null) {
            return false;
        }

        ItemStack stack = mc.player.getMainHandItem();
        if (stack.isEmpty()) {
            return false;
        }

        return stack.typeHolder().is(ItemTags.SWORDS)
            || stack.typeHolder().is(ItemTags.AXES)
            || stack.typeHolder().is(ItemTags.SPEARS)
            || stack.has(DataComponents.WEAPON);
    }

    @Unique
    private boolean entityinfo$shouldRestrictToWeapons() {
        return entityinfo$onlyWithWeapon != null
            && entityinfo$onlyWithWeapon.getValue()
            && !entityinfo$isHoldingWeapon();
    }

    @Unique
    private boolean entityinfo$tryPiercingAttack() {
        if (mc.player == null || mc.gameMode == null) {
            return false;
        }

        ItemStack stack = mc.player.getMainHandItem();
        if (stack.isEmpty()) {
            return false;
        }

        PiercingWeapon piercingWeapon = stack.get(DataComponents.PIERCING_WEAPON);
        if (piercingWeapon == null) {
            return false;
        }

        // Spear-like weapons need the vanilla piercing attack path rather than a normal melee attack packet.
        mc.gameMode.piercingAttack(piercingWeapon);
        mc.player.swing(InteractionHand.MAIN_HAND);
        return true;
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
