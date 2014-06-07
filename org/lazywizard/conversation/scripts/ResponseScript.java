package org.lazywizard.conversation.scripts;

import org.lazywizard.conversation.Conversation.Info;

public interface ResponseScript
{
    public void onMousedOver(Info info, boolean wasLastMousedOver);

    public void onChosen(Info info, Object[] args);
}
