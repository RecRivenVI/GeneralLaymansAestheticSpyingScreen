package io.github.recrivenvi.glass.probe.performance;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import net.minecraft.network.Connection;

/** Counts encoded Minecraft stream bytes before the socket, without changing packet contents. */
public final class PerformanceWire {
    public static final ConcurrentHashMap<UUID, LongAdder> SENT = new ConcurrentHashMap<>(),
            RECEIVED = new ConcurrentHashMap<>(),
            PAYLOAD = new ConcurrentHashMap<>();

    private PerformanceWire() {}

    public static void reset() {
        SENT.clear();
        RECEIVED.clear();
        PAYLOAD.clear();
    }

    public static void install(Connection connection, UUID player) {
        try {
            var field = Connection.class.getDeclaredField("channel");
            field.setAccessible(true);
            var channel = (Channel) field.get(connection);
            channel.eventLoop()
                    .execute(
                            () ->
                                    channel.pipeline()
                                            .addFirst(
                                                    "glass-performance-wire",
                                                    new ChannelDuplexHandler() {
                                                        @Override
                                                        public void write(
                                                                ChannelHandlerContext context,
                                                                Object message,
                                                                ChannelPromise promise)
                                                                throws Exception {
                                                            if (PerformanceCounters.measuring
                                                                    && message
                                                                            instanceof
                                                                            ByteBuf bytes)
                                                                SENT.computeIfAbsent(
                                                                                player,
                                                                                k ->
                                                                                        new LongAdder())
                                                                        .add(bytes.readableBytes());
                                                            super.write(context, message, promise);
                                                        }

                                                        @Override
                                                        public void channelRead(
                                                                ChannelHandlerContext context,
                                                                Object message)
                                                                throws Exception {
                                                            if (PerformanceCounters.measuring
                                                                    && message
                                                                            instanceof
                                                                            ByteBuf bytes)
                                                                RECEIVED.computeIfAbsent(
                                                                                player,
                                                                                k ->
                                                                                        new LongAdder())
                                                                        .add(bytes.readableBytes());
                                                            super.channelRead(context, message);
                                                        }
                                                    }));
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }
}
