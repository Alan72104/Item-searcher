package a7.itemSearcher.mixin;

import net.minecraft.client.gui.inventory.GuiContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiContainer.class)
public interface AccessorGuiContainer
{
    @Accessor("guiLeft")
    int getGuiLeft();
    
    @Accessor("guiTop")
    int getGuiTop();
    
    @Accessor("xSize")
    int getXSize();
    
    @Accessor("ySize")
    int getYSize();
}
