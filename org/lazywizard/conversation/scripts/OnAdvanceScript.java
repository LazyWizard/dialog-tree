package org.lazywizard.conversation.scripts;

import org.lazywizard.conversation.Conversation.Node;

public interface OnAdvanceScript
{
    public void advance(float amount, Node currentNode);
}
