package org.lazywizard.conversation.scripts;

import org.lazywizard.conversation.DialogInfo;

public interface ResponseScript
{
    public void onMousedOver(DialogInfo info, boolean wasLastMousedOver);

    public void onChosen(DialogInfo info, Object[] args);
}
