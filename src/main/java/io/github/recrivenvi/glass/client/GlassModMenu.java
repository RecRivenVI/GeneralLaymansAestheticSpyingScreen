package io.github.recrivenvi.glass.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.recrivenvi.glass.client.gui.GlassConfigScreen;

/** Mod Menu 是可选入口；GLASS 本体不依赖其存在。 */
public final class GlassModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return GlassConfigScreen::new;
    }
}
