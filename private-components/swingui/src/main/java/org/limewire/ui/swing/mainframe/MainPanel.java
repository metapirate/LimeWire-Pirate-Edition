package org.limewire.ui.swing.mainframe;

import java.awt.CardLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavComponent;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.NavMediator;
import org.limewire.ui.swing.nav.NavSelectable;
import org.limewire.ui.swing.nav.NavigationListener;
import org.limewire.ui.swing.nav.Navigator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MainPanel extends JPanel {
    
    private static final Log LOG = LogFactory.getLog(MainPanel.class);
    
    private final Map<String, JComponent> loadedComponents = new HashMap<String, JComponent>();
    private final CardLayout cardLayout;    
    private final Navigator navigator;
    
    private final String TEMP_CARD = "temporaryComponentCard";    
    private JComponent temporaryPanel;
    
    @Inject
    public MainPanel(Navigator navigator) {   
        this.cardLayout = new CardLayout();
        this.navigator = navigator;
        setLayout(cardLayout);

        this.addComponentListener(new ComponentListener(){
            @Override
            public void componentHidden(ComponentEvent e) {}
            @Override
            public void componentMoved(ComponentEvent e) {}
            @Override
            public void componentShown(ComponentEvent e) {}
            
            @Override
            public void componentResized(ComponentEvent e) {
                MainPanel.this.revalidate();
            }

        });

        navigator.addNavigationListener(new NavigationListener() {
            @Override
            public void itemAdded(NavCategory category, NavItem navItem) {
            }

            @Override
            public void itemRemoved(NavCategory category, NavItem navItem, boolean wasSelected) {
                LOG.debugf("Removed item {0}", navItem);
                JComponent component = loadedComponents.get(asString(navItem));
                if(component != null){
                    remove(component);
                    loadedComponents.remove(asString(navItem));
                }
            }
            @Override
            public void itemSelected(NavCategory category, NavItem navItem,
                    NavSelectable selectable, NavMediator navMediator) {
                LOG.debugf("Selected item {0}", navItem);
                if (navItem != null) {
                    if(temporaryPanel != null) {
                        remove(temporaryPanel);
                        temporaryPanel = null;                        
                    }
                    JComponent panel = loadedComponents.get(asString(navItem));
                    if(panel == null) {
                        panel = navMediator.getComponent();
                        loadedComponents.put(asString(navItem), panel);
                        add(panel, asString(navItem));
                    }
                    cardLayout.show(MainPanel.this, asString(navItem));
                    if (selectable != null && panel instanceof NavComponent) {
                        NavComponent navComponent = (NavComponent) panel;
                        navComponent.select(selectable);
                    }
                }
            }
            
            @Override public void categoryAdded(NavCategory category) {}
            @Override public void categoryRemoved(NavCategory category, boolean wasSelected) {}
        });
    }
    
    /** Shows a panel temporarily.  As soon as another panel is shown, this panel is erased. */
    public void showTemporaryPanel(JComponent panel) {
        navigator.showNothing();
        temporaryPanel = panel;
        add(TEMP_CARD, temporaryPanel);
        cardLayout.show(MainPanel.this, TEMP_CARD);
    }
    
    private String asString(Object key) {
        return System.identityHashCode(key) + "";
    }

}