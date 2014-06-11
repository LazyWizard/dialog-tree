package org.lazywizard.conversation;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;

public class ConversationModPlugin extends BaseModPlugin
{
    @Override
    public void onApplicationLoad() throws Exception
    {
        ConversationMaster.reloadConversations();
    }

    @Override
    public void onGameLoad()
    {
        Global.getSector().registerPlugin(new ConversationCampaignPlugin());
    }
}