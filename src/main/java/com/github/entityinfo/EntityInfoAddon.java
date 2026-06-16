package com.github.entityinfo;

import com.github.entityinfo.modules.ArmorHudPlus;
import com.github.entityinfo.modules.BedRender;
import com.github.entityinfo.modules.CriticalsPlus;
import com.github.entityinfo.modules.CrystalChams;
import com.github.entityinfo.modules.DroppedItemHUD;
import com.github.entityinfo.modules.ElytraBounce;
import com.github.entityinfo.modules.FeetTrapAirRender;
import com.github.entityinfo.modules.ItemHud;
import com.github.entityinfo.modules.ShulkerViewer;
import com.github.entityinfo.modules.XCarry;
import com.github.entityinfo.modules.Zoom;
import com.github.epsilon.addon.EpsilonAddon;
import com.github.epsilon.events.bus.EventBus;
import java.lang.invoke.MethodHandles;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.resources.language.I18n;

@Environment(EnvType.CLIENT)
public final class EntityInfoAddon extends EpsilonAddon {

    private static final String DISPLAY_NAME_KEY = "entityinfo_addon.name";
    private static final String DESCRIPTION_KEY = "entityinfo_addon.description";
    private static final String DEFAULT_DISPLAY_NAME = "EntityInfo Plus";
    private static final String DEFAULT_DESCRIPTION =
        "Adds armor HUD, bed render, crystal chams, dropped item HUD, item HUD, feet trap air render, elytra bounce, shulker preview, xcarry, zoom and extends Epsilon NameTags with dropped item labels.";

    public static final EntityInfoAddon INSTANCE = new EntityInfoAddon();

    private EntityInfoAddon() {
        super("entityinfo_addon");
    }

    @Override
    public void onSetup() {
        EventBus.INSTANCE.registerLambdaFactory(
            EntityInfoAddon.class.getPackageName(),
            (method, klass) -> MethodHandles.privateLookupIn(klass, MethodHandles.lookup())
        );
        registerModule(ArmorHudPlus.INSTANCE);
        registerModule(BedRender.INSTANCE);
        registerModule(CriticalsPlus.INSTANCE);
        registerModule(CrystalChams.INSTANCE);
        registerModule(DroppedItemHUD.INSTANCE);
        registerModule(ElytraBounce.INSTANCE);
        registerModule(FeetTrapAirRender.INSTANCE);
        registerModule(ItemHud.INSTANCE);
        registerModule(ShulkerViewer.INSTANCE);
        registerModule(XCarry.INSTANCE);
        registerModule(Zoom.INSTANCE);
    }

    @Override
    public String getDisplayName() {
        return entityinfo$translate(DISPLAY_NAME_KEY, DEFAULT_DISPLAY_NAME);
    }

    @Override
    public String getDescription() {
        return entityinfo$translate(DESCRIPTION_KEY, DEFAULT_DESCRIPTION);
    }

    @Override
    public String getVersion() {
        return FabricLoader.getInstance()
            .getModContainer("entityinfo_addon")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("dev");
    }

    private static String entityinfo$translate(String key, String fallback) {
        try {
            return I18n.exists(key) ? I18n.get(key) : fallback;
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
