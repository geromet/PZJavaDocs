/*
 * Decompiled with CFR 0.152.
 */
package zombie.ui;

import zombie.core.Color;
import zombie.ui.UINineGrid;

public interface UITextEntryInterface {
    public boolean isDoingTextEntry();

    public void setDoingTextEntry(boolean var1);

    public String getUIName();

    public boolean isEditable();

    public UINineGrid getFrame();

    public boolean isIgnoreFirst();

    public void setIgnoreFirst(boolean var1);

    public void setSelectingRange(boolean var1);

    public Color getStandardFrameColour();

    public void onKeyEnter();

    public void onKeyHome();

    public void onKeyEnd();

    public void onKeyUp();

    public void onKeyDown();

    public void onKeyLeft();

    public void onKeyRight();

    public void onKeyDelete();

    public void onKeyBack();

    public void pasteFromClipboard();

    public void copyToClipboard();

    public void cutToClipboard();

    public void selectAll();

    public boolean isTextLimit();

    public boolean isOnlyNumbers();

    public boolean isOnlyText();

    public void onOtherKey(int var1);

    public void putCharacter(char var1);
}

