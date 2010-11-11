package org.limewire.ui.swing.friends.chat;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.jdesktop.application.Resource;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.action.CopyAction;
import org.limewire.ui.swing.action.CutAction;
import org.limewire.ui.swing.action.DeleteAction;
import org.limewire.ui.swing.action.PasteAction;
import org.limewire.ui.swing.action.PopupUtil;
import org.limewire.ui.swing.action.SelectAllAction;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.collection.Periodic;
import org.limewire.friend.api.ChatState;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.MessageWriter;

class ResizingInputPanel extends JPanel implements Displayable, Disposable {
    private static final Log LOG = LogFactory.getLog(ResizingInputPanel.class);
    private static final int DELAY_IN_MILLIS_FOR_CHAT_PAUSED = 3000;
    private final JTextArea text;
    private final MessageWriter writer;
    private final ChatStateManager chatStateManager;

    @Resource(key="ChatInputPanel.textFont") private Font inputTextFont;

    public ResizingInputPanel(MessageWriter writer, ScheduledExecutorService schedExecService) {
        super(new BorderLayout());

        this.writer = writer;
        
        GuiUtils.assignResources(this);

        chatStateManager = new ChatStateManager(schedExecService);
        text = new JTextArea();
        text.setFont(inputTextFont);
        text.setRows(2);
        text.setWrapStyleWord(true);
        text.setLineWrap(true);
        text.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "sendMessage");
        text.getActionMap().put("sendMessage", new SendMessage());
        text.getDocument().addDocumentListener(chatStateManager);
        text.setBorder(BorderFactory.createEmptyBorder(2,0,0,0));
        
        JPopupMenu popup = PopupUtil.addPopupMenus(text, new CutAction(text), new CopyAction(text), 
                new PasteAction(), new DeleteAction(text));
        popup.addSeparator();
        popup.add(new SelectAllAction());

        final JScrollPane scrollPane = new JScrollPane(text, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getViewport().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (text.getText().length() > 0 && scrollPane.getVerticalScrollBar().isVisible()) {
                    resizeTextBox(3);
                }
            }
        });
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        add(scrollPane);
    }
    
    private void resizeTextBox(int rowCount) {
        if (text.getRows() != rowCount) {
            text.setRows(rowCount);
            revalidate();
        }
    }

    /**
     * Stop tracking chat state updates.
     */
    @Override
    public void dispose() {
        chatStateManager.cancelPendingChatStateUpdates();        
    }
    
    @Override
    public void handleDisplay() {
        text.requestFocusInWindow();
    }
    
    public JTextComponent getInputComponent() {
        return text;
    }

    private class SendMessage extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                String message = text.getText();
                if (message.trim().length() > 0) {
                    writer.writeMessage(message);
                    text.setText("");
                    resizeTextBox(2);
                    chatStateManager.updateChatState(ChatState.active);
                }
            } catch (FriendException e1) {
                LOG.error("Unable to write message", e1);
            }
        }
    }
    
    private class ChatStateManager implements DocumentListener {
        private final Periodic chatPausedTimer;
        private volatile ChatState currentInputChatState = ChatState.active;

        ChatStateManager(ScheduledExecutorService schedExecService) {
            chatPausedTimer = new Periodic(new Runnable() {

                public void run() {
                    if (currentInputChatState == ChatState.composing) {
                        updateChatState(ChatState.paused);
                    }
                }
            }, schedExecService);
        }

        void updateChatState(ChatState state) {
            if (currentInputChatState != state) {
                currentInputChatState = state;
                try {
                    writer.setChatState(currentInputChatState);
                } catch (FriendException e) {
                    LOG.error("Unable to set chat state", e);
                }
            }
        }

        void cancelPendingChatStateUpdates() {
            chatPausedTimer.unschedule();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            updateChatStateComposing();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            updateChatStateComposing();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            if (e.getDocument().getLength() == 0) {
                updateChatState(ChatState.active);
            } else {
                updateChatStateComposing();
            }
        }

        private void updateChatStateComposing() {
            // update chat state to "composing", if necessary
            updateChatState(ChatState.composing);

            chatPausedTimer.rescheduleIfLater(DELAY_IN_MILLIS_FOR_CHAT_PAUSED);
        }
    }
}
