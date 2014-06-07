package org.lazywizard.conversation.scripts;

import org.lazywizard.conversation.Conversation.ConversationInfo;

public interface ResponseScript
{
    public void onMousedOver(ConversationInfo info, boolean wasLastMousedOver);

    public void onChosen(ConversationInfo info, Object[] args);
}
