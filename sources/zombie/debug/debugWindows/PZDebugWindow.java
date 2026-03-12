/*
 * Decompiled with CFR 0.152.
 */
package zombie.debug.debugWindows;

import zombie.SoundAssetManager;
import zombie.SoundManager;
import zombie.debug.BaseDebugWindow;

public class PZDebugWindow
extends BaseDebugWindow {
    @Override
    protected void onWindowDocked() {
        if (SoundAssetManager.useImGuiSounds && !SoundManager.instance.isPlayingUISound("UIDocked")) {
            SoundManager.instance.playUISound("UIDocked");
        }
    }

    @Override
    protected void onWindowUndocked() {
        if (SoundAssetManager.useImGuiSounds && !SoundManager.instance.isPlayingUISound("UIUndocked")) {
            SoundManager.instance.playUISound("UIUndocked");
        }
    }

    @Override
    protected void onOpenWindow() {
        if (SoundAssetManager.useImGuiSounds && !SoundManager.instance.isPlayingUISound("UIDocked")) {
            SoundManager.instance.playUISound("UIDocked");
        }
    }

    @Override
    protected void onCloseWindow() {
        if (SoundAssetManager.useImGuiSounds && !SoundManager.instance.isPlayingUISound("UIUndocked")) {
            SoundManager.instance.playUISound("UIUndocked");
        }
    }
}

