package net.earthcomputer.clientcommands.mixin;

import net.earthcomputer.clientcommands.features.PlayerRandCracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerboundContainerClosePacket.class)
public class MixinCloseHandledScreenPacket {

    @Inject(method = "<init>(I)V", at = @At("RETURN"))
    public void onCreate(int containerId, CallbackInfo ci) {
        LocalPlayer player = Minecraft.getInstance().player;
        assert player != null;

        AbstractContainerMenu menu = player.containerMenu;

        if (!menu.getCarried().isEmpty()) {
            PlayerRandCracker.onDropItem();
        }

        if (menu instanceof BeaconMenu) {
            Slot paymentSlot = menu.getSlot(0);
            if (paymentSlot.getItem().getCount() > paymentSlot.getMaxStackSize()) {
                PlayerRandCracker.onDropItem();
            }
        }
    }

}
