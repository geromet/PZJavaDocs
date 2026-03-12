/*
 * Decompiled with CFR 0.152.
 */
package zombie.gizmo;

import java.util.Objects;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import se.krka.kahlua.vm.KahluaTable;
import zombie.core.opengl.VBORenderer;
import zombie.gizmo.Axis;
import zombie.gizmo.GridPlane;
import zombie.gizmo.Scene;
import zombie.gizmo.SceneObject;
import zombie.gizmo.StateData;
import zombie.gizmo.TransformMode;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.UI3DScene;

public abstract class Gizmo {
    static VBORenderer vboRenderer;
    protected Scene scene;
    static final float LENGTH = 0.5f;
    static final float THICKNESS = 0.05f;
    boolean visible;
    TransformMode transformMode = TransformMode.Global;
    private final Vector3f gizmoRotate = new Vector3f();
    final Vector3f gizmoWorldPos = new Vector3f();
    private final SceneObject gizmoParent = null;
    private final SceneObject gizmoOrigin = null;
    SceneObject gizmoChild;
    SceneObject originGeometry;
    float gizmoScale = 1.0f;
    boolean gizmoAxisVisibleX = true;
    boolean gizmoAxisVisibleY = true;
    boolean gizmoAxisVisibleZ = true;
    boolean reverseZAxis;
    KahluaTable table;

    Gizmo(Scene scene) {
        this.scene = scene;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void setWorldPosition(float x, float y, float z) {
        this.gizmoWorldPos.set(x, y, z);
    }

    public Vector3f getWorldPosition() {
        return this.gizmoWorldPos;
    }

    public void setRotation(float rx, float ry, float rz) {
        this.gizmoRotate.set(rx, rz, ry);
    }

    public Vector3f getRotation() {
        return this.gizmoRotate;
    }

    public SceneObject getParent() {
        return this.gizmoParent;
    }

    public SceneObject getOrigin() {
        return this.gizmoOrigin;
    }

    public SceneObject getChild() {
        return this.gizmoChild;
    }

    public float getScale() {
        return this.gizmoScale;
    }

    public void setTransformMode(TransformMode mode) {
        Objects.requireNonNull(mode);
        this.transformMode = mode;
    }

    public TransformMode getTransformMode() {
        return this.transformMode;
    }

    public void setTable(KahluaTable table) {
        this.table = table;
    }

    public KahluaTable getTable() {
        return this.table;
    }

    abstract Axis hitTest(float var1, float var2);

    abstract void startTracking(float var1, float var2, Axis var3);

    abstract void updateTracking(float var1, float var2);

    abstract void stopTracking();

    abstract void render();

    Vector3f getPointOnAxis(float uiX, float uiY, Axis axis1, Matrix4f gizmoXfrm, Vector3f out) {
        StateData stateData = this.scene.stateDataMain();
        if (axis1 == Axis.XY || axis1 == Axis.XZ || axis1 == Axis.YZ) {
            Vector3f planePoint = gizmoXfrm.transformPosition(this.allocVector3f().set(0.0f, 0.0f, 0.0f));
            Vector3f planeRotate = this.allocVector3f();
            GridPlane gridPlane = GridPlane.XY;
            switch (axis1) {
                case XY: {
                    planeRotate.set(0.0f, 0.0f, 0.0f);
                    gridPlane = GridPlane.XY;
                    break;
                }
                case XZ: {
                    planeRotate.set(90.0f, 0.0f, 0.0f);
                    gridPlane = GridPlane.XZ;
                    break;
                }
                case YZ: {
                    planeRotate.set(0.0f, 90.0f, 0.0f);
                    gridPlane = GridPlane.YZ;
                }
            }
            this.scene.polygonEditor.setPlane(planePoint, planeRotate, gridPlane);
            this.scene.polygonEditor.uiToPlane3D(uiX, uiY, out.set(0.0f));
            this.releaseVector3f(planePoint);
            this.releaseVector3f(planeRotate);
            return out;
        }
        uiY = (float)this.scene.screenHeight() - uiY;
        UI3DScene.Ray cameraRay = this.scene.getCameraRay(uiX, uiY, this.allocRay());
        UI3DScene.Ray axis = this.allocRay();
        gizmoXfrm.transformPosition(axis.origin.set(0.0f, 0.0f, 0.0f));
        switch (axis1) {
            case X: {
                axis.direction.set(1.0f, 0.0f, 0.0f);
                break;
            }
            case Y: {
                axis.direction.set(0.0f, 1.0f, 0.0f);
                break;
            }
            case Z: {
                axis.direction.set(0.0f, 0.0f, this.transformMode == TransformMode.Global && this.reverseZAxis ? -1.0f : 1.0f);
            }
        }
        gizmoXfrm.transformDirection(axis.direction).normalize();
        UI3DScene.closest_distance_between_lines(axis, cameraRay);
        Gizmo.releaseRay(cameraRay);
        out.set(axis.direction).mul(axis.t).add(axis.origin);
        Gizmo.releaseRay(axis);
        return out;
    }

    boolean getPointOnDualAxis(float uiX, float uiY, Axis axis, Matrix4f gizmoXfrm, Vector3f pointOnPlane3D, Vector2f pointOnPlane2D) {
        UI3DScene.Plane plane = this.allocPlane();
        gizmoXfrm.transformPosition(plane.point.set(0.0f, 0.0f, 0.0f));
        switch (axis) {
            case XY: {
                plane.normal.set(0.0f, 0.0f, 1.0f);
                break;
            }
            case XZ: {
                plane.normal.set(0.0f, 1.0f, 0.0f);
                break;
            }
            case YZ: {
                plane.normal.set(1.0f, 0.0f, 0.0f);
            }
        }
        gizmoXfrm.transformDirection(plane.normal);
        UI3DScene.Ray cameraRay = this.scene.getCameraRay(uiX, (float)this.scene.screenHeight() - uiY, this.allocRay());
        boolean hit = UI3DScene.intersect_ray_plane(plane, cameraRay, pointOnPlane3D) == 1;
        Gizmo.releaseRay(cameraRay);
        this.releasePlane(plane);
        if (hit) {
            Matrix4f m = this.allocMatrix4f().set(gizmoXfrm);
            m.invert();
            Vector3f localToPlaneOrigin = m.transformPosition(pointOnPlane3D, this.allocVector3f());
            this.releaseMatrix4f(m);
            switch (axis) {
                case XY: {
                    pointOnPlane2D.set(localToPlaneOrigin.x, localToPlaneOrigin.y);
                    break;
                }
                case XZ: {
                    pointOnPlane2D.set(localToPlaneOrigin.x, localToPlaneOrigin.z);
                    break;
                }
                case YZ: {
                    pointOnPlane2D.set(localToPlaneOrigin.y, localToPlaneOrigin.z);
                }
            }
            this.releaseVector3f(localToPlaneOrigin);
            return true;
        }
        return false;
    }

    void renderLineToOrigin() {
        StateData stateData = this.scene.stateDataRender();
        if (!stateData.hasGizmoOrigin) {
            return;
        }
        this.scene.renderAxis(stateData.gizmoTranslate, stateData.gizmoRotate, false);
        Vector3f gizmoPos = stateData.gizmoTranslate;
        vboRenderer.flush();
        Matrix4f matrix4f = this.allocMatrix4f();
        matrix4f.set(stateData.modelView);
        matrix4f.mul(stateData.gizmoParentTransform);
        matrix4f.mul(stateData.gizmoOriginTransform);
        matrix4f.mul(stateData.gizmoChildTransform);
        if (stateData.selectedAttachmentIsChildAttachment) {
            matrix4f.mul(stateData.gizmoChildAttachmentTransformInv);
        }
        vboRenderer.cmdPushAndLoadMatrix(5888, matrix4f);
        this.releaseMatrix4f(matrix4f);
        vboRenderer.startRun(Gizmo.vboRenderer.formatPositionColor);
        vboRenderer.setMode(1);
        vboRenderer.setLineWidth(2.0f);
        vboRenderer.addLine(gizmoPos.x, gizmoPos.y, gizmoPos.z, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f);
        vboRenderer.endRun();
        vboRenderer.cmdPopMatrix(5888);
        vboRenderer.flush();
    }

    protected Matrix4f allocMatrix4f() {
        return BaseVehicle.allocMatrix4f();
    }

    protected void releaseMatrix4f(Matrix4f m) {
        BaseVehicle.releaseMatrix4f(m);
    }

    protected UI3DScene.Plane allocPlane() {
        return UI3DScene.allocPlane();
    }

    protected void releasePlane(UI3DScene.Plane plane) {
        UI3DScene.releasePlane(plane);
    }

    protected Quaternionf allocQuaternionf() {
        return BaseVehicle.allocQuaternionf();
    }

    protected void releaseQuaternionf(Quaternionf q) {
        BaseVehicle.releaseQuaternionf(q);
    }

    protected UI3DScene.Ray allocRay() {
        return UI3DScene.allocRay();
    }

    public static void releaseRay(UI3DScene.Ray ray) {
        UI3DScene.releaseRay(ray);
    }

    protected Vector2f allocVector2f() {
        return BaseVehicle.allocVector2f();
    }

    protected void releaseVector2f(Vector2f vector2f) {
        BaseVehicle.releaseVector2f(vector2f);
    }

    protected Vector3f allocVector3f() {
        return BaseVehicle.allocVector3f();
    }

    protected void releaseVector3f(Vector3f vector3f) {
        BaseVehicle.releaseVector3f(vector3f);
    }
}

