package io.github.recrivenvi.glass.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
    void modernSurfaceModelsAndConcreteStyleDropsArePackaged() {
        JsonObject projectorState =
                resource("assets/" + MOD_ID + "/blockstates/glass_projector.json");
        JsonObject extensionState =
                resource("assets/" + MOD_ID + "/blockstates/glass_extension.json");
        assertEquals(24, projectorState.getAsJsonArray("multipart").size());
        assertEquals(13, extensionState.getAsJsonArray("multipart").size());
        assertEquals(
                7,
                resource("assets/" + MOD_ID + "/blockstates/glass_wireless_extension.json")
                        .getAsJsonArray("multipart")
                        .size());

        assertModelElement("glass_projector", "minecraft:block/blue_concrete", 4, 4, 4, 12, 12, 12);
        assertModelElement(
                "glass_extension", "minecraft:block/white_concrete", 4, 4, 4, 12, 12, 12);
        assertModelElement(
                "glass_wireless_extension", "minecraft:block/green_concrete", 4, 4, 4, 12, 12, 12);
        assertModelElement("glass_terminal", "minecraft:block/black_concrete", 4, 4, 4, 12, 12, 12);
        assertSixWayDirectionalState(projectorState, true);
        assertSixWayDirectionalState(
                resource("assets/" + MOD_ID + "/blockstates/glass_terminal.json"), false);
        assertModelDimensions(
                "glass_connection", "minecraft:block/white_concrete", java.util.List.of(4, 6, 6));
        assertModelDimensions(
                "glass_support_white",
                "minecraft:block/white_concrete",
                java.util.List.of(4, 6, 6));
        assertEquals(
                "minecraft:block/blue_concrete",
                resource("assets/" + MOD_ID + "/models/block/glass_support_blue.json")
                        .getAsJsonObject("textures")
                        .get("all")
                        .getAsString());
        assertEquals(
                "minecraft:block/green_concrete",
                resource("assets/" + MOD_ID + "/models/block/glass_support_green.json")
                        .getAsJsonObject("textures")
                        .get("all")
                        .getAsString());
        assertTrue(
                projectorState.getAsJsonArray("multipart").asList().stream()
                        .map(value -> value.getAsJsonObject().getAsJsonObject("when"))
                        .filter(java.util.Objects::nonNull)
                        .anyMatch(
                                when ->
                                        when.has("north")
                                                && when.get("north").getAsString().equals("support")
                                                && when.has("facing")
                                                && when.get("facing")
                                                        .getAsString()
                                                        .equals("north")));

        for (String block :
                new String[] {"glass_projector", "glass_extension", "glass_wireless_extension"}) {
            JsonObject pool =
                    resource("data/" + MOD_ID + "/loot_table/blocks/" + block + ".json")
                            .getAsJsonArray("pools")
                            .get(0)
                            .getAsJsonObject();
            assertFalse(pool.has("conditions"), block);
        }
    }

    private static void assertSixWayDirectionalState(JsonObject state, boolean multipart) {
        java.util.Set<String> facings = new java.util.HashSet<>();
        if (multipart) {
            state.getAsJsonArray("multipart")
                    .forEach(
                            value -> {
                                JsonObject entry = value.getAsJsonObject();
                                if (entry.has("when")
                                        && entry.getAsJsonObject("when").has("facing")) {
                                    java.util.Collections.addAll(
                                            facings,
                                            entry.getAsJsonObject("when")
                                                    .get("facing")
                                                    .getAsString()
                                                    .split("\\|"));
                                }
                            });
        } else {
            state.getAsJsonObject("variants")
                    .keySet()
                    .forEach(key -> facings.add(key.substring("facing=".length())));
        }
        assertEquals(java.util.Set.of("north", "east", "south", "west", "up", "down"), facings);
    }

    @Test
    void terminalUsesBlackCoreAndGreenFrontMarker() {
        JsonObject model = resource("assets/" + MOD_ID + "/models/block/glass_terminal.json");
        assertEquals(
                "minecraft:block/black_concrete",
                model.getAsJsonObject("textures").get("all").getAsString());
        assertEquals(
                "minecraft:block/green_concrete",
                model.getAsJsonObject("textures").get("marker").getAsString());
        assertEquals(
                java.util.List.of(5, 5, 3),
                coordinates(model.getAsJsonArray("elements").get(1).getAsJsonObject(), "from"));
        assertEquals(
                java.util.List.of(11, 11, 4),
                coordinates(model.getAsJsonArray("elements").get(1).getAsJsonObject(), "to"));
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
        assertEquals(MOD_ID + ":block/glass_projector", model.get("parent").getAsString());
    }

    @Test
    void neoForgeMetadataAndConfigTranslationsArePackaged() {
        try {
            String metadata =
                    java.nio.file.Files.readString(
                            java.nio.file.Path.of(
                                    System.getProperty("glass.test.resources"),
                                    "META-INF/neoforge.mods.toml"));
            assertTrue(metadata.contains("modId = \"glass\""));
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }

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

    private void assertModelElement(
            String modelName,
            String texture,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ) {
        JsonObject model = resource("assets/" + MOD_ID + "/models/block/" + modelName + ".json");
        assertEquals(texture, model.getAsJsonObject("textures").get("all").getAsString());
        JsonObject element = model.getAsJsonArray("elements").get(0).getAsJsonObject();
        assertEquals(java.util.List.of(minX, minY, minZ), coordinates(element, "from"));
        assertEquals(java.util.List.of(maxX, maxY, maxZ), coordinates(element, "to"));
        java.util.List<Integer> expectedUv = java.util.List.of(4, 4, 12, 12);
        element.getAsJsonObject("faces")
                .entrySet()
                .forEach(
                        entry ->
                                assertEquals(
                                        expectedUv,
                                        entry
                                                .getValue()
                                                .getAsJsonObject()
                                                .getAsJsonArray("uv")
                                                .asList()
                                                .stream()
                                                .map(value -> value.getAsInt())
                                                .toList(),
                                        modelName + ":" + entry.getKey()));
    }

    private void assertModelDimensions(
            String name, String texture, java.util.List<Integer> dimensions) {
        JsonObject model = resource("assets/" + MOD_ID + "/models/block/" + name + ".json");
        assertEquals(texture, model.getAsJsonObject("textures").get("all").getAsString());
        JsonObject element = model.getAsJsonArray("elements").get(0).getAsJsonObject();
        java.util.List<Integer> from = coordinates(element, "from");
        java.util.List<Integer> to = coordinates(element, "to");
        assertEquals(
                dimensions,
                java.util.stream.IntStream.range(0, 3)
                        .map(index -> to.get(index) - from.get(index))
                        .boxed()
                        .sorted()
                        .toList());
    }

    private static java.util.List<Integer> coordinates(JsonObject element, String member) {
        return element.getAsJsonArray(member).asList().stream()
                .map(value -> value.getAsInt())
                .toList();
    }

    private JsonObject resource(String path) {
        try (var reader =
                java.nio.file.Files.newBufferedReader(
                        java.nio.file.Path.of(System.getProperty("glass.test.resources"), path),
                        StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (java.io.IOException exception) {
            throw new java.io.UncheckedIOException(exception);
        }
    }
}
