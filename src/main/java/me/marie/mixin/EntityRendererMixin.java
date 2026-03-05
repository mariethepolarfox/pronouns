package me.marie.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import me.marie.pronouns.PronounDbIntegration;
import me.marie.pronouns.handler.PlayerHandler;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    @Inject(
            method = "extractRenderState",
            at = @At("HEAD")
    )
    private void extractRenderState(T entity, S entityRenderState, float f, CallbackInfo ci) {
        entityRenderState.setData(PronounDbIntegration.ENTITY_DATA_KEY, entity);
    }

    @Inject(
            method = "submitNameTag",
            at = @At("HEAD")
    )
    private void renderPronounsExtension(S entityRenderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState cameraRenderState, CallbackInfo ci) {
        PlayerHandler.INSTANCE.renderNameTagExtension(entityRenderState, poseStack, submitNodeCollector, cameraRenderState);
    }
}
