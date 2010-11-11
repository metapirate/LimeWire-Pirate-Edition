package org.limewire.ui.swing.options;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Application;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.options.OptionPanel.ApplyOptionResult;
import org.limewire.ui.swing.options.actions.ApplyOptionAction;
import org.limewire.ui.swing.options.actions.CancelOptionAction;
import org.limewire.ui.swing.options.actions.HelpAction;
import org.limewire.ui.swing.options.actions.TabAction;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Main Dialog for the Options.
 */
public class OptionsDialog extends LimeJDialog implements OptionsTabNavigator {

    @Resource
    private Icon advancedIcon;
    @Resource
    private Icon transfersIcon;
    @Resource
    private Icon libraryIcon;
    @Resource
    private Icon miscIcon;
    @Resource
    private Icon searchIcon;
    
    
    public static final String LIBRARY = I18n.tr("My Files");
    public static final String SEARCH = I18n.tr("Search");
    public static final String TRANSFERS = I18n.tr("Transfers");
    public static final String SECURITY = I18n.tr("Security");
    public static final String MISC = I18n.tr("Misc");
    public static final String ADVANCED = I18n.tr("Advanced");
    
    private final Provider<LibraryOptionPanel> libraryOptionPanel;
    private final Provider<SearchOptionPanel> searchOptionPanel;
    private final Provider<TransferOptionPanel> transferOptionPanel;
    private final Provider<MiscOptionPanel> miscOptionPanel;
    private final Provider<AdvancedOptionPanel> advancedOptionPanel;
    private final UnsafeTypeOptionPanelStateManager unsafeTypeOptionPanelStateManager;
    private final Application application;
    
    private final Map<String, OptionTabItem> cards = new HashMap<String,OptionTabItem>();
    private final Map<String, OptionPanel> panels = new HashMap<String, OptionPanel>();
    private final List<String> list = new ArrayList<String>();
    private OptionTabItem selectedItem;
    
    private JPanel cardPanel;
    private CardLayout cardLayout;
    
    private JXPanel headerPanel;
    private JPanel footerPanel;
    
    private JButton helpButton;
    private JButton okButton;
    private JButton cancelButton;
    
    @Inject
    public OptionsDialog(Provider<LibraryOptionPanel> libraryOptionPanel, Provider<SearchOptionPanel> searchOptionPanel,
            Provider<TransferOptionPanel> transferOptionPanel,
            Provider<MiscOptionPanel> miscOptionPanel, Provider<AdvancedOptionPanel> advancedOptionPanel,
            BarPainterFactory barPainterFactory, UnsafeTypeOptionPanelStateManager unsafeTypeOptionPanelStateManager,
            Application application) {
        super(GuiUtils.getMainFrame(), (OSUtils.isMacOSX()) ? I18n.tr("Preferences") : I18n.tr("Options"), true);

        GuiUtils.assignResources(this); 
        
        this.application = application;
        this.libraryOptionPanel = libraryOptionPanel;
        this.searchOptionPanel = searchOptionPanel;
        this.transferOptionPanel = transferOptionPanel;
        this.miscOptionPanel = miscOptionPanel;
        this.advancedOptionPanel = advancedOptionPanel;
        this.unsafeTypeOptionPanelStateManager = unsafeTypeOptionPanelStateManager;
        
        if (!OSUtils.isMacOSX()) {
            setSize(725, 562);
            setPreferredSize(getSize());
        } else {
            setSize(835, 613);
            setPreferredSize(getSize());
        }
        setResizable(false);
        
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        createComponents(barPainterFactory);
        
        pack();
    }
    
    public ApplyOptionResult applyOptions() {
        ApplyOptionResult result = null;
        boolean done = false;
        Iterator<OptionPanel> it = panels.values().iterator();
        while (it.hasNext() && !done) {
            OptionPanel panel = it.next();
            if (result == null)
                result = panel.applyOptions();
            else
                result.applyResult(panel.applyOptions());
            done = !result.isSuccessful();
        }
        
        if (result != null && result.isRestartRequired() && result.isSuccessful()) {
            FocusJOptionPane
                    .showMessageDialog(
                            this,
                            I18n.tr("One or more options will take effect the next time LimeWire is restarted."),
                            I18n.tr("Message"), JOptionPane.INFORMATION_MESSAGE);

        }
        return result;

    }
    
    private void createComponents(BarPainterFactory barPainterFactory) {
        setLayout(new BorderLayout());
        
        cardLayout = new CardLayout();
        cardPanel = new JPanel();
        cardPanel.setLayout(cardLayout);
        
        headerPanel = new JXPanel();
        headerPanel.setBackgroundPainter(barPainterFactory.createTopBarPainter());
                
        footerPanel = new JPanel();
        footerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        
        createFooter();
        createHeader();
        
        select(LIBRARY);
        
        add(headerPanel, BorderLayout.NORTH);
        add(cardPanel, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
        

    }
    
    private void createHeader() {
        headerPanel.setLayout(new MigLayout("insets 0 0 0 0, gap 0"));
        
        MoveDown down = new MoveDown();
        MoveUp up = new MoveUp();
        
        createButton(LIBRARY, libraryIcon, libraryOptionPanel, down, up);
        createButton(SEARCH, searchIcon, searchOptionPanel, down, up);
        createButton(TRANSFERS, transfersIcon, transferOptionPanel, down, up);
        createButton(MISC, miscIcon, miscOptionPanel, down, up);
        createButton(ADVANCED, advancedIcon, advancedOptionPanel, down, up);
    }
    
    private void createButton(String title, Icon icon,  Provider<? extends OptionPanel> provider, MoveDown down, MoveUp up) {
        FancyOptionTabButton button = new FancyOptionTabButton(new TabAction(icon, addOptionTab(title, this, provider)));
        
        button.getActionMap().put(MoveDown.KEY, down);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), MoveDown.KEY);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), MoveDown.KEY);
        
        button.getActionMap().put(MoveUp.KEY, up);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), MoveUp.KEY);
        button.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), MoveUp.KEY);
        
        headerPanel.add(button);
    }
    
    private class MoveDown extends AbstractAction {
        final static String KEY = "MOVE_DOWN";
        
        @Override
        public void actionPerformed(ActionEvent e) {
            next();
        }
    }
    
    private class MoveUp extends AbstractAction {
        final static String KEY = "MOVE_UP";
        
        @Override
        public void actionPerformed(ActionEvent e) {
            previous();
        }
    }
    
    private void createFooter() {
        footerPanel.setLayout(new MigLayout("insets 0 15 0 15, aligny 50%"));
        footerPanel.setBackground(GuiUtils.getMainFrame().getBackground());
        
        helpButton = new JButton(new HelpAction(application));
        
        okButton = new JButton(I18n.tr("OK"));
        okButton.addActionListener(new ApplyOptionAction(this));
        
        cancelButton = new JButton(I18n.tr("Cancel"));
        cancelButton.addActionListener(new CancelOptionAction(this));
        
        footerPanel.add(helpButton, "push");
        footerPanel.add(okButton, "split, tag ok");
        footerPanel.add(cancelButton, "tag cancel");
    }

    @Override
    public OptionTabItem addOptionTab(final String title, final OptionsTabNavigator navigator, final Provider<? extends OptionPanel> optionProvider) {
        return new OptionsTabItemImpl(title, navigator, optionProvider);
    }
    
    public void next() {
        for(int i = 0; i < list.size(); i++) {
            if(list.get(i).equals(selectedItem.getId())) {
                if(i == list.size() -1) {
                    select(list.get(0));
                } else
                    select(list.get(i+1));
                break;
            }
        }
    }
    
    public void previous() {
        for(int i = 0; i < list.size(); i++) {
            if(list.get(i).equals(selectedItem.getId())) {
                if(i == 0) {
                    select(list.get(list.size()-1));
                } else
                    select(list.get(i-1));
                break;
            }
        }
    }

    @Override
    public void select(String title) {
        if(selectedItem != null)
            ((OptionsTabItemImpl)selectedItem).fireSelected(false);
        selectedItem = cards.get(title);
        if(!panels.containsKey(title)) {
            createPanel(selectedItem.getId(), selectedItem.getOptionPanel());
        }
        ((OptionsTabItemImpl)selectedItem).fireSelected(true);
        cardLayout.show(cardPanel, title);
    }
    
    /**
     * Lazily loads and inits a subPanel in the OptionDialog.
     */
    private void createPanel(String id, OptionPanel panel) {
        panel.setBackground(GuiUtils.getMainFrame().getBackground());
        panels.put(selectedItem.getId(), panel);
        cardPanel.add(panels.get(selectedItem.getId()), selectedItem.getId());
        // Library is always shown so it will always be initialized prior to being shown
        // no need to do it now.
        
        if(!selectedItem.getId().equals(LIBRARY)) 
            panel.initOptions();
    }
    
    private class OptionsTabItemImpl implements OptionTabItem {

        private final List<TabItemListener> listeners = new CopyOnWriteArrayList<TabItemListener>();
        private final String name;
        private final OptionsTabNavigator navigator;
        private final Provider<? extends OptionPanel> provider;
        
        public OptionsTabItemImpl(String title, OptionsTabNavigator navigator, Provider<? extends OptionPanel> optionProvider) {
            this.name = title;
            this.navigator = navigator;
            this.provider = optionProvider;
            
            cards.put(title, this);
            list.add(title);
        }
        
        @Override
        public String getId() {
            return name;
        }

        @Override
        public boolean isSelected() {
            return false;
        }

        @Override
        public void select() {
            navigator.select(name);
        }
        
        public OptionPanel getOptionPanel() {
            OptionPanel panel = provider.get();
            panel.setOptionTabItem(this);
            
            return panel;
        }

        @Override
        public void addTabItemListener(TabItemListener listener) {
            listeners.add(listener);
        }

        @Override
        public void removeTabItemListener(TabItemListener listener) {
            listeners.remove(listener);
        }
        
        public void fireSelected(boolean selected) {
            for(TabItemListener listener : listeners) {
                listener.itemSelected(selected);
            }
        }
    }

    /**
     * Recalls init options on all created panels.
     */
    public void initOptions() {
        unsafeTypeOptionPanelStateManager.initOptions();
        
        for(OptionPanel optionPanel : panels.values()) {
            optionPanel.initOptions();
        }
    }
}
