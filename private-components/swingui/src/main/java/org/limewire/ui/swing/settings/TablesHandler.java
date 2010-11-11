package org.limewire.ui.swing.settings;

import java.util.HashMap;
import java.util.Map;

import org.limewire.core.settings.LimeWireSettings;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.Setting;
import org.limewire.setting.SettingsFactory;

/**
 * Handles preferences for tables.  Stored settings include
 * the table header's width, order and visibility.
 * Accessor are slightly different than other settings classes,
 * because they are accessed less-frequently and must be slightly
 * more mutable than other settings classes.
 */
public final class TablesHandler extends LimeWireSettings {

    private static final TablesHandler INSTANCE =
        new TablesHandler();
    private static final SettingsFactory FACTORY =
        INSTANCE.getFactory();

    public static TablesHandler instance() {
        return INSTANCE;
    }

    private TablesHandler() {
        super("tables.props", "LimeWire tables file");
    }

    private static final String WIDTH = "_WIDTH";
    private static final String ORDER = "_ORDER";
    private static final String VISBL = "_VISIBLE";
    private static final String SORTED_COLUMN = "_SORTED";
    private static final String SORT_ORDER = "_SORT_ORDER";

   /**
    * The list of settings.  The Key is the name of the setting,
    * and the Setting is the actual setting.  The subclass of
    * Setting is either BooleanSetting or IntSetting.
    * The name of the setting is in the format of:
    * <columnId>_<width|order|visible>
    */
    private static final Map<String, Setting> SETS = new HashMap<String, Setting>();

    /**
     * Returns the IntSetting for the specified column's width.
     */
    public static IntSetting getWidth(String id, int def) {
        return getSetting(id + WIDTH, def);
    }

    /**
     * Returns the IntSetting for the specified column's order.
     */
    public static IntSetting getOrder(String id, int def) {
        return getSetting(id + ORDER, def);
    }

    /**
     * Returns the BooleanSetting for the specified column's visibility.
     */
    public static BooleanSetting getVisibility(String id, boolean def) {
        return getSetting(id + VISBL, def);
    }

    /**
     * Returns the IntSetting for the column that was last sorted.
     */
    public static IntSetting getSortedColumn(String id, int def) {
        return getSetting(id + SORTED_COLUMN, def);
    }
    
    /**
     * Returns true if the column was in ascending order, false if
     * it was in descending order.
     */
    public static BooleanSetting getSortedOrder(String id, boolean def) {
        return getSetting(id + SORT_ORDER, def);
    }
    
    /**
     * Returns the setting stored within SETS for the specified setting.
     * If none exists, one is created.
     */
    private static IntSetting getSetting(String id, int def) {
        IntSetting set = (IntSetting)SETS.get(id);
        if (set == null) {
            set = FACTORY.createIntSetting(id, def);
            SETS.put(id, set);
        }
        return set;
    }

    /**
     * Returns the setting stored within SETS for the specified setting.
     * If none exists, one is created.
     */
    private static BooleanSetting getSetting(String id, boolean def) {
        BooleanSetting set = (BooleanSetting)SETS.get(id);
        if (set == null) {
            set = FACTORY.createBooleanSetting(id, def);
            SETS.put(id, set);
        }
        return set;
    }
}