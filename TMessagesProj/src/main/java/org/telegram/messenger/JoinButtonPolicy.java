package org.telegram.messenger;

public final class JoinButtonPolicy {

    private static final long DISABLE_JOIN_CHANNEL_ID = 3406182093L;

    private JoinButtonPolicy() {
    }

    public static boolean isJoinDisabled(long dialogId, long chatId) {
        if (chatId == DISABLE_JOIN_CHANNEL_ID) {
            return true;
        }
        if (dialogId == -DISABLE_JOIN_CHANNEL_ID) {
            return true;
        }
        return -dialogId == DISABLE_JOIN_CHANNEL_ID;
    }
}
