package io.github.recrivenvi.glass.probe;

import java.nio.file.Files;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Native visual reference fixture shared by the checkpoint and independent-view candidates. */
final class GlassRichProbe {
    private boolean built, moving;
    private ArmorStand stand;
    private Pig pig;
    private int ticks;

    GlassRichProbe() {
        NeoForge.EVENT_BUS.addListener(this::tick);
    }

    private void tick(ServerTickEvent.Post event) {
        var server = event.getServer();
        var out = GlassOverlayProbe.output();
        if (!Files.exists(out.resolve("fixture.json"))
                || server.getPlayerList().getPlayers().isEmpty()) return;
        var level = server.overworld();
        try {
            if (!built) {
                built = true;
                for (int x = 11; x <= 21; x++)
                    for (int y = 195; y <= 205; y++)
                        level.setBlockAndUpdate(
                                new BlockPos(x, y, 19), Blocks.AIR.defaultBlockState());
                for (int x = 11; x <= 21; x++)
                    for (int y = 195; y <= 205; y++)
                        level.setBlockAndUpdate(
                                new BlockPos(x, y, 22),
                                (x < 16 ? Blocks.LIME_CONCRETE : Blocks.RED_CONCRETE)
                                        .defaultBlockState());
                for (int x = 12; x <= 20; x++)
                    for (int z = 18; z <= 24; z++)
                        level.setBlockAndUpdate(
                                new BlockPos(x, 198, z), Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(
                        new BlockPos(17, 199, 21), Blocks.CHEST.defaultBlockState());
                level.setBlockAndUpdate(
                        new BlockPos(14, 199, 21), Blocks.TORCH.defaultBlockState());
                level.setBlockAndUpdate(
                        new BlockPos(18, 200, 20), Blocks.GLASS.defaultBlockState());
                var sign = new BlockPos(15, 200, 20);
                level.setBlockAndUpdate(
                        sign,
                        Blocks.OAK_SIGN
                                .defaultBlockState()
                                .setValue(
                                        net.minecraft.world.level.block.StandingSignBlock.ROTATION,
                                        8));
                text(level, sign, "GLASS A");
                stand = new ArmorStand(level, 16.5, 199, 21.25);
                stand.setNoGravity(true);
                stand.setYRot(180);
                stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
                stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE));
                stand.setGlowingTag(true);
                level.addFreshEntity(stand);
                pig = EntityType.PIG.create(level);
                pig.setPos(15.1, 199, 21.3);
                pig.setNoAi(true);
                pig.setNoGravity(true);
                pig.setPersistenceRequired();
                level.addFreshEntity(pig);
                Files.writeString(out.resolve("rich-ready.txt"), "ready");
            }
            var request = out.resolve("rich-request.txt");
            if (Files.exists(request)) {
                String action = Files.readString(request).strip();
                if (!action.isEmpty()) {
                    Files.writeString(request, "");
                    if (action.equals("change")) {
                        stand.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                        stand.setGlowingTag(false);
                        text(level, new BlockPos(15, 200, 20), "GLASS B");
                        level.blockEvent(new BlockPos(17, 199, 21), Blocks.CHEST, 1, 1);
                        level.sendParticles(
                                net.minecraft.core.particles.ParticleTypes.FLAME,
                                16.5,
                                201,
                                20.5,
                                40,
                                .5,
                                .5,
                                .5,
                                .01);
                    }
                    if (action.equals("motion")) {
                        moving = true;
                        pig.setNoAi(false);
                    }
                    if (action.equals("hurt")) {
                        level.broadcastEntityEvent(pig, (byte) 2);
                        level.getChunkSource()
                                .broadcastAndSend(
                                        pig,
                                        new net.minecraft.network.protocol.game
                                                .ClientboundHurtAnimationPacket(pig.getId(), 0));
                    }
                    if (action.equals("remove")) {
                        pig.discard();
                        stand.discard();
                        moving = false;
                    }
                    if (action.equals("fog") || action.equals("clear"))
                        for (int x = 15; x <= 17; x++)
                            for (int y = 199; y <= 202; y++)
                                for (int z = 10; z <= 13; z++)
                                    level.setBlockAndUpdate(
                                            new BlockPos(x, y, z),
                                            (action.equals("fog") ? Blocks.WATER : Blocks.AIR)
                                                    .defaultBlockState());
                    Files.writeString(out.resolve("rich-arranged.txt"), action);
                }
            }
            if (moving && !pig.isRemoved()) {
                if (pig.getX() > 16.3) pig.setPos(15.1, 199, 21.3);
                pig.setDeltaMovement(.035, 0, 0);
                pig.hurtMarked = true;
            }
            if (++ticks % 10 == 0)
                Files.writeString(
                        out.resolve("rich-state.json"),
                        new com.google.gson.Gson()
                                .toJson(
                                        Map.of(
                                                "pigX",
                                                pig.getX(),
                                                "pigTickCount",
                                                pig.tickCount,
                                                "pigRemoved",
                                                pig.isRemoved(),
                                                "standRemoved",
                                                stand.isRemoved(),
                                                "helmetEmpty",
                                                stand.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),
                                                "glowing",
                                                stand.isCurrentlyGlowing())));
        } catch (java.io.IOException error) {
            throw new java.io.UncheckedIOException(error);
        }
    }

    private static void text(ServerLevel level, BlockPos pos, String line) {
        var sign =
                (net.minecraft.world.level.block.entity.SignBlockEntity) level.getBlockEntity(pos);
        sign.setText(
                sign.getFrontText()
                        .setMessage(0, net.minecraft.network.chat.Component.literal(line)),
                true);
        sign.setChanged();
        level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
    }
}
