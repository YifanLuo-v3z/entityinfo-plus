package com.github.entityinfo.fabric;

import com.github.entityinfo.EntityInfoAddon;
import com.github.epsilon.addon.EpsilonAddonSetupEvent;
import com.github.epsilon.fabric.addon.FabricEpsilonAddonEntrypoint;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public final class EntityInfoFabricEntrypoint implements FabricEpsilonAddonEntrypoint {

    @Override
    public void registerAddon(EpsilonAddonSetupEvent event) {
        event.registerAddon(EntityInfoAddon.INSTANCE);
    }
}
