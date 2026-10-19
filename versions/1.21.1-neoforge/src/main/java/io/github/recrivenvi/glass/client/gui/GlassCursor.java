package io.github.recrivenvi.glass.client.gui;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Owns the standard native cursors used by GLASS screens. */
public final class GlassCursor {
    private static final Map<Integer, Long> CURSORS = new HashMap<>();

    private GlassCursor() {}

    public static void request(int shape) {
        long cursor = CURSORS.computeIfAbsent(shape, GLFW::glfwCreateStandardCursor);
        GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), cursor);
    }

    public static void reset() {
        GLFW.glfwSetCursor(Minecraft.getInstance().getWindow().getWindow(), 0);
    }

    public static void close() {
        reset();
        CURSORS.values().forEach(GLFW::glfwDestroyCursor);
        CURSORS.clear();
    }
}
