/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso;

import fmod.fmod.FMODManager;
import fmod.fmod.FMOD_STUDIO_EVENT_DESCRIPTION;
import fmod.javafmod;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import zombie.GameSounds;
import zombie.GameTime;
import zombie.SoundManager;
import zombie.WorldSoundManager;
import zombie.audio.GameSound;
import zombie.audio.GameSoundClip;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.core.Core;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.debug.DebugLog;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoUtils;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.Vector2;
import zombie.iso.zones.Zone;
import zombie.network.GameClient;
import zombie.network.GameServer;
import zombie.scripting.objects.CharacterTrait;

public class Helicopter {
    private static final float MAX_BOTHER_SECONDS = 60.0f;
    private static final float MAX_UNSEEN_SECONDS = 15.0f;
    private static final int RADIUS_HOVER = 50;
    private static final int RADIUS_SEARCH = 100;
    protected State state;
    public IsoGameCharacter target;
    protected float timeSinceChopperSawPlayer;
    protected float hoverTime;
    protected float searchTime;
    public float x;
    public float y;
    protected float targetX;
    protected float targetY;
    protected Vector2 move = new Vector2();
    protected boolean active;
    protected static long inst;
    protected static FMOD_STUDIO_EVENT_DESCRIPTION event;
    protected boolean soundStarted;
    protected float volume;
    protected float occlusion;

    public void pickRandomTarget() {
        ArrayList<IsoPlayer> players;
        if (GameServer.server) {
            players = GameServer.getPlayers();
        } else {
            if (GameClient.client) {
                throw new IllegalStateException("can't call this on the client");
            }
            players = new ArrayList();
            for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
                IsoPlayer player = IsoPlayer.players[i];
                if (player == null || !player.isAlive()) continue;
                players.add(player);
            }
        }
        if (players.isEmpty()) {
            this.active = false;
            this.target = null;
            return;
        }
        this.setTarget(players.get(Rand.Next(players.size())));
    }

    public void setTarget(IsoGameCharacter chr) {
        this.target = chr;
        this.x = this.target.getX() + 1000.0f;
        this.y = this.target.getY() + 1000.0f;
        this.targetX = this.target.getX();
        this.targetY = this.target.getY();
        this.move.x = this.targetX - this.x;
        this.move.y = this.targetY - this.y;
        this.move.normalize();
        this.move.setLength(0.5f);
        this.state = State.Arriving;
        this.active = true;
        DebugLog.log("chopper: activated");
    }

    protected void changeState(State newState) {
        DebugLog.log("chopper: state " + String.valueOf((Object)this.state) + " -> " + String.valueOf((Object)newState));
        this.state = newState;
    }

    /*
     * Unable to fully structure code
     */
    public void update() {
        if (!this.active) {
            return;
        }
        if (GameClient.client) {
            this.updateSound();
            this.checkMusicIntensityEvent();
            return;
        }
        timeMulti = 1.0f;
        if (GameServer.server) {
            if (!GameServer.Players.contains(this.target)) {
                this.target = null;
            }
        } else {
            timeMulti = GameTime.getInstance().getTrueMultiplier();
        }
        switch (this.state.ordinal()) {
            case 0: {
                if (this.target == null || this.target.isDead()) {
                    this.changeState(State.Leaving);
                    break;
                }
                if (IsoUtils.DistanceToSquared(this.x, this.y, this.targetX, this.targetY) < 4.0f) {
                    this.changeState(State.Hovering);
                    this.hoverTime = 0.0f;
                    this.searchTime = 0.0f;
                    this.timeSinceChopperSawPlayer = 0.0f;
                    break;
                }
                this.targetX = this.target.getX();
                this.targetY = this.target.getY();
                this.move.x = this.targetX - this.x;
                this.move.y = this.targetY - this.y;
                this.move.normalize();
                this.move.setLength(0.75f);
                break;
            }
            case 1: {
                if (this.target == null || this.target.isDead()) {
                    this.changeState(State.Leaving);
                    break;
                }
                this.hoverTime += GameTime.getInstance().getRealworldSecondsSinceLastUpdate() * timeMulti;
                if (this.hoverTime + this.searchTime > 60.0f) {
                    this.changeState(State.Leaving);
                    break;
                }
                if (!this.isTargetVisible()) {
                    this.timeSinceChopperSawPlayer += GameTime.getInstance().getRealworldSecondsSinceLastUpdate() * timeMulti;
                    if (this.timeSinceChopperSawPlayer > 15.0f) {
                        this.changeState(State.Searching);
                        break;
                    }
                }
                if (!(IsoUtils.DistanceToSquared(this.x, this.y, this.targetX, this.targetY) < 1.0f)) break;
                this.targetX = this.target.getX() + (float)(Rand.Next(100) - 50);
                this.targetY = this.target.getY() + (float)(Rand.Next(100) - 50);
                this.move.x = this.targetX - this.x;
                this.move.y = this.targetY - this.y;
                this.move.normalize();
                this.move.setLength(0.5f);
                break;
            }
            case 2: {
                if (this.target == null || this.target.isDead()) {
                    this.state = State.Leaving;
                    break;
                }
                this.searchTime += GameTime.getInstance().getRealworldSecondsSinceLastUpdate() * timeMulti;
                if (this.hoverTime + this.searchTime > 60.0f) {
                    this.changeState(State.Leaving);
                    break;
                }
                if (this.isTargetVisible()) {
                    this.timeSinceChopperSawPlayer = 0.0f;
                    this.changeState(State.Hovering);
                    break;
                }
                if (!(IsoUtils.DistanceToSquared(this.x, this.y, this.targetX, this.targetY) < 1.0f)) break;
                this.targetX = this.target.getX() + (float)(Rand.Next(200) - 100);
                this.targetY = this.target.getY() + (float)(Rand.Next(200) - 100);
                this.move.x = this.targetX - this.x;
                this.move.y = this.targetY - this.y;
                this.move.normalize();
                this.move.setLength(0.5f);
                break;
            }
            case 3: {
                listener = false;
                if (!GameServer.server) ** GOTO lbl88
                players = GameServer.getPlayers();
                for (i = 0; i < players.size(); ++i) {
                    player = players.get(i);
                    if (!(IsoUtils.DistanceToSquared(this.x, this.y, player.getX(), player.getY()) < 1000000.0f)) continue;
                    listener = true;
                    ** GOTO lbl93
                }
                ** GOTO lbl93
lbl88:
                // 2 sources

                for (i = 0; i < IsoPlayer.numPlayers; ++i) {
                    player = IsoPlayer.players[i];
                    if (player == null || !(IsoUtils.DistanceToSquared(this.x, this.y, player.getX(), player.getY()) < 1000000.0f)) continue;
                    listener = true;
                    break;
                }
lbl93:
                // 4 sources

                if (listener) break;
                this.deactivate();
                return;
            }
        }
        if (Rand.Next(Rand.AdjustForFramerate(300)) == 0) {
            WorldSoundManager.instance.addSound(null, PZMath.fastfloor(this.x), PZMath.fastfloor(this.y), 0, 500, 500);
        }
        dx = this.move.x * GameTime.getInstance().getThirtyFPSMultiplier();
        dy = this.move.y * GameTime.getInstance().getThirtyFPSMultiplier();
        if (this.state != State.Leaving && IsoUtils.DistanceToSquared(this.x + dx, this.y + dy, this.targetX, this.targetY) > IsoUtils.DistanceToSquared(this.x, this.y, this.targetX, this.targetY)) {
            this.x = this.targetX;
            this.y = this.targetY;
        } else {
            this.x += dx;
            this.y += dy;
        }
        if (GameServer.server) {
            GameServer.sendHelicopter(this.x, this.y, this.active);
        }
        this.updateSound();
        this.checkMusicIntensityEvent();
    }

    protected void updateSound() {
        if (GameServer.server) {
            return;
        }
        if (Core.soundDisabled) {
            return;
        }
        if (FMODManager.instance.getNumListeners() == 0) {
            return;
        }
        GameSound gameSound = GameSounds.getSound("Helicopter");
        if (gameSound == null || gameSound.clips.isEmpty()) {
            return;
        }
        if (inst == 0L) {
            GameSoundClip clip = gameSound.getRandomClip();
            event = clip.eventDescription;
            if (event != null) {
                javafmod.FMOD_Studio_LoadEventSampleData(Helicopter.event.address);
                inst = javafmod.FMOD_Studio_System_CreateEventInstance(Helicopter.event.address);
            }
        }
        if (inst != 0L) {
            IsoGridSquare sqPlayer;
            float volume = SoundManager.instance.getSoundVolume();
            volume = 1.0f;
            if ((volume *= gameSound.getUserVolume()) != this.volume) {
                javafmod.FMOD_Studio_EventInstance_SetVolume(inst, volume);
                this.volume = volume;
            }
            javafmod.FMOD_Studio_EventInstance3D(inst, this.x, this.y, 200.0f);
            float occlusion = 0.0f;
            if (IsoPlayer.numPlayers == 1 && (sqPlayer = IsoPlayer.getInstance().getCurrentSquare()) != null && !sqPlayer.has(IsoFlagType.exterior)) {
                occlusion = 1.0f;
            }
            if (this.occlusion != occlusion) {
                this.occlusion = occlusion;
                javafmod.FMOD_Studio_EventInstance_SetParameterByName(inst, "Occlusion", this.occlusion);
            }
            if (!this.soundStarted) {
                javafmod.FMOD_Studio_StartEvent(inst);
                this.soundStarted = true;
            }
        }
    }

    protected boolean isTargetVisible() {
        if (this.target == null || this.target.isDead()) {
            return false;
        }
        IsoGridSquare sq = this.target.getCurrentSquare();
        if (sq == null) {
            return false;
        }
        if (!sq.getProperties().has(IsoFlagType.exterior)) {
            return false;
        }
        Zone zone = sq.getZone();
        if (zone == null) {
            return true;
        }
        return !"Forest".equals(zone.getType()) && !"DeepForest".equals(zone.getType());
    }

    public void deactivate() {
        if (this.active) {
            this.active = false;
            if (this.soundStarted) {
                javafmod.FMOD_Studio_EventInstance_Stop(inst, false);
                this.soundStarted = false;
            }
            if (GameServer.server) {
                GameServer.sendHelicopter(this.x, this.y, this.active);
            }
            DebugLog.log("chopper: deactivated");
        }
    }

    public boolean isActive() {
        return this.active;
    }

    public void clientSync(float x, float y, boolean active) {
        if (!GameClient.client) {
            return;
        }
        this.x = x;
        this.y = y;
        if (!active) {
            this.deactivate();
        }
        this.active = active;
    }

    public void save(ByteBuffer bb) {
        bb.put((byte)(this.active ? 1 : 0));
        bb.putInt(this.state == null ? 0 : this.state.ordinal());
        bb.putFloat(this.x);
        bb.putFloat(this.y);
    }

    public void load(ByteBuffer bb, int worldVersion) {
        this.active = bb.get() != 0;
        this.state = State.values()[bb.getInt()];
        this.x = bb.getFloat();
        this.y = bb.getFloat();
        DebugLog.General.debugln("Re-Initializing Chopper %s", this.active);
        if (!this.active || GameServer.server || GameClient.client) {
            return;
        }
        this.target = IsoPlayer.players[0];
        if (this.target == null) {
            this.active = false;
            return;
        }
        this.targetX = this.target.getX();
        this.targetY = this.target.getY();
        DebugLog.General.debugln("target at %.4f/%.4f", Float.valueOf(this.targetX), Float.valueOf(this.targetY));
        this.move.x = this.targetX - this.x;
        this.move.y = this.targetY - this.y;
        this.move.normalize();
        this.move.setLength(0.5f);
    }

    private void checkMusicIntensityEvent() {
        if (GameServer.server) {
            return;
        }
        if (!this.active) {
            return;
        }
        for (int i = 0; i < IsoPlayer.numPlayers; ++i) {
            float distSq;
            IsoPlayer player = IsoPlayer.players[i];
            if (player == null || player.hasTrait(CharacterTrait.DEAF) || player.isDead() || (distSq = IsoUtils.DistanceToSquared(this.x, this.y, player.getX(), player.getY())) > 2500.0f) continue;
            player.triggerMusicIntensityEvent("HelicopterOverhead");
            break;
        }
    }

    private static enum State {
        Arriving,
        Hovering,
        Searching,
        Leaving;

    }
}

