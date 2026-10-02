package dev.local.ae2patternscaler;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;

import dev.local.ae2patternscaler.client.PatternTerminalControls;

@Mod(value = Ae2PatternScaler.MOD_ID, dist = Dist.CLIENT)
public final class Ae2PatternScaler {
    public static final String MOD_ID = "ae2_pattern_scaler";

    public Ae2PatternScaler() {
        NeoForge.EVENT_BUS.addListener(PatternTerminalControls::onScreenInit);
        NeoForge.EVENT_BUS.addListener(PatternTerminalControls::onScreenRender);
        NeoForge.EVENT_BUS.addListener(PatternTerminalControls::onScreenClosing);
    }
}
