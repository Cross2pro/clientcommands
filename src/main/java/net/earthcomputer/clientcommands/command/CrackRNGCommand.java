package net.earthcomputer.clientcommands.command;

import com.mojang.brigadier.Command;
import static net.earthcomputer.clientcommands.command.ClientCommandHelper.sendFeedback;
import com.mojang.brigadier.CommandDispatcher;
import net.cortex.clientAddon.cracker.SeedCracker;
import net.earthcomputer.clientcommands.ServerBrandManager;
import net.earthcomputer.clientcommands.Configs;
import net.earthcomputer.clientcommands.features.PlayerRandCracker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import static net.minecraft.commands.Commands.*;
public class CrackRNGCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("ccrackrng")
            .executes(ctx -> crackPlayerRNG(ctx.getSource())));
    }
    private static int crackPlayerRNG(CommandSourceStack source) {
        ServerBrandManager.rngWarning();
        SeedCracker.crack(seed -> {
            sendFeedback(Component.translatable("commands.ccrackrng.success", Long.toHexString(seed)));
            PlayerRandCracker.setSeed(seed);
            Configs.playerCrackState = PlayerRandCracker.CrackState.CRACKED;
        });
        return Command.SINGLE_SUCCESS;
    }
}
