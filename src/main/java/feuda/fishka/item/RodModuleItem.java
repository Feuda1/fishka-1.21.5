package feuda.fishka.item;

import feuda.fishka.fishing.FishEncounterTier;
import feuda.fishka.fishing.RodModuleCatalog;
import feuda.fishka.fishing.RodModuleSlot;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;
import java.util.function.Consumer;

public final class RodModuleItem extends Item {
	private final RodModuleSlot slot;
	private final FishEncounterTier tier;

	public RodModuleItem(RodModuleSlot slot, FishEncounterTier tier, Settings settings) {
		super(settings);
		this.slot = slot;
		this.tier = tier;
	}

	public RodModuleSlot slot() {
		return slot;
	}

	public FishEncounterTier tier() {
		return tier;
	}

	@Override
	public void appendTooltip(
		ItemStack stack,
		Item.TooltipContext context,
		TooltipDisplayComponent displayComponent,
		Consumer<Text> textConsumer,
		TooltipType type
	) {
		textConsumer.accept(Text.translatable("fishka.module.tooltip.slot", Text.translatable(slot.translationKey())).formatted(Formatting.GRAY));
		textConsumer.accept(
			Text.translatable("fishka.module.tooltip.tier", Text.translatable("fishka.rarity." + tier.name().toLowerCase(Locale.ROOT)))
				.formatted(RodModuleCatalog.tierColor(tier))
		);
		textConsumer.accept(RodModuleCatalog.effectText(slot, tier));
	}
}
