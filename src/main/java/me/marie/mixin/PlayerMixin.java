package me.marie.mixin;

import me.marie.pronouns.impl.PronounDbImpl;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin extends LivingEntity {
    protected PlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(
            method = "decorateDisplayNameComponent",
            at = @At("HEAD")
    )
    private void onDecorateDisplayNameComponent(MutableComponent mutableComponent, CallbackInfoReturnable<MutableComponent> cir) {
        mutableComponent.append(" ").append(PronounDbImpl.INSTANCE.getPronounExtensionComponent(uuid));
    }
}
