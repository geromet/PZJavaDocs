/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.rendering;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GL44;
import zombie.GameProfiler;
import zombie.core.ShaderHelper;
import zombie.core.opengl.GLStateRenderThread;
import zombie.core.opengl.PZGLUtil;
import zombie.core.opengl.ShaderProgram;
import zombie.core.rendering.ShaderPropertyBlock;
import zombie.core.skinnedmodel.advancedanimation.AnimatedModel;
import zombie.core.skinnedmodel.model.Model;
import zombie.core.skinnedmodel.model.ModelInstanceRenderData;
import zombie.core.skinnedmodel.model.ModelMesh;
import zombie.core.skinnedmodel.model.ModelSlotRenderData;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.core.skinnedmodel.shader.ShaderManager;
import zombie.core.textures.SmartTexture;
import zombie.core.textures.Texture;

public class RenderList {
    private static final HashMap<Model, QueuedModelList> OpaqueLists = new HashMap();
    private static final HashMap<Model, QueuedModelList> TransparentLists = new HashMap();
    private static final ArrayList<Texture> UniqueTextures = new ArrayList();
    private static final ArrayList<ArrayTexture> ATPool = new ArrayList();
    private static final ArrayDeque<RenderData> RDPool = new ArrayDeque();
    private static final QueuedModelList ImmediateModel = new QueuedModelList(null);

    public static void DrawQueued(ModelSlotRenderData slotData, AnimatedModel.AnimatedModelInstanceRenderData instData) {
        Model model = instData.modelInstance.model;
        QueuedModelList drawList = OpaqueLists.getOrDefault(model, null);
        if (drawList == null) {
            drawList = new QueuedModelList(model);
            OpaqueLists.put(model, drawList);
        }
        drawList.list.add(RenderList.GetRenderData(slotData, instData));
    }

    public static void DrawImmediate(ModelSlotRenderData slotData, AnimatedModel.AnimatedModelInstanceRenderData instData) {
        int program = GL43.glGetInteger(35725);
        RenderList.ImmediateModel.key = instData.modelInstance.model;
        RenderList.ImmediateModel.list.add(RenderList.GetRenderData(slotData, instData));
        ImmediateModel.Draw();
        ImmediateModel.Reset();
        GL43.glBindBuffer(37074, 0);
        GL43.glUseProgram(program);
    }

    private static RenderData GetRenderData(ModelSlotRenderData slotData, AnimatedModel.AnimatedModelInstanceRenderData instData) {
        RenderData rd;
        if (RDPool.isEmpty()) {
            rd = new RenderData(slotData, instData);
        } else {
            rd = RDPool.removeFirst();
            rd.slotData = slotData;
            rd.instData = instData;
        }
        return rd;
    }

    private static int CompareOpaque(RenderData data1, RenderData data2) {
        return data1.mesh.hashCode() - data2.mesh.hashCode();
    }

    public static void SortOpaque() {
        ArrayList list = null;
        list.sort(RenderList::CompareOpaque);
    }

    private static int CompareTransparent(RenderData data1, RenderData data2) {
        return data1.mesh.hashCode() - data2.mesh.hashCode();
    }

    public static void SortTransparent() {
        ArrayList list = null;
        list.sort(RenderList::CompareTransparent);
    }

    private static <K, V extends BaseDrawList> void Render(HashMap<K, V> list) {
        if (list.isEmpty()) {
            return;
        }
        int program = GL43.glGetInteger(35725);
        list.forEach(RenderList::RenderLists);
        GL43.glBindBuffer(37074, 0);
        ShaderHelper.glUseProgramObjectARB(program);
    }

    public static void RenderOpaque() {
        GL43.glPushClientAttrib(-1);
        GL43.glPushAttrib(1048575);
        GL43.glEnable(2884);
        GL43.glCullFace(1028);
        GL43.glEnable(2929);
        GL43.glDepthFunc(513);
        GL43.glDepthMask(true);
        GL43.glDepthRange(0.0, 1.0);
        GL43.glEnable(3008);
        GL43.glAlphaFunc(516, 0.01f);
        GL43.glBlendFunc(770, 771);
        try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("Render Opaque");){
            RenderList.Render(OpaqueLists);
        }
        GL43.glPopAttrib();
        GL43.glPopClientAttrib();
        Model.SwapInstancedBasic();
        GLStateRenderThread.restore();
    }

    public static void RenderTransparent() {
        GL43.glPushClientAttrib(-1);
        GL43.glPushAttrib(1048575);
        GL43.glEnable(2884);
        GL43.glCullFace(1028);
        GL43.glEnable(2929);
        GL43.glDepthFunc(513);
        GL43.glDepthMask(false);
        GL43.glDepthRange(0.0, 1.0);
        GL43.glEnable(3008);
        GL43.glAlphaFunc(516, 0.01f);
        GL43.glBlendFunc(770, 771);
        try (GameProfiler.ProfileArea profileArea = GameProfiler.getInstance().profile("Render Transparent");){
            RenderList.Render(TransparentLists);
        }
        GL43.glPopAttrib();
        GL43.glPopClientAttrib();
        GLStateRenderThread.restore();
    }

    public static void Reset() {
        OpaqueLists.forEach(RenderList::ResetLists);
        TransparentLists.forEach(RenderList::ResetLists);
        RenderList.RemoveEmptyLists(OpaqueLists);
        RenderList.RemoveEmptyLists(TransparentLists);
    }

    public static void ResetOpaque() {
        OpaqueLists.forEach(RenderList::ResetLists);
        RenderList.RemoveEmptyLists(OpaqueLists);
    }

    public static void ResetTransparent() {
        TransparentLists.forEach(RenderList::ResetLists);
        RenderList.RemoveEmptyLists(TransparentLists);
    }

    private static <K> void RenderLists(K key, BaseDrawList drawList) {
        drawList.Draw();
    }

    private static <K, L extends BaseDrawList> void ResetLists(K key, L drawList) {
        drawList.Reset();
    }

    private static <K, L extends BaseDrawList> void RemoveEmptyLists(HashMap<K, L> map) {
        Set<Map.Entry<K, L>> set = map.entrySet();
        set.removeIf(list -> ((BaseDrawList)list.getValue()).IsEmpty());
    }

    private static class QueuedModelList
    extends DrawList<Model, RenderData> {
        public QueuedModelList(Model model) {
            super(model);
            this.list = new ArrayList();
        }

        @Override
        protected void DrawSingular() {
            GL43.glDisable(2884);
            for (RenderData data : this.list) {
                AnimatedModel.AnimatedModelInstanceRenderData animatedModelInstanceRenderData = data.instData;
                if (animatedModelInstanceRenderData instanceof ModelInstanceRenderData) {
                    ModelInstanceRenderData modelInstanceRenderData = (ModelInstanceRenderData)animatedModelInstanceRenderData;
                    ((Model)this.key).effect.startCharacter(data.slotData, modelInstanceRenderData);
                }
                ((Model)this.key).effect.instancedData.PushUniforms(data.instData.properties);
                ((Model)this.key).mesh.Draw(((Model)this.key).effect);
            }
        }

        @Override
        protected Texture GetTexture(int index) {
            RenderData data = (RenderData)this.list.get(index);
            Texture tex = data.instData.tex;
            if (tex == null) {
                tex = data.instData.modelInstance.tex;
            }
            if (tex == null && data.instData.model != null) {
                tex = data.instData.model.tex;
            }
            return tex;
        }

        @Override
        protected ShaderPropertyBlock GetProperties(int index) {
            RenderData data = (RenderData)this.list.get(index);
            if (((Model)this.key).effect.isInstanced()) {
                data.instData.properties.SetShader(((Model)this.key).effect);
            }
            return data.instData.properties;
        }

        @Override
        protected Shader GetEffect() {
            if (((Model)this.key).effect == null) {
                ((Model)this.key).effect = ShaderManager.instance.getOrCreateShader("basicEffect", ((Model)this.key).isStatic, true);
            }
            return ((Model)this.key).effect;
        }

        @Override
        protected void DrawCount(int count) {
            ((Model)this.key).mesh.DrawInstanced(((Model)this.key).effect, count);
        }

        @Override
        protected void OnComplete(int index) {
            RenderData data = (RenderData)this.list.get(index);
            RDPool.add(data);
        }

        @Override
        protected void UpdateProperties(int index) {
        }
    }

    private static class RenderData {
        public ModelMesh mesh;
        public ModelSlotRenderData slotData;
        public AnimatedModel.AnimatedModelInstanceRenderData instData;
        public float z;

        public RenderData(ModelSlotRenderData slotData, AnimatedModel.AnimatedModelInstanceRenderData instData) {
            this.slotData = slotData;
            this.instData = instData;
        }
    }

    private static abstract class BaseDrawList {
        private BaseDrawList() {
        }

        protected abstract int Count();

        public abstract boolean IsEmpty();

        public abstract void Draw();

        protected abstract void DrawCount(int var1);

        protected abstract void DrawSingular();

        protected void DrawInstanced() {
            Shader shader = this.GetEffect();
            ShaderPropertyBlock first = this.GetProperties(0);
            PZGLUtil.checkGLError(true);
            shader.instancedData.PushUniforms(first);
            PZGLUtil.checkGLError(true);
            int totalCount = this.Count();
            int counter = 0;
            while (counter < totalCount) {
                int count = Math.min(128, totalCount - counter);
                int end = counter + count;
                UniqueTextures.clear();
                GameProfiler profiler = GameProfiler.getInstance();
                try (GameProfiler.ProfileArea profileArea = profiler.profile("Push Data");){
                    this.PushData(counter, end);
                }
                counter = end;
                ArrayTexture at = this.GetTextureArray();
                ShaderProgram program = shader.getShaderProgram();
                ShaderProgram.Uniform uniform = program.getUniform("InstTexArr", 36289);
                if (uniform != null) {
                    GL44.glActiveTexture(33984 + uniform.sampler);
                } else {
                    GL44.glActiveTexture(33984);
                }
                try (GameProfiler.ProfileArea profileArea = profiler.profile("Copy Textures");){
                    BaseDrawList.CopyTextures(at);
                }
                at.Bind();
                if (uniform != null) {
                    GL44.glUniform1i(uniform.loc, uniform.sampler);
                }
                if ((uniform = program.getUniform("TextureDimensions", 35664)) != null) {
                    GL44.glUniform2f(uniform.loc, at.width, at.height);
                }
                profileArea = profiler.profile("Update Instanced");
                try {
                    this.UpdateData();
                }
                finally {
                    if (profileArea != null) {
                        profileArea.close();
                    }
                }
                profileArea = profiler.profile("Draw Instanced");
                try {
                    this.DrawCount(count);
                }
                finally {
                    if (profileArea == null) continue;
                    profileArea.close();
                }
            }
            shader.End();
        }

        protected abstract Texture GetTexture(int var1);

        protected abstract ShaderPropertyBlock GetProperties(int var1);

        protected abstract Shader GetEffect();

        public abstract void Reset();

        protected final void UpdateData() {
            Shader shader = this.GetEffect();
            shader.instancedData.UpdateData();
        }

        protected ArrayTexture GetTextureArray() {
            int atWidth = 1;
            int atHeight = 1;
            for (Texture tex : UniqueTextures) {
                if (tex == null) continue;
                atWidth = Math.max(atWidth, tex.getWidth());
                atHeight = Math.max(atHeight, tex.getHeight());
            }
            for (ArrayTexture at : ATPool) {
                if (at.width != atWidth || at.height < atHeight) continue;
                if (at.length < UniqueTextures.size()) {
                    at.length = UniqueTextures.size();
                    at.Recreate();
                }
                return at;
            }
            ArrayTexture newAT = new ArrayTexture(atWidth, atHeight, UniqueTextures.size());
            ATPool.add(newAT);
            return newAT;
        }

        protected void OnComplete(int index) {
        }

        protected abstract void UpdateProperties(int var1);

        private void WriteToBuffer(ShaderPropertyBlock properties) {
            Shader shader = this.GetEffect();
            shader.instancedData.PushInstanced(properties);
        }

        protected final void PushData(int counter, int end) {
            while (counter < end) {
                int id;
                Texture tex = this.GetTexture(counter);
                ShaderPropertyBlock properties = this.GetProperties(counter);
                if (tex instanceof SmartTexture) {
                    SmartTexture smart = (SmartTexture)tex;
                    smart.getID();
                    tex = smart.result;
                }
                if ((id = UniqueTextures.indexOf(tex)) == -1) {
                    id = UniqueTextures.size();
                    UniqueTextures.add(tex);
                }
                properties.SetInt("textureIndex", id);
                if (tex == null) {
                    properties.SetVector2("textureSize", 0.0f, 0.0f);
                } else {
                    properties.SetVector2("textureSize", tex.getWidth(), tex.getHeight());
                }
                GameProfiler profiler = GameProfiler.getInstance();
                try (GameProfiler.ProfileArea profileArea = profiler.profile("Update Properties");){
                    this.UpdateProperties(counter);
                }
                profileArea = profiler.profile("Write to Buffer");
                try {
                    this.WriteToBuffer(properties);
                }
                finally {
                    if (profileArea != null) {
                        profileArea.close();
                    }
                }
                this.OnComplete(counter);
                ++counter;
            }
        }

        protected static void CopyTextures(ArrayTexture at) {
            for (int i = 0; i < UniqueTextures.size(); ++i) {
                at.Copy(UniqueTextures.get(i), i);
            }
        }
    }

    private static class ArrayTexture {
        private static final float[] White = new float[]{1.0f, 1.0f, 1.0f, 1.0f};
        public int texture = GL44.glGenTextures();
        public int width;
        public int height;
        public int length;

        public ArrayTexture(int width, int height, int length) {
            this.width = width;
            this.height = height;
            this.length = length;
            this.Init();
        }

        private void Init() {
            GL44.glBindTexture(35866, this.texture);
            GL44.glTexImage3D(35866, 0, 32856, this.width, this.height, this.length, 0, 6408, 5121, 0L);
            GL44.glTexParameteri(35866, 10240, 9729);
            GL44.glTexParameteri(35866, 10241, 9729);
            GL44.glTexParameteri(35866, 10242, 33071);
            GL44.glTexParameteri(35866, 10243, 33071);
            GL44.glTexParameteri(35866, 32882, 33071);
        }

        public void Recreate() {
            this.Init();
        }

        public void Copy(Texture from, int index) {
            if (from == null || from.isDestroyed() || !from.isValid() || !from.isReady() || from.getID() == -1) {
                GL44.glClearTexSubImage(this.texture, 0, 0, 0, index, this.width, this.height, 1, 6408, 5126, White);
            } else {
                int id = from.getID();
                int x = Math.max(0, from.getX());
                int y = Math.max(0, from.getY());
                GL44.glCopyImageSubData(id, 3553, 0, x, y, 0, this.texture, 35866, 0, 0, 0, index, from.getWidth(), from.getHeight(), 1);
            }
        }

        public void Bind() {
            GL44.glBindTexture(35866, this.texture);
        }
    }

    private static abstract class DrawList<Key, Value>
    extends BaseDrawList {
        public Key key;
        public ArrayList<Value> list;

        public DrawList(Key key) {
            this.key = key;
            this.list = new ArrayList();
        }

        public String toString() {
            return String.format("Draw List: %1s - %2s", this.key, this.list.size());
        }

        @Override
        protected final int Count() {
            return this.list.size();
        }

        @Override
        public boolean IsEmpty() {
            return this.list.isEmpty();
        }

        @Override
        public void Draw() {
            if (this.list.isEmpty()) {
                return;
            }
            Shader effect = this.GetEffect();
            effect.Start();
            if (effect.isInstanced()) {
                this.DrawInstanced();
            } else {
                this.DrawSingular();
            }
            effect.End();
        }

        @Override
        public final void Reset() {
            this.list.clear();
        }
    }
}

