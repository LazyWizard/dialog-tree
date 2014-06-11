package org.lazywizard.conversation.scripts;

import org.lazywizard.conversation.Conversation;
import org.lazywizard.conversation.Conversation.Node;
import org.lazywizard.conversation.DialogInfo;

public interface ConversationScript
{
    public void init(Conversation conv, DialogInfo info);

    public void advance(float amount, Node currentNode);

    public void end(Conversation conv, DialogInfo info);
}
