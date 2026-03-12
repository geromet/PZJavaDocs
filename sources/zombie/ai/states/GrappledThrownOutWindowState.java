/*
 * Decompiled with CFR 0.152.
 */
package zombie.ai.states;

import fmod.fmod.FMODManager;
import java.util.Map;
import zombie.GameTime;
import zombie.SoundManager;
import zombie.ai.State;
import zombie.ai.states.PathFindState;
import zombie.ai.states.WalkTowardNetworkState;
import zombie.ai.states.WalkTowardState;
import zombie.audio.parameters.ParameterZombieState;
import zombie.characters.CharacterStat;
import zombie.characters.IsoGameCharacter;
import zombie.characters.IsoPlayer;
import zombie.characters.IsoZombie;
import zombie.characters.MoveDeltaModifiers;
import zombie.characters.skills.PerkFactory;
import zombie.core.math.PZMath;
import zombie.core.random.Rand;
import zombie.core.skinnedmodel.advancedanimation.AnimEvent;
import zombie.core.skinnedmodel.advancedanimation.AnimLayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoMovingObject;
import zombie.iso.IsoObject;
import zombie.iso.IsoWorld;
import zombie.iso.SpriteDetails.IsoFlagType;
import zombie.iso.objects.IsoThumpable;
import zombie.iso.objects.IsoWindow;
import zombie.iso.objects.IsoWindowFrame;
import zombie.scripting.objects.MoodleType;
import zombie.util.Type;

public class GrappledThrownOutWindowState
extends State {
    private static final GrappledThrownOutWindowState INSTANCE = new GrappledThrownOutWindowState();
    public static final State.Param<Integer> START_X = State.Param.ofInt("start_x", 0);
    public static final State.Param<Integer> START_Y = State.Param.ofInt("start_y", 0);
    public static final State.Param<Integer> Z = State.Param.ofInt("z", 0);
    public static final State.Param<Integer> OPPOSITE_X = State.Param.ofInt("opposite_x", 0);
    public static final State.Param<Integer> OPPOSITE_Y = State.Param.ofInt("opposite_y", 0);
    public static final State.Param<IsoDirections> DIR = State.Param.of("dir", IsoDirections.class);
    public static final State.Param<Boolean> ZOMBIE_ON_FLOOR = State.Param.ofBool("zombie_on_floor", false);
    public static final State.Param<State> PREV_STATE = State.Param.of("prev_state", State.class);
    public static final State.Param<Boolean> SCRATCH = State.Param.ofBool("scratch", false);
    public static final State.Param<Boolean> COUNTER = State.Param.ofBool("counter", false);
    public static final State.Param<Boolean> SOLID_FLOOR = State.Param.ofBool("solid_floor", false);
    public static final State.Param<Boolean> SHEET_ROPE = State.Param.ofBool("sheet_rope", false);
    public static final State.Param<Integer> END_X = State.Param.ofInt("end_x", 0);
    public static final State.Param<Integer> END_Y = State.Param.ofInt("end_y", 0);
    public static final State.Param<Boolean> SCRATCHED = State.Param.ofBool("scratched", false);

    public static GrappledThrownOutWindowState instance() {
        return INSTANCE;
    }

    private GrappledThrownOutWindowState() {
        super(true, false, false, false);
    }

    @Override
    public void enter(IsoGameCharacter owner) {
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        owner.setIgnoreMovement(true);
        owner.setHideWeaponModel(true);
        boolean isCounter = owner.get(COUNTER);
        owner.setVariable("ClimbWindowStarted", false);
        owner.setVariable("ClimbWindowEnd", false);
        owner.setVariable("ClimbWindowFinished", false);
        owner.clearVariable("ClimbWindowGetUpBack");
        owner.clearVariable("ClimbWindowGetUpFront");
        owner.setVariable("ClimbWindowOutcome", isCounter ? "obstacle" : "success");
        owner.clearVariable("ClimbWindowFlopped");
        IsoZombie zombie = Type.tryCastTo(owner, IsoZombie.class);
        if (!isCounter && zombie != null && zombie.shouldDoFenceLunge()) {
            this.setLungeXVars(zombie);
            owner.setVariable("ClimbWindowOutcome", "lunge");
        }
        if (!owner.get(SOLID_FLOOR).booleanValue()) {
            owner.setVariable("ClimbWindowOutcome", "fall");
        }
        if (!(owner instanceof IsoZombie) && owner.get(SHEET_ROPE).booleanValue()) {
            owner.setVariable("ClimbWindowOutcome", "rope");
        }
        if (player != null && player.isLocalPlayer()) {
            player.dirtyRecalcGridStackTime = 20.0f;
            player.triggerMusicIntensityEvent("ClimbThroughWindow");
        }
    }

    @Override
    public void execute(IsoGameCharacter owner) {
        float endY;
        float endX;
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        if (this.isWindowClosing(owner)) {
            return;
        }
        IsoDirections dir = owner.get(DIR);
        if (dir == null) {
            return;
        }
        if (owner.getVariableBoolean("ClimbWindowStarted", false)) {
            if (owner.isFallOnFront()) {
                owner.setDir(dir.Rot180());
            } else {
                owner.setDir(dir);
            }
        }
        String climbWindowOutcome = owner.getVariableString("ClimbWindowOutcome");
        float startX = (float)owner.get(START_X).intValue() + 0.5f;
        float startY = (float)owner.get(START_Y).intValue() + 0.5f;
        if (owner instanceof IsoPlayer && climbWindowOutcome.equalsIgnoreCase("obstacle") && owner.DistToSquared(endX = (float)owner.get(END_X).intValue() + 0.5f, endY = (float)owner.get(END_Y).intValue() + 0.5f) < 0.5625f) {
            owner.setVariable("ClimbWindowOutcome", "obstacleEnd");
        }
        if (!(!(owner instanceof IsoPlayer) || owner.getVariableBoolean("ClimbWindowEnd") || "fallfront".equals(climbWindowOutcome) || "back".equals(climbWindowOutcome) || "fallback".equals(climbWindowOutcome))) {
            int oppositeZ;
            int oppositeY;
            int oppositeX = owner.get(OPPOSITE_X);
            IsoGridSquare oppositeSq = IsoWorld.instance.currentCell.getGridSquare(oppositeX, oppositeY = owner.get(OPPOSITE_Y).intValue(), oppositeZ = owner.get(Z).intValue());
            if (oppositeSq != null) {
                this.checkForFallingBack(oppositeSq, owner);
                if (oppositeSq != owner.getSquare() && oppositeSq.TreatAsSolidFloor()) {
                    this.checkForFallingFront(owner.getSquare(), owner);
                }
            }
            if (owner.getMoodles().getMoodleLevel(MoodleType.DRUNK) > 1 && owner.getVariableString("ClimbWindowOutcome").equals(climbWindowOutcome) && (float)Rand.Next(2000) < owner.getStats().get(CharacterStat.INTOXICATION)) {
                if (Rand.NextBool(2)) {
                    owner.setVariable("ClimbWindowOutcome", "fallback");
                } else {
                    owner.setVariable("ClimbWindowOutcome", "fallfront");
                }
            }
        }
        if (!(!owner.getVariableBoolean("TransitioningThroughWindow") || "back".equals(climbWindowOutcome) || "fallback".equals(climbWindowOutcome) || "lunge".equals(climbWindowOutcome) || "obstacle".equals(climbWindowOutcome) || "obstacleEnd".equals(climbWindowOutcome))) {
            if (owner.getX() != startX && (dir == IsoDirections.N || dir == IsoDirections.S)) {
                this.slideX(owner, startX, 0.25f);
            }
            if (owner.getY() != startY && (dir == IsoDirections.W || dir == IsoDirections.E)) {
                this.slideY(owner, startY, 0.25f);
            }
            float x = owner.get(START_X).intValue();
            float y = owner.get(START_Y).intValue();
            switch (dir) {
                case N: {
                    y -= 0.1f;
                    break;
                }
                case S: {
                    y += 1.1f;
                    break;
                }
                case W: {
                    x -= 0.1f;
                    break;
                }
                case E: {
                    x += 1.1f;
                }
            }
            if (PZMath.fastfloor(owner.getX()) != PZMath.fastfloor(x) && (dir == IsoDirections.W || dir == IsoDirections.E)) {
                this.slideX(owner, x, 0.1f);
            }
            if (PZMath.fastfloor(owner.getY()) != PZMath.fastfloor(y) && (dir == IsoDirections.N || dir == IsoDirections.S)) {
                this.slideY(owner, y, 0.1f);
            }
        }
        if (owner.getVariableBoolean("ClimbWindowStarted") && owner.get(SCRATCH).booleanValue()) {
            owner.set(SCRATCH, false);
            owner.getBodyDamage().setScratchedWindow();
            if (player != null) {
                player.playerVoiceSound("PainFromGlassCut");
            }
        }
        if (owner.getVariableBoolean("ClimbWindowStarted") && owner.isVariable("ClimbWindowOutcome", "fall")) {
            owner.setbFalling(true);
        }
    }

    private void checkForFallingBack(IsoGridSquare sq, IsoGameCharacter owner) {
        for (int i = 0; i < sq.getMovingObjects().size(); ++i) {
            IsoMovingObject movingObj = sq.getMovingObjects().get(i);
            IsoZombie zombie = Type.tryCastTo(movingObj, IsoZombie.class);
            if (zombie == null || zombie.isOnFloor() || zombie.isSitAgainstWall()) continue;
            if (zombie.isVariable("AttackOutcome", "success") || Rand.Next(5 + owner.getPerkLevel(PerkFactory.Perks.Fitness)) == 0) {
                owner.setVariable("ClimbWindowOutcome", "fallback");
                continue;
            }
            owner.setVariable("ClimbWindowOutcome", "back");
        }
    }

    private void checkForFallingFront(IsoGridSquare sq, IsoGameCharacter owner) {
        for (int i = 0; i < sq.getMovingObjects().size(); ++i) {
            IsoMovingObject movingObj = sq.getMovingObjects().get(i);
            IsoZombie zombie = Type.tryCastTo(movingObj, IsoZombie.class);
            if (zombie == null || zombie.isOnFloor() || zombie.isSitAgainstWall() || !zombie.isVariable("AttackOutcome", "success")) continue;
            owner.setVariable("ClimbWindowOutcome", "fallfront");
        }
    }

    @Override
    public void exit(IsoGameCharacter owner) {
        owner.setIgnoreMovement(false);
        owner.setForwardDirectionFromAnimAngle();
        owner.setHideWeaponModel(false);
        if (owner.isVariable("ClimbWindowOutcome", "fall") || owner.isVariable("ClimbWindowOutcome", "fallback") || owner.isVariable("ClimbWindowOutcome", "fallfront")) {
            owner.setHitReaction("");
        }
        owner.clearVariable("ClimbWindowFinished");
        owner.clearVariable("ClimbWindowOutcome");
        owner.clearVariable("ClimbWindowStarted");
        owner.clearVariable("ClimbWindowFlopped");
        owner.clearVariable("PlayerVoiceSound");
        owner.clearVariable("grappledThrownOutWindow");
        if (owner instanceof IsoZombie) {
            owner.setOnFloor(false);
            owner.setKnockedDown(false);
        }
        if (owner instanceof IsoZombie) {
            IsoZombie zombie = (IsoZombie)owner;
            zombie.allowRepathDelay = 0.0f;
            State prevState = owner.get(PREV_STATE);
            if (prevState == PathFindState.instance()) {
                if (owner.getPathFindBehavior2().getTargetChar() == null) {
                    owner.setVariable("bPathFind", true);
                    owner.setVariable("bMoving", false);
                } else if (zombie.isTargetLocationKnown()) {
                    owner.pathToCharacter(owner.getPathFindBehavior2().getTargetChar());
                } else if (zombie.lastTargetSeenX != -1) {
                    owner.pathToLocation(zombie.lastTargetSeenX, zombie.lastTargetSeenY, zombie.lastTargetSeenZ);
                }
            } else if (prevState == WalkTowardState.instance() || prevState == WalkTowardNetworkState.instance()) {
                owner.setVariable("bPathFind", false);
                owner.setVariable("bMoving", true);
            }
        }
        if (owner instanceof IsoZombie) {
            IsoZombie isoZombie = (IsoZombie)owner;
            isoZombie.networkAi.isClimbing = false;
        }
    }

    public void slideX(IsoGameCharacter owner, float x, float multiplier) {
        float dx = multiplier * GameTime.getInstance().getThirtyFPSMultiplier();
        dx = x > owner.getX() ? Math.min(dx, x - owner.getX()) : Math.max(-dx, x - owner.getX());
        owner.setX(owner.getX() + dx);
        owner.setNextX(owner.getX());
    }

    public void slideY(IsoGameCharacter owner, float y, float multiplier) {
        float dy = multiplier * GameTime.getInstance().getThirtyFPSMultiplier();
        dy = y > owner.getY() ? Math.min(dy, y - owner.getY()) : Math.max(-dy, y - owner.getY());
        owner.setY(owner.getY() + dy);
        owner.setNextY(owner.getY());
    }

    @Override
    public void animEvent(IsoGameCharacter owner, AnimLayer layer, AnimationTrack track, AnimEvent event) {
        IsoMovingObject isoMovingObject;
        IsoPlayer player = Type.tryCastTo(owner, IsoPlayer.class);
        IsoZombie zombie = Type.tryCastTo(owner, IsoZombie.class);
        if (event.eventName.equalsIgnoreCase("CheckAttack") && zombie != null && (isoMovingObject = zombie.target) instanceof IsoGameCharacter) {
            IsoGameCharacter isoGameCharacter = (IsoGameCharacter)isoMovingObject;
            isoGameCharacter.attackFromWindowsLunge(zombie);
        }
        if (event.eventName.equalsIgnoreCase("OnFloor") && zombie != null) {
            boolean bOnFloor = Boolean.parseBoolean(event.parameterValue);
            owner.set(ZOMBIE_ON_FLOOR, bOnFloor);
            if (bOnFloor) {
                this.setLungeXVars(zombie);
                IsoThumpable windows = Type.tryCastTo(this.getWindow(owner), IsoThumpable.class);
                if (windows != null && windows.getSquare() != null && zombie.target != null) {
                    windows.health -= Rand.Next(10, 20);
                    if (windows.health <= 0) {
                        windows.destroy();
                    }
                }
                owner.setVariable("ClimbWindowFlopped", true);
            }
        }
        if (event.eventName.equalsIgnoreCase("PlayerVoiceSound")) {
            if (owner.getVariableBoolean("PlayerVoiceSound")) {
                return;
            }
            if (player == null) {
                return;
            }
            owner.setVariable("PlayerVoiceSound", true);
            player.playerVoiceSound(event.parameterValue);
        }
        if (event.eventName.equalsIgnoreCase("PlayWindowSound")) {
            if (!SoundManager.instance.isListenerInRange(owner.getX(), owner.getY(), 10.0f)) {
                return;
            }
            long instance = owner.getEmitter().playSoundImpl(event.parameterValue, null);
            owner.getEmitter().setParameterValue(instance, FMODManager.instance.getParameterDescription("TripObstacleType"), 9.0f);
        }
        if (event.eventName.equalsIgnoreCase("SetState")) {
            if (zombie == null) {
                return;
            }
            try {
                ParameterZombieState.State state = ParameterZombieState.State.valueOf(event.parameterValue);
                zombie.parameterZombieState.setState(state);
            }
            catch (IllegalArgumentException illegalArgumentException) {
                // empty catch block
            }
        }
    }

    @Override
    public boolean isIgnoreCollide(IsoGameCharacter owner, int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
        return true;
    }

    public IsoObject getWindow(IsoGameCharacter owner) {
        if (!owner.isCurrentState(this)) {
            return null;
        }
        int startX = owner.get(START_X);
        int startY = owner.get(START_Y);
        int z = owner.get(Z);
        IsoGridSquare startSq = IsoWorld.instance.currentCell.getGridSquare(startX, startY, z);
        int endX = owner.get(END_X);
        int endY = owner.get(END_Y);
        IsoGridSquare endSq = IsoWorld.instance.currentCell.getGridSquare(endX, endY, z);
        if (startSq == null || endSq == null) {
            return null;
        }
        IsoObject obj = startSq.getWindowTo(endSq);
        if (obj == null) {
            obj = startSq.getWindowThumpableTo(endSq);
        }
        if (obj == null) {
            obj = startSq.getHoppableTo(endSq);
        }
        return obj;
    }

    public boolean isWindowClosing(IsoGameCharacter owner) {
        if (owner.getVariableBoolean("ClimbWindowStarted")) {
            return false;
        }
        int startX = owner.get(START_X);
        int startY = owner.get(START_Y);
        int z = owner.get(Z);
        IsoGridSquare startSq = IsoWorld.instance.currentCell.getGridSquare(startX, startY, z);
        if (owner.getCurrentSquare() != startSq) {
            return false;
        }
        IsoObject isoObject = this.getWindow(owner);
        if (!(isoObject instanceof IsoWindow)) {
            return false;
        }
        IsoWindow window = (IsoWindow)isoObject;
        IsoGameCharacter chrClosing = window.getFirstCharacterClosing();
        if (chrClosing == null || !chrClosing.isVariable("CloseWindowOutcome", "success")) {
            return false;
        }
        if (owner.isZombie()) {
            owner.setHitReaction("HeadLeft");
        } else {
            owner.setVariable("ClimbWindowFinished", true);
        }
        return true;
    }

    @Override
    public void getDeltaModifiers(IsoGameCharacter owner, MoveDeltaModifiers modifiers) {
        boolean hasPath = owner.getPath2() != null;
        boolean isPlayer = owner instanceof IsoPlayer;
        if (hasPath && isPlayer) {
            modifiers.setMaxTurnDelta(10.0f);
        }
        if (isPlayer && owner.getVariableBoolean("isTurning")) {
            modifiers.setMaxTurnDelta(5.0f);
        }
    }

    private boolean isFreeSquare(IsoGridSquare square) {
        return square != null && square.TreatAsSolidFloor() && !square.has(IsoFlagType.solid) && !square.has(IsoFlagType.solidtrans);
    }

    private boolean isObstacleSquare(IsoGridSquare square) {
        return square != null && square.TreatAsSolidFloor() && !square.has(IsoFlagType.solid) && square.has(IsoFlagType.solidtrans) && !square.has(IsoFlagType.water);
    }

    private IsoGridSquare getFreeSquareAfterObstacles(IsoGridSquare square, IsoDirections dir) {
        IsoGridSquare square1;
        while ((square1 = square.getAdjacentSquare(dir)) != null && !square.isSomethingTo(square1) && square.getWindowFrameTo(square1) == null && square.getWindowThumpableTo(square1) == null) {
            if (this.isFreeSquare(square1)) {
                return square1;
            }
            if (!this.isObstacleSquare(square1)) {
                return null;
            }
            square = square1;
        }
        return null;
    }

    private void setLungeXVars(IsoZombie zombie) {
        IsoMovingObject target = zombie.getTarget();
        if (target == null) {
            return;
        }
        zombie.setVariable("FenceLungeX", 0.0f);
        zombie.setVariable("FenceLungeY", 0.0f);
        float lungeX = 0.0f;
        float forwardX = zombie.getForwardDirectionX();
        float forwardY = zombie.getForwardDirectionY();
        PZMath.SideOfLine side = PZMath.testSideOfLine(zombie.getX(), zombie.getY(), zombie.getX() + forwardX, zombie.getY() + forwardY, target.getX(), target.getY());
        float angleRad = (float)Math.acos(zombie.getDotWithForwardDirection(target.getX(), target.getY()));
        float angleDeg = PZMath.clamp(PZMath.radToDeg(angleRad), 0.0f, 90.0f);
        switch (side) {
            case Left: {
                lungeX = -angleDeg / 90.0f;
                break;
            }
            case OnLine: {
                lungeX = 0.0f;
                break;
            }
            case Right: {
                lungeX = angleDeg / 90.0f;
            }
        }
        zombie.setVariable("FenceLungeX", lungeX);
    }

    public boolean isPastInnerEdgeOfSquare(IsoGameCharacter owner, int x, int y, IsoDirections moveDir) {
        if (moveDir == IsoDirections.N) {
            return owner.getY() < (float)(y + 1) - 0.3f;
        }
        if (moveDir == IsoDirections.S) {
            return owner.getY() > (float)y + 0.3f;
        }
        if (moveDir == IsoDirections.W) {
            return owner.getX() < (float)(x + 1) - 0.3f;
        }
        if (moveDir == IsoDirections.E) {
            return owner.getX() > (float)x + 0.3f;
        }
        throw new IllegalArgumentException("unhandled direction");
    }

    public boolean isPastOuterEdgeOfSquare(IsoGameCharacter owner, int x, int y, IsoDirections moveDir) {
        if (moveDir == IsoDirections.N) {
            return owner.getY() < (float)y - 0.3f;
        }
        if (moveDir == IsoDirections.S) {
            return owner.getY() > (float)(y + 1) + 0.3f;
        }
        if (moveDir == IsoDirections.W) {
            return owner.getX() < (float)x - 0.3f;
        }
        if (moveDir == IsoDirections.E) {
            return owner.getX() > (float)(x + 1) + 0.3f;
        }
        throw new IllegalArgumentException("unhandled direction");
    }

    public void setParams(IsoGameCharacter owner, IsoObject obj) {
        IsoDirections dir;
        boolean north;
        owner.clear(this);
        boolean scratch = false;
        if (obj instanceof IsoWindow) {
            IsoWindow window = (IsoWindow)obj;
            north = window.isNorth();
            if (owner instanceof IsoPlayer && window.isDestroyed() && !window.isGlassRemoved() && Rand.Next(2) == 0) {
                scratch = true;
            }
        } else if (obj instanceof IsoThumpable) {
            IsoThumpable thumpable = (IsoThumpable)obj;
            north = thumpable.north;
            if (owner instanceof IsoPlayer && thumpable.getName().equals("Barbed Fence") && Rand.Next(101) > 75) {
                scratch = true;
            }
        } else if (obj instanceof IsoWindowFrame) {
            IsoWindowFrame windowFrame = (IsoWindowFrame)obj;
            north = windowFrame.getNorth();
        } else {
            throw new IllegalArgumentException("expected thumpable, window, or window-frame");
        }
        int x = obj.getSquare().getX();
        int y = obj.getSquare().getY();
        int z = obj.getSquare().getZ();
        int startX = x;
        int startY = y;
        int oppositeX = x;
        int oppositeY = y;
        if (north) {
            if ((float)y < owner.getY()) {
                --oppositeY;
                dir = IsoDirections.N;
            } else {
                --startY;
                dir = IsoDirections.S;
            }
        } else if ((float)x < owner.getX()) {
            --oppositeX;
            dir = IsoDirections.W;
        } else {
            --startX;
            dir = IsoDirections.E;
        }
        IsoGridSquare oppositeSq = IsoWorld.instance.currentCell.getGridSquare(oppositeX, oppositeY, z);
        boolean isCounter = oppositeSq != null && oppositeSq.has(IsoFlagType.solidtrans);
        boolean isFloor = oppositeSq != null && oppositeSq.TreatAsSolidFloor();
        boolean isSheetRope = oppositeSq != null && owner.canClimbDownSheetRope(oppositeSq);
        int endX = oppositeX;
        int endY = oppositeY;
        if (isCounter && owner.isZombie()) {
            IsoGridSquare square = oppositeSq.getAdjacentSquare(dir);
            if (!this.isFreeSquare(square) || oppositeSq.isSomethingTo(square) || oppositeSq.getWindowFrameTo(square) != null || oppositeSq.getWindowThumpableTo(square) != null) {
                isCounter = false;
            } else {
                endX = square.x;
                endY = square.y;
            }
        }
        if (isCounter && !owner.isZombie()) {
            IsoGridSquare freeSq = this.getFreeSquareAfterObstacles(oppositeSq, dir);
            if (freeSq == null) {
                isCounter = false;
            } else {
                endX = freeSq.x;
                endY = freeSq.y;
            }
        }
        owner.set(START_X, startX);
        owner.set(START_Y, startY);
        owner.set(Z, z);
        owner.set(OPPOSITE_X, oppositeX);
        owner.set(OPPOSITE_Y, oppositeY);
        owner.set(END_X, endX);
        owner.set(END_Y, endY);
        owner.set(DIR, dir);
        owner.set(ZOMBIE_ON_FLOOR, false);
        owner.set(PREV_STATE, owner.getCurrentState());
        owner.set(SCRATCH, scratch);
        owner.set(COUNTER, isCounter);
        owner.set(SOLID_FLOOR, isFloor);
        owner.set(SHEET_ROPE, isSheetRope);
        owner.set(SCRATCHED, scratch);
    }

    @Override
    public boolean isProcessedOnEnter() {
        return true;
    }

    @Override
    public void processOnEnter(IsoGameCharacter owner, Map<Object, Object> delegate) {
        if (SCRATCH.fromDelegate(delegate).booleanValue()) {
            owner.getBodyDamage().setScratchedWindow();
        }
    }
}

