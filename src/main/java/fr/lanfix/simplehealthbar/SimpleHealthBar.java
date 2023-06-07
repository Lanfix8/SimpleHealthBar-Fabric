package fr.lanfix.simplehealthbar;

import fr.lanfix.simplehealthbar.overlays.HealthBar;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class SimpleHealthBar implements ModInitializer {

    public static final String MOD_ID = "simple-health-bar";

    @Override
    public void onInitialize() {

        HudRenderCallback.EVENT.register(new HealthBar());

    }
}
