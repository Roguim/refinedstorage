package com.refinedmods.refinedstorage.integration.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;


import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class RecipeTransferCraftingGridError implements IRecipeTransferError {
    protected static final Color AUTOCRAFTING_HIGHLIGHT_COLOR = new Color(0.0f, 0.0f, 1.0f, 0.4f);
    private static final Color MISSING_HIGHLIGHT_COLOR = new Color(1.0f, 0.0f, 0.0f, 0.4f);
    private static final boolean HOST_OS_IS_MACOS = System.getProperty("os.name").equals("Mac OS X");
    protected final IngredientTracker tracker;

    public RecipeTransferCraftingGridError(IngredientTracker tracker) {
        this.tracker = tracker;
    }

    @Override
    public Type getType() {
        return Type.COSMETIC;
    }

    @Override
    public void showError(PoseStack poseStack, int mouseX, int mouseY, IRecipeSlotsView recipeSlotsView, int recipeX, int recipeY) {
        poseStack.translate(recipeX, recipeY, 0);
        List<Component> message = drawIngredientHighlights(poseStack, recipeX, recipeY);

        Screen currentScreen = Minecraft.getInstance().screen;
        currentScreen.renderComponentTooltip(poseStack, message, mouseX, mouseY);
    }

    protected List<Component> drawIngredientHighlights(PoseStack stack, int recipeX, int recipeY) {
        List<Component> message = new ArrayList<>();
        message.add(Component.translatable("jei.tooltip.transfer"));

        boolean craftMessage = false;
        boolean missingMessage = false;

        for (Ingredient ingredient : tracker.getIngredients()) {
            if (!ingredient.isAvailable()) {
                if (ingredient.isCraftable()) {
                    ingredient.getSlotView().drawHighlight(stack, AUTOCRAFTING_HIGHLIGHT_COLOR.getRGB());
                    craftMessage = true;
                } else {
                    ingredient.getSlotView().drawHighlight(stack, MISSING_HIGHLIGHT_COLOR.getRGB());
                    missingMessage = true;
                }
            }
        }

        if (missingMessage) {
            message.add(Component.translatable("jei.tooltip.error.recipe.transfer.missing").withStyle(ChatFormatting.RED));
        }

        if (craftMessage) {
            if (HOST_OS_IS_MACOS) {
                message.add(Component.translatable("gui.refinedstorage.jei.transfer.request_autocrafting_mac").withStyle(ChatFormatting.BLUE));
            } else {
                message.add(Component.translatable("gui.refinedstorage.jei.transfer.request_autocrafting").withStyle(ChatFormatting.BLUE));
            }
        }

        return message;
    }
}
