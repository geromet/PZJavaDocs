/*
 * Decompiled with CFR 0.152.
 */
package zombie.characters;

import java.lang.constant.Constable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import zombie.GameTime;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.Safety;
import zombie.core.math.PZMath;
import zombie.core.raknet.UdpConnection;
import zombie.debug.DebugType;
import zombie.debug.LogSeverity;
import zombie.iso.areas.NonPvpZone;
import zombie.network.GameServer;
import zombie.network.PVPLogTool;
import zombie.network.ServerOptions;
import zombie.util.Type;

public class SafetySystemManager {
    private static final LinkedHashMap<String, Float> playerCooldown = new LinkedHashMap();
    private static final LinkedHashMap<String, Boolean> playerSafety = new LinkedHashMap();
    private static final LinkedHashMap<String, Long> playerDelay = new LinkedHashMap();
    private static final long safetyDelay = 1500L;

    private static void updateTimers(Safety safety) {
        float time = GameTime.instance.getRealworldSecondsSinceLastUpdate();
        if (safety.getToggle() > 0.0f) {
            safety.setToggle(safety.getToggle() - time);
            if (safety.getToggle() <= 0.0f) {
                safety.setToggle(0.0f);
                if (!safety.isLast()) {
                    safety.setEnabled(!safety.isEnabled());
                }
            }
        } else if (safety.getCooldown() > 0.0f) {
            safety.setCooldown(safety.getCooldown() - time);
        } else {
            safety.setCooldown(0.0f);
        }
    }

    private static void updateNonPvpZone(IsoPlayer player, boolean isNonPvpZone) {
        if (isNonPvpZone && !player.networkAi.wasNonPvpZone) {
            SafetySystemManager.storeSafety(player);
            GameServer.sendChangeSafety(player.getSafety());
        } else if (!isNonPvpZone && player.networkAi.wasNonPvpZone) {
            SafetySystemManager.restoreSafety(player);
            GameServer.sendChangeSafety(player.getSafety());
        }
        player.networkAi.wasNonPvpZone = isNonPvpZone;
    }

    static void update(IsoPlayer player) {
        boolean isNonPvpZone;
        boolean bl = isNonPvpZone = NonPvpZone.getNonPvpZone(PZMath.fastfloor(player.getX()), PZMath.fastfloor(player.getY())) != null;
        if (!isNonPvpZone) {
            SafetySystemManager.updateTimers(player.getSafety());
        }
        if (GameServer.server) {
            SafetySystemManager.updateNonPvpZone(player, isNonPvpZone);
        }
    }

    public static void clear() {
        playerCooldown.clear();
        playerSafety.clear();
        playerDelay.clear();
    }

    public static void clearSafety(IsoPlayer player) {
        playerCooldown.remove(player.getUsername());
        playerSafety.remove(player.getUsername());
        playerDelay.remove(player.getUsername());
        player.getSafety().setCooldown(0.0f);
        player.getSafety().setToggle(0.0f);
        PVPLogTool.logSafety(player, "clear");
    }

    public static void storeSafety(IsoPlayer player) {
        try {
            if (player != null && player.isAlive()) {
                Iterator<Map.Entry<String, Constable>> i;
                Safety safety = player.getSafety();
                playerSafety.put(player.getUsername(), safety.isEnabled());
                playerCooldown.put(player.getUsername(), Float.valueOf(safety.getCooldown()));
                playerDelay.put(player.getUsername(), System.currentTimeMillis());
                if (playerCooldown.size() > ServerOptions.instance.maxPlayers.getValue() * 1000 && (i = playerCooldown.entrySet().iterator()).hasNext()) {
                    i.next();
                    i.remove();
                }
                if (playerSafety.size() > ServerOptions.instance.maxPlayers.getValue() * 1000 && (i = playerSafety.entrySet().iterator()).hasNext()) {
                    i.next();
                    i.remove();
                }
                if (playerDelay.size() > ServerOptions.instance.maxPlayers.getValue() * 1000 && (i = playerDelay.entrySet().iterator()).hasNext()) {
                    i.next();
                    i.remove();
                }
                PVPLogTool.logSafety(player, "store");
            } else {
                DebugType.Combat.debugln("StoreSafety: player not found");
            }
        }
        catch (Exception e) {
            DebugType.Multiplayer.printException(e, "StoreSafety failed", LogSeverity.Error);
        }
    }

    public static void restoreSafety(IsoPlayer player) {
        try {
            if (player != null) {
                Safety safety = player.getSafety();
                if (playerSafety.containsKey(player.getUsername())) {
                    safety.setEnabled((Boolean)playerSafety.remove(player.getUsername()));
                }
                if (playerCooldown.containsKey(player.getUsername())) {
                    safety.setCooldown(((Float)playerCooldown.remove(player.getUsername())).floatValue());
                }
                playerDelay.put(player.getUsername(), System.currentTimeMillis());
                PVPLogTool.logSafety(player, "restore");
            } else {
                DebugType.Combat.debugln("RestoreSafety: player not found");
            }
        }
        catch (Exception e) {
            DebugType.Multiplayer.printException(e, "RestoreSafety failed", LogSeverity.Error);
        }
    }

    public static void updateOptions() {
        block3: {
            boolean safety;
            block2: {
                boolean pvp = ServerOptions.instance.pvp.getValue();
                safety = ServerOptions.instance.safetySystem.getValue();
                if (pvp) break block2;
                SafetySystemManager.clear();
                for (IsoPlayer player : GameServer.IDToPlayerMap.values()) {
                    if (player == null) continue;
                    player.getSafety().setEnabled(true);
                    player.getSafety().setLast(false);
                    player.getSafety().setCooldown(0.0f);
                    player.getSafety().setToggle(0.0f);
                    GameServer.sendChangeSafety(player.getSafety());
                }
                break block3;
            }
            if (safety) break block3;
            SafetySystemManager.clear();
            for (IsoPlayer player : GameServer.IDToPlayerMap.values()) {
                if (player == null) continue;
                player.getSafety().setEnabled(false);
                player.getSafety().setLast(false);
                player.getSafety().setCooldown(0.0f);
                player.getSafety().setToggle(0.0f);
                GameServer.sendChangeSafety(player.getSafety());
            }
        }
    }

    public static boolean checkUpdateDelay(IsoGameCharacter wielder, IsoGameCharacter target) {
        boolean result = false;
        IsoPlayer wielderPlayer = Type.tryCastTo(wielder, IsoPlayer.class);
        IsoPlayer targetPlayer = Type.tryCastTo(target, IsoPlayer.class);
        if (wielderPlayer != null && targetPlayer != null) {
            long time = System.currentTimeMillis();
            if (playerDelay.containsKey(wielderPlayer.getUsername())) {
                boolean isWielderDelayed;
                long wielderDelay = time - playerDelay.getOrDefault(wielderPlayer.getUsername(), 0L);
                result = isWielderDelayed = wielderDelay < 1500L;
                if (!isWielderDelayed) {
                    playerDelay.remove(wielderPlayer.getUsername());
                }
            }
            if (playerDelay.containsKey(targetPlayer.getUsername())) {
                boolean isTargetDelayed;
                long targetDelay = time - playerDelay.getOrDefault(targetPlayer.getUsername(), 0L);
                boolean bl = isTargetDelayed = targetDelay < 1500L;
                if (!result) {
                    result = isTargetDelayed;
                }
                if (!isTargetDelayed) {
                    playerDelay.remove(targetPlayer.getUsername());
                }
            }
        }
        return result;
    }

    public static long getSafetyTimestamp(String username) {
        return playerDelay.getOrDefault(username, System.currentTimeMillis());
    }

    public static long getSafetyDelay() {
        return 1500L;
    }

    public static float getCooldown(UdpConnection connection) {
        if (ServerOptions.getInstance().pvp.getValue() && ServerOptions.getInstance().safetySystem.getValue() && ServerOptions.getInstance().safetyDisconnectDelay.getValue() > 0) {
            float result = 0.0f;
            IsoPlayer[] players = GameServer.server ? connection.players : IsoPlayer.players;
            for (IsoPlayer player : players) {
                if (player == null || !(player.getSafety().getCooldown() + player.getSafety().getToggle() > result)) continue;
                result = player.getSafety().getCooldown() + player.getSafety().getToggle();
            }
            if (GameServer.server) {
                if (result > 0.0f) {
                    result = ServerOptions.getInstance().safetyDisconnectDelay.getValue();
                }
                DebugType.Multiplayer.debugln("Delay %f", Float.valueOf(result));
            }
            return result;
        }
        return 0.0f;
    }
}

