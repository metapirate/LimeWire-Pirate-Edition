package org.limewire.ui.support;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.limewire.core.api.support.LocalClientInfo;
import org.limewire.core.api.support.LocalClientInfoFactory;
import org.limewire.core.impl.support.LocalClientInfoImpl;
import org.limewire.io.IOUtils;
import org.limewire.ui.swing.action.UrlAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.settings.BugSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;
import org.limewire.util.NotImplementedException;

import com.google.inject.Inject;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Interface for reporting bugs.
 * This can do any of the following:
 * <ul>
 *  <li>Allow the user to copy the bug & email it.</li>
 *  <li>Suppress the bug entirely.</li>
 *  </ul>
 */
public final class BugManager {

    @Inject private static volatile LocalClientInfoFactory localClientInfoFactory;
    
    /**
     * The width of the internal error dialog box.
     */
    private final int DIALOG_BOX_WIDTH = 300;
    /**
     * The height of the internal error dialog box.
     */
    private final int DIALOG_BOX_HEIGHT = 100;
    
    /**
     * A mapping of stack traces (String) to next allowed time (long)
     * that the bug can be reported.
     *
     * Used only if reporting the bug to the servlet.
     */
    private final Map<String, Long> BUG_TIMES = Collections.synchronizedMap(new HashMap<String, Long>());
    
    /**
     * A lock to be used when writing to the logfile, if the log is to be
     * recorded locally.
     */
    private final Object WRITE_LOCK = new Object();
    
    /**
     * A separator between bug reports.
     */
    // saved to local file system only, so use platform encoding
    private final byte[] SEPARATOR = "-----------------\n".getBytes();
    
    /**
     * The next time we're allowed to send any bug.
     *
     * Used only if reporting the bug to the servlet.
     */
    private volatile long _nextAllowedTime = 0;
    
    /**
     * The number of bug dialogs currently showing.
     */
    private volatile int _dialogsShowing = 0;
    
    /**
     * The maximum number of dialogs we're allowed to show.
     */
    private final int MAX_DIALOGS = 3;
    
    /**
     * Whether or not we have dirty data after the last save.
     */
    private boolean dirty = false;
    
    public BugManager() {
        loadOldBugs();
    }
    
    /**
     * Shuts down the BugManager.
     */
    public void shutdown() {
        writeBugsToDisk();
    }
    
    /**
     * Handles a single bug report.
     * If bug is a ThreadDeath, rethrows it.
     * If the user wants to ignore all bugs, this effectively does nothing.
     * The the server told us to stop reporting this (or any) bug(s) for
     * awhile, this effectively does nothing.
     * Otherwise, it will either send the bug directly to the servlet
     * or ask the user to review it before sending.
     */
    public void handleBug(Throwable bug, String threadName, String detail) {
        if( bug instanceof ThreadDeath ) // must rethrow.
            throw (ThreadDeath)bug;
        
        // Try to dispatch the bug to a friendly handler.
        if (bug instanceof IOException
                && IOUtils.handleException((IOException) bug, IOUtils.ErrorType.GENERIC)) {
            return; // handled already.
        }   
        
        bug.printStackTrace();
        
        // Build the LocalClientInfo out of the info ...
        LocalClientInfo info;
        if(localClientInfoFactory == null) {
            info = new LocalClientInfoImpl(bug, threadName, detail, false, new NoSessionInfo());
        } else {
            info = localClientInfoFactory.createLocalClientInfo(bug, threadName, detail, false);
        }

        if( BugSettings.LOG_BUGS_LOCALLY.getValue() ) {
            logBugLocally(info);
        }

        // never ignore bugs or auto-send when developing.
        if(!LimeWireUtils.isTestingVersion()) {
            if(!BugSettings.REPORT_BUGS.getValue() ) {
                return; // ignore.
            }
            
            // If we have already sent information about this bug, leave.
            if( !shouldInform(info) ) {
               return; // ignore.
            }
        }
        
        if (_dialogsShowing < MAX_DIALOGS ) {
            reviewBug(info, bug instanceof NotImplementedException);
        }
    }
    
    /**
     * Logs the bug report to a local file.
     * If the file reaches a certain size it is erased.
     */
    private void logBugLocally(LocalClientInfo info) {
        File f = BugSettings.BUG_LOG_FILE.get();
        FileUtils.setWriteable(f);
        OutputStream os = null;
        try {
            synchronized(WRITE_LOCK) {
                if ( f.length() > BugSettings.MAX_BUGFILE_SIZE.getValue() )
                    f.delete();
                os = new BufferedOutputStream(
                        new FileOutputStream(f.getPath(), true));
                // saved to local file system, use default encoding
                os.write((new Date().toString() + "\n").getBytes());
                os.write(info.toBugReport().getBytes());
                os.write(SEPARATOR);
                os.flush();
            }
        } catch(IOException ignored) {
        } finally {
            IOUtils.close(os);
        }
    }
    
    /**
     * Loads bugs from disk.
     */
    private void loadOldBugs() {
        ObjectInputStream in = null;
        File f = BugSettings.BUG_INFO_FILE.get();
        try {
            // Purposely not a ConverterObjectInputStream --
            // we never want to read old version's bug info.
            in = new ObjectInputStream(
                    new BufferedInputStream(
                        new FileInputStream(f)));
            String version = (String)in.readObject();
            long nextTime = in.readLong();
            if( version.equals(LimeWireUtils.getLimeWireVersion()) ) {
                Map<String, Long> bugs = GenericsUtils.scanForMap(
                        in.readObject(), String.class, Long.class,
                        GenericsUtils.ScanMode.REMOVE);
                // Only load them if we're continuing to use the same version
                // This way bugs for newer versions get reported.
                // We could check to make sure this is a newer version,
                // but it's not all that necessary.
                _nextAllowedTime = nextTime;
                long now = System.currentTimeMillis();
                for(Map.Entry<String, Long> entry : bugs.entrySet()) {
                    // Only insert those whose times haven't expired.
                    Long allowed = entry.getValue();
                    if( allowed != null && now < allowed.longValue() )
                        BUG_TIMES.put(entry.getKey(), allowed);
                }
            } else {
                // Otherwise, we're using a different version than the last time.
                // Unset 'discard all bugs'.
                if(! BugSettings.REPORT_BUGS.getValue()) {
                    BugSettings.REPORT_BUGS.setValue(true);
                    BugSettings.SHOW_BUGS.setValue(true);
                }
            }
        } catch(Throwable t) {
            // ignore errors from disk.
        } finally {
            IOUtils.close(in);
        }
                    
    }
    
    /**
     * Write bugs out to disk.
     */
    private void writeBugsToDisk() {
        synchronized(WRITE_LOCK) {
            if(!dirty)
                return;
            
            ObjectOutputStream out = null;
            try {
                File f = BugSettings.BUG_INFO_FILE.get();
                out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
                String version = LimeWireUtils.getLimeWireVersion();
                out.writeObject(version);
                out.writeLong(_nextAllowedTime);
                out.writeObject(BUG_TIMES);
                out.flush();
            } catch(Exception e) {
                // oh well, no biggie if we couldn't write to disk.
            } finally {
                IOUtils.close(out);
            }
            
            dirty = false;
        }
    }
    
    /**
     * Determines if the bug has already been reported enough.
     * If it has, this returns false.  Otherwise (if the bug should
     * be reported) this returns true.
     */
    private boolean shouldInform(LocalClientInfo info) {
        long now = System.currentTimeMillis();
        
        // If we aren't allowed to report a bug, exit.
        if( now < _nextAllowedTime )
            return false;

        Long allowed = BUG_TIMES.get(info.getParsedBug());
        return allowed == null || now >= allowed.longValue();
    }
    
    /**
     * Displays a message to the user informing them an internal error
     * has occurred.  The user is asked to click 'send' to send the bug
     * report to the servlet and has the option to review the bug
     * before it is sent.
     */
    private void reviewBug(final LocalClientInfo info, boolean notImplemented) {
        _dialogsShowing++;
        
        String title = notImplemented ? I18n.tr("Oops!") : I18n.tr("A problem occurred...");
        final JDialog DIALOG = new LimeJDialog(GuiUtils.getMainFrame(), title, ModalityType.APPLICATION_MODAL);
        final Dimension DIALOG_DIMENSION = new Dimension(DIALOG_BOX_WIDTH, DIALOG_BOX_HEIGHT);
        DIALOG.setSize(DIALOG_DIMENSION);
        DIALOG.setResizable(false);
        DIALOG.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);


        // make sure number of current dialogs gets decremented when user closes it 
        DIALOG.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                _dialogsShowing--;
            }
        });

        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        mainPanel.setLayout(new GridBagLayout());

        String msg;
        if(notImplemented) {
            msg = I18n.tr("Oops!  You did something we haven't written yet.  Sorry about that.  LimeWire's still going to run just fine, so don't worry.  If you want, you can click \'Show Bug\' to look at the information about the error.");
        } else {
            msg = I18n.tr("LimeWire has encountered an internal error. It is possible for LimeWire to recover and continue running normally. To continue using LimeWire, close this window. If desired, you can click \'Show Bug\' to look at the information about the error.");
        }

        MultiLineLabel label = new MultiLineLabel(msg, 500);
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;        
        labelPanel.add(label, constraints); 

        final JPanel bugSpecificsPanel = new JPanel();
        bugSpecificsPanel.setLayout(new GridBagLayout());
        bugSpecificsPanel.setVisible(false);

        // the component with the bug stacktrace
        JTextArea showBug = new JTextArea(info.toBugReport());
        showBug.setColumns(50);
        showBug.setEditable(false);
        showBug.setCaretPosition(0);
        showBug.setLineWrap(true);
        showBug.setWrapStyleWord(true);
        JScrollPane showBugScroller = new JScrollPane(showBug);
        showBugScroller.setBorder(BorderFactory.createEtchedBorder());
        showBugScroller.setPreferredSize( new Dimension(500, 200) );

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        bugSpecificsPanel.add(showBugScroller, constraints);

        final String defaultDesc = I18n.tr("Tell us what you were doing before LimeWire ran into this problem.");
        final JTextArea userCommentsTextArea = new JTextArea(defaultDesc);
        userCommentsTextArea.setLineWrap(true);
        userCommentsTextArea.setWrapStyleWord(true);

        // When the user clicks anywhere in the text field, it highlights the whole text
        // so that user could just type over it without having to delete it manually
        userCommentsTextArea.addFocusListener(new FocusAdapter() {
             @Override
            public void focusGained(FocusEvent e) {
                 if(userCommentsTextArea.getText().equals(defaultDesc)) {
                    userCommentsTextArea.selectAll();
                 }
             }
        });
        JScrollPane userCommentsScrollPane = new JScrollPane(userCommentsTextArea);
        userCommentsScrollPane.setBorder(BorderFactory.createEtchedBorder());
        userCommentsScrollPane.setPreferredSize( new Dimension(500, 60) );

        final HyperlinkButton showHideBugLink = new HyperlinkButton(I18n.tr("Show Bug"));
        showHideBugLink.addActionListener(new AbstractAction() {
            boolean panelVisible = false;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (panelVisible) {
                    bugSpecificsPanel.setVisible(false);
                    showHideBugLink.setText(I18n.tr("Show Bug"));
                    DIALOG.pack();
                } else {
                    bugSpecificsPanel.setVisible(true);
                    showHideBugLink.setText(I18n.tr("Hide Bug"));
                }
                DIALOG.pack();
                panelVisible = !panelVisible;
            }
        });

        HyperlinkButton helpLink =
            new HyperlinkButton(I18n.tr("Ask for help on the forums"));
        String url = "http://www.gnutellaforums.com/";
        helpLink.addActionListener(new UrlAction(url));

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        mainPanel.add(labelPanel, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets = new Insets(10, 0, 0, 0);
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(showHideBugLink, constraints);
        
        constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 1;
        constraints.insets = new Insets(10, 0, 0, 0);
        constraints.anchor = GridBagConstraints.LINE_END;
        mainPanel.add(helpLink, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(30, 0, 6, 0);
        mainPanel.add(bugSpecificsPanel, constraints);
        
        mainPanel.validate();
        DIALOG.getContentPane().add(mainPanel);
        DIALOG.pack();
        DIALOG.setLocationRelativeTo(GuiUtils.getMainFrame());

        try {
            DIALOG.setVisible(true);
        } catch(InternalError ie) {
            //happens occasionally, ignore.
        } catch(ArrayIndexOutOfBoundsException npe) {
            //happens occasionally, ignore.
        }
    }   
}