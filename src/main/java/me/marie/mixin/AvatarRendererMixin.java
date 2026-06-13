package me.marie.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import me.marie.pronouns.Pronouns;
import me.marie.pronouns.handler.PlayerHandler;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("HEAD")
    )
    private void extractRenderState(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer clientAvatarEntity)) return;
        state.setData(Pronouns.ENTITY_DATA_KEY, clientAvatarEntity);
    }

    @Inject(
            method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("TAIL")
    )
    private void renderPronounsExtension(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera, CallbackInfo ci) {
        PlayerHandler.INSTANCE.renderNameTagExtension(state, poseStack, submitNodeCollector, camera);
    }
}
