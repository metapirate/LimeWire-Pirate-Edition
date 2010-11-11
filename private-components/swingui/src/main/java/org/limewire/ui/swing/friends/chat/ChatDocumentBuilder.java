package org.limewire.ui.swing.friends.chat;

import static org.limewire.ui.swing.util.I18n.tr;

import java.util.List;

import org.limewire.friend.api.ChatState;
import org.limewire.ui.swing.friends.chat.Message.Type;

/**
 *
 */
class ChatDocumentBuilder {
    static final String LIBRARY_LINK = "#library";
    static final String MY_LIBRARY_LINK = "#mylibrary";

    private static final String LINE_BREAK = "<br/>";

    final static String TOP = 
        "<html>" +
            "<head>" +
                "<style>" +
                    "body { " +
                        "font-family: Arial;" +
                        "font-size: 11;" +
                        "color: #313131;" +
                        "background-color: #ffffff;}" +
                    ".me { " +
                        "color: #004e8b;" +
                        "font-weight: bold;}" +
                    ".them { " +
                        "color: #af0511;" + 
                        "font-weight: bold;}" +
                    ".typing { " +
                        "color: #646464;}" + 
                    "form { text-align: center;}" +
                "</style>" +
            "</head>" +
            "<body>";
    
    final static String BOTTOM = 
        "</body>" +
        "</html>";
    
    public static String buildChatText(List<Message> messages, ChatState currentChatState, 
            String conversationName, boolean friendSignedOff) {
        StringBuilder builder = new StringBuilder();
        builder.append(TOP);
        
        Type lastMessageType = null;
        long lastMessageTimeFromMe = 0;
        for(Message message : messages) {

            Type type = message.getType();

            if (message.getType() != Message.Type.SERVER) {
                if (lastMessageType == null) {
                    //The first message of a conversation
                    appendMessageSender(builder, message);
                } else if (lastMessageType != type || sixtySecondRule(lastMessageTimeFromMe, message)) {
                    builder.append(LINE_BREAK);
                    appendMessageSender(builder, message);
                }
            }
            
            lastMessageType = type;
            
            builder.append(message.format());
            
            builder.append(LINE_BREAK);
            
            if (type == Type.SENT) {
                lastMessageTimeFromMe = message.getMessageTimeMillis();
            }
        }

        appendIsTypingMessage(builder, conversationName, currentChatState, friendSignedOff);
        
        builder.append(BOTTOM);
        return builder.toString();
    }

    private static boolean sixtySecondRule(long lastMessageTimeFromMe, Message message) {
        return message.getType() == Type.SENT && lastMessageTimeFromMe + 60000 < message.getMessageTimeMillis();
    }

    private static StringBuilder appendMessageSender(StringBuilder builder, Message message) {
        Type type = message.getType();
        String cssClass = type == Type.SENT ? "me" : "them";
        String content = message.getSenderName();
        return builder.append("<div class=\"")
        .append(cssClass)
        .append("\">")
        .append(content)
        .append(":")
        .append("</div>");
    }
    
    private static void appendIsTypingMessage(StringBuilder builder, String senderName, ChatState chatState, boolean friendSignedOff) {
        String stateMessage;
        if (friendSignedOff) {
            stateMessage = tr("{0} has signed off", senderName);
        } else if (chatState == ChatState.composing) {
            stateMessage = tr("{0} is typing...", senderName);
        } else if (chatState == ChatState.paused) {
            stateMessage = tr("{0} has entered text", senderName);
        } else {
            return;
        }
        
        String cssClass = "typing";
        
        builder.append("<div class=\"")
               .append(cssClass)
               .append("\">")
               .append(stateMessage)
               .append("</div>")
               .append("<br/>");
    }
}
