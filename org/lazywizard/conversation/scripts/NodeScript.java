package org.lazywizard.conversation.scripts;

import org.lazywizard.conversation.Conversation.Node;
import org.lazywizard.conversation.DialogInfo;

public interface NodeScript
{
    public void init(Node node, DialogInfo info);

    public void advance(float amount);
}
