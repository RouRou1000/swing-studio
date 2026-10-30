package dev.swingstudio.mixin;

import dev.swingstudio.Preview;
import dev.swingstudio.SwingConfig;
import net.minecraft.client.player.FirstPersonHandsAndItems;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FirstPersonHandsAndItems.class)
public abstract class FirstPersonHandsAndItemsMixin {
	@Shadow
	private ItemStack mainHandItem;
	@Shadow
	private float mainHandHeight;
	@Shadow
	private float oMainHandHeight;

	@Inject(method = "tick", at = @At("TAIL"))
	private void swingstudio$preview(LocalPlayer player, CallbackInfo ci) {
		ItemStack preview = Preview.stack();
		if (preview != null) {
			this.mainHandItem = preview;
			this.mainHandHeight = 1.0F;
			this.oMainHandHeight = 1.0F;
		}
	}

	@Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("HEAD"), cancellable = true)
	private void swingstudio$instantSwap(ItemStack currentlyVisibleItem, ItemStack expectedItem, LocalPlayer player, CallbackInfoReturnable<Boolean> cir) {
		SwingConfig config = SwingConfig.get();
		if (config.enabled && !config.equipAnimation) {
			cir.setReturnValue(true);
		}
	}
}
