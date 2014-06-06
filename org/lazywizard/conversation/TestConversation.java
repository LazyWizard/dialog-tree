package org.lazywizard.conversation;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

public class TestConversation implements BaseCommand
{
    @Override
    public CommandResult runCommand(String args, CommandContext context)
    {
        if (context != CommandContext.CAMPAIGN_MAP)
        {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if ("reload".equalsIgnoreCase(args))
        {
            try
            {
                ConversationMaster.reloadConversations();
            }
            catch (Exception ex)
            {
                Console.showException("Failed to reload conversations:", ex);
                return CommandResult.ERROR;
            }

            Console.showMessage("Reloaded conversations.");
            return CommandResult.SUCCESS;
        }

        String convId = (args.isEmpty() ? "testConv" : args);
        if (!ConversationMaster.hasConversation(convId))
        {
            Console.showMessage("No conversation with ID \"" + convId + "\" loaded!");
            return CommandResult.ERROR;
        }

        Console.showMessage("Showing conversation " + convId + "...");
        Global.getSector().addScript(new ShowConvLaterScript(convId));
        return CommandResult.SUCCESS;
    }

    private static class ShowConvLaterScript implements EveryFrameScript
    {
        private final String convId;
        private boolean isDone = false;

        private ShowConvLaterScript(String convId)
        {
            this.convId = convId;
        }

        @Override
        public boolean isDone()
        {
            return isDone;
        }

        @Override
        public boolean runWhilePaused()
        {
            return false;
        }

        @Override
        public void advance(float amount)
        {
            if (!isDone)
            {
                isDone = true;
                Conversation conv = ConversationMaster.getConversation(convId);
                ConversationMaster.showConversation(conv,
                        Global.getSector().getPlayerFleet());
            }
        }
    }
}
