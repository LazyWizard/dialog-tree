package org.lazywizard.conversation.scripts;

import org.lazywizard.conversation.Conversation.Info;
import org.lazywizard.conversation.Conversation.Node;

public interface NodeScript
{
    public void init(Node node, Info info);

    public void advance(float amount);
}
