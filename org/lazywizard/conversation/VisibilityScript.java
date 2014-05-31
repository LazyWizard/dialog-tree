package org.lazywizard.conversation;

public interface VisibilityScript
{
    public enum Visibility
    {
        HIDDEN,
        DISABLED,
        VISIBLE
    }

    public Visibility getVisibility();
}
