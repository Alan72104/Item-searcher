package a7.itemSearcher;

import a7.itemSearcher.utils.RenderUtils;
import com.sun.jna.WeakIdentityHashMap;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

import static a7.itemSearcher.utils.McUtils.getFontRenderer;
import static a7.itemSearcher.utils.McUtils.getMc;

public class Searcher {
    private static final int MATCHES_PER_FRAME = 5;
    private boolean enabled = true;
    private final GuiTextField textField = new GuiTextField(6, getFontRenderer(), 15, 15, 450, 15);
    private SearchPredicate predicate = null;
    private final WeakIdentityHashMap /*<ItemStack, Boolean>*/ matchCache = new WeakIdentityHashMap();
    private int matchesInThisFrame = 0;

    public Searcher() {
        textField.setMaxStringLength(1000);
    }

    // region Events

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public void onGuiDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        matchesInThisFrame = 0;
    }

    @SubscribeEvent
    public void onGuiDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!enabled)
            return;

        if (!(getMc().currentScreen instanceof GuiContainer))
            return;

        textField.drawTextBox();
    }

    @SubscribeEvent
    public void onGuiMouseInputPre(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!enabled)
            return;

        if (!(getMc().currentScreen instanceof GuiContainer))
            return;

        if (Mouse.getEventButtonState() && Mouse.getEventButton() == 0) {
            final ScaledResolution scaledresolution = new ScaledResolution(getMc());
            final int scaledHeight = scaledresolution.getScaledHeight();
            final int mouseX = Mouse.getEventX() / scaledresolution.getScaleFactor();
            final int mouseY = scaledHeight - Mouse.getEventY() / scaledresolution.getScaleFactor();

            textField.mouseClicked(mouseX, mouseY, 0);
            if (textField.isFocused())
                event.setCanceled(true);
        }

    }

    @SubscribeEvent
    public void onGuiKeyInputPre(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!enabled)
            return;

        if (!(getMc().currentScreen instanceof GuiContainer))
            return;

        if (textField.isFocused() && Keyboard.getEventKeyState()) {
            textField.textboxKeyTyped(Keyboard.getEventCharacter(), Keyboard.getEventKey());

            String text = textField.getText();
            if (text.trim().isEmpty())
                predicate = null;
            else
                predicate = SearchPredicate.create(text);
            matchCache.clear();
            event.setCanceled(true);
        }


        if (Keyboard.getEventKey() == Keyboard.KEY_RCONTROL &&
                Keyboard.getEventKeyState() && !Keyboard.isRepeatEvent()) {

            GuiScreen screen = getMc().currentScreen;
            GuiContainer container = (GuiContainer) screen;
            Slot currentSlot = container.getSlotUnderMouse();

            if (currentSlot != null && currentSlot.getHasStack())
            {
                String nbtString = currentSlot.getStack().serializeNBT().toString();

                StringSelection ss = new StringSelection(nbtString);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, ss);
            }
        }
    }

    public void onSlotDrawn(Slot slot, CallbackInfo ci) {
        if (!enabled)
            return;

        ItemStack stack = slot.getStack();
        if (stack == null)
            return;

        if (predicate != null) {
            Object result = matchCache.get(stack);
            boolean matched = false;

            if (result == null) {
                if (matchesInThisFrame < MATCHES_PER_FRAME) {
                    matchesInThisFrame++;
                    matched = predicate.match(stack);
                    matchCache.put(stack, matched);
                }
            } else {
                matched = (Boolean) result;
            }

            if (matched)
                RenderUtils.drawGradientRect(slot.xDisplayPosition, slot.yDisplayPosition,
                        slot.xDisplayPosition + 16, slot.yDisplayPosition + 16, 200, 0x800080FF, 0xB000B0FF, 3);
        }
    }

    // endregion Events

    // region Misc

    public boolean isOn() {
        return enabled;
    }

    public boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    // region Misc
}