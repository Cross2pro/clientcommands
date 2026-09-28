package net.earthcomputer.clientcommands.command;

import com.mojang.brigadier.Command;
import static net.earthcomputer.clientcommands.command.ClientCommandHelper.sendFeedback;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.earthcomputer.clientcommands.Configs;
import net.earthcomputer.clientcommands.command.arguments.ItemAndEnchantmentsPredicateArgumentType.ItemAndEnchantmentsPredicate;
import net.earthcomputer.clientcommands.features.EnchantmentCracker;
import net.earthcomputer.clientcommands.features.PlayerRandCracker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import static net.earthcomputer.clientcommands.command.ClientCommandHelper.*;
import static net.earthcomputer.clientcommands.command.arguments.ItemAndEnchantmentsPredicateArgumentType.*;
import static net.minecraft.commands.Commands.*;

public class CEnchantCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> cenchant = literal("cenchant")
                .then(argument("itemAndEnchantmentsPredicate", itemAndEnchantmentsPredicate().withEnchantmentPredicate(CEnchantCommand::enchantmentPredicate).constrainMaxLevel())
                        .executes(ctx -> cenchant(ctx.getSource(), getItemAndEnchantmentsPredicate(ctx, "itemAndEnchantmentsPredicate"), false)));
        LiteralArgumentBuilder<CommandSourceStack> cenchantSimulate = literal("cenchant")
                .then(literal("--simulate")
                        .then(argument("itemAndEnchantmentsPredicate", itemAndEnchantmentsPredicate().withEnchantmentPredicate(CEnchantCommand::enchantmentPredicate).constrainMaxLevel())
                                .executes(ctx -> cenchant(ctx.getSource(), getItemAndEnchantmentsPredicate(ctx, "itemAndEnchantmentsPredicate"), true))));
        dispatcher.register(cenchant);
        dispatcher.register(cenchantSimulate);
    }

    private static boolean enchantmentPredicate(Item item, Enchantment ench) {
        return !ench.isTreasureOnly() && ench.isDiscoverable() && (item == Items.BOOK || ench.category.canEnchant(item));
    }

    private static int cenchant(CommandSourceStack source, ItemAndEnchantmentsPredicate itemAndEnchantmentsPredicate, boolean simulate) {
        if (!Configs.getEnchantingPrediction()) {
            Component text = Component.translatable("commands.cenchant.needEnchantingPrediction")
                    .withStyle(ChatFormatting.RED)
                    .append(" ")
                    .append(getCommandTextComponent("commands.client.enable", "/cconfig clientcommands enchantingPrediction set true"));
            sendFeedback(text);
            return Command.SINGLE_SUCCESS;
        }
        if (!Configs.playerCrackState.knowsSeed() && Configs.enchCrackState != EnchantmentCracker.CrackState.CRACKED) {
            Component text = Component.translatable("commands.cenchant.uncracked")
                    .withStyle(ChatFormatting.RED)
                    .append(" ")
                    .append(getCommandTextComponent("commands.client.crack", "/ccrackrng"));
            sendFeedback(text);
            return Command.SINGLE_SUCCESS;
        }

        var result = EnchantmentCracker.manipulateEnchantments(
                itemAndEnchantmentsPredicate.item(),
                itemAndEnchantmentsPredicate.predicate(),
                simulate
        );
        if (result == null) {
            sendFeedback(Component.translatable("commands.cenchant.failed"));
            if (Configs.playerCrackState != PlayerRandCracker.CrackState.CRACKED) {
                MutableComponent help = Component.translatable("commands.cenchant.help.uncrackedPlayerSeed")
                    .append(" ")
                    .append(getCommandTextComponent("commands.client.crack", "/ccrackrng"));
                sendHelp(help);
            }
        } else {
            if (result.itemThrows() < 0) {
                sendFeedback(Component.translatable("enchCrack.insn.itemThrows.noDummy"));
            } else {
                sendFeedback(Component.translatable("enchCrack.insn.itemThrows", result.itemThrows(), (float)result.itemThrows() / 20f));
            }
            sendFeedback(Component.translatable("enchCrack.insn.bookshelves", result.bookshelves()));
            sendFeedback(Component.translatable("enchCrack.insn.slot", result.slot() + 1));
            sendFeedback(Component.translatable("enchCrack.insn.enchantments"));
            for (EnchantmentInstance ench : result.enchantments()) {
                sendFeedback(Component.literal("- ").append(ench.enchantment.getFullname(ench.level)));
            }
            if (!simulate) {
                sendFeedback(Component.translatable("commands.cenchant.success"));
            }
        }
        return Command.SINGLE_SUCCESS;
    }
}
