package dev.swingstudio.screen;

import dev.swingstudio.SwingConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

enum ItemCategory {
	ALL,
	EDITED,
	COMBAT,
	SWORDS,
	AXES,
	TOOLS,
	BLOCKS,
	FOOD,
	OTHER;

	Component label() {
		return Component.translatable("swingstudio.category." + this.name().toLowerCase(java.util.Locale.ROOT));
	}

	boolean test(ItemStack stack, String key) {
		return switch (this) {
			case ALL -> true;
			case EDITED -> SwingConfig.get().hasOwn(key);
			case COMBAT -> isCombat(stack);
			case SWORDS -> stack.is(ItemTags.SWORDS);
			case AXES -> stack.is(ItemTags.AXES);
			case TOOLS -> isTool(stack);
			case BLOCKS -> isBlock(stack);
			case FOOD -> isFood(stack);
			case OTHER -> !stack.isEmpty() && !isCombat(stack) && !isTool(stack) && !isBlock(stack) && !isFood(stack);
		};
	}

	private static boolean isCombat(ItemStack stack) {
		return stack.is(ItemTags.SWORDS)
			|| stack.is(ItemTags.AXES)
			|| stack.is(ItemTags.SPEARS)
			|| stack.is(Items.MACE)
			|| stack.is(Items.TRIDENT)
			|| stack.is(Items.BOW)
			|| stack.is(Items.CROSSBOW)
			|| stack.is(Items.SHIELD)
			|| stack.is(Items.TOTEM_OF_UNDYING)
			|| stack.is(Items.ENDER_PEARL)
			|| stack.is(Items.WIND_CHARGE)
			|| stack.is(Items.END_CRYSTAL)
			|| stack.is(Items.GOLDEN_APPLE)
			|| stack.is(Items.ENCHANTED_GOLDEN_APPLE);
	}

	private static boolean isTool(ItemStack stack) {
		return stack.is(ItemTags.PICKAXES)
			|| stack.is(ItemTags.AXES)
			|| stack.is(ItemTags.SHOVELS)
			|| stack.is(ItemTags.HOES)
			|| stack.is(Items.SHEARS)
			|| stack.is(Items.FISHING_ROD)
			|| stack.is(Items.FLINT_AND_STEEL)
			|| stack.is(Items.BRUSH);
	}

	private static boolean isBlock(ItemStack stack) {
		return stack.getItem() instanceof BlockItem;
	}

	private static boolean isFood(ItemStack stack) {
		return stack.has(DataComponents.FOOD);
	}
}
