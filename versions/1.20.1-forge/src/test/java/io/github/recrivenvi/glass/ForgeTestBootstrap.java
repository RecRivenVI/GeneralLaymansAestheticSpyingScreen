package io.github.recrivenvi.glass;

/** Isolated registry fixture; these unit tests do not launch FML or verify mod loading. */
public final class ForgeTestBootstrap {
    private static boolean initialized;

    private ForgeTestBootstrap() {}

    private static void prepareEvent(java.lang.reflect.Method method, Class<?> event)
            throws ReflectiveOperationException {
        if (event == net.minecraftforge.eventbus.api.Event.class) return;
        prepareEvent(method, event.getSuperclass());
        method.invoke(null, event, true);
    }

    public static synchronized void initialize() {
        if (initialized) return;
        try {
            // Plain JUnit lacks the launcher's side and EventBus subclass transformation.
            var side = net.minecraftforge.fml.loading.FMLLoader.class.getDeclaredField("dist");
            side.setAccessible(true);
            side.set(null, net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER);
            var listeners =
                    net.minecraftforge.eventbus.api.EventListenerHelper.class.getDeclaredMethod(
                            "getListenerListInternal", Class.class, boolean.class);
            listeners.setAccessible(true);
            Class<?> root = net.minecraftforge.network.NetworkEvent.class;
            prepareEvent(listeners, root);
            for (Class<?> event : root.getDeclaredClasses()) {
                if (net.minecraftforge.eventbus.api.Event.class.isAssignableFrom(event)) {
                    prepareEvent(listeners, event);
                }
            }
            net.minecraft.SharedConstants.tryDetectVersion();
            net.minecraft.server.Bootstrap.bootStrap();
            initialized = true;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Cannot initialize Forge's isolated registry fixture", exception);
        }
    }
}
