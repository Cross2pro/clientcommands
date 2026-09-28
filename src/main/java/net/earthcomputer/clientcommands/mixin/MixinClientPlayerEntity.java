package net.earthcomputer.clientcommands.mixin;

import com.mojang.authlib.GameProfile;
import net.earthcomputer.clientcommands.features.PlayerRandCracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class MixinClientPlayerEntity extends AbstractClientPlayer {

    public MixinClientPlayerEntity(ClientLevel level, GameProfile profile) {
        super(level, profile);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V"))
    private void onTick(CallbackInfo ci) {
        if (!level().getEntitiesOfClass(ExperienceOrb.class, getBoundingBox().inflate(0.5), entity -> true).isEmpty()) {
            PlayerRandCracker.onXpOrb();
            // check armor + hands for mending
            boolean hasMending = false;
            for (ItemStack stack : getArmorSlots()) {
                if (couldMendingRepair(stack)) {
                    hasMending = true;
                    break;
                }
            }
            if (!hasMending) {
                for (ItemStack stack : getHandSlots()) {
                    if (couldMendingRepair(stack)) {
                        hasMending = true;
                        break;
                    }
                }
            }
            if (hasMending) {
                PlayerRandCracker.onMending();
            }
        }
    }

    @Unique
    private boolean couldMendingRepair(ItemStack stack) {
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.MENDING, stack) <= 0) {
            return false;
        }
        return stack.isDamaged();
    }

    @Inject(method = "drop(Z)Z", at = @At("HEAD"))
    public void onDrop(boolean dropAll, CallbackInfoReturnable<Boolean> ci) {
        PlayerRandCracker.onDropItem();
    }

    @Inject(method = "hurt", at = @At("HEAD"))
    public void onHurt(DamageSource source, float amount, CallbackInfoReturnable<Boolean> ci) {
        PlayerRandCracker.onDamage();
    }
}
