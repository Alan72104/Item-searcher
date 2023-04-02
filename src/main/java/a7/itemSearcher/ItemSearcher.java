package a7.itemSearcher;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = ItemSearcher.MOD_ID, version = ItemSearcher.VERSION)
public class ItemSearcher {
    public static final String MOD_ID = "itemsearcher";
    public static final String VERSION = "0.0.1";
    @Mod.Instance(MOD_ID)
    public static ItemSearcher instance;
    public static Searcher searcher;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        searcher = new Searcher();
    }
}
