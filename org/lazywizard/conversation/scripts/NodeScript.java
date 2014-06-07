package org.lazywizard.conversation.scripts;

import org.lazywizard.conversation.Conversation.ConversationInfo;
import org.lazywizard.conversation.Conversation.Node;

public interface NodeScript
{
    public void init(Node node, ConversationInfo info);

    public void advance(float amount);
}
