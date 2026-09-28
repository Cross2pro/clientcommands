package net.earthcomputer.clientcommands.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static net.minecraft.commands.Commands.*;

public class PredictBrushablesCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("cpredictbrushables")
                .executes(ctx -> predictBrushables(ctx, 64))
                .then(argument("radius", IntegerArgumentType.integer(16, 256))
                        .executes(ctx -> predictBrushables(ctx, IntegerArgumentType.getInteger(ctx, "radius")))));
    }

    private static int predictBrushables(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, int radius) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            ctx.getSource().sendFailure(Component.translatable("commands.cpredictbrushables.noWorld"));
            return 0;
        }

        BlockPos playerPos = mc.player.blockPosition();
        List<BrushableInfo> found = new ArrayList<>();

        // Scan for brushable block entities in loaded chunks
        int r = radius;
        for (int x = -r; x <= r; x += 16) {
            for (int z = -r; z <= r; z += 16) {
                int chunkX = (playerPos.getX() + x) >> 4;
                int chunkZ = (playerPos.getZ() + z) >> 4;
                if (!mc.level.hasChunk(chunkX, chunkZ)) continue;

                var chunk = mc.level.getChunk(chunkX, chunkZ);
                for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                    if (Math.abs(pos.getX() - playerPos.getX()) > r ||
                        Math.abs(pos.getY() - playerPos.getY()) > 64 ||
                        Math.abs(pos.getZ() - playerPos.getZ()) > r) {
                        continue;
                    }
                    BlockEntity be = chunk.getBlockEntity(pos);
                    if (be instanceof BrushableBlockEntity brushable) {
                        found.add(new BrushableInfo(pos.immutable(), brushable));
                    }
                }
            }
        }

        if (found.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("commands.cpredictbrushables.none", r), false);
            return 1;
        }

        ctx.getSource().sendSuccess(() -> Component.translatable("commands.cpredictbrushables.found", found.size()), false);

        // Try to get server-side data in singleplayer via reflection
        // (avoids compile-time dependency on server classes)
        Object serverLevel = getServerLevel(mc);
        for (BrushableInfo info : found) {
            ItemStack predicted = predictLoot(info, serverLevel);
            Component posText = Component.literal(String.format("[%d, %d, %d]", 
                info.pos.getX(), info.pos.getY(), info.pos.getZ()));
            
            if (predicted != null && !predicted.isEmpty()) {
                Component itemText = Component.translatable("commands.cpredictbrushables.predicted",
                    posText, predicted.getHoverName());
                ctx.getSource().sendSuccess(() -> itemText, false);
            } else {
                // Check if item is already visible (partially brushed)
                ItemStack visibleItem = getVisibleItem(info.brushable);
                if (visibleItem != null && !visibleItem.isEmpty()) {
                    Component itemText = Component.translatable("commands.cpredictbrushables.visible",
                        posText, visibleItem.getHoverName());
                    ctx.getSource().sendSuccess(() -> itemText, false);
                } else {
                    Component unknownText = Component.translatable("commands.cpredictbrushables.unknown", posText);
                    ctx.getSource().sendSuccess(() -> unknownText, false);
                }
            }
        }

        return 1;
    }

    private static Object getServerLevel(Minecraft mc) {
        try {
            if (!mc.hasSingleplayerServer()) return null;
            Object server = mc.getSingleplayerServer();
            // Get the overworld level via reflection
            java.lang.reflect.Method getLevel = server.getClass().getMethod("getLevel", net.minecraft.resources.ResourceKey.class);
            return getLevel.invoke(server, Level.OVERWORLD);
        } catch (Exception e) {
            return null;
        }
    }

    private static ItemStack predictLoot(BrushableInfo info, Object serverLevel) {
        if (serverLevel == null) return null;

        try {
            // Get the server-side block entity which has the loot table seed
            java.lang.reflect.Method getBlockEntity = serverLevel.getClass().getMethod("getBlockEntity", BlockPos.class);
            BlockEntity serverBe = (BlockEntity) getBlockEntity.invoke(serverLevel, info.pos);
            if (!(serverBe instanceof BrushableBlockEntity serverBrushable)) {
                return null;
            }

            // Use reflection to get lootTable and lootTableSeed (they're private)
            Field lootTableField = BrushableBlockEntity.class.getDeclaredField("lootTable");
            Field lootTableSeedField = BrushableBlockEntity.class.getDeclaredField("lootTableSeed");
            lootTableField.setAccessible(true);
            lootTableSeedField.setAccessible(true);

            ResourceLocation lootTableId = (ResourceLocation) lootTableField.get(serverBrushable);
            long lootTableSeed = (long) lootTableSeedField.get(serverBrushable);

            if (lootTableId == null) return null;

            // Get LootData via reflection to avoid compile-time server dependency
            java.lang.reflect.Method getServer = serverLevel.getClass().getMethod("getServer");
            Object minecraftServer = getServer.invoke(serverLevel);
            java.lang.reflect.Method getLootData = minecraftServer.getClass().getMethod("getLootData");
            Object lootData = getLootData.invoke(minecraftServer);
            java.lang.reflect.Method getLootTable = lootData.getClass().getMethod("getLootTable", ResourceLocation.class);
            Object lootTable = getLootTable.invoke(lootData, lootTableId);

            // Simulate the loot table roll
            LootParams params = new LootParams.Builder((ServerLevel) serverLevel)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(info.pos))
                    .create(LootContextParamSets.ARCHAEOLOGY);
            
            java.lang.reflect.Method getRandomItems = lootTable.getClass().getMethod("getRandomItems", LootParams.class, long.class);
            @SuppressWarnings("unchecked")
            List<ItemStack> items = (List<ItemStack>) getRandomItems.invoke(lootTable, params, lootTableSeed);
            if (!items.isEmpty()) {
                return items.get(0);
            }
        } catch (Exception e) {
            // Reflection failed or other error, return null
        }
        return null;
    }

    private static ItemStack getVisibleItem(BrushableBlockEntity brushable) {
        try {
            Field itemField = BrushableBlockEntity.class.getDeclaredField("item");
            itemField.setAccessible(true);
            return (ItemStack) itemField.get(brushable);
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private record BrushableInfo(BlockPos pos, BrushableBlockEntity brushable) {}
}
