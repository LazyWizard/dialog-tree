package org.lazywizard.conversation.impl;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI;
import com.fs.starfarer.api.campaign.JumpPointAPI.JumpDestination;
import com.fs.starfarer.api.campaign.VisualPanelAPI;
import org.lazywizard.conversation.Conversation;
import org.lazywizard.conversation.scripts.NodeScript;
import org.lazywizard.conversation.scripts.ResponseScript;

public class JumpPointNodeScript implements NodeScript
{
    private String getString(String id, CampaignFleetAPI player)
    {
        String str = Global.getSettings().getString("jumpPointInteractionDialog", id);

        String fleetOrShip = "fleet";
        if (player.getFleetData().getMembersListCopy().size() == 1)
        {
            fleetOrShip = "ship";
            if (player.getFleetData().getMembersListCopy().get(0).isFighterWing())
            {
                fleetOrShip = "fighter wing";
            }
        }
        str = str.replaceAll("\\$fleetOrShip", fleetOrShip);

        return str;
    }

    @Override
    public void init(Conversation.Node node, Conversation.Info info)
    {
        JumpPointAPI jumpPoint = (JumpPointAPI) info.getConversationPartner();
        CampaignFleetAPI player = info.getPlayer();

        VisualPanelAPI visual = info.getDialog().getInteractionDialog().getVisualPanel();
        visual.setVisualFade(0.25f, 0.25f);
        if (jumpPoint.getCustomInteractionDialogImageVisual() != null)
        {
            visual.showImageVisual(jumpPoint.getCustomInteractionDialogImageVisual());
        }
        else
        {
            if (player.getContainingLocation().isHyperspace())
            {
                visual.showImagePortion("illustrations", "jump_point_hyper", 400, 400, 0, 0, 400, 400);
            }
            else
            {
                visual.showImagePortion("illustrations", "jump_point_normal", 400, 400, 0, 0, 400, 400);
            }
        }

        node.setText(getString("approach", player));

        if (jumpPoint.isStarAnchor())
        {
            node.appendText(getString("starAnchor", player));
        }

        boolean dev = Global.getSettings().isDevMode();
        float navigation = Global.getSector().getPlayerFleet().getCommanderStats().getSkillLevel("navigation");
        boolean isStarAnchor = jumpPoint.isStarAnchor();
        boolean okToUseIfAnchor = isStarAnchor && navigation >= 7;

        if (isStarAnchor && !okToUseIfAnchor && dev)
        {
            node.appendText("(Can always be used in dev mode)");
        }
        okToUseIfAnchor |= dev;

        if (jumpPoint.getDestinations().isEmpty())
        {
            node.appendText(getString("noExits", player));
        }
        else if (player.getCargo().getFuel() <= 0)
        {
            node.appendText(getString("noFuel", player));
        }
        else if (isStarAnchor && !okToUseIfAnchor)
        {
            node.appendText(getString("starAnchorUnusable", player));
        }
        else
        {
            for (JumpDestination dest : jumpPoint.getDestinations())
            {
                node.addResponse(new Conversation.Response(
                        "Order a jump to " + dest.getLabelInInteractionDialog(),
                        null, null, new JumpPointResponseScript(dest), null, null));
            }
        }

        node.addResponse(new Conversation.Response("Leave", null));
    }

    @Override
    public void advance(float amount)
    {
    }

    private static class JumpPointResponseScript implements ResponseScript
    {
        private final JumpDestination dest;

        public JumpPointResponseScript(JumpDestination dest)
        {
            this.dest = dest;
        }

        @Override
        public void onMousedOver(Conversation.Info info, boolean wasLastMousedOver)
        {
        }

        @Override
        public void onChosen(Conversation.Info info, Object[] args)
        {
            info.getDialog().endConversation();
            Global.getSector().setPaused(false);
            Global.getSector().doHyperspaceTransition(
                    info.getPlayer(), info.getConversationPartner(), dest);
        }
    }
}
