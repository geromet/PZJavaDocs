/*
 * Decompiled with CFR 0.152.
 */
package zombie.core.skinnedmodel.model;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;
import zombie.core.Core;
import zombie.core.VBO.IGLBufferObject;
import zombie.core.opengl.RenderThread;
import zombie.core.opengl.ShaderProgram;
import zombie.core.skinnedmodel.model.VertexPositionNormalTangentTexture;
import zombie.core.skinnedmodel.model.VertexPositionNormalTangentTextureSkin;
import zombie.core.skinnedmodel.shader.Shader;
import zombie.debug.DebugLog;
import zombie.debug.DebugOptions;
import zombie.util.list.PZArrayUtil;

public final class VertexBufferObject {
    public static IGLBufferObject funcs;
    int[] elements;
    Vbo handle;
    private final VertexFormat vertexFormat;
    private final BeginMode beginMode;
    public boolean isStatic;

    public VertexBufferObject() {
        this.isStatic = false;
        this.vertexFormat = new VertexFormat(4);
        this.vertexFormat.setElement(0, VertexType.VertexArray, 12);
        this.vertexFormat.setElement(1, VertexType.NormalArray, 12);
        this.vertexFormat.setElement(2, VertexType.ColorArray, 4);
        this.vertexFormat.setElement(3, VertexType.TextureCoordArray, 8);
        this.vertexFormat.calculate();
        this.beginMode = BeginMode.Triangles;
    }

    @Deprecated
    public VertexBufferObject(VertexPositionNormalTangentTexture[] vertices, int[] elements) {
        this.elements = elements;
        this.isStatic = true;
        RenderThread.invokeOnRenderContext(this, vertices, elements, (lThis, lVertices, lElements) -> {
            lThis.handle = this.LoadVBO((VertexPositionNormalTangentTexture[])lVertices, (int[])lElements);
        });
        this.vertexFormat = new VertexFormat(4);
        this.vertexFormat.setElement(0, VertexType.VertexArray, 12);
        this.vertexFormat.setElement(1, VertexType.NormalArray, 12);
        this.vertexFormat.setElement(2, VertexType.TangentArray, 12);
        this.vertexFormat.setElement(3, VertexType.TextureCoordArray, 8);
        this.vertexFormat.calculate();
        this.beginMode = BeginMode.Triangles;
    }

    @Deprecated
    public VertexBufferObject(VertexPositionNormalTangentTextureSkin[] vertices, int[] elements, boolean bReverse) {
        this.elements = elements;
        if (bReverse) {
            int[] elements2 = new int[elements.length];
            int ii = 0;
            for (int i = elements.length - 1 - 2; i >= 0; i -= 3) {
                elements2[ii] = elements[i];
                elements2[ii + 1] = elements[i + 1];
                elements2[ii + 2] = elements[i + 2];
                ii += 3;
            }
            elements = elements2;
        }
        this.isStatic = false;
        this.handle = this.LoadVBO(vertices, elements);
        this.vertexFormat = new VertexFormat(6);
        this.vertexFormat.setElement(0, VertexType.VertexArray, 12);
        this.vertexFormat.setElement(1, VertexType.NormalArray, 12);
        this.vertexFormat.setElement(3, VertexType.TextureCoordArray, 8);
        this.vertexFormat.setElement(4, VertexType.BlendWeightArray, 16);
        this.vertexFormat.setElement(5, VertexType.BlendIndexArray, 16);
        this.vertexFormat.calculate();
        this.beginMode = BeginMode.Triangles;
    }

    public VertexBufferObject(VertexArray vertices, int[] elements) {
        this.vertexFormat = vertices.format;
        this.elements = elements;
        this.isStatic = true;
        RenderThread.invokeOnRenderContext(this, vertices, elements, (lThis, lVertices, lElements) -> {
            lThis.handle = this.LoadVBO((VertexArray)lVertices, (int[])lElements);
        });
        this.beginMode = BeginMode.Triangles;
    }

    public VertexBufferObject(VertexArray vertices, int[] elements, boolean bReverse) {
        this.vertexFormat = vertices.format;
        if (bReverse) {
            int[] elements2 = new int[elements.length];
            int ii = 0;
            for (int i = elements.length - 1 - 2; i >= 0; i -= 3) {
                elements2[ii] = elements[i];
                elements2[ii + 1] = elements[i + 1];
                elements2[ii + 2] = elements[i + 2];
                ii += 3;
            }
            elements = elements2;
        }
        this.elements = elements;
        this.isStatic = false;
        this.handle = this.LoadVBO(vertices, elements);
        this.beginMode = BeginMode.Triangles;
    }

    @Deprecated
    private Vbo LoadVBO(VertexPositionNormalTangentTextureSkin[] vertices, int[] elements) {
        int n;
        Vbo handle = new Vbo();
        int stride = 76;
        handle.faceDataOnly = false;
        ByteBuffer buf = BufferUtils.createByteBuffer(vertices.length * 76);
        ByteBuffer elementsBuffer = BufferUtils.createByteBuffer(elements.length * 4);
        for (n = 0; n < vertices.length; ++n) {
            vertices[n].put(buf);
        }
        for (n = 0; n < elements.length; ++n) {
            elementsBuffer.putInt(elements[n]);
        }
        buf.flip();
        elementsBuffer.flip();
        handle.vboId = funcs.glGenBuffers();
        funcs.glBindBuffer(funcs.GL_ARRAY_BUFFER(), handle.vboId);
        funcs.glBufferData(funcs.GL_ARRAY_BUFFER(), buf, funcs.GL_STATIC_DRAW());
        funcs.glGetBufferParameter(funcs.GL_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), handle.b);
        int size = handle.b.get();
        if (vertices.length * 76 != size) {
            throw new RuntimeException("Vertex data not uploaded correctly");
        }
        handle.eboId = funcs.glGenBuffers();
        funcs.glBindBuffer(funcs.GL_ELEMENT_ARRAY_BUFFER(), handle.eboId);
        funcs.glBufferData(funcs.GL_ELEMENT_ARRAY_BUFFER(), elementsBuffer, funcs.GL_STATIC_DRAW());
        handle.b.clear();
        funcs.glGetBufferParameter(funcs.GL_ELEMENT_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), handle.b);
        size = handle.b.get();
        if (elements.length * 4 != size) {
            throw new RuntimeException("Element data not uploaded correctly");
        }
        handle.numElements = elements.length;
        handle.vertexStride = 76;
        return handle;
    }

    public Vbo LoadSoftwareVBO(ByteBuffer vertices, Vbo vbo, int[] elements) {
        Vbo handle = vbo;
        ByteBuffer elBuf = null;
        if (handle == null) {
            handle = new Vbo();
            handle.vboId = funcs.glGenBuffers();
            ByteBuffer elementsBuffer = BufferUtils.createByteBuffer(elements.length * 4);
            for (int n = 0; n < elements.length; ++n) {
                elementsBuffer.putInt(elements[n]);
            }
            elementsBuffer.flip();
            elBuf = elementsBuffer;
            handle.vertexStride = 36;
            handle.numElements = elements.length;
        } else {
            handle.b.clear();
        }
        handle.faceDataOnly = false;
        funcs.glBindBuffer(funcs.GL_ARRAY_BUFFER(), handle.vboId);
        funcs.glBufferData(funcs.GL_ARRAY_BUFFER(), vertices, funcs.GL_STATIC_DRAW());
        funcs.glGetBufferParameter(funcs.GL_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), handle.b);
        if (elBuf != null) {
            handle.eboId = funcs.glGenBuffers();
            funcs.glBindBuffer(funcs.GL_ELEMENT_ARRAY_BUFFER(), handle.eboId);
            funcs.glBufferData(funcs.GL_ELEMENT_ARRAY_BUFFER(), elBuf, funcs.GL_STATIC_DRAW());
        }
        return handle;
    }

    @Deprecated
    private Vbo LoadVBO(VertexPositionNormalTangentTexture[] vertices, int[] elements) {
        int n;
        Vbo handle = new Vbo();
        int stride = 44;
        handle.faceDataOnly = false;
        ByteBuffer buf = BufferUtils.createByteBuffer(vertices.length * 44);
        ByteBuffer elementsBuffer = BufferUtils.createByteBuffer(elements.length * 4);
        for (n = 0; n < vertices.length; ++n) {
            vertices[n].put(buf);
        }
        for (n = 0; n < elements.length; ++n) {
            elementsBuffer.putInt(elements[n]);
        }
        buf.flip();
        elementsBuffer.flip();
        handle.vboId = funcs.glGenBuffers();
        funcs.glBindBuffer(funcs.GL_ARRAY_BUFFER(), handle.vboId);
        funcs.glBufferData(funcs.GL_ARRAY_BUFFER(), buf, funcs.GL_STATIC_DRAW());
        funcs.glGetBufferParameter(funcs.GL_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), handle.b);
        int size = handle.b.get();
        if (vertices.length * 44 != size) {
            throw new RuntimeException("Vertex data not uploaded correctly");
        }
        handle.eboId = funcs.glGenBuffers();
        funcs.glBindBuffer(funcs.GL_ELEMENT_ARRAY_BUFFER(), handle.eboId);
        funcs.glBufferData(funcs.GL_ELEMENT_ARRAY_BUFFER(), elementsBuffer, funcs.GL_STATIC_DRAW());
        handle.b.clear();
        funcs.glGetBufferParameter(funcs.GL_ELEMENT_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), handle.b);
        size = handle.b.get();
        if (elements.length * 4 != size) {
            throw new RuntimeException("Element data not uploaded correctly");
        }
        handle.numElements = elements.length;
        handle.vertexStride = 44;
        return handle;
    }

    private Vbo LoadVBO(VertexArray vertices, int[] elements) {
        Vbo handle = new Vbo();
        handle.faceDataOnly = false;
        ByteBuffer elementsBuffer = MemoryUtil.memAlloc(elements.length * 4);
        for (int n = 0; n < elements.length; ++n) {
            elementsBuffer.putInt(elements[n]);
        }
        vertices.buffer.position(0);
        vertices.buffer.limit(vertices.numVertices * vertices.format.stride);
        elementsBuffer.flip();
        handle.vboId = funcs.glGenBuffers();
        funcs.glBindBuffer(funcs.GL_ARRAY_BUFFER(), handle.vboId);
        funcs.glBufferData(funcs.GL_ARRAY_BUFFER(), vertices.buffer, funcs.GL_STATIC_DRAW());
        funcs.glGetBufferParameter(funcs.GL_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), handle.b);
        int size = handle.b.get();
        if (vertices.numVertices * vertices.format.stride != size) {
            throw new RuntimeException("Vertex data not uploaded correctly");
        }
        handle.eboId = funcs.glGenBuffers();
        funcs.glBindBuffer(funcs.GL_ELEMENT_ARRAY_BUFFER(), handle.eboId);
        funcs.glBufferData(funcs.GL_ELEMENT_ARRAY_BUFFER(), elementsBuffer, funcs.GL_STATIC_DRAW());
        MemoryUtil.memFree(elementsBuffer);
        handle.b.clear();
        funcs.glGetBufferParameter(funcs.GL_ELEMENT_ARRAY_BUFFER(), funcs.GL_BUFFER_SIZE(), handle.b);
        size = handle.b.get();
        if (elements.length * 4 != size) {
            throw new RuntimeException("Element data not uploaded correctly");
        }
        handle.numElements = elements.length;
        handle.vertexStride = vertices.format.stride;
        return handle;
    }

    public void clear() {
        if (this.handle == null) {
            return;
        }
        if (this.handle.vboId > 0) {
            funcs.glDeleteBuffers(this.handle.vboId);
            this.handle.vboId = -1;
        }
        if (this.handle.eboId > 0) {
            funcs.glDeleteBuffers(this.handle.eboId);
            this.handle.eboId = -1;
        }
        this.handle = null;
    }

    public int BeginInstancedDraw(Shader shader) {
        if (VertexBufferObject.CanDraw(this.handle)) {
            boolean bBlendWeights = VertexBufferObject.BeginDraw(this.handle, this.vertexFormat, shader, 4);
            return bBlendWeights ? 1 : 0;
        }
        return -1;
    }

    public void FinishInstancedDraw(Shader shader, boolean bBlendWeights) {
        this.FinishDraw(shader, bBlendWeights);
    }

    public boolean BeginDraw(Shader shader) {
        return VertexBufferObject.BeginDraw(this.handle, this.vertexFormat, shader, 4);
    }

    public void Draw(Shader shader) {
        VertexBufferObject.Draw(this.handle, this.vertexFormat, shader, 4);
    }

    public void DrawInstanced(Shader shader, int instanceCount) {
        VertexBufferObject.DrawInstanced(this.handle, this.vertexFormat, shader, 4, instanceCount);
    }

    public void DrawStrip(Shader shader) {
        VertexBufferObject.Draw(this.handle, this.vertexFormat, shader, 5);
    }

    private static boolean CanDraw(Vbo handle) {
        return handle != null && !DebugOptions.instance.debugDrawSkipVboDraw.getValue();
    }

    private static boolean BeginDraw(Vbo handle, VertexFormat vertexFormat, Shader shader, int vertexType) {
        int textureNumber = 33984;
        boolean bBlendWeights = false;
        if (!handle.faceDataOnly) {
            VertexBufferObject.setModelViewProjection(shader);
            funcs.glBindBuffer(funcs.GL_ARRAY_BUFFER(), handle.vboId);
            block9: for (int i = 0; i < vertexFormat.elements.length; ++i) {
                VertexElement element = vertexFormat.elements[i];
                switch (element.type.ordinal()) {
                    case 0: {
                        GL20.glVertexAttribPointer(i, 3, 5126, false, handle.vertexStride, element.byteOffset);
                        GL20.glEnableVertexAttribArray(i);
                        continue block9;
                    }
                    case 1: {
                        GL20.glVertexAttribPointer(i, 3, 5126, true, handle.vertexStride, element.byteOffset);
                        GL20.glEnableVertexAttribArray(i);
                        continue block9;
                    }
                    case 2: {
                        GL20.glVertexAttribPointer(i, 3, 5121, true, handle.vertexStride, element.byteOffset);
                        GL20.glEnableVertexAttribArray(i);
                        continue block9;
                    }
                    case 4: {
                        GL13.glActiveTexture(textureNumber);
                        GL20.glVertexAttribPointer(i, 2, 5126, false, handle.vertexStride, element.byteOffset);
                        GL20.glEnableVertexAttribArray(i);
                        ++textureNumber;
                        continue block9;
                    }
                    case 5: {
                        continue block9;
                    }
                    case 6: {
                        GL20.glVertexAttribPointer(i, 4, 5126, false, handle.vertexStride, element.byteOffset);
                        GL20.glEnableVertexAttribArray(i);
                        bBlendWeights = true;
                        continue block9;
                    }
                    case 7: {
                        GL20.glVertexAttribPointer(i, 4, 5126, false, handle.vertexStride, element.byteOffset);
                        GL20.glEnableVertexAttribArray(i);
                    }
                }
            }
        }
        funcs.glBindBuffer(funcs.GL_ELEMENT_ARRAY_BUFFER(), handle.eboId);
        return bBlendWeights;
    }

    public void FinishDraw(Shader shader, boolean bBlendWeights) {
        VertexBufferObject.FinishDraw(this.vertexFormat, shader, bBlendWeights);
    }

    public static void FinishDraw(VertexFormat vertexFormat, Shader shader, boolean bBlendWeights) {
        if (bBlendWeights && shader != null) {
            int bw = PZArrayUtil.indexOf(vertexFormat.elements, x -> x.type == VertexType.BlendWeightArray);
            int bi = PZArrayUtil.indexOf(vertexFormat.elements, x -> x.type == VertexType.BlendIndexArray);
            GL20.glDisableVertexAttribArray(bw);
            GL20.glDisableVertexAttribArray(bi);
        }
    }

    private static void Draw(Vbo handle, VertexFormat vertexFormat, Shader shader, int vertexType) {
        if (VertexBufferObject.CanDraw(handle)) {
            boolean bBlendWeights = VertexBufferObject.BeginDraw(handle, vertexFormat, shader, vertexType);
            GL11.glDrawElements(vertexType, handle.numElements, 5125, 0L);
            VertexBufferObject.FinishDraw(vertexFormat, shader, bBlendWeights);
        }
    }

    private static void DrawInstanced(Vbo handle, VertexFormat vertexFormat, Shader shader, int vertexType, int instanceCount) {
        if (VertexBufferObject.CanDraw(handle)) {
            boolean bBlendWeights = VertexBufferObject.BeginDraw(handle, vertexFormat, shader, vertexType);
            GL31.glDrawElementsInstanced(vertexType, handle.numElements, 5125, 0L, instanceCount);
            VertexBufferObject.FinishDraw(vertexFormat, shader, bBlendWeights);
        }
    }

    public void PushDrawCall() {
        GL11.glDrawElements(4, this.handle.numElements, 5125, 0L);
    }

    public static void getModelViewProjection(Matrix4f mvp) {
        Core core = Core.getInstance();
        if (core.projectionMatrixStack.isEmpty() || core.modelViewMatrixStack.isEmpty()) {
            DebugLog.Shader.warn("Matrix stack is empty");
            mvp.identity();
            return;
        }
        Matrix4f projection = Core.getInstance().projectionMatrixStack.peek();
        Matrix4f modelView = Core.getInstance().modelViewMatrixStack.peek();
        projection.mul(modelView, mvp);
    }

    public static float getDepthValueAt(float x, float y, float z) {
        Matrix4f mvp = L_getModelViewProjection.MVPjoml;
        VertexBufferObject.getModelViewProjection(mvp);
        Vector3f pos = L_getModelViewProjection.vector3f.set(x, y, z);
        mvp.transformPosition(pos);
        return pos.z;
    }

    public static void setModelViewProjection(Shader shader) {
        if (shader == null) {
            return;
        }
        VertexBufferObject.setModelViewProjection(shader.getShaderProgram());
    }

    public static void setModelViewProjection(ShaderProgram shaderProgram) {
        if (shaderProgram == null || !shaderProgram.isCompiled()) {
            return;
        }
        ShaderProgram.Uniform uMVP = shaderProgram.getUniform("ModelViewProjection", 35676);
        if (uMVP == null) {
            return;
        }
        Matrix4f projection = L_setModelViewProjection.PRJ;
        Matrix4f modelView = L_setModelViewProjection.MV;
        if (Core.getInstance().modelViewMatrixStack.isEmpty()) {
            modelView.identity();
            projection.identity();
        } else {
            modelView.set(Core.getInstance().modelViewMatrixStack.peek());
            projection.set(Core.getInstance().projectionMatrixStack.peek());
        }
        if (modelView.equals(shaderProgram.modelView) && projection.equals(shaderProgram.projection)) {
            return;
        }
        shaderProgram.modelView.set(modelView);
        shaderProgram.projection.set(projection);
        projection.mul(modelView);
        shaderProgram.setValue("ModelViewProjection", projection);
    }

    public static final class VertexFormat {
        final VertexElement[] elements;
        int stride;

        public VertexFormat(int numElements) {
            this.elements = PZArrayUtil.newInstance(VertexElement.class, numElements, VertexElement::new);
        }

        public void setElement(int index, VertexType type, int byteSize) {
            this.elements[index].type = type;
            this.elements[index].byteSize = byteSize;
        }

        public int getNumElements() {
            return this.elements.length;
        }

        public VertexElement getElement(int index) {
            return this.elements[index];
        }

        public int indexOf(VertexType vertexType) {
            for (int i = 0; i < this.elements.length; ++i) {
                VertexElement element = this.elements[i];
                if (element.type != vertexType) continue;
                return i;
            }
            return -1;
        }

        public void calculate() {
            this.stride = 0;
            for (int i = 0; i < this.elements.length; ++i) {
                this.elements[i].byteOffset = this.stride;
                this.stride += this.elements[i].byteSize;
            }
        }

        public int getStride() {
            return this.stride;
        }
    }

    public static enum VertexType {
        VertexArray,
        NormalArray,
        ColorArray,
        IndexArray,
        TextureCoordArray,
        TangentArray,
        BlendWeightArray,
        BlendIndexArray,
        Depth;

    }

    public static enum BeginMode {
        Triangles;

    }

    public static final class Vbo {
        public final IntBuffer b = BufferUtils.createIntBuffer(4);
        public int vboId;
        public int eboId;
        public int numElements;
        public int vertexStride;
        public boolean faceDataOnly;
    }

    public static final class VertexArray {
        public final VertexFormat format;
        public final int numVertices;
        public final ByteBuffer buffer;

        public VertexArray(VertexFormat format, int numVertices) {
            this.format = format;
            this.numVertices = numVertices;
            this.buffer = BufferUtils.createByteBuffer(this.numVertices * this.format.stride);
        }

        public void setElement(int vertex, int element, float v1, float v2) {
            int index = vertex * this.format.stride + this.format.elements[element].byteOffset;
            this.buffer.putFloat(index, v1);
            this.buffer.putFloat(index += 4, v2);
        }

        public void setElement(int vertex, int element, float v1, float v2, float v3) {
            int index = vertex * this.format.stride + this.format.elements[element].byteOffset;
            this.buffer.putFloat(index, v1);
            this.buffer.putFloat(index += 4, v2);
            this.buffer.putFloat(index += 4, v3);
        }

        public void setElement(int vertex, int element, float v1, float v2, float v3, float v4) {
            int index = vertex * this.format.stride + this.format.elements[element].byteOffset;
            this.buffer.putFloat(index, v1);
            this.buffer.putFloat(index += 4, v2);
            this.buffer.putFloat(index += 4, v3);
            this.buffer.putFloat(index += 4, v4);
        }

        public float getElementFloat(int vertex, int element, int n) {
            int index = vertex * this.format.stride + this.format.elements[element].byteOffset + n * 4;
            return this.buffer.getFloat(index);
        }
    }

    public static final class VertexElement {
        public VertexType type;
        public int byteSize;
        public int byteOffset;
    }

    private static final class L_getModelViewProjection {
        static final Matrix4f MVPjoml = new Matrix4f();
        static final Vector3f vector3f = new Vector3f();

        private L_getModelViewProjection() {
        }
    }

    private static final class L_setModelViewProjection {
        static final Matrix4f MV = new Matrix4f();
        static final Matrix4f PRJ = new Matrix4f();

        private L_setModelViewProjection() {
        }
    }
}

