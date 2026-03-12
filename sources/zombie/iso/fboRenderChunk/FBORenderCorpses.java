/*
 * Decompiled with CFR 0.152.
 */
package zombie.iso.fboRenderChunk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Stack;
import org.lwjgl.opengl.GL11;
import zombie.characters.AttachedItems.AttachedModelNames;
import zombie.core.Core;
import zombie.core.SpriteRenderer;
import zombie.core.math.PZMath;
import zombie.core.skinnedmodel.IGrappleable;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.animation.AnimationPlayer;
import zombie.core.skinnedmodel.animation.AnimationTrack;
import zombie.core.skinnedmodel.animation.TwistableBoneTransform;
import zombie.core.skinnedmodel.model.VertexBufferObject;
import zombie.core.skinnedmodel.visual.BaseVisual;
import zombie.core.skinnedmodel.visual.ItemVisuals;
import zombie.core.textures.ColorInfo;
import zombie.core.textures.TextureDraw;
import zombie.iso.IsoCamera;
import zombie.iso.IsoDepthHelper;
import zombie.iso.IsoDirections;
import zombie.iso.IsoGridSquare;
import zombie.iso.IsoObject;
import zombie.iso.Vector2;
import zombie.iso.fboRenderChunk.FBORenderChunk;
import zombie.iso.fboRenderChunk.FBORenderChunkCamera;
import zombie.iso.fboRenderChunk.FBORenderChunkManager;
import zombie.iso.objects.IsoDeadBody;
import zombie.iso.objects.IsoMannequin;
import zombie.util.Pool;
import zombie.util.StringUtils;
import zombie.util.list.PZArrayUtil;

public final class FBORenderCorpses {
    private static FBORenderCorpses instance;
    private final Vector2 tempVector2 = new Vector2();
    private final Stack<RenderJob> jobPool = new Stack();
    private final ArrayList<RenderJob> renderJobs = new ArrayList();
    private static final ChunkCamera chunkCamera;

    public static FBORenderCorpses getInstance() {
        if (instance == null) {
            instance = new FBORenderCorpses();
        }
        return instance;
    }

    public void render(int renderChunkIndex, IsoDeadBody body) {
        RenderJob renderJob = RenderJob.getNew();
        renderJob.renderChunkIndex = renderChunkIndex;
        BodyParams bodyParams = new BodyParams();
        bodyParams.init(body);
        renderJob.init(bodyParams);
        if (!body.isAnimal()) {
            boolean bRagdoll = body.ragdollFall && !PZArrayUtil.isNullOrEmpty(body.getDiedBoneTransforms());
            renderJob.animatedModel.calculateShadowParams(body.getShadowParams(), bRagdoll);
        }
        this.renderJobs.add(renderJob);
    }

    public void render(int renderChunkIndex, IsoMannequin body) {
        RenderJob renderJob = RenderJob.getNew();
        renderJob.renderChunkIndex = renderChunkIndex;
        BodyParams bodyParams = new BodyParams();
        bodyParams.init(body);
        renderJob.init(bodyParams);
        this.renderJobs.add(renderJob);
    }

    public void update() {
        for (int i = 0; i < this.renderJobs.size(); ++i) {
            RenderJob job = this.renderJobs.get(i);
            if (job.done == 1 && job.renderRefCount > 0) continue;
            if (job.done == 1 && job.renderRefCount == 0) {
                this.renderJobs.remove(i--);
                assert (!this.jobPool.contains(job));
                this.jobPool.push(job);
                continue;
            }
            FBORenderChunk renderChunk = FBORenderChunkManager.instance.chunks.get(job.renderChunkIndex);
            if (renderChunk == null) {
                job.done = 1;
                continue;
            }
            if (!job.renderMain()) continue;
            ++job.renderRefCount;
            if (!FBORenderChunkManager.instance.toRenderThisFrame.contains(renderChunk)) {
                FBORenderChunkManager.instance.toRenderThisFrame.add(renderChunk);
            }
            int playerIndex = IsoCamera.frameState.playerIndex;
            SpriteRenderer.instance.glDoEndFrame();
            SpriteRenderer.instance.glDoStartFrameFlipY(renderChunk.w, renderChunk.h, 1.0f, playerIndex);
            renderChunk.beginMainThread(false);
            SpriteRenderer.instance.drawGeneric(job);
            renderChunk.endMainThread();
            SpriteRenderer.instance.glDoEndFrame();
            SpriteRenderer.instance.glDoStartFrame(Core.getInstance().getScreenWidth(), Core.getInstance().getScreenHeight(), Core.getInstance().getCurrentPlayerZoom(), playerIndex);
        }
    }

    public void Reset() {
        this.jobPool.forEach(RenderJob::Reset);
        this.jobPool.clear();
        this.renderJobs.clear();
    }

    static {
        chunkCamera = new ChunkCamera();
    }

    private static final class RenderJob
    extends TextureDraw.GenericDrawer {
        public int renderChunkIndex;
        public final BodyParams body = new BodyParams();
        public AnimatedModel animatedModel;
        public float animPlayerAngle;
        public int done;
        public int renderRefCount;

        private RenderJob() {
        }

        public static RenderJob getNew() {
            if (FBORenderCorpses.instance.jobPool.isEmpty()) {
                return new RenderJob();
            }
            return FBORenderCorpses.instance.jobPool.pop();
        }

        public RenderJob init(BodyParams body) {
            this.body.init(body);
            if (this.animatedModel == null) {
                this.animatedModel = new AnimatedModel();
                this.animatedModel.setAnimate(false);
            }
            this.animatedModel.setAnimSetName(body.animSetName);
            this.animatedModel.setState(body.stateName);
            this.animatedModel.setPrimaryHandModelName(body.primaryHandItem);
            this.animatedModel.setSecondaryHandModelName(body.secondaryHandItem);
            this.animatedModel.setAttachedModelNames(body.attachedModelNames);
            this.animatedModel.setAmbient(body.ambient, body.outside, body.room);
            this.animatedModel.setLights(body.lights, body.x, body.y, body.z);
            this.animatedModel.setCullFace(1029);
            this.animatedModel.setVariable("FallOnFront", body.fallOnFront);
            this.animatedModel.setVariable("KilledByFall", body.killedByFall);
            body.variables.forEach((k, v) -> this.animatedModel.setVariable((String)k, (String)v));
            this.animatedModel.setModelData(body.baseVisual, body.itemVisuals);
            this.animatedModel.setAngle(FBORenderCorpses.instance.tempVector2.setLengthAndDirection(this.body.angle, 1.0f));
            if (body.ragdollFall && !PZArrayUtil.isNullOrEmpty(body.diedBoneTransforms)) {
                this.setRagdollDeathPose();
            }
            this.animatedModel.setTrackTime(body.trackTime);
            this.animatedModel.setGrappleable(body.grappleable);
            this.animatedModel.update();
            this.done = 0;
            this.renderRefCount = 0;
            return this;
        }

        private void setRagdollDeathPose() {
            AnimationPlayer animationPlayer = this.animatedModel.getAnimationPlayer();
            animationPlayer.stopAll();
            AnimationTrack animationTrack = animationPlayer.play("DeathPose", true, true, -1.0f);
            if (animationTrack != null) {
                animationTrack.setBlendWeight(1.0f);
                animationTrack.initRagdollTransforms(this.body.diedBoneTransforms);
            }
        }

        public boolean renderMain() {
            if (this.animatedModel.isReadyToRender()) {
                this.animatedModel.setTint(this.body.tintR, this.body.tintG, this.body.tintB);
                this.animatedModel.renderMain();
                this.animPlayerAngle = this.animatedModel.getAnimationPlayer().getRenderedAngle();
                return true;
            }
            return false;
        }

        @Override
        public void render() {
            if (this.done == 1) {
                return;
            }
            GL11.glDepthMask(true);
            GL11.glColorMask(true, true, true, true);
            this.animatedModel.setOffsetWhileRendering(0.0f, 0.0f, 0.0f);
            FBORenderChunk renderChunk = (FBORenderChunk)SpriteRenderer.instance.getRenderingState().cachedRenderChunkIndexMap.get(this.renderChunkIndex);
            chunkCamera.set(renderChunk, this.body.x, this.body.y, this.body.z, this.animPlayerAngle);
            FBORenderCorpses.chunkCamera.animatedModel = this.animatedModel;
            if (renderChunk.highRes) {
                this.animatedModel.setHighResDepthMultiplier(0.5f);
            }
            this.animatedModel.DoRender(chunkCamera);
            if (renderChunk.highRes) {
                this.animatedModel.setHighResDepthMultiplier(0.0f);
            }
            if (this.animatedModel.isRendered()) {
                this.done = 1;
            }
        }

        @Override
        public void postRender() {
            if (this.animatedModel == null) {
                return;
            }
            this.animatedModel.postRender(this.done == 1);
            assert (this.renderRefCount > 0);
            --this.renderRefCount;
        }

        public void Reset() {
            this.body.Reset();
            if (this.animatedModel != null) {
                this.animatedModel.releaseAnimationPlayer();
                this.animatedModel = null;
            }
        }
    }

    private static final class BodyParams {
        BaseVisual baseVisual;
        final ItemVisuals itemVisuals = new ItemVisuals();
        IsoDirections dir;
        float angle;
        boolean female;
        boolean zombie;
        boolean skeleton;
        String animSetName;
        String stateName;
        final HashMap<String, String> variables = new HashMap();
        TwistableBoneTransform[] diedBoneTransforms;
        boolean standing;
        String primaryHandItem;
        String secondaryHandItem;
        final AttachedModelNames attachedModelNames = new AttachedModelNames();
        float x;
        float y;
        float z;
        float trackTime;
        boolean outside;
        boolean room;
        final ColorInfo ambient = new ColorInfo();
        boolean fallOnFront;
        boolean killedByFall;
        boolean ragdollFall;
        final IsoGridSquare.ResultLight[] lights = new IsoGridSquare.ResultLight[5];
        float tintR = 1.0f;
        float tintG = 1.0f;
        float tintB = 1.0f;
        IGrappleable grappleable;

        BodyParams() {
            for (int i = 0; i < this.lights.length; ++i) {
                this.lights[i] = new IsoGridSquare.ResultLight();
            }
        }

        void init(BodyParams body) {
            this.baseVisual = body.baseVisual;
            this.itemVisuals.clear();
            this.itemVisuals.addAll(body.itemVisuals);
            this.dir = body.dir;
            this.angle = body.angle;
            this.female = body.female;
            this.zombie = body.zombie;
            this.skeleton = body.skeleton;
            this.animSetName = body.animSetName;
            this.stateName = body.stateName;
            this.variables.clear();
            this.variables.putAll(body.variables);
            this.standing = body.standing;
            this.primaryHandItem = body.primaryHandItem;
            this.secondaryHandItem = body.secondaryHandItem;
            this.attachedModelNames.copyFrom(body.attachedModelNames);
            this.x = body.x;
            this.y = body.y;
            this.z = body.z;
            this.trackTime = body.trackTime;
            this.fallOnFront = body.fallOnFront;
            this.killedByFall = body.killedByFall;
            this.ragdollFall = body.ragdollFall;
            this.outside = body.outside;
            this.room = body.room;
            this.tintR = body.tintR;
            this.tintG = body.tintG;
            this.tintB = body.tintB;
            this.ambient.set(body.ambient.r, body.ambient.g, body.ambient.b, 1.0f);
            this.grappleable = body.grappleable;
            for (int i = 0; i < this.lights.length; ++i) {
                this.lights[i].copyFrom(body.lights[i]);
            }
            this.diedBoneTransforms = Pool.tryRelease(this.diedBoneTransforms);
            this.diedBoneTransforms = PZArrayUtil.clone(body.diedBoneTransforms, TwistableBoneTransform::alloc, TwistableBoneTransform::set);
        }

        void init(IsoDeadBody body) {
            int playerIndex = IsoCamera.frameState.playerIndex;
            this.baseVisual = body.getVisual();
            body.getItemVisuals(this.itemVisuals);
            this.dir = body.dir;
            this.angle = body.getAngle();
            this.female = body.isFemale();
            this.zombie = body.isZombie();
            this.skeleton = body.isSkeleton();
            this.primaryHandItem = null;
            this.secondaryHandItem = null;
            this.attachedModelNames.initFrom(body.getAttachedItems());
            this.animSetName = "zombie";
            this.stateName = body.isBeingGrappled() ? "grappled" : "onground";
            this.variables.clear();
            this.grappleable = body;
            this.standing = false;
            this.trackTime = 0.0f;
            if (body.getPrimaryHandItem() != null || body.getSecondaryHandItem() != null) {
                if (body.getPrimaryHandItem() != null && !StringUtils.isNullOrEmpty(body.getPrimaryHandItem().getStaticModel())) {
                    this.primaryHandItem = body.getPrimaryHandItem().getStaticModel();
                }
                if (body.getSecondaryHandItem() != null && !StringUtils.isNullOrEmpty(body.getSecondaryHandItem().getStaticModel())) {
                    this.secondaryHandItem = body.getSecondaryHandItem().getStaticModel();
                }
                this.animSetName = "player";
                this.stateName = "deadbody";
                if (body.isKilledByFall()) {
                    this.trackTime = 1.0f;
                }
            } else if (!body.isAnimal() && !body.isZombie() && body.isKilledByFall()) {
                this.animSetName = "player";
                this.stateName = "deadbody";
                this.trackTime = 1.0f;
            }
            if (body.isAnimal()) {
                this.animSetName = body.animalAnimSet;
                this.stateName = "deadbody";
            }
            this.x = body.getX();
            this.y = body.getY();
            this.z = body.getZ();
            this.fallOnFront = body.isFallOnFront();
            this.killedByFall = body.isKilledByFall();
            this.ragdollFall = body.ragdollFall;
            this.diedBoneTransforms = Pool.tryRelease(this.diedBoneTransforms);
            this.diedBoneTransforms = PZArrayUtil.clone(body.getDiedBoneTransforms(), TwistableBoneTransform::alloc, TwistableBoneTransform::set);
            this.outside = body.square != null && body.square.isOutside();
            boolean bl = this.room = body.square != null && body.square.getRoom() != null;
            if (body.isHighlighted(playerIndex)) {
                this.tintR = body.getHighlightColor((int)playerIndex).r;
                this.tintG = body.getHighlightColor((int)playerIndex).g;
                this.tintB = body.getHighlightColor((int)playerIndex).b;
            } else {
                this.tintB = 1.0f;
                this.tintG = 1.0f;
                this.tintR = 1.0f;
            }
            this.initAmbient(body.square);
            this.initLights(body.square);
        }

        void init(IsoMannequin body) {
            int playerIndex = IsoCamera.frameState.playerIndex;
            this.baseVisual = body.getHumanVisual();
            body.getItemVisuals(this.itemVisuals);
            this.dir = body.dir;
            this.angle = this.dir.ToVector().getDirection();
            this.female = body.isFemale();
            this.zombie = body.isZombie();
            this.skeleton = body.isSkeleton();
            this.primaryHandItem = null;
            this.secondaryHandItem = null;
            this.attachedModelNames.clear();
            this.animSetName = body.getAnimSetName();
            this.stateName = body.getAnimStateName();
            this.variables.clear();
            body.getVariables(this.variables);
            this.standing = true;
            this.x = body.getX() + 0.5f;
            this.y = body.getY() + 0.5f;
            this.z = body.getZ();
            if (body.getObjectIndex() != -1) {
                IsoObject object;
                IsoObject[] objects = body.square.getObjects().getElements();
                for (int i = 0; i < body.square.getObjects().size() && (object = objects[i]) != body; ++i) {
                    if (!object.isTableSurface()) continue;
                    this.z += (object.getSurfaceOffset() + 1.0f) / 96.0f;
                }
            }
            this.trackTime = 0.0f;
            this.fallOnFront = false;
            this.ragdollFall = false;
            this.killedByFall = false;
            this.diedBoneTransforms = Pool.tryRelease(this.diedBoneTransforms);
            this.outside = body.square != null && body.square.isOutside();
            boolean bl = this.room = body.square != null && body.square.getRoom() != null;
            if (body.isHighlighted(playerIndex)) {
                this.tintR = body.getHighlightColor((int)playerIndex).r;
                this.tintG = body.getHighlightColor((int)playerIndex).g;
                this.tintB = body.getHighlightColor((int)playerIndex).b;
            } else {
                this.tintB = 1.0f;
                this.tintG = 1.0f;
                this.tintR = 1.0f;
            }
            this.initAmbient(body.square);
            this.initLights(null);
        }

        void initAmbient(IsoGridSquare square) {
            this.ambient.set(1.0f, 1.0f, 1.0f, 1.0f);
            if (square != null) {
                square.interpolateLight(this.ambient, this.x % 1.0f, this.y % 1.0f);
            }
        }

        void initLights(IsoGridSquare square) {
            for (int i = 0; i < this.lights.length; ++i) {
                this.lights[i].radius = 0;
            }
            if (square != null) {
                IsoGridSquare.ILighting lighting = square.lighting[0];
                int lightCount = lighting.resultLightCount();
                for (int i = 0; i < lightCount; ++i) {
                    this.lights[i].copyFrom(lighting.getResultLight(i));
                }
            }
        }

        void Reset() {
            this.baseVisual = null;
            this.itemVisuals.clear();
            Arrays.fill(this.lights, null);
            this.diedBoneTransforms = Pool.tryRelease(this.diedBoneTransforms);
        }
    }

    private static final class ChunkCamera
    extends FBORenderChunkCamera {
        AnimatedModel animatedModel;

        private ChunkCamera() {
        }

        @Override
        public void Begin() {
            super.Begin();
            float targetDepth = IsoDepthHelper.calculateDepth(this.x, this.y, this.z);
            if (!this.renderChunk.chunk.containsPoint(this.x, this.y)) {
                float camCharacterX = Core.getInstance().floatParamMap.get(0).floatValue();
                float camCharacterY = Core.getInstance().floatParamMap.get(1).floatValue();
                int chunksPerWidth = 8;
                targetDepth = IsoDepthHelper.getSquareDepthData((int)PZMath.fastfloor((float)camCharacterX), (int)PZMath.fastfloor((float)camCharacterY), (float)this.x, (float)this.y, (float)this.z).depthStart;
                float chunkDepth = IsoDepthHelper.getChunkDepthData((int)PZMath.fastfloor((float)(camCharacterX / 8.0f)), (int)PZMath.fastfloor((float)(camCharacterY / 8.0f)), (int)this.renderChunk.chunk.wx, (int)this.renderChunk.chunk.wy, (int)PZMath.fastfloor((float)this.z)).depthStart;
                targetDepth -= chunkDepth;
            }
            float depthBufferValue = VertexBufferObject.getDepthValueAt(0.0f, 0.0f, 0.0f);
            targetDepth = targetDepth - (depthBufferValue + 1.0f) / 2.0f + 0.5f;
            this.animatedModel.setTargetDepth(targetDepth);
        }
    }
}

