package org.limewire.ui.swing.advanced;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.LoggerRepository;
import org.jdesktop.application.Resource;
import org.limewire.concurrent.ManagedThread;
import org.limewire.core.api.mojito.MojitoManager;
import org.limewire.core.api.support.LocalClientInfo;
import org.limewire.core.api.support.LocalClientInfoFactory;
import org.limewire.i18n.I18nMarker;
import org.limewire.service.ErrorService;
import org.limewire.ui.swing.components.NonNullJComboBox;
import org.limewire.ui.swing.components.NumericTextField;
import org.limewire.ui.swing.components.TextFieldClipboardControl;
import org.limewire.ui.swing.settings.ConsoleSettings;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.ThreadUtils;

import com.google.inject.Inject;

/**
 * A Console for log/any output.
 */
public class Console extends JPanel {

    @Resource
    private Color tableColor;
    
    @Resource
    private Font outputFont;
    
    /** Factory for LocalClientInfo object. */
    private volatile LocalClientInfoFactory localClientInfoFactory;
    
    /** Manager for Mojito DHT. */
    private final MojitoManager mojitoManager;
    
    private final int idealSize;
    private final int maxExcess;
    
    private JScrollPane scrollPane;

    private JTextArea output;

    private JButton apply;
    private JButton clear;
    private JButton save;
    
    private JComboBox loggerComboBox;

    private JComboBox levelComboBox;

    /**
     * Text field into which the delay time (in seconds) can be input.
     */
    private NumericTextField delayTxt;
    
    private boolean scroll = true;
    
    private boolean altCtrlDown = false;
    
    private List<ConsoleListener> listeners = null;
    
    /**
     * Delay time (in seconds) for updating the console text area.
     */
    private int delay;
    
    /**
     * Buffer for text to be appended to the console text 
     * area. Uses a StringBuffer because it is 
     * synchronized.
     */
    private StringBuffer delayBuf;
    
    /**
     * The timer we use to schedule updates to the console
     * text area.
     */
    private Timer delayTimer;

    /** Appender for the log. */
    private Appender logAppender;

    /**
     * Constructs the Console for displaying messages.
     */
    @Inject
    public Console(MojitoManager mojitoManager, 
            LocalClientInfoFactory localClientInfoFactory) {
        this.mojitoManager = mojitoManager;
        this.localClientInfoFactory = localClientInfoFactory;
        
        idealSize = ConsoleSettings.CONSOLE_IDEAL_SIZE.getValue();
        maxExcess = ConsoleSettings.CONSOLE_MAX_EXCESS.getValue();
        
        output = new JTextArea();
        output.setEditable(false);
        TextFieldClipboardControl.install(output);

        scrollPane = new JScrollPane(output);
        scrollPane.setBorder(BorderFactory.createMatteBorder(0,0,1,0, Color.black));
        scrollPane.getVerticalScrollBar().addAdjustmentListener(
                new AdjustmentListener() {
                    public void adjustmentValueChanged(AdjustmentEvent e) {
                        if (e.getValueIsAdjusting()) {
                            scroll = false;
                        } else {
                            scroll = true;
                        }
                    }
                });

        loggerComboBox = new NonNullJComboBox(new LoggerComboBoxModel());
        levelComboBox = new NonNullJComboBox(new LevelComboBoxModel());
        
        loggerComboBox.setAutoscrolls(true);
        loggerComboBox.setMaximumRowCount(20);
        loggerComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent evt) {
                selectLoggerLevel();
            }
        });
        
        loggerComboBox.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent evt) {
                refreshLoggers();
            }
            
            public void popupMenuCanceled(PopupMenuEvent evt) {}
            public void popupMenuWillBecomeInvisible(PopupMenuEvent evt) {}
        });
        
        levelComboBox.setAutoscrolls(true);
        
        apply = new JButton(I18n.tr("Apply"));
        apply.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                applyLevel();
            }
        });

        clear = new JButton(I18n.tr("Clear"));
        clear.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                clear();
            }
        });
        
        save = new JButton(I18n.tr("Save"));
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                save();
            }
        });
        
        // default the delay time to zero (ie, live 
        // updates).
        JLabel delayLabel = new JLabel(I18n.tr("Delay: "));
        delayLabel.setMinimumSize(new Dimension(50, 23));
        delayLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        delay = 0;
        delayBuf = new StringBuffer();
        delayTimer = null;
        
        // Create text field for numeric value.
        delayTxt = new NumericTextField(3);
        delayTxt.setValue(0);
        delayTxt.setHorizontalAlignment(JTextField.RIGHT);
        delayTxt.setMinimumSize(new Dimension(50, 23));
        delayTxt.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent evt) {
               setDelay();
           }
        });
        
        // Install clipboard actions on text field.
        TextFieldClipboardControl.install(delayTxt);
        
        // Developers can press and hold Alt+Ctrl while clicking
        // on Save to get the current stack traces.
        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                altCtrlDown = e.isAltDown() && e.isControlDown();
            }

            // Note: if the user is holding Alt+Ctrl while
            // switching to a different Tab this will never
            // get called! We need a second Listener!
            @Override
            public void keyReleased(KeyEvent e) {
                altCtrlDown = false;
            }
        };
        
        // Install the listener on all components
        addKeyListener(keyListener);
        scrollPane.addKeyListener(keyListener);
        output.addKeyListener(keyListener);
        loggerComboBox.addKeyListener(keyListener);
        levelComboBox.addKeyListener(keyListener);
        apply.addKeyListener(keyListener);
        clear.addKeyListener(keyListener);
        save.addKeyListener(keyListener);
        
        // Reset the flag if this Tab gets invisible
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                altCtrlDown = false;
            }
        });
        
        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, scrollPane);

        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        controlsPanel.add(loggerComboBox, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 5, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        controlsPanel.add(levelComboBox, gbc);

        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        controlsPanel.add(delayLabel, gbc);
        
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        controlsPanel.add(delayTxt, gbc);
        
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 5, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        controlsPanel.add(apply, gbc);
        
        gbc.gridx = 5;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        controlsPanel.add(clear, gbc);
        
        gbc.gridx = 6;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        controlsPanel.add(save, gbc);
        
        if (ConsoleSettings.SHOW_INPUT_FIELD.getValue()) {
            listeners = new ArrayList<ConsoleListener>();
            
            JTextField inputField = new JTextField();
            inputField.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    JTextField textField = (JTextField)evt.getSource();
                    String command = textField.getText().trim();
                    if (command.length() == 0) {
                        return;
                    }
                    
                    textField.setText("");
                    
                    try {
                        ConsoleWriter writer = new ConsoleWriter();
                        PrintWriter out = new PrintWriter(writer);
                        for(ConsoleListener l : listeners) {
                            if (l.handleCommand(command, out)) {
                                return;
                            }
                        }
                        
                        appendText("Unknown command: " + command + "\n");
                    } catch (IOException err) {
                        appendText(err.getMessage());
                    }
                }
            });
            
            addConsoleListener(new ConsoleListener() {
                public boolean handleCommand(final String command, final PrintWriter out) throws IOException {
                    Runnable task = new Runnable() {
                        public void run() {
                            try {
                                // Invoke method to pass command to DHT.
                                Console.this.mojitoManager.handle(command, out);
                                
                            } catch (SecurityException e) {
                                e.printStackTrace(out);
                            } catch (IllegalArgumentException e) {
                                e.printStackTrace(out);
                            } finally {
                                out.flush();
                            }
                        }
                    };
                    
                    new ManagedThread(task).start();
                    return true;
                }
            });
            
            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 5;
            gbc.gridheight = 1;
            gbc.weightx = 0;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            controlsPanel.add(inputField, gbc);
        }
        
        add(BorderLayout.SOUTH, controlsPanel);

        // Update resource values.
        GuiUtils.assignResources(this);
        
        scrollPane.getViewport().setBackground(this.tableColor);
        output.setFont(this.outputFont);

        refreshLoggers();
    }
    
    /**
     * Adds the specified listener to the list that is notified when a command
     * is entered in the input field.
     */
    public void addConsoleListener(ConsoleListener l) {
        if (listeners != null && l != null) {
            listeners.add(l);
        }
    }
    
    /**
     * Adds console appender to logger. 
     */
    public void attachLogs() {
        logAppender = new WriterAppender(new PatternLayout(
                ConsoleSettings.CONSOLE_PATTERN_LAYOUT.get()), new ConsoleWriter());
        LogManager.getRootLogger().addAppender(logAppender);
    }
    
    /**
     * Removes appender from logger.
     */
    public void removeLogs() {
        LogManager.getRootLogger().removeAppender(logAppender);
    }
    
    /**
     * Rebuilds the Logger ComboBox.
     */
    private void refreshLoggers() {
        LoggerRepository repository = LogManager.getLoggerRepository();
        Enumeration currentLoggers = repository.getCurrentLoggers();
        
        LoggerComboBoxModel loggerModel = (LoggerComboBoxModel) loggerComboBox.getModel();
        int loggerIndex = loggerComboBox.getSelectedIndex();
        LoggerNode currentLogger = (loggerIndex >= 0) ? loggerModel.getLogger(loggerIndex) : null;
        
        /*
         * Step 1: Create a Tree of Packages and Classes
         */
        List<PackageNode> pkgList = new ArrayList<PackageNode>();
        Map<String, PackageNode> pkgMap = new HashMap<String, PackageNode>();
        while (currentLoggers.hasMoreElements()) {
            Logger lggr = (Logger)currentLoggers.nextElement();
            
            String pkg = PackageNode.getPackage(lggr);
            PackageNode node = pkgMap.get(pkg);
            if (node == null) {
                node = new PackageNode(pkg);
                pkgMap.put(pkg, node);
                pkgList.add(node);
            }
            node.add(lggr);
        }
        
        /*
         * Step 2: Sort the Packages by name
         */
        Collections.sort(pkgList, new Comparator<PackageNode>() {
            public int compare(PackageNode o1, PackageNode o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        
        /*
         * Step 3: Turn the Tree into a flat List of
         * 
         * Package
         *     Class
         *     Class
         * Package
         *     Class
         *     ...
         */
        loggerIndex = -1;
        List<LoggerNode> nodes = new ArrayList<LoggerNode>();
        for(PackageNode pkgNode : pkgList) {
            pkgNode.sort();
            nodes.add(pkgNode);            
            if (loggerIndex == -1 && pkgNode.equals(currentLogger)) {
                loggerIndex = nodes.size()-1;
            }
            
            for (ClassNode classNode : pkgNode.getNodes()) {
                nodes.add(classNode);
                
                if (loggerIndex == -1 
                        && classNode.equals(currentLogger)) {
                    loggerIndex = nodes.size()-1;
                }
            }
        }
        
        loggerModel.refreshLoggers(nodes);
        
        boolean empty = nodes.isEmpty();
        loggerComboBox.setEnabled(!empty);
        levelComboBox.setEnabled(!empty);
        apply.setEnabled(!empty);
        
        if (!empty) {
            loggerComboBox.setSelectedIndex(loggerIndex >= 0 ? loggerIndex : 0);
            selectLoggerLevel();
        }
    }

    /**
     * Selects the Level of the currently selected Logger.
     */
    private void selectLoggerLevel() {
        LoggerComboBoxModel loggerModel = (LoggerComboBoxModel) loggerComboBox.getModel();
        LevelComboBoxModel levelModel = (LevelComboBoxModel) levelComboBox.getModel();
        
        int loggerIndex = loggerComboBox.getSelectedIndex();
        if (loggerIndex < 0)
            return;
        
        Level level = getLevel(loggerModel.getLogger(loggerIndex));

        levelModel.setSelectedItem(level);
    }
    
    /**
     * Applies the currently selected logging level.
     */
    private void applyLevel() {
        // because the user might not hit enter after
        // typing a delay, and then subsequently hit apply,
        // also set the delay here.
        //
        setDelay();
        
        LoggerComboBoxModel loggerModel = (LoggerComboBoxModel) loggerComboBox.getModel();
        LevelComboBoxModel levelModel = (LevelComboBoxModel) levelComboBox.getModel();

        int loggerIndex = loggerComboBox.getSelectedIndex();
        if (loggerIndex < 0)
            return;
        
        LoggerNode logger = loggerModel.getLogger(loggerIndex);
        Level currentLevel = getLevel(logger);

        int levelIndex = levelComboBox.getSelectedIndex();
        Level newLevel = (levelIndex > 0) ? levelModel.getLevel(levelIndex) : null;

        if (!currentLevel.equals(newLevel)) {
            logger.setLevel(newLevel);
            loggerComboBox.setSelectedIndex(loggerIndex); // update the ComboxBox  (the text)
            loggerModel.updateIndex(loggerIndex);
        }
    }

    /**
     * Appends text to the console.
     * 
     * @param text the text to be appended
     */
    public void appendText(final String text) {
        if (!output.isEnabled()) {
            return;
        }
        
        // if there is a non-zero delay value, then append
        // the text to the buffer instead of immediately
        // scheduling it to be added to the console text
        // area.
        //
        if (0 != delay) {
            delayBuf.append(text);
            return;
        }
        
        invokeLaterConsoleAppend(text);
    }

    /**
     * Clears the console.
     */
    public void clear() {
        output.setText(null);
    }

    /**
     * Saves the current Console output and the stack traces of
     * all active Threads if available.
     */
    public void save() {
        try {
            output.setEnabled(altCtrlDown);
            
            String log = output.getText().trim();
            String traces = ThreadUtils.getAllStackTraces();
            
            if (log.length() == 0 
                    && traces.length() == 0) {
                return;
            }
            
            if (altCtrlDown) {
                StringBuilder buffer = new StringBuilder();
                buffer.append("-- BEGIN STACK TRACES --\n");
                buffer.append(traces.length() > 0 ? traces : "NONE");
                buffer.append("\n-- END STACK TRACES --\n");
                appendText(buffer.toString());
            } else {
                StringBuilder buffer = new StringBuilder();
                buffer.append(new Date()).append("\n\n");
                
                Exception e = new Exception() {
                    @Override
                    public void printStackTrace(PrintWriter out) {
                        /* PRINT NOTHING */
                    }
                };
                
                LocalClientInfo info = this.localClientInfoFactory.createLocalClientInfo(
                    e, Thread.currentThread().getName(), "Console Log", false);
                buffer.append(info.toBugReport());
                
                buffer.append("-- BEGIN STACK TRACES --\n");
                buffer.append(traces.length() > 0 ? traces : "NONE");
                buffer.append("\n-- END STACK TRACES --\n");
            
                buffer.append("\n-- BEGIN LOG --\n");
                buffer.append(log.length() > 0 ? log : "NONE");
                buffer.append("\n-- END LOG --\n");
                
                File file = FileChooser.getSaveAsFile(this,
                    I18nMarker.marktr("Save As"), 
                    new File(FileChooser.getLastInputDirectory(), "limewire-log.txt"));
                if (file == null) {
                    return;
                }
                
                BufferedWriter out = new BufferedWriter(new FileWriter(file));
                out.write(buffer.toString());
                out.close();
            }
        } catch (IOException err) {
            ErrorService.error(err);
        } finally {
            output.setEnabled(true);
        }
    }
    
    /**
     * Appends consoleTxt to the console text area in the
     * swing thread.
     * 
     * @param consoleTxt string to append to the console text area
     */
    public void invokeLaterConsoleAppend (final String consoleTxt) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                _appendText(consoleTxt);
            }
        });
    }
    
    /**
     * Appends the specified text to the console text area.  This should be
     * called from within the Swing thread.
     */
    public void _appendText (String consoleTxt) {
        output.append(consoleTxt);

        int excess = output.getDocument().getLength() - idealSize;
        if (excess >= maxExcess) {
            output.replaceRange("", 0, excess);
        }
        if (scroll)
            output.setCaretPosition(output.getText().length());
    }
    
    /**
     * Called when the value of the delay text field is 
     * changed. Parse the value as an integer, understood
     * to be seconds, and set the update delay to that
     * value.
     */
    public void setDelay() {
        delay = delayTxt.getValue(0);
        
        // if the delay is set to zero, flush the buffer to
        // the console text area, since it is possible that
        // some text was dumped in there, and we don't want
        // to lose it.
        //
        if (0 == delay) {
            synchronized (delayBuf) {
                if (0 != delayBuf.length()) {
                    String strbuf = delayBuf.toString();
                    delayBuf.delete(0, strbuf.length());
                    invokeLaterConsoleAppend(strbuf);
                }
            }
            
            if (delayTimer != null) {
                delayTimer.stop();
                delayTimer = null;
            }
        }
        // a non-zero delay value. schedule a timer to 
        // update the console text area every 'delay'
        // seconds.
        //
        else {
            if (null == delayTimer) {
                delayTimer = new Timer(delay*1000, new ActionListener() {
                    public void actionPerformed (ActionEvent evt) {
                        if (0 == delayBuf.length())
                            return;
                        
                        synchronized(delayBuf) {
                            String strbuf = delayBuf.toString();
                            delayBuf.delete(0, strbuf.length());
                            _appendText(strbuf);
                        }
                    }
                });
                delayTimer.start();
            }
            else
                delayTimer.setDelay(delay * 1000);
        }
    }
    
    /**
     * Returns Level.OFF instead of null if logging is turned off.
     */
    private static final Level getLevel(LoggerNode logger) {
        Level level = logger.getLevel();
        if (level == null)
            level = Level.OFF;
        return level;
    }
    
    private final class ConsoleWriter extends Writer {

        private StringBuilder buffer = new StringBuilder();

        @Override
        public void write(char[] cbuf, int off, int len) {
            buffer.append(cbuf, off, len);
        }

        @Override
        public void close() {
            buffer = null;
        }

        @Override
        public void flush() {
            Console.this.appendText(buffer.toString());
            buffer.setLength(0);
        }
    }
    
    /**
     * Logger ComboBox model.
     */
    private static class LoggerComboBoxModel extends DefaultComboBoxModel {
        
        private static final String SPACER = "    ";
        
        private List<LoggerNode> nodes = Collections.emptyList();
        
        private void updateIndex(int index) {
            fireContentsChanged(this, index, index);
        }
        
        private void refreshLoggers(List<LoggerNode> nodes) {
            this.nodes = nodes;
            fireContentsChanged(this, 0, nodes.size());
        }

        @Override
        public int getSize() {
            return nodes.size();
        }

        private LoggerNode getLogger(int index) {
            return nodes.get(index);
        }

        @Override
        public Object getElementAt(int index) {
            LoggerNode logger = getLogger(index);
            Level level = getLevel(logger);
            
            if (level.equals(Level.OFF)) {
                if (logger.isLeaf()) {
                    return SPACER + logger.getName();
                } else {
                    return logger.getName();
                }
            } else {
                if (logger.isLeaf()) {
                    return SPACER + logger.getName() + " [" + level + "]";
                } else {
                    return logger.getName();
                }
            }
        }
    }

    /**
     * Logging level ComboBox model.
     */
    private static class LevelComboBoxModel extends DefaultComboBoxModel {

        private final Level[] levels = new Level[] { 
                Level.OFF, 
                Level.ALL,
                Level.DEBUG, 
                Level.ERROR, 
                Level.FATAL, 
                Level.INFO, 
                Level.WARN 
        };

        @Override
        public int getSize() {
            return levels.length;
        }

        private Level getLevel(int index) {
            return levels[index];
        }

        @Override
        public Object getElementAt(int index) {
            return getLevel(index).toString();
        }
    }
    
    /**
     * A interface to build a very simple Tree of 
     * Packages and Classes.
     */
    private interface LoggerNode {
        boolean isLeaf();
        Level getLevel();
        void setLevel(Level level);
        String getName();
    }
    
    private static class PackageNode implements LoggerNode {
        
        private String pkg;
        private List<ClassNode> classNodes = new ArrayList<ClassNode>();
        
        private PackageNode(String pkg) {
            this.pkg = pkg;
        }
        
        public void add(Logger logger) {
            classNodes.add(new ClassNode(logger));
        }
        
        public Level getLevel() {
            return Level.OFF;
        }
        
        public void setLevel(Level level) {
            for(int i = classNodes.size()-1; i >= 0; i--) {
                classNodes.get(i).setLevel(level);
            }
        }
        
        public boolean isLeaf() {
            return false;
        }
        
        public void sort() {
            Collections.sort(classNodes, new Comparator<ClassNode>() {
                public int compare(ClassNode o1, ClassNode o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
        }
        
        public List<ClassNode> getNodes() {
            return classNodes;
        }
        
        @Override
        public int hashCode() {
            return pkg.hashCode();
        }
        
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PackageNode)) {
                return false;
            }
            return pkg.equals(((PackageNode)o).pkg);
        }
        
        public String getName() {
            return pkg;
        }
        
        @Override
        public String toString() {
            return getName();
        }
        
        private static String getPackage(Logger logger) {
            String name = logger.getName();
            int i = name.lastIndexOf('.');
            return (i != -1) ? name.substring(0, i) + ".*" : name + ".*";
        }
    }
    
    private static class ClassNode implements LoggerNode {
        private Logger logger;
        
        private ClassNode(Logger logger) {
            this.logger = logger;
        }
        
        public Level getLevel() {
            return logger.getLevel();
        }
        
        public void setLevel(Level level) {
            logger.setLevel(level);
        }
        
        public boolean isLeaf() {
            return true;
        }
        
        public String getName() {
            return logger.getName();
        }
        
        @Override
        public int hashCode() {
            return getName().hashCode();
        }
        
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ClassNode)) {
                return false;
            }
            return getName().equals(((ClassNode)o).getName());
        }
        
        @Override
        public String toString() {
            return getName();
        }
    }
    
    /**
     * Defines a listener to handle commands entered in the input field.
     */
    public static interface ConsoleListener {
        public boolean handleCommand(String command, PrintWriter out) throws IOException;
    }
}
