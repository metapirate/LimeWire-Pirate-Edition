package org.limewire.ui.swing.friends.chat;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.Timer;

import org.jdesktop.beans.AbstractBean;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.feature.LimewireFeature;
import org.limewire.ui.swing.util.SwingUtils;

class ChatFriendImpl extends AbstractBean implements ChatFriend {

    private boolean chatting;
    private final Friend friend;
    private String status;
    private FriendPresence.Mode mode;
    private long chatStartTime;
    private boolean hasUnviewedMessages;
    private Timer timer;
    private int flashCount = 0;

    ChatFriendImpl(final FriendPresence presence) {
        this.friend = presence.getFriend();
        this.status = presence.getStatus();
        this.mode = presence.getMode();
    }

    @Override
    public Friend getFriend() {
        return friend;
    }
    
    @Override
    public String getID() {
        return friend.getId();
    }

    @Override
    public FriendPresence.Mode getMode() {
        return mode;
    }
    
    void setMode(FriendPresence.Mode mode) {
        FriendPresence.Mode oldMode = getMode();
        this.mode = mode;
        firePropertyChange("mode", oldMode, mode);
    }

    @Override
    public String getName() {
        return friend.getRenderName();
    }

    @Override
    public String getStatus() {
        return status;
    }
    
    void setStatus(String status) {
        String oldStatus = getStatus();
        this.status = status;
        firePropertyChange("status", oldStatus, status);
    }

    @Override
    public boolean isChatting() {
        return chatting;
    }

    void setChatting(final boolean chatting) {
        final boolean oldChatting = isChatting();
        this.chatting = chatting;
        SwingUtils.invokeNowOrLater(new Runnable(){
            public void run() {
                firePropertyChange("chatting", oldChatting, chatting);                
            }
        });
    }

    @Override
    public MessageWriter createChat(MessageReader reader) {
        return friend.createChat(reader);
    }

    @Override
    public void startChat() {
        if (isChatting() == false) {
            chatStartTime = System.currentTimeMillis();
            setChatting(true);
        }
    }

    @Override
    public void update() {
        // If there's an available presence, set to "Available"
        // If no available presence, use highest priority presence.
        FriendPresence presence = getPresenceForModeAndStatus();
        if (presence != null) {
            setStatus(presence.getStatus());
            setMode(presence.getMode());
        }
    }

    @Override
    public void stopChat() {
        stopTimer();
        setChatting(false);
        setHasUnviewedMessages(false);
    }

    @Override
    public long getChatStartTime() {
        return chatStartTime;
    }

    @Override
    public boolean isSignedInToLimewire() {
        for (FriendPresence presence : friend.getPresences().values()) {
            if (presence.getFeature(LimewireFeature.ID) != null) {
                return true;
            }
         }
        return false;
    }

    @Override
    public boolean isSignedIn() {
        return friend.isSignedIn();
    }

    @Override
    public boolean hasUnviewedMessages() {
        return hasUnviewedMessages;
    }

    public boolean isFlashState() {
        return flashCount % 2 == 0;
    }
    
    @Override
    public void setHasUnviewedMessages(boolean hasMessages) {
        if(hasMessages)
            startTimer();
        else
            stopTimer();
        boolean oldHasUnviewedMessages = hasUnviewedMessages;
        hasUnviewedMessages = hasMessages;
        firePropertyChange("receivingUnviewedMessages", oldHasUnviewedMessages, hasMessages);
    }
    
    /**
	 * Starts a timer to flash the chat icon when a new
     * message has been received but the friend is not selected.
	 */    
    private void startTimer() {
        if(timer == null) {
            timer = new Timer(1500, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(flashCount > 4)
                        stopTimer();
                    firePropertyChange("flashIncrement", flashCount, flashCount + 1);
                    flashCount += 1;
                }
            });
            timer.start();
        } else {
            flashCount = 0;
            timer.restart();
        }
    }
    
    private void stopTimer() {
        if(timer != null) {
            timer.stop();
            flashCount = 0;
            timer = null;
        }
    }

    private FriendPresence getPresenceForModeAndStatus() {
        ArrayList<FriendPresence> presences = new ArrayList<FriendPresence>(friend.getPresences().values());
        Collections.sort(presences, new ModeAndPriorityPresenceComparator());
        return presences.size() == 0 ? null : presences.get(presences.size()-1);
    }
    
    private static class ModeAndPriorityPresenceComparator implements Comparator<FriendPresence> {
        @Override
        public int compare(FriendPresence o1, FriendPresence o2) {
            if (!o1.getMode().equals(o2.getMode())) {
                if (o1.getMode() == FriendPresence.Mode.available) {
                    return 1;
                } else if (o2.getMode() == FriendPresence.Mode.available) {
                    return -1;
                }
            }

            return Integer.valueOf(o1.getPriority()).compareTo(o2.getPriority());
        }
    }
}
