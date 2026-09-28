package net.earthcomputer.clientcommands;

import com.mojang.logging.LogUtils;
import net.earthcomputer.clientcommands.command.CEnchantCommand;
import net.earthcomputer.clientcommands.command.ChorusCommand;
import net.earthcomputer.clientcommands.command.CrackRNGCommand;
import net.earthcomputer.clientcommands.command.FishCommand;
import net.earthcomputer.clientcommands.command.PredictBrushablesCommand;
import net.earthcomputer.clientcommands.render.RenderQueue;
import net.earthcomputer.clientcommands.task.TaskManager;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

/**
 * RNG-only port of ClientCommands for Forge 1.20.1.
 * Provides /ccrackrng, /cenchant, /cfish and chorus fruit teleport manipulation.
 */
@Mod(RngCommands.MOD_ID)
public class RngCommands {
    public static final String MOD_ID = "rngcommands";
    private static final Logger LOGGER = LogUtils.getLogger();

    public RngCommands() {
        Configs.init(FMLPaths.CONFIGDIR.get());
        MinecraftForge.EVENT_BUS.register(this);
        LOGGER.info("RNGCommands initialized");
    }

    @SubscribeEvent
    public void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CrackRNGCommand.register(event.getDispatcher());
        CEnchantCommand.register(event.getDispatcher());
        FishCommand.register(event.getDispatcher(), event.getBuildContext());
        ChorusCommand.register(event.getDispatcher());
        PredictBrushablesCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            TaskManager.tick();
        }
    }

    @SubscribeEvent
    public void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        ServerBrandManager.onDisconnect();
    }

    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            var bufferSource = mc.renderBuffers().bufferSource();
            var poseStack = event.getPoseStack();
            // Translate to world coordinates (camera-relative)
            var cameraPos = mc.gameRenderer.getMainCamera().getPosition();
            poseStack.pushPose();
            poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
            var vertexConsumer = bufferSource.getBuffer(RenderQueue.NO_DEPTH_LAYER);
            RenderQueue.render(RenderQueue.Layer.ON_TOP, poseStack, vertexConsumer, event.getPartialTick());
            bufferSource.endBatch(RenderQueue.NO_DEPTH_LAYER);
            poseStack.popPose();
        }
    }

    public static void renderWorld(RenderQueue.Layer layer) {
        // hooked via RenderLevelStageEvent above
    }
}
