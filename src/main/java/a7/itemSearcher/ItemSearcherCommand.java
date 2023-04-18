package a7.itemSearcher;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

import static a7.itemSearcher.utils.McUtils.sendChat;

public class ItemSearcherCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "itemsearcher";
    }

    @Override
    public String getCommandUsage(ICommandSender icommandsender) {
        return "/itemsearcher - Toggles the mod";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        boolean enabled = ItemSearcher.searcher.toggle();
        sendChat("Item searcher is " + (enabled ? "on" : "off"));
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
