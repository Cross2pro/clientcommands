package net.earthcomputer.clientcommands.mixin;

import net.earthcomputer.clientcommands.features.EnchantmentCracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantmentScreen.class)
public abstract class MixinEnchantingScreen extends AbstractContainerScreen<EnchantmentMenu> {

    public MixinEnchantingScreen(EnchantmentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void postRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (EnchantmentCracker.isEnchantingPredictionEnabled()) {
            EnchantmentCracker.drawEnchantmentGUIOverlay(graphics);
        }
    }

    @Inject(method = "mouseClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handleInventoryButtonClick(II)V"))
    public void onItemEnchanted(CallbackInfoReturnable<Boolean> ci) {
        EnchantmentCracker.onEnchantedItem();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (EnchantmentCracker.isEnchantingPredictionEnabled()) {
            addRenderableWidget(Button.builder(Component.translatable("enchCrack.addInfo"), button -> {
                EnchantmentCracker.addEnchantmentSeedInfo(Minecraft.getInstance().level, getMenu());
            }).pos(width - 150, 0).build());
        }
    }
}
