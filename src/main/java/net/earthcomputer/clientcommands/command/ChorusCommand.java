package net.earthcomputer.clientcommands.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.earthcomputer.clientcommands.features.ChorusManipulation;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.world.phys.Vec3;

import static net.minecraft.commands.Commands.*;

public class ChorusCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("cchorus")
                .then(literal("setGoal")
                        .then(literal("relative")
                                .then(literal("area")
                                        .then(areaThen(true)))
                                .then(literal("block")
                                        .then(blockThen(true))))
                        .then(literal("absolute")
                                .then(literal("area")
                                        .then(areaThen(false)))
                                .then(literal("block")
                                        .then(blockThen(false))))
                ));
    }

    public static RequiredArgumentBuilder<CommandSourceStack, ?> areaThen(boolean relative) {
        return argument("posFrom", Vec3Argument.vec3())
                .then(argument("posTo", Vec3Argument.vec3())
                        .executes(ctx -> ChorusManipulation.setGoal(Vec3Argument.getVec3(ctx, "posFrom"), Vec3Argument.getVec3(ctx, "posTo"), relative)));
    }

    public static RequiredArgumentBuilder<CommandSourceStack, ?> blockThen(boolean relative) {
        return argument("posGoal", Vec3Argument.vec3())
                .executes(ctx -> {
                    Vec3 posGoal = Vec3Argument.getVec3(ctx, "posGoal");
                    Vec3 floored = new Vec3(Math.floor(posGoal.x), Math.floor(posGoal.y), Math.floor(posGoal.z));
                    return ChorusManipulation.setGoal(
                            floored.add(-0.2, 0, -0.2),
                            floored.add(1.2, 1, 1.2), relative);
                })
                .then(literal("--perfectly")
                        .executes(ctx -> {
                            Vec3 posGoal = Vec3Argument.getVec3(ctx, "posGoal");
                            Vec3 floored = new Vec3(Math.floor(posGoal.x), Math.floor(posGoal.y), Math.floor(posGoal.z));
                            return ChorusManipulation.setGoal(
                                    floored.add(0.3, 0, 0.3),
                                    floored.add(0.7, 1, 0.7), relative);
                        }));
    }
}
