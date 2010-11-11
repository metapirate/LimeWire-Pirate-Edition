package org.limewire.ui.swing.friends.chat;

/**
 * Chat Message denoting an error. When an ErrorMessage
 * wraps/contains a Message, this means that the error message
 * refers to that Message.  For example, we can wrap
 * a {@link MessageText} to indicate that the chat message did not
 * get sent.
 */
class ErrorMessage extends AbstractMessageImpl {
    
    static final String SENDER_NAME = "chat server";
    
    private final Message message;
    private final String errorMessage;
    
    public ErrorMessage(String friendId, String errorMessage, Type type) {
        super(SENDER_NAME, friendId, type);
        this.message = null;
        this.errorMessage = errorMessage;
    }
    
    public ErrorMessage(String errorMessage, Message message) {
        super(message.getSenderName(), message.getFriendID(), message.getType());
        this.message = message;
        this.errorMessage = errorMessage;
    }

    @Override
    public String format() {
        StringBuffer buffer = new StringBuffer();
        if (message != null) {
            buffer.append(message.format());    
        }
        buffer.append("<br/><b><font color=red>").
               append(errorMessage).
               append("</font></b><br/>");
        
        return buffer.toString();
    }
}
