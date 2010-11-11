package org.limewire.ui.support;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.limewire.core.api.support.LocalClientInfo;
import org.limewire.core.api.support.LocalClientInfoFactory;
import org.limewire.core.impl.support.LocalClientInfoImpl;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.MultiLineLabel;

import com.google.inject.Inject;


/**
 * A bare-bones bug manager, for fatal errors.
 */
public final class FatalBugManager {
    
    @Inject private static volatile LocalClientInfoFactory localClientInfoFactory;    
    
    private FatalBugManager() {}
    
    /**
     * Handles a fatal bug.
     */
    public static void handleFatalBug(Throwable bug) {
        if( bug instanceof ThreadDeath ) // must rethrow.
            throw (ThreadDeath)bug;
        
        bug.printStackTrace();
        
        final LocalClientInfo info;
        if(localClientInfoFactory != null) {
            info = localClientInfoFactory.createLocalClientInfo(bug, Thread.currentThread().getName(), null, true);
        } else {
            info = new LocalClientInfoImpl(bug, Thread.currentThread().getName(), null, true, new NoSessionInfo());
        }
        
        if(!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        reviewBug(info);                    
                    }
                });
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else {
            reviewBug(info);
        }
    }
    
    /**
     * Reviews the bug.
     */
    public static void reviewBug(final LocalClientInfo info) {
        final JDialog dialog = new LimeJDialog();
        dialog.setSize(new Dimension(300, 100));
        dialog.setTitle("LimeWire Couldn't Start :-(");
        
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(1);
            }
        });

        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        mainPanel.setLayout(new GridBagLayout());
        
        String msg = "Sorry, LimeWire ran into a problem while it was starting up. This is usually caused by a problem during installation. Try downloading and installing LimeWire again. If that doesn't fix it, visit www.limewire.com and click on 'Support'. You can also send the bug to us so we can try and fix it.";
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

        final String defaultDesc = "Want to help? Write in some extra info to help us find what's causing LimeWire to crash.";
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

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets = new Insets(10, 0, 0, 0);
        bugSpecificsPanel.add(userCommentsScrollPane, constraints);

        final HyperlinkButton showHideBugLink = new HyperlinkButton("Show Bug");
        showHideBugLink.addActionListener(new AbstractAction() {
            boolean panelVisible = false;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (panelVisible) {
                    bugSpecificsPanel.setVisible(false);
                    showHideBugLink.setText("Show Bug");
                    dialog.pack();
                } else {
                    bugSpecificsPanel.setVisible(true);
                    showHideBugLink.setText("Hide Bug");
                }
                dialog.pack();
                panelVisible = !panelVisible;
            }
        });

        final JButton sendButton = new JButton("Send Bug");
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String userComments = userCommentsTextArea.getText();
                if(!userComments.equals(defaultDesc))
                    info.addUserComments(userComments);
                sendButton.setEnabled(false);
                sendButton.setText("Sending...");
                new Thread("Fatal Bug Sending Thread") {
                    @Override
                    public void run() {
                        try {
                            sendToServlet(info);
                        } finally {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    dialog.dispose();
                                    System.exit(1);
                                }
                            });
                        }
                    }
                }.start();
            }
        });

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        mainPanel.add(labelPanel, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.insets = new Insets(10, 0, 0, 0);
        constraints.anchor = GridBagConstraints.LINE_START;
        mainPanel.add(showHideBugLink, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.insets = new Insets(30, 0, 6, 0);
        mainPanel.add(bugSpecificsPanel, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 4;
        mainPanel.add(sendButton, constraints);
        
        mainPanel.validate();
        dialog.getContentPane().add(mainPanel);
        dialog.pack();
        sendButton.requestFocusInWindow();
        dialog.setLocationRelativeTo(null);
        
        // Get rid of all other windows.
        for(Window window : Window.getWindows()) {
            if(window == dialog) {
                continue;
            }
            window.setVisible(false);
            window.dispose();
        }
        
        dialog.setVisible(true);
        dialog.toFront();
    }
    
    /**
     * Sends a bug to the servlet & then exits.
     */
    private static void sendToServlet(LocalClientInfo info) {
        new ServletAccessor().getRemoteBugInfo(info);
    }
}