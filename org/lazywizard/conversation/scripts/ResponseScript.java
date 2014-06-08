package org.lazywizard.conversation.scripts;

import org.lazywizard.conversation.DialogInfo;
import java.util.List;

public interface ResponseScript
{
    public void onMousedOver(DialogInfo info, boolean wasLastMousedOver);

    public void onChosen(DialogInfo info, List args);
}
