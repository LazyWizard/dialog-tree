package org.lazywizard.conversation;

import com.fs.starfarer.api.BaseModPlugin;

public class ConversationsModPlugin extends BaseModPlugin
{
    @Override
    public void onApplicationLoad() throws Exception
    {
        ConversationMaster.reloadConversations();
    }
}
