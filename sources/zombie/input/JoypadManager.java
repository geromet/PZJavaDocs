/*
 * Decompiled with CFR 0.152.
 */
package zombie.input;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import org.lwjglx.input.Controller;
import zombie.GameWindow;
import zombie.Lua.LuaEventManager;
import zombie.ZomboidFileSystem;
import zombie.characters.IsoPlayer;
import zombie.core.BoxedStaticValues;
import zombie.core.logger.ExceptionLogger;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.iso.Vector2;

public final class JoypadManager {
    public static final JoypadManager instance = new JoypadManager();
    public final Joypad[] joypads = new Joypad[4];
    public final Joypad[] joypadsController = new Joypad[16];
    public final ArrayList<Joypad> joypadList = new ArrayList();
    public final HashSet<String> activeControllerGuids = new HashSet();
    private static final int VERSION_1 = 1;
    private static final int VERSION_2 = 2;
    private static final int VERSION_LATEST = 2;

    public Joypad addJoypad(int controller, String guid, String name) {
        Joypad j = new Joypad();
        j.id = controller;
        j.guid = guid;
        j.name = name;
        this.joypadsController[controller] = j;
        this.doControllerFile(j);
        if (!j.isDisabled() && this.activeControllerGuids.contains(guid)) {
            this.joypadList.add(j);
        }
        return j;
    }

    private Joypad checkJoypad(int index) {
        if (index == -1) {
            return null;
        }
        if (this.joypadsController[index] == null) {
            Controller controller = GameWindow.GameInput.getController(index);
            this.addJoypad(index, controller.getGUID(), controller.getGamepadName());
        }
        return this.joypadsController[index];
    }

    private void doControllerFile(Joypad j) {
        File file = new File(ZomboidFileSystem.instance.getCacheDirSub("joypads"));
        if (!file.exists()) {
            file.mkdir();
        }
        file = new File(ZomboidFileSystem.instance.getCacheDirSub("joypads" + File.separator + j.guid + ".config"));
        try (FileReader fileReader2 = new FileReader(file.getAbsolutePath());
             BufferedReader br = new BufferedReader(fileReader2);){
            DebugLog.DetailedInfo.trace("reloading " + file.getAbsolutePath());
            int version = -1;
            try {
                String line = "";
                while (line != null) {
                    String[] split;
                    line = br.readLine();
                    if (line == null || line.trim().isEmpty() || line.trim().startsWith("//") || (split = line.split("=")).length != 2) continue;
                    split[0] = split[0].trim();
                    split[1] = split[1].trim();
                    if (split[0].equals("Version")) {
                        version = Integer.parseInt(split[1]);
                        if (version < 1 || version > 2) {
                            DebugLog.DetailedInfo.warn("Unknown version %d in %s", version, file.getAbsolutePath());
                            break;
                        }
                        if (version == 1) {
                            DebugLog.DetailedInfo.warn("Obsolete version %d in %s.  Using default values.", version, file.getAbsolutePath());
                            break;
                        }
                    }
                    if (version == -1) {
                        DebugLog.General.warn("Ignoring %s=%s because Version is missing", split[0], split[1]);
                        continue;
                    }
                    if (split[0].equals("MovementAxisX")) {
                        j.movementAxisX = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("MovementAxisXFlipped")) {
                        j.movementAxisXFlipped = split[1].equals("true");
                        continue;
                    }
                    if (split[0].equals("MovementAxisY")) {
                        j.movementAxisY = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("MovementAxisYFlipped")) {
                        j.movementAxisYFlipped = split[1].equals("true");
                        continue;
                    }
                    if (split[0].equals("MovementAxisDeadZone")) {
                        j.movementAxisDeadZone = Float.parseFloat(split[1]);
                        continue;
                    }
                    if (split[0].equals("AimingAxisX")) {
                        j.aimingAxisX = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("AimingAxisXFlipped")) {
                        j.aimingAxisXFlipped = split[1].equals("true");
                        continue;
                    }
                    if (split[0].equals("AimingAxisY")) {
                        j.aimingAxisY = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("AimingAxisYFlipped")) {
                        j.aimingAxisYFlipped = split[1].equals("true");
                        continue;
                    }
                    if (split[0].equals("AimingAxisDeadZone")) {
                        j.aimingAxisDeadZone = Float.parseFloat(split[1]);
                        continue;
                    }
                    if (split[0].equals("AButton")) {
                        j.aButton = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("BButton")) {
                        j.bButton = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("XButton")) {
                        j.xButton = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("YButton")) {
                        j.yButton = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("LBumper")) {
                        j.bumperLeft = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("RBumper")) {
                        j.bumperRight = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("L3")) {
                        j.leftStickButton = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("R3")) {
                        j.rightStickButton = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("Back")) {
                        j.back = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("Start")) {
                        j.start = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("DPadUp")) {
                        j.dPadUp = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("DPadDown")) {
                        j.dPadDown = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("DPadLeft")) {
                        j.dPadLeft = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("DPadRight")) {
                        j.dPadRight = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("TriggersFlipped")) {
                        j.triggersFlipped = split[1].equals("true");
                        continue;
                    }
                    if (split[0].equals("TriggerLeft")) {
                        j.triggerLeft = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("TriggerRight")) {
                        j.triggerRight = Integer.parseInt(split[1]);
                        continue;
                    }
                    if (split[0].equals("Disabled")) {
                        j.disabled = split[1].equals("true");
                        continue;
                    }
                    if (!split[0].equals("Sensitivity")) continue;
                    j.setDeadZone(Float.parseFloat(split[1]));
                }
            }
            catch (Exception ex) {
                ExceptionLogger.logException(ex);
            }
        }
        catch (FileNotFoundException fileReader2) {
        }
        catch (IOException ex) {
            ExceptionLogger.logException(ex);
        }
        this.saveFile(j);
    }

    private void saveFile(Joypad j) {
        File file = new File(ZomboidFileSystem.instance.getCacheDir() + File.separator + "joypads");
        if (!file.exists()) {
            file.mkdir();
        }
        file = new File(ZomboidFileSystem.instance.getCacheDirSub("joypads" + File.separator + j.guid + ".config"));
        try (FileWriter fileWriter = new FileWriter(file.getAbsolutePath());
             BufferedWriter bw = new BufferedWriter(fileWriter);){
            String lineSep = System.getProperty("line.separator");
            bw.write("Version=2" + lineSep);
            bw.write("Name=" + j.name + lineSep);
            bw.write("MovementAxisX=" + j.movementAxisX + lineSep);
            bw.write("MovementAxisXFlipped=" + j.movementAxisXFlipped + lineSep);
            bw.write("MovementAxisY=" + j.movementAxisY + lineSep);
            bw.write("MovementAxisYFlipped=" + j.movementAxisYFlipped + lineSep);
            bw.write("// Set the dead zone to the smallest number between 0.0 and 1.0." + lineSep);
            bw.write("// This is to fix \"loose sticks\"." + lineSep);
            bw.write("MovementAxisDeadZone=" + j.movementAxisDeadZone + lineSep);
            bw.write("AimingAxisX=" + j.aimingAxisX + lineSep);
            bw.write("AimingAxisXFlipped=" + j.aimingAxisXFlipped + lineSep);
            bw.write("AimingAxisY=" + j.aimingAxisY + lineSep);
            bw.write("AimingAxisYFlipped=" + j.aimingAxisYFlipped + lineSep);
            bw.write("AimingAxisDeadZone=" + j.aimingAxisDeadZone + lineSep);
            bw.write("AButton=" + j.aButton + lineSep);
            bw.write("BButton=" + j.bButton + lineSep);
            bw.write("XButton=" + j.xButton + lineSep);
            bw.write("YButton=" + j.yButton + lineSep);
            bw.write("LBumper=" + j.bumperLeft + lineSep);
            bw.write("RBumper=" + j.bumperRight + lineSep);
            bw.write("L3=" + j.leftStickButton + lineSep);
            bw.write("R3=" + j.rightStickButton + lineSep);
            bw.write("Back=" + j.back + lineSep);
            bw.write("Start=" + j.start + lineSep);
            bw.write("// Normally the D-pad is treated as a single axis (the POV Hat), and these should be -1." + lineSep);
            bw.write("// If your D-pad is actually 4 separate buttons, set the button numbers here." + lineSep);
            bw.write("DPadUp=" + j.dPadUp + lineSep);
            bw.write("DPadDown=" + j.dPadDown + lineSep);
            bw.write("DPadLeft=" + j.dPadLeft + lineSep);
            bw.write("DPadRight=" + j.dPadRight + lineSep);
            bw.write("TriggersFlipped=" + j.triggersFlipped + lineSep);
            bw.write("// If your triggers are buttons, set the button numbers here." + lineSep);
            bw.write("// If these are set to something other than -1, then Triggers= is ignored." + lineSep);
            bw.write("TriggerLeft=" + j.triggerLeft + lineSep);
            bw.write("TriggerRight=" + j.triggerRight + lineSep);
            bw.write("Disabled=" + j.disabled + lineSep);
            bw.write("Sensitivity=" + j.getDeadZone(0) + lineSep);
        }
        catch (IOException e) {
            ExceptionLogger.logException(e);
        }
    }

    public void reloadControllerFiles() {
        for (int i = 0; i < GameWindow.GameInput.getControllerCount(); ++i) {
            Controller controller = GameWindow.GameInput.getController(i);
            if (controller == null) continue;
            if (this.joypadsController[i] == null) {
                this.addJoypad(i, controller.getGUID(), controller.getGamepadName());
                continue;
            }
            this.doControllerFile(this.joypadsController[i]);
        }
    }

    public void assignJoypad(int controller, int player) {
        this.checkJoypad(controller);
        this.joypads[player] = this.joypadsController[controller];
        this.joypads[player].player = player;
    }

    public Joypad getFromPlayer(int player) {
        return this.joypads[player];
    }

    public Joypad getFromControllerID(int id) {
        return this.joypadsController[id];
    }

    public void onPressed(int c, int i) {
        this.checkJoypad(c);
        this.joypadsController[c].onPressed(i);
    }

    public boolean isDownPressed(int c) {
        this.checkJoypad(c);
        return this.joypadsController[c].isDownPressed();
    }

    public boolean isUpPressed(int c) {
        this.checkJoypad(c);
        return this.joypadsController[c].isUpPressed();
    }

    public boolean isRightPressed(int c) {
        this.checkJoypad(c);
        return this.joypadsController[c].isRightPressed();
    }

    public boolean isLeftPressed(int c) {
        this.checkJoypad(c);
        return this.joypadsController[c].isLeftPressed();
    }

    public boolean isLBPressed(int c) {
        if (c < 0) {
            for (int n = 0; n < this.joypadList.size(); ++n) {
                if (!this.joypadList.get(n).isLBPressed()) continue;
                return true;
            }
            return false;
        }
        this.checkJoypad(c);
        return this.joypadsController[c].isLBPressed();
    }

    public boolean isRBPressed(int c) {
        if (c < 0) {
            for (int n = 0; n < this.joypadList.size(); ++n) {
                if (!this.joypadList.get(n).isRBPressed()) continue;
                return true;
            }
            return false;
        }
        this.checkJoypad(c);
        return this.joypadsController[c].isRBPressed();
    }

    public boolean isL3Pressed(int c) {
        if (c < 0) {
            for (int n = 0; n < this.joypadList.size(); ++n) {
                if (!this.joypadList.get(n).isL3Pressed()) continue;
                return true;
            }
            return false;
        }
        this.checkJoypad(c);
        return this.joypadsController[c].isL3Pressed();
    }

    public boolean isR3Pressed(int c) {
        if (c < 0) {
            for (int n = 0; n < this.joypadList.size(); ++n) {
                if (!this.joypadList.get(n).isR3Pressed()) continue;
                return true;
            }
            return false;
        }
        this.checkJoypad(c);
        return this.joypadsController[c].isR3Pressed();
    }

    public boolean isRTPressed(int c) {
        if (c < 0) {
            for (int n = 0; n < this.joypadList.size(); ++n) {
                if (!this.joypadList.get(n).isRTPressed()) continue;
                return true;
            }
            return false;
        }
        this.checkJoypad(c);
        return this.joypadsController[c].isRTPressed();
    }

    public boolean isLTPressed(int c) {
        if (c < 0) {
            for (int n = 0; n < this.joypadList.size(); ++n) {
                if (!this.joypadList.get(n).isLTPressed()) continue;
                return true;
            }
            return false;
        }
        this.checkJoypad(c);
        return this.joypadsController[c].isLTPressed();
    }

    public boolean isAPressed(int c) {
        if (c < 0) {
            for (int n = 0; n < this.joypadList.size(); ++n) {
                if (!this.joypadList.get(n).isAPressed()) continue;
                return true;
            }
            return false;
        }
        this.checkJoypad(c);
        return this.joypadsController[c].isAPressed();
    }

    public boolean isBPressed(int c) {
        if (c < 0) {
            for (int n = 0; n < this.joypadList.size(); ++n) {
                if (!this.joypadList.get(n).isBPressed()) continue;
                return true;
            }
            return false;
        }
        this.checkJoypad(c);
        return this.joypadsController[c].isBPressed();
    }

    public boolean isXPressed(int c) {
        if (c < 0) {
            for (int n = 0; n < this.joypadList.size(); ++n) {
                if (!this.joypadList.get(n).isXPressed()) continue;
                return true;
            }
            return false;
        }
        this.checkJoypad(c);
        return this.joypadsController[c].isXPressed();
    }

    public boolean isYPressed(int c) {
        if (c < 0) {
            for (int n = 0; n < this.joypadList.size(); ++n) {
                if (!this.joypadList.get(n).isYPressed()) continue;
                return true;
            }
            return false;
        }
        this.checkJoypad(c);
        return this.joypadsController[c].isYPressed();
    }

    public boolean isButtonStartPress(int c, int button) {
        Joypad joypad = this.checkJoypad(c);
        return joypad.isButtonStartPress(button);
    }

    public boolean isButtonReleasePress(int c, int button) {
        Joypad joypad = this.checkJoypad(c);
        return joypad.isButtonReleasePress(button);
    }

    public boolean isAButtonStartPress(int c) {
        Joypad joypad = this.checkJoypad(c);
        return this.isButtonStartPress(c, joypad.getAButton());
    }

    public boolean isBButtonStartPress(int c) {
        Joypad joypad = this.checkJoypad(c);
        return joypad.isButtonStartPress(joypad.getBButton());
    }

    public boolean isXButtonStartPress(int c) {
        Joypad joypad = this.checkJoypad(c);
        return joypad.isButtonStartPress(joypad.getXButton());
    }

    public boolean isYButtonStartPress(int c) {
        Joypad joypad = this.checkJoypad(c);
        return joypad.isButtonStartPress(joypad.getYButton());
    }

    public boolean isAButtonReleasePress(int c) {
        Joypad joypad = this.checkJoypad(c);
        return joypad.isButtonReleasePress(joypad.getAButton());
    }

    public boolean isBButtonReleasePress(int c) {
        Joypad joypad = this.checkJoypad(c);
        return joypad.isButtonReleasePress(joypad.getBButton());
    }

    public boolean isXButtonReleasePress(int c) {
        Joypad joypad = this.checkJoypad(c);
        return joypad.isButtonReleasePress(joypad.getXButton());
    }

    public boolean isYButtonReleasePress(int c) {
        Joypad joypad = this.checkJoypad(c);
        return joypad.isButtonReleasePress(joypad.getYButton());
    }

    public float getMovementAxisX(int joypadIndex) {
        Joypad joypad = this.checkJoypad(joypadIndex);
        if (joypad == null || !joypad.isMovementAxisBeingApplied()) {
            return 0.0f;
        }
        return joypad.getMovementAxisX();
    }

    public float getMovementAxisY(int joypadIndex) {
        Joypad joypad = this.checkJoypad(joypadIndex);
        if (joypad == null || !joypad.isMovementAxisBeingApplied()) {
            return 0.0f;
        }
        return joypad.getMovementAxisY();
    }

    public Vector2 getMovementAxis(int joypadIndex, Vector2 out) {
        Joypad joypad = this.checkJoypad(joypadIndex);
        if (joypad == null || !joypad.isMovementAxisBeingApplied()) {
            return out.set(0.0f, 0.0f);
        }
        float moveX = joypad.getMovementAxisX();
        float moveY = joypad.getMovementAxisY();
        return out.set(moveX, moveY);
    }

    public boolean isMovementAxisBeingApplied(int joypadIndex) {
        Joypad joypad = this.checkJoypad(joypadIndex);
        return joypad != null && joypad.isMovementAxisBeingApplied();
    }

    public float getAimingAxisX(int joypadIndex) {
        Joypad joypad = this.checkJoypad(joypadIndex);
        if (joypad == null || !joypad.isAimingAxisBeingApplied()) {
            return 0.0f;
        }
        return joypad.getAimingAxisX();
    }

    public float getAimingAxisY(int joypadIndex) {
        Joypad joypad = this.checkJoypad(joypadIndex);
        if (joypad == null || !joypad.isAimingAxisBeingApplied()) {
            return 0.0f;
        }
        return joypad.getAimingAxisY();
    }

    public Vector2 getAimingAxis(int joypadIndex, Vector2 out) {
        Joypad joypad = this.checkJoypad(joypadIndex);
        if (joypad == null || !joypad.isAimingAxisBeingApplied()) {
            return out.set(0.0f, 0.0f);
        }
        float aimX = joypad.getAimingAxisX();
        float aimY = joypad.getAimingAxisY();
        return out.set(aimX, aimY);
    }

    public boolean isAimingAxisBeingApplied(int joypadIndex) {
        Joypad joypad = this.checkJoypad(joypadIndex);
        return joypad != null && joypad.isAimingAxisBeingApplied();
    }

    public void onPressedAxis(int c, int i) {
        this.checkJoypad(c);
        this.joypadsController[c].onPressedAxis(i);
    }

    public void onPressedAxisNeg(int c, int i) {
        this.checkJoypad(c);
        this.joypadsController[c].onPressedAxisNeg(i);
    }

    public void onPressedTrigger(int c, int i) {
        this.checkJoypad(c);
        this.joypadsController[c].onPressedTrigger(i);
    }

    public void onPressedPov(int c) {
        this.checkJoypad(c);
        this.joypadsController[c].onPressedPov();
    }

    public float getDeadZone(int joypadIndex, int axis) {
        Joypad joypad = this.checkJoypad(joypadIndex);
        if (joypad == null) {
            return 0.0f;
        }
        return joypad.getDeadZone(axis);
    }

    public void setDeadZone(int c, int axis, float value) {
        this.checkJoypad(c);
        this.joypadsController[c].setDeadZone(axis, value);
    }

    public void saveControllerSettings(int c) {
        this.checkJoypad(c);
        this.saveFile(this.joypadsController[c]);
    }

    public long getLastActivity(int c) {
        if (this.joypadsController[c] == null) {
            return 0L;
        }
        return this.joypadsController[c].lastActivity;
    }

    public void setControllerActive(String guid, boolean active) {
        if (active) {
            this.activeControllerGuids.add(guid);
        } else {
            this.activeControllerGuids.remove(guid);
        }
        this.syncActiveControllers();
    }

    public void syncActiveControllers() {
        this.joypadList.clear();
        for (int i = 0; i < this.joypadsController.length; ++i) {
            Joypad joypad = this.joypadsController[i];
            if (joypad == null || joypad.isDisabled() || !this.activeControllerGuids.contains(joypad.guid)) continue;
            this.joypadList.add(joypad);
        }
    }

    public boolean isJoypadConnected(int index) {
        if (index < 0 || index >= 16) {
            return false;
        }
        assert (Thread.currentThread() == GameWindow.gameThread);
        return GameWindow.GameInput.getController(index) != null;
    }

    public void onControllerConnected(Controller controller) {
        Joypad joypad = this.joypadsController[controller.getID()];
        if (joypad == null) {
            return;
        }
        LuaEventManager.triggerEvent("OnJoypadBeforeReactivate", BoxedStaticValues.toDouble(joypad.getID()));
        joypad.connected = true;
        LuaEventManager.triggerEvent("OnJoypadReactivate", BoxedStaticValues.toDouble(joypad.getID()));
    }

    public void onControllerDisconnected(Controller controller) {
        Joypad joypad = this.joypadsController[controller.getID()];
        if (joypad == null) {
            return;
        }
        LuaEventManager.triggerEvent("OnJoypadBeforeDeactivate", BoxedStaticValues.toDouble(joypad.getID()));
        joypad.connected = false;
        LuaEventManager.triggerEvent("OnJoypadDeactivate", BoxedStaticValues.toDouble(joypad.getID()));
    }

    public void revertToKeyboardAndMouseFromMainMenu() {
        if (GameWindow.activatedJoyPad != null) {
            GameWindow.activatedJoyPad = null;
        }
    }

    public void revertToKeyboardAndMouse() {
        for (int i = 0; i < this.joypadList.size(); ++i) {
            IsoPlayer player;
            Joypad joypad = this.joypadList.get(i);
            if (joypad.player != 0) continue;
            if (GameWindow.activatedJoyPad == joypad) {
                GameWindow.activatedJoyPad = null;
            }
            if ((player = IsoPlayer.players[0]) != null) {
                player.joypadBind = -1;
            }
            this.joypadsController[joypad.getID()] = null;
            this.joypads[0] = null;
            this.joypadList.remove(i);
            break;
        }
    }

    public void renderUI() {
        assert (Thread.currentThread() == GameWindow.gameThread);
        if (!DebugOptions.instance.joypadRenderUi.getValue()) {
            return;
        }
        if (GameWindow.drawReloadingLua) {
            return;
        }
        LuaEventManager.triggerEvent("OnJoypadRenderUI");
    }

    public void Reset() {
        for (int n = 0; n < this.joypads.length; ++n) {
            this.joypads[n] = null;
        }
    }

    public static final class Joypad {
        String guid;
        String name;
        int id;
        int player = -1;
        int movementAxisX = 0;
        boolean movementAxisXFlipped;
        int movementAxisY = 1;
        boolean movementAxisYFlipped;
        float movementAxisDeadZone;
        int aimingAxisX = 2;
        boolean aimingAxisXFlipped;
        int aimingAxisY = 3;
        boolean aimingAxisYFlipped;
        float aimingAxisDeadZone;
        int aButton = 0;
        int bButton = 1;
        int xButton = 2;
        int yButton = 3;
        int dPadUp = -1;
        int dPadDown = -1;
        int dPadLeft = -1;
        int dPadRight = -1;
        int bumperLeft = 4;
        int bumperRight = 5;
        int back = 6;
        int start = 7;
        int leftStickButton = 9;
        int rightStickButton = 10;
        boolean triggersFlipped;
        int triggerLeft = 4;
        int triggerRight = 5;
        boolean disabled;
        boolean connected = true;
        long lastActivity;
        private static final Vector2 tempVec2 = new Vector2();

        public boolean isDownPressed() {
            if (this.dPadDown != -1) {
                return GameWindow.GameInput.isButtonPressedD(this.dPadDown, this.id);
            }
            return GameWindow.GameInput.isControllerDownD(this.id);
        }

        public boolean isUpPressed() {
            if (this.dPadUp != -1) {
                return GameWindow.GameInput.isButtonPressedD(this.dPadUp, this.id);
            }
            return GameWindow.GameInput.isControllerUpD(this.id);
        }

        public boolean isRightPressed() {
            if (this.dPadRight != -1) {
                return GameWindow.GameInput.isButtonPressedD(this.dPadRight, this.id);
            }
            return GameWindow.GameInput.isControllerRightD(this.id);
        }

        public boolean isLeftPressed() {
            if (this.dPadLeft != -1) {
                return GameWindow.GameInput.isButtonPressedD(this.dPadLeft, this.id);
            }
            return GameWindow.GameInput.isControllerLeftD(this.id);
        }

        public boolean isLBPressed() {
            return GameWindow.GameInput.isButtonPressedD(this.bumperLeft, this.id);
        }

        public boolean isRBPressed() {
            return GameWindow.GameInput.isButtonPressedD(this.bumperRight, this.id);
        }

        public boolean isL3Pressed() {
            return GameWindow.GameInput.isButtonPressedD(this.leftStickButton, this.id);
        }

        public boolean isR3Pressed() {
            return GameWindow.GameInput.isButtonPressedD(this.rightStickButton, this.id);
        }

        public boolean isRTPressed() {
            int trigger = this.triggerRight;
            if (GameWindow.GameInput.getAxisCount(this.id) <= trigger) {
                return this.isRBPressed();
            }
            if (this.triggersFlipped) {
                return GameWindow.GameInput.getAxisValue(this.id, trigger) < -0.7f;
            }
            return GameWindow.GameInput.getAxisValue(this.id, trigger) > 0.7f;
        }

        public boolean isLTPressed() {
            int trigger = this.triggerLeft;
            if (GameWindow.GameInput.getAxisCount(this.id) <= trigger) {
                return this.isLBPressed();
            }
            if (this.triggersFlipped) {
                return GameWindow.GameInput.getAxisValue(this.id, trigger) < -0.7f;
            }
            return GameWindow.GameInput.getAxisValue(this.id, trigger) > 0.7f;
        }

        public boolean isAPressed() {
            return GameWindow.GameInput.isButtonPressedD(this.aButton, this.id);
        }

        public boolean isBPressed() {
            return GameWindow.GameInput.isButtonPressedD(this.bButton, this.id);
        }

        public boolean isXPressed() {
            return GameWindow.GameInput.isButtonPressedD(this.xButton, this.id);
        }

        public boolean isYPressed() {
            return GameWindow.GameInput.isButtonPressedD(this.yButton, this.id);
        }

        public boolean isButtonPressed(int button) {
            return GameWindow.GameInput.isButtonPressedD(button, this.id);
        }

        public boolean wasButtonPressed(int button) {
            return GameWindow.GameInput.wasButtonPressed(this.id, button);
        }

        public boolean isButtonStartPress(int button) {
            return GameWindow.GameInput.isButtonStartPress(this.id, button);
        }

        public boolean isButtonReleasePress(int button) {
            return GameWindow.GameInput.isButtonReleasePress(this.id, button);
        }

        public float getMovementAxisX() {
            if (GameWindow.GameInput.getAxisCount(this.id) <= this.movementAxisX) {
                return 0.0f;
            }
            this.movementAxisDeadZone = GameWindow.GameInput.getController(this.id).getDeadZone(this.movementAxisX);
            float deadZone = this.movementAxisDeadZone;
            if (deadZone > 0.0f && deadZone < 1.0f) {
                float yAxis;
                float xAxis = GameWindow.GameInput.getAxisValue(this.id, this.movementAxisX);
                Vector2 stickInput = tempVec2.set(xAxis, yAxis = GameWindow.GameInput.getAxisValue(this.id, this.movementAxisY));
                if (stickInput.getLength() < deadZone) {
                    stickInput.set(0.0f, 0.0f);
                } else {
                    stickInput.setLength((stickInput.getLength() - deadZone) / (1.0f - deadZone));
                }
                return this.movementAxisXFlipped ? -stickInput.getX() : stickInput.getX();
            }
            if (this.movementAxisXFlipped) {
                return -GameWindow.GameInput.getAxisValue(this.id, this.movementAxisX);
            }
            return GameWindow.GameInput.getAxisValue(this.id, this.movementAxisX);
        }

        public float getMovementAxisY() {
            if (GameWindow.GameInput.getAxisCount(this.id) <= this.movementAxisY) {
                return 0.0f;
            }
            this.movementAxisDeadZone = GameWindow.GameInput.getController(this.id).getDeadZone(this.movementAxisY);
            float deadZone = this.movementAxisDeadZone;
            if (deadZone > 0.0f && deadZone < 1.0f) {
                float yAxis;
                float xAxis = GameWindow.GameInput.getAxisValue(this.id, this.movementAxisX);
                Vector2 stickInput = tempVec2.set(xAxis, yAxis = GameWindow.GameInput.getAxisValue(this.id, this.movementAxisY));
                if (stickInput.getLength() < deadZone) {
                    stickInput.set(0.0f, 0.0f);
                } else {
                    stickInput.setLength((stickInput.getLength() - deadZone) / (1.0f - deadZone));
                }
                return this.movementAxisYFlipped ? -stickInput.getY() : stickInput.getY();
            }
            if (this.movementAxisYFlipped) {
                return -GameWindow.GameInput.getAxisValue(this.id, this.movementAxisY);
            }
            return GameWindow.GameInput.getAxisValue(this.id, this.movementAxisY);
        }

        public float getAimingAxisX() {
            if (GameWindow.GameInput.getAxisCount(this.id) <= this.aimingAxisX) {
                return 0.0f;
            }
            this.aimingAxisDeadZone = GameWindow.GameInput.getController(this.id).getDeadZone(this.aimingAxisX);
            float deadZone = this.aimingAxisDeadZone;
            if (deadZone > 0.0f && deadZone < 1.0f) {
                float yAxis;
                float xAxis = GameWindow.GameInput.getAxisValue(this.id, this.aimingAxisX);
                Vector2 stickInput = tempVec2.set(xAxis, yAxis = GameWindow.GameInput.getAxisValue(this.id, this.aimingAxisY));
                if (stickInput.getLength() < deadZone) {
                    stickInput.set(0.0f, 0.0f);
                } else {
                    stickInput.setLength((stickInput.getLength() - deadZone) / (1.0f - deadZone));
                }
                return this.aimingAxisXFlipped ? -stickInput.getX() : stickInput.getX();
            }
            if (this.aimingAxisXFlipped) {
                return -GameWindow.GameInput.getAxisValue(this.id, this.aimingAxisX);
            }
            return GameWindow.GameInput.getAxisValue(this.id, this.aimingAxisX);
        }

        public float getAimingAxisY() {
            if (GameWindow.GameInput.getAxisCount(this.id) <= this.aimingAxisY) {
                return 0.0f;
            }
            this.aimingAxisDeadZone = GameWindow.GameInput.getController(this.id).getDeadZone(this.aimingAxisY);
            float deadZone = this.aimingAxisDeadZone;
            if (deadZone > 0.0f && deadZone < 1.0f) {
                float yAxis;
                float xAxis = GameWindow.GameInput.getAxisValue(this.id, this.aimingAxisX);
                Vector2 stickInput = tempVec2.set(xAxis, yAxis = GameWindow.GameInput.getAxisValue(this.id, this.aimingAxisY));
                if (stickInput.getLength() < deadZone) {
                    stickInput.set(0.0f, 0.0f);
                } else {
                    stickInput.setLength((stickInput.getLength() - deadZone) / (1.0f - deadZone));
                }
                return this.aimingAxisYFlipped ? -stickInput.getY() : stickInput.getY();
            }
            if (this.aimingAxisYFlipped) {
                return -GameWindow.GameInput.getAxisValue(this.id, this.aimingAxisY);
            }
            return GameWindow.GameInput.getAxisValue(this.id, this.aimingAxisY);
        }

        public void onPressed(int i) {
            this.lastActivity = System.currentTimeMillis();
        }

        public void onPressedAxis(int i) {
            this.lastActivity = System.currentTimeMillis();
        }

        public void onPressedAxisNeg(int i) {
            this.lastActivity = System.currentTimeMillis();
        }

        public void onPressedTrigger(int i) {
            this.lastActivity = System.currentTimeMillis();
        }

        public void onPressedPov() {
            this.lastActivity = System.currentTimeMillis();
        }

        public float getMovementAxisDeadZoneX() {
            return this.getDeadZone(this.movementAxisX);
        }

        public float getMovementAxisDeadZoneY() {
            return this.getDeadZone(this.movementAxisY);
        }

        public float getAimingAxisDeadZoneX() {
            return this.getDeadZone(this.aimingAxisX);
        }

        public float getAimingAxisDeadZoneY() {
            return this.getDeadZone(this.aimingAxisY);
        }

        public float getDeadZone(int axis) {
            float deadZoneFromSettings = 0.0f;
            if ((axis == this.movementAxisX || axis == this.movementAxisY) && this.movementAxisDeadZone > 0.0f && this.movementAxisDeadZone < 1.0f) {
                deadZoneFromSettings = this.movementAxisDeadZone;
            }
            if ((axis == this.aimingAxisX || axis == this.aimingAxisY) && this.aimingAxisDeadZone > 0.0f && this.aimingAxisDeadZone < 1.0f) {
                deadZoneFromSettings = this.aimingAxisDeadZone;
            }
            if (axis < 0 || axis >= GameWindow.GameInput.getAxisCount(this.id)) {
                return deadZoneFromSettings;
            }
            float deadZoneFromController = GameWindow.GameInput.getController(this.id).getDeadZone(axis);
            return Math.max(deadZoneFromController, deadZoneFromSettings);
        }

        public void setDeadZone(int axis, float value) {
            if (axis < 0 || axis >= GameWindow.GameInput.getAxisCount(this.id)) {
                return;
            }
            GameWindow.GameInput.getController(this.id).setDeadZone(axis, value);
        }

        public void setDeadZone(float value) {
            for (int axis = 0; axis < GameWindow.GameInput.getAxisCount(this.id); ++axis) {
                GameWindow.GameInput.getController(this.id).setDeadZone(axis, value);
            }
        }

        public int getID() {
            return this.id;
        }

        public boolean isDisabled() {
            return this.disabled;
        }

        public int getAButton() {
            return this.aButton;
        }

        public int getBButton() {
            return this.bButton;
        }

        public int getXButton() {
            return this.xButton;
        }

        public int getYButton() {
            return this.yButton;
        }

        public int getLBumper() {
            return this.bumperLeft;
        }

        public int getRBumper() {
            return this.bumperRight;
        }

        public int getL3() {
            return this.leftStickButton;
        }

        public int getR3() {
            return this.rightStickButton;
        }

        public int getBackButton() {
            return this.back;
        }

        public int getStartButton() {
            return this.start;
        }

        public boolean isMovementInsideDeadZone(float x, float y) {
            float deadZoneY;
            float movementDistanceSq = x * x + y * y;
            float deadZoneX = this.getMovementAxisDeadZoneX();
            float deadZoneSq = deadZoneX * (deadZoneY = this.getMovementAxisDeadZoneY());
            return movementDistanceSq < deadZoneSq;
        }

        public boolean isMovementAxisBeingApplied() {
            float moveY;
            float moveX = this.getMovementAxisX();
            return !this.isMovementInsideDeadZone(moveX, moveY = this.getMovementAxisY());
        }

        public boolean isAimingInsideDeadZone(float x, float y) {
            float deadZoneY;
            float aimDistanceSq = x * x + y * y;
            float deadZoneX = this.getAimingAxisDeadZoneX();
            float deadZoneSq = deadZoneX * (deadZoneY = this.getAimingAxisDeadZoneY());
            return aimDistanceSq < deadZoneSq;
        }

        public boolean isAimingAxisBeingApplied() {
            float aimY;
            float aimX = this.getAimingAxisX();
            return !this.isAimingInsideDeadZone(aimX, aimY = this.getAimingAxisY());
        }
    }
}

