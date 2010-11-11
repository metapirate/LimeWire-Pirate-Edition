package org.limewire.ui.swing.options;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.settings.FilterSettings;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.TextFieldClipboardControl;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.swing.DefaultEventTableModel;

import com.google.inject.Inject;

/**
 * Creates a table to manage which words will not show up in search results.
 */
public class FilterKeywordOptionPanel extends AbstractFilterOptionPanel {

    private JButton okButton;
    private JTextField keywordTextField;
    private JButton addKeywordButton;
    private FilterTable filterTable;
    private SpamManager spamManager;
    
    @Inject
    public FilterKeywordOptionPanel(SpamManager spamManager) {
        this.spamManager = spamManager;
        setLayout(new MigLayout("gapy 10"));
        
        keywordTextField = new JTextField(30);
        addKeywordButton = new JButton(I18n.tr("Add Keyword"));
        
        filterTable = new FilterTable(new DefaultEventTableModel<String>(eventList, new FilterTableFormat(I18n.tr("Keyword"))));
        okButton = new JButton(new OKDialogAction());
        
        TextFieldClipboardControl.install(keywordTextField);
        
        addKeywordButton.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = keywordTextField.getText();
                if(text == null || text.trim().length() == 0)
                    return;
                if(!eventList.contains(text)) {
                    eventList.add(text);
                }
                keywordTextField.setText("");
            }
        });
        
        add(new MultiLineLabel(I18n.tr("LimeWire will not show files with the following keywords in your search results"), 300), "span, wrap");
        add(keywordTextField, "gapright 10");
        add(addKeywordButton,"wrap");
        add(new JScrollPane(filterTable), "span 2, grow, wrap");
        
        add(okButton, "tag ok, skip 1, alignx right");
    }
    
    @Override
    ApplyOptionResult applyOptions() {
        String[] values = eventList.toArray(new String[eventList.size()]);
        FilterSettings.BANNED_WORDS.set(values);
        BackgroundExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                spamManager.adjustSpamFilters();
            }
        });
        return new ApplyOptionResult(false, true);
    }

    @Override
    boolean hasChanged() {
        List model = Arrays.asList(FilterSettings.BANNED_WORDS.get());
        String[] values = eventList.toArray(new String[eventList.size()]);
        
        return model.equals(new ArrayList<String>(Arrays.asList(values)));
    }

    @Override
    public void initOptions() {
        eventList.clear();
        String[] bannedWords = FilterSettings.BANNED_WORDS.get();
        eventList.addAll(new ArrayList<String>(Arrays.asList(bannedWords)));
    }
}
