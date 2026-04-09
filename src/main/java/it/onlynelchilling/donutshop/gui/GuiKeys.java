package it.onlynelchilling.donutshop.gui;

import it.onlynelchilling.donutshop.DonutShop;
import org.bukkit.NamespacedKey;

public final class GuiKeys {

    public static final NamespacedKey ACTION;
    public static final NamespacedKey CATEGORY_ID;
    public static final NamespacedKey ITEM_INDEX;
    public static final NamespacedKey DELTA;
    public static final NamespacedKey SHOP_STAMP;

    public static final String OPEN_CATEGORY = "open_category";
    public static final String BACK           = "back";
    public static final String PREV_PAGE      = "prev_page";
    public static final String NEXT_PAGE      = "next_page";
    public static final String SELECT_ITEM    = "select_item";
    public static final String CONFIRM        = "confirm";
    public static final String CANCEL         = "cancel";
    public static final String SET_MIN        = "set_min";
    public static final String SET_MAX        = "set_max";
    public static final String CHANGE_AMOUNT  = "change_amount";

    static {
        DonutShop plugin = DonutShop.getInstance();
        ACTION     = new NamespacedKey(plugin, "action");
        CATEGORY_ID = new NamespacedKey(plugin, "category_id");
        ITEM_INDEX = new NamespacedKey(plugin, "item_index");
        DELTA      = new NamespacedKey(plugin, "delta");
        SHOP_STAMP = new NamespacedKey(plugin, "shop_stamp");
    }

    private GuiKeys() {}
}

