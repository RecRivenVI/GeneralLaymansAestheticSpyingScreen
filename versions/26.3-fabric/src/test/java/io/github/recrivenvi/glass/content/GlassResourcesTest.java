package io.github.recrivenvi.glass.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class GlassResourcesTest {
    private static final String MOD_ID = "glass";

    @Test
    void everyModernBlockHasALootTable() {
        for (String block :
                new String[] {
                    "glass_terminal",
                    "glass_projector",
                    "glass_extension",
                    "glass_wireless_extension"
                }) {
            JsonObject loot = resource("data/" + MOD_ID + "/loot_table/blocks/" + block + ".json");
            assertEquals("minecraft:block", loot.get("type").getAsString());
            assertEquals(
                    MOD_ID + ":" + block,
                    loot.getAsJsonArray("pools")
                            .get(0)
                            .getAsJsonObject()
                            .getAsJsonArray("entries")
                            .get(0)
                            .getAsJsonObject()
                            .get("name")
                            .getAsString());
        }
    }

    @Test
    void terminalUsesPickaxeAndDiamondToolTags() {
        JsonObject pickaxe = resource("data/minecraft/tags/block/mineable/pickaxe.json");
        for (String block :
                new String[] {
                    "glass_terminal",
                    "glass_projector",
                    "glass_extension",
                    "glass_wireless_extension"
                }) {
            assertTrue(
                    pickaxe.getAsJsonArray("values").asList().stream()
                            .anyMatch(value -> value.getAsString().equals(MOD_ID + ":" + block)),
                    block);
        }
        JsonObject diamond = resource("data/minecraft/tags/block/needs_diamond_tool.json");
        assertTrue(
                diamond.getAsJsonArray("values").asList().stream()
                        .anyMatch(value -> value.getAsString().equals(MOD_ID + ":glass_terminal")));
    }

    @Test
    void upstreamFullCubesUseVanillaGlassAndObsidian() {
        for (String block :
                new String[] {
                    "glass_projector",
                    "glass_extension",
                    "glass_wireless_extension",
                    "glass_terminal"
                }) {
            JsonObject model = resource("assets/" + MOD_ID + "/models/block/" + block + ".json");
            assertEquals("minecraft:block/cube_all", model.get("parent").getAsString());
            assertEquals(
                    "minecraft:block/" + (block.equals("glass_terminal") ? "obsidian" : "glass"),
                    model.getAsJsonObject("textures").get("all").getAsString());
        }
    }

    @Test
    void legacyGuiTexturesAndModernProjectorItemModelArePackaged() {
        assertNotNull(
                getClass()
                        .getClassLoader()
                        .getResource("assets/" + MOD_ID + "/textures/gui/channel_set.png"));
        assertNotNull(
                getClass()
                        .getClassLoader()
                        .getResource("assets/" + MOD_ID + "/textures/gui/wireless_order.png"));
        JsonObject model = resource("assets/" + MOD_ID + "/models/item/glass_projector.json");
        assertEquals(4, model.getAsJsonArray("elements").size());
    }

    @Test
    void modMenuConfigEntrypointAndTranslationsArePackaged() {
        JsonObject metadata = resource("fabric.mod.json");
        assertEquals(
                "io.github.recrivenvi.glass.client.GlassModMenu",
                metadata.getAsJsonObject("entrypoints")
                        .getAsJsonArray("modmenu")
                        .get(0)
                        .getAsString());
        assertEquals("*", metadata.getAsJsonObject("suggests").get("modmenu").getAsString());

        for (String language : new String[] {"en_us", "zh_cn"}) {
            JsonObject translations = resource("assets/" + MOD_ID + "/lang/" + language + ".json");
            for (String key :
                    new String[] {
                        "glass.config.title",
                        "glass.config.propagation_limit",
                        "glass.config.propagation_delay_ticks",
                        "glass.config.surface_animation_ticks",
                        "glass.config.observation_distance",
                        "glass.config.wireless_min_nodes",
                        "glass.config.wireless_max_nodes",
                        "glass.config.channel_name_length",
                        "glass.config.channel_render_distance",
                        "glass.config.legacy_fade_animation"
                    }) {
                assertTrue(translations.has(key), language + ":" + key);
                assertFalse(translations.get(key).getAsString().isBlank(), language + ":" + key);
            }
        }
    }

    private JsonObject resource(String path) {
        var stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path);
        return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .getAsJsonObject();
    }
}
