/*
 * Decompiled with CFR 0.152.
 */
package zombie.tileDepth;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import zombie.core.Core;
import zombie.iso.Vector2;
import zombie.iso.Vector2ObjectPool;
import zombie.tileDepth.CylinderUtils;
import zombie.vehicles.BaseVehicle;
import zombie.vehicles.UI3DScene;

public final class TileGeometryUtils {
    private static final UI3DScene.Plane m_plane = new UI3DScene.Plane();
    private static final Vector3f m_viewRotation = new Vector3f(30.0f, 315.0f, 0.0f);
    private static final int[] m_viewport = new int[]{0, 0, 0, 0};
    private static final int m_viewWidth = 1;
    private static final int m_viewHeight = 2;

    static void calcMatricesForSquare(Matrix4f projection, Matrix4f modelView) {
        float s = (float)Math.sqrt(2.0);
        projection.setOrtho(-1.0f * s / 2.0f, 1.0f * s / 2.0f, -2.0f * s / 2.0f, 2.0f * s / 2.0f, -2.0f, 2.0f);
        projection.translate(0.0f, -2.0f * s * 0.375f, 0.0f);
        modelView.rotationXYZ(TileGeometryUtils.m_viewRotation.x * ((float)Math.PI / 180), TileGeometryUtils.m_viewRotation.y * ((float)Math.PI / 180), TileGeometryUtils.m_viewRotation.z * ((float)Math.PI / 180));
    }

    public static float getDepthOnBoxAt(float tileX, float tileY, Vector3f center, Vector3f rotation, Vector3f min, Vector3f max) {
        Matrix4f projection = TileGeometryUtils.allocMatrix4f();
        Matrix4f modelView = TileGeometryUtils.allocMatrix4f();
        TileGeometryUtils.calcMatricesForSquare(projection, modelView);
        Vector3f scenePos = TileGeometryUtils.allocVector3f();
        UI3DScene.Ray cameraRay = TileGeometryUtils.getCameraRay(tileX *= 1.0f / (64.0f * (float)Core.tileScale), 2.0f - (tileY *= 2.0f / (128.0f * (float)Core.tileScale)), projection, modelView, 1, 2, UI3DScene.allocRay());
        Matrix4f boxMatrix = TileGeometryUtils.allocMatrix4f();
        boxMatrix.translation(center);
        boxMatrix.rotateXYZ(rotation.x * ((float)Math.PI / 180), rotation.y * ((float)Math.PI / 180), rotation.z * ((float)Math.PI / 180));
        boxMatrix.invert();
        boxMatrix.transformPosition(cameraRay.origin);
        boxMatrix.transformDirection(cameraRay.direction);
        TileGeometryUtils.releaseMatrix4f(boxMatrix);
        Vector2f nearFar = TileGeometryUtils.allocVector2f();
        boolean ok = TileGeometryUtils.intersectRayAab(cameraRay.origin.x, cameraRay.origin.y, cameraRay.origin.z, cameraRay.direction.x, cameraRay.direction.y, cameraRay.direction.z, min.x, min.y, min.z, max.x, max.y, max.z, nearFar);
        if (ok) {
            scenePos.set(cameraRay.origin).add(cameraRay.direction.mul(nearFar.x));
            Matrix4f boxMatrix2 = TileGeometryUtils.allocMatrix4f();
            boxMatrix2.translation(center);
            boxMatrix2.rotateXYZ(rotation.x * ((float)Math.PI / 180), rotation.y * ((float)Math.PI / 180), rotation.z * ((float)Math.PI / 180));
            boxMatrix2.transformPosition(scenePos);
            TileGeometryUtils.releaseMatrix4f(boxMatrix2);
        }
        TileGeometryUtils.releaseVector2f(nearFar);
        UI3DScene.releaseRay(cameraRay);
        Matrix4f mvp = TileGeometryUtils.allocMatrix4f();
        mvp.set(projection);
        mvp.mul(modelView);
        mvp.transformPosition(scenePos);
        float depth = scenePos.z;
        TileGeometryUtils.releaseMatrix4f(mvp);
        TileGeometryUtils.releaseVector3f(scenePos);
        TileGeometryUtils.releaseMatrix4f(projection);
        TileGeometryUtils.releaseMatrix4f(modelView);
        return ok ? depth : 666.0f;
    }

    public static float getDepthOnCylinderAt(float tileX, float tileY, Vector3f center, Vector3f rotation, float radius, float zLength) {
        Matrix4f projection = TileGeometryUtils.allocMatrix4f();
        Matrix4f modelView = TileGeometryUtils.allocMatrix4f();
        TileGeometryUtils.calcMatricesForSquare(projection, modelView);
        Vector3f scenePos = TileGeometryUtils.allocVector3f();
        UI3DScene.Ray cameraRay = TileGeometryUtils.getCameraRay(tileX *= 1.0f / (64.0f * (float)Core.tileScale), 2.0f - (tileY *= 2.0f / (128.0f * (float)Core.tileScale)), projection, modelView, 1, 2, UI3DScene.allocRay());
        Matrix4f cylinderMatrix = TileGeometryUtils.allocMatrix4f();
        cylinderMatrix.translation(center);
        cylinderMatrix.rotateXYZ(rotation.x * ((float)Math.PI / 180), rotation.y * ((float)Math.PI / 180), rotation.z * ((float)Math.PI / 180));
        cylinderMatrix.invert();
        cylinderMatrix.transformPosition(cameraRay.origin);
        cylinderMatrix.transformDirection(cameraRay.direction);
        TileGeometryUtils.releaseMatrix4f(cylinderMatrix);
        CylinderUtils.IntersectionRecord intersectionRecord = new CylinderUtils.IntersectionRecord();
        boolean ok = CylinderUtils.intersect(radius, zLength, cameraRay, intersectionRecord);
        if (ok) {
            Matrix4f cylinderMatrix2 = TileGeometryUtils.allocMatrix4f();
            cylinderMatrix2.translation(center);
            cylinderMatrix2.rotateXYZ(rotation.x * ((float)Math.PI / 180), rotation.y * ((float)Math.PI / 180), rotation.z * ((float)Math.PI / 180));
            cylinderMatrix2.transformPosition(intersectionRecord.location);
            cylinderMatrix2.transformDirection(intersectionRecord.normal);
            TileGeometryUtils.releaseMatrix4f(cylinderMatrix2);
            scenePos.set(intersectionRecord.location);
        }
        UI3DScene.releaseRay(cameraRay);
        Matrix4f mvp = TileGeometryUtils.allocMatrix4f();
        mvp.set(projection);
        mvp.mul(modelView);
        mvp.transformPosition(scenePos);
        float depth = scenePos.z;
        TileGeometryUtils.releaseMatrix4f(mvp);
        TileGeometryUtils.releaseVector3f(scenePos);
        TileGeometryUtils.releaseMatrix4f(projection);
        TileGeometryUtils.releaseMatrix4f(modelView);
        return ok ? depth : 666.0f;
    }

    public static float getDepthOnPlaneAt(float tileX, float tileY, UI3DScene.GridPlane gridPlane, Vector3f planePoint) {
        Vector3f normal = TileGeometryUtils.allocVector3f();
        switch (gridPlane) {
            case XY: {
                normal.set(0.0f, 0.0f, 1.0f);
                break;
            }
            case XZ: {
                normal.set(0.0f, 1.0f, 0.0f);
                break;
            }
            case YZ: {
                normal.set(1.0f, 0.0f, 0.0f);
            }
        }
        float depth = TileGeometryUtils.getDepthOnPlaneAt(tileX, tileY, planePoint, normal);
        TileGeometryUtils.releaseVector3f(normal);
        return depth;
    }

    public static float getDepthOnPlaneAt(float tileX, float tileY, Vector3f planePoint, Vector3f planeNormal) {
        return TileGeometryUtils.getDepthOnPlaneAt(tileX, tileY, planePoint, planeNormal, null);
    }

    public static float getDepthOnPlaneAt(float tileX, float tileY, Vector3f planePoint, Vector3f planeNormal, Vector3f pointOnPlane) {
        Matrix4f projection = TileGeometryUtils.allocMatrix4f();
        Matrix4f modelView = TileGeometryUtils.allocMatrix4f();
        TileGeometryUtils.calcMatricesForSquare(projection, modelView);
        TileGeometryUtils.m_plane.point.set(planePoint);
        TileGeometryUtils.m_plane.normal.set(planeNormal);
        Vector3f scenePos = TileGeometryUtils.allocVector3f();
        UI3DScene.Ray cameraRay = TileGeometryUtils.getCameraRay(tileX *= 1.0f / (64.0f * (float)Core.tileScale), 2.0f - (tileY *= 2.0f / (128.0f * (float)Core.tileScale)), projection, modelView, 1, 2, UI3DScene.allocRay());
        boolean ok = UI3DScene.intersect_ray_plane(m_plane, cameraRay, scenePos) == 1;
        UI3DScene.releaseRay(cameraRay);
        if (pointOnPlane != null) {
            pointOnPlane.set(scenePos);
        }
        Matrix4f mvp = TileGeometryUtils.allocMatrix4f();
        mvp.set(projection);
        mvp.mul(modelView);
        mvp.transformPosition(scenePos);
        float depth = scenePos.z;
        TileGeometryUtils.releaseMatrix4f(mvp);
        TileGeometryUtils.releaseVector3f(scenePos);
        TileGeometryUtils.releaseMatrix4f(projection);
        TileGeometryUtils.releaseMatrix4f(modelView);
        return ok ? depth : 666.0f;
    }

    static float getDepthOfScenePoint(float x, float y, float z) {
        Matrix4f projection = TileGeometryUtils.allocMatrix4f();
        Matrix4f modelView = TileGeometryUtils.allocMatrix4f();
        TileGeometryUtils.calcMatricesForSquare(projection, modelView);
        Matrix4f mvp = TileGeometryUtils.allocMatrix4f();
        mvp.set(projection);
        mvp.mul(modelView);
        Vector3f pos = TileGeometryUtils.allocVector3f().set(x, y, z);
        mvp.transformPosition(pos);
        float depth = pos.z;
        TileGeometryUtils.releaseMatrix4f(mvp);
        TileGeometryUtils.releaseVector3f(pos);
        TileGeometryUtils.releaseMatrix4f(projection);
        TileGeometryUtils.releaseMatrix4f(modelView);
        return depth;
    }

    static float getNormalizedDepth(float depth) {
        float depthNW = Math.abs(TileGeometryUtils.getDepthOfScenePoint(-0.5f, 0.0f, -0.5f));
        float scale = 1.0f / depthNW;
        float offset = 0.75f;
        return depth * (scale *= 0.25f) + 0.75f;
    }

    public static float getNormalizedDepthOnBoxAt(float tileX, float tileY, Vector3f center, Vector3f rotation, Vector3f min, Vector3f max) {
        float depth = TileGeometryUtils.getDepthOnBoxAt(tileX, tileY, center, rotation, min, max);
        if (depth == 666.0f) {
            return -1.0f;
        }
        return TileGeometryUtils.getNormalizedDepth(depth);
    }

    public static float getNormalizedDepthOnCylinderAt(float tileX, float tileY, Vector3f center, Vector3f rotation, float radius, float zLength) {
        float depth = TileGeometryUtils.getDepthOnCylinderAt(tileX, tileY, center, rotation, radius, zLength);
        if (depth == 666.0f) {
            return -1.0f;
        }
        return TileGeometryUtils.getNormalizedDepth(depth);
    }

    public static float getNormalizedDepthOnPlaneAt(float tileX, float tileY, Vector3f planePoint, Vector3f planeNormal) {
        float depth = TileGeometryUtils.getDepthOnPlaneAt(tileX, tileY, planePoint, planeNormal);
        if (depth == 666.0f) {
            return -1.0f;
        }
        return TileGeometryUtils.getNormalizedDepth(depth);
    }

    public static float getNormalizedDepthOnPlaneAt(float tileX, float tileY, UI3DScene.GridPlane gridPlane, Vector3f planePoint) {
        float depth = TileGeometryUtils.getDepthOnPlaneAt(tileX, tileY, gridPlane, planePoint);
        if (depth == 666.0f) {
            return -1.0f;
        }
        return TileGeometryUtils.getNormalizedDepth(depth);
    }

    public static UI3DScene.Ray getCameraRay(float uiX, float uiY, Matrix4f projection, Matrix4f modelView, int viewWidth, int viewHeight, UI3DScene.Ray cameraRay) {
        Matrix4f matrix4f = TileGeometryUtils.allocMatrix4f();
        matrix4f.set(projection);
        matrix4f.mul(modelView);
        matrix4f.invert();
        TileGeometryUtils.m_viewport[2] = viewWidth;
        TileGeometryUtils.m_viewport[3] = viewHeight;
        Vector3f rayStart = matrix4f.unprojectInv(uiX, uiY, 0.0f, m_viewport, TileGeometryUtils.allocVector3f());
        Vector3f rayEnd = matrix4f.unprojectInv(uiX, uiY, 1.0f, m_viewport, TileGeometryUtils.allocVector3f());
        cameraRay.origin.set(rayStart);
        cameraRay.direction.set(rayEnd.sub(rayStart).normalize());
        TileGeometryUtils.releaseVector3f(rayEnd);
        TileGeometryUtils.releaseVector3f(rayStart);
        TileGeometryUtils.releaseMatrix4f(matrix4f);
        return cameraRay;
    }

    public static boolean intersectRayAab(float originX, float originY, float originZ, float dirX, float dirY, float dirZ, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, Vector2f result) {
        float tzmax;
        float tzmin;
        float tymax;
        float tymin;
        float tFar;
        float tNear;
        float invDirX = 1.0f / dirX;
        float invDirY = 1.0f / dirY;
        float invDirZ = 1.0f / dirZ;
        if (invDirX >= 0.0f) {
            tNear = (minX - originX) * invDirX;
            tFar = (maxX - originX) * invDirX;
        } else {
            tNear = (maxX - originX) * invDirX;
            tFar = (minX - originX) * invDirX;
        }
        if (invDirY >= 0.0f) {
            tymin = (minY - originY) * invDirY;
            tymax = (maxY - originY) * invDirY;
        } else {
            tymin = (maxY - originY) * invDirY;
            tymax = (minY - originY) * invDirY;
        }
        if (tNear > tymax || tymin > tFar) {
            return false;
        }
        if (invDirZ >= 0.0f) {
            tzmin = (minZ - originZ) * invDirZ;
            tzmax = (maxZ - originZ) * invDirZ;
        } else {
            tzmin = (maxZ - originZ) * invDirZ;
            tzmax = (minZ - originZ) * invDirZ;
        }
        if (tNear > tzmax || tzmin > tFar) {
            return false;
        }
        tNear = tymin > tNear || Float.isNaN(tNear) ? tymin : tNear;
        tFar = tymax < tFar || Float.isNaN(tFar) ? tymax : tFar;
        tNear = tzmin > tNear ? tzmin : tNear;
        float f = tFar = tzmax < tFar ? tzmax : tFar;
        if (tNear < tFar && tFar >= 0.0f) {
            result.x = tNear;
            result.y = tFar;
            return true;
        }
        return false;
    }

    private static Matrix4f allocMatrix4f() {
        return (Matrix4f)BaseVehicle.TL_matrix4f_pool.get().alloc();
    }

    private static void releaseMatrix4f(Matrix4f matrix) {
        BaseVehicle.TL_matrix4f_pool.get().release(matrix);
    }

    private static Quaternionf allocQuaternionf() {
        return (Quaternionf)BaseVehicle.TL_quaternionf_pool.get().alloc();
    }

    private static void releaseQuaternionf(Quaternionf q) {
        BaseVehicle.TL_quaternionf_pool.get().release(q);
    }

    private static Vector2 allocVector2() {
        return (Vector2)Vector2ObjectPool.get().alloc();
    }

    private static void releaseVector2(Vector2 vector2) {
        Vector2ObjectPool.get().release(vector2);
    }

    private static Vector2f allocVector2f() {
        return BaseVehicle.allocVector2f();
    }

    private static void releaseVector2f(Vector2f vector2f) {
        BaseVehicle.releaseVector2f(vector2f);
    }

    private static Vector3f allocVector3f() {
        return (Vector3f)BaseVehicle.TL_vector3f_pool.get().alloc();
    }

    private static void releaseVector3f(Vector3f vector3f) {
        BaseVehicle.TL_vector3f_pool.get().release(vector3f);
    }
}

