package com.refinedmods.refinedstorage.integration.jei;

import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPattern;
import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPatternProvider;
import com.refinedmods.refinedstorage.api.util.IComparer;
import com.refinedmods.refinedstorage.apiimpl.API;
import com.refinedmods.refinedstorage.item.PatternItem;
import com.refinedmods.refinedstorage.screen.grid.stack.IGridStack;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

public class IngredientTracker {
    private final List<Ingredient> ingredients = new ArrayList<>();
    private final Map<ResourceLocation, Integer> storedItems = new HashMap<>();
    private boolean doTransfer;

    public IngredientTracker(IRecipeSlotsView recipeLayout, boolean doTransfer) {
        for (IRecipeSlotView slotView : recipeLayout.getSlotViews(RecipeIngredientRole.INPUT)) {
            Optional<ItemStack> optionalItemStack = slotView.getIngredients(VanillaTypes.ITEM_STACK).findAny();

            optionalItemStack.ifPresent(stack -> ingredients.add(new Ingredient(slotView, stack.getCount())));
        }

        this.doTransfer = doTransfer;
    }

    public Collection<Ingredient> getIngredients() {
        return ingredients;
    }

    public void addAvailableStack(ItemStack stack, @Nullable IGridStack gridStack) {
        int available = stack.getCount();
        if (doTransfer) {
            if (stack.getItem() instanceof ICraftingPatternProvider) {
                ICraftingPattern pattern = PatternItem.fromCache(Minecraft.getInstance().level, stack);
                if (pattern.isValid()) {
                    for (ItemStack outputStack : pattern.getOutputs()) {
                        storedItems.merge(registryName(outputStack.getItem()), outputStack.getCount(), Integer::sum);
                    }
                }

            } else {
                storedItems.merge(registryName(stack.getItem()), available, Integer::sum);
            }
        }

        for (Ingredient ingredient : ingredients) {
            if (available == 0) {
                return;
            }

            Optional<ItemStack> match = ingredient
                .getSlotView()
                .getIngredients(VanillaTypes.ITEM_STACK)
                .filter(s -> API.instance().getComparer().isEqual(stack, s, IComparer.COMPARE_NBT))
                .findFirst();

            if (match.isPresent()) {
                // Craftables and non-craftables are 2 different gridstacks
                // As such we need to ignore craftable stacks as they are not actual items
                if (gridStack != null && gridStack.isCraftable()) {
                    ingredient.setCraftStackId(gridStack.getId());
                } else if (!ingredient.isAvailable()) {
                    int needed = ingredient.getMissingAmount();
                    int used = Math.min(available, needed);
                    ingredient.fulfill(used);
                    available -= used;
                }
            }
        }
    }

    public boolean hasMissing() {
        return ingredients.stream().anyMatch(ingredient -> !ingredient.isAvailable());
    }

    public boolean hasMissingButAutocraftingAvailable() {
        return ingredients.stream().anyMatch(ingredient -> !ingredient.isAvailable() && ingredient.isCraftable());
    }

    public boolean isAutocraftingAvailable() {
        return ingredients.stream().anyMatch(Ingredient::isCraftable);
    }

    public Map<UUID, Integer> createCraftingRequests() {
        Map<UUID, Integer> toRequest = new HashMap<>();

        for (Ingredient ingredient : ingredients) {
            if (!ingredient.isAvailable() && ingredient.isCraftable()) {
                toRequest.merge(ingredient.getCraftStackId(), ingredient.getMissingAmount(), Integer::sum);
            }
        }

        return toRequest;
    }

    public ItemStack findBestMatch(List<ItemStack> list) {
        ItemStack stack = ItemStack.EMPTY;
        int count = 0;

        for (ItemStack itemStack : list) {
            Integer stored = storedItems.get(registryName(itemStack.getItem()));
            if (stored != null && stored > count) {
                stack = itemStack;
                count = stored;
            }
        }

        return stack;
    }

    private ResourceLocation registryName(final Item item) {
        return ForgeRegistries.ITEMS.getKey(item);
    }
}
