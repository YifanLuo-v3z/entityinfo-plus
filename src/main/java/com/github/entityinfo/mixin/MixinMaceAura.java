package com.github.entityinfo.mixin;

import com.github.epsilon.events.bus.EventHandler;
import com.github.epsilon.events.impl.PlayerTickEvent;
import com.github.epsilon.modules.Category;
import com.github.epsilon.modules.Module;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Environment(EnvType.CLIENT)
@Mixin(targets = "com.github.epsilon.modules.impl.combat.MaceAura")
public abstract class MixinMaceAura extends Module {

    protected MixinMaceAura(String name, Category category) {
        super(name, category);
    }

    @Shadow
    public abstract void onTick(PlayerTickEvent event);

    @Unique
    @EventHandler
    private void entityinfo$onPlayerTickPre(PlayerTickEvent.Pre event) {
        // Epsilon's MaceAura listens to PlayerTickEvent, but the bus posts PlayerTickEvent.Pre/Post exact classes.
        this.onTick(null);
    }
}
