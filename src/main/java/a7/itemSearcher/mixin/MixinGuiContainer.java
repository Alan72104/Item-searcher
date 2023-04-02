package a7.itemSearcher.mixin;

import a7.itemSearcher.ItemSearcher;
import a7.itemSearcher.utils.RenderUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer extends GuiScreen
{
    @Shadow
    public Container inventorySlots;
    
    @Inject(method = "drawSlot", at = @At("TAIL"))
    public void slotDrawn(Slot slot, CallbackInfo ci)
    {
        ItemStack stack = slot.getStack();
        if (stack == null)
            return;

        if (ItemSearcher.searcher.isOn())// &&
//                ItemSearcher.searcher.stackMatches(stack))
        {
            RenderUtils.drawGradientRect(slot.xDisplayPosition, slot.yDisplayPosition,
                    slot.xDisplayPosition + 16, slot.yDisplayPosition + 16, 200, 0x800080FF, 0xB000B0FF, 3);
        }
    }
}