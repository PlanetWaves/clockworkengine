
package com.clockwork.math;

import com.clockwork.export.*;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Plane defines a plane where Normal dot (x,y,z) = Constant.
 * This provides methods for calculating a "distance" of a point from this
 * plane. The distance is pseudo due to the fact that it can be negative if the
 * point is on the non-normal side of the plane.
 * 
 * 
 * 
 */
public class Plane implements Savable, Cloneable, java.io.Serializable {

    static final long serialVersionUID = 1;

    private static final Logger logger = Logger
            .getLogger(Plane.class.getName());

    public static enum Side {
        None,
        Positive,
        Negative
    }

    /** 
     * Vector normal to the plane.
     */
    protected Vector3f normal = new Vector3f();

    /** 
     * Constant of the plane. See formula in class definition.
     */
    protected float constant;

    /**
     * Constructor instantiates a new Plane object. This is the
     * default object and contains a normal of (0,0,0) and a constant of 0.
     */
    public Plane() {
    }

    /**
     * Constructor instantiates a new Plane object. The normal
     * and constant values are set at creation.
     * 
     * @param normal
     *            the normal of the plane.
     * @param constant
     *            the constant of the plane.
     */
    public Plane(Vector3f normal, float constant) {
        if (normal == null) {
            throw new IllegalArgumentException("normal cannot be null");
        }

        this.normal.set(normal);
        this.constant = constant;
    }

    /**
     * setNormal sets the normal of the plane.
     * 
     * @param normal
     *            the new normal of the plane.
     */
    public void setNormal(Vector3f normal) {
        if (normal == null) {
            throw new IllegalArgumentException("normal cannot be null");
        }
        this.normal.set(normal);
    }

    /**
     * setNormal sets the normal of the plane.
     *
     */
    public void setNormal(float x, float y, float z) {
        this.normal.set(x,y,z);
    }

    /**
     * getNormal retrieves the normal of the plane.
     * 
     * @return the normal of the plane.
     */
    public Vector3f getNormal() {
        return normal;
    }

    /**
     * setConstant sets the constant value that helps define the
     * plane.
     * 
     * @param constant
     *            the new constant value.
     */
    public void setConstant(float constant) {
        this.constant = constant;
    }

    /**
     * getConstant returns the constant of the plane.
     * 
     * @return the constant of the plane.
     */
    public float getConstant() {
        return constant;
    }

    public Vector3f getClosestPoint(Vector3f point, Vector3f store){
//        float t = constant - normal.dot(point);
//        return store.set(normal).multLocal(t).addLocal(point);
        float t = (constant - normal.dot(point)) / normal.dot(normal);
        return store.set(normal).multLocal(t).addLocal(point);
    }

    public Vector3f getClosestPoint(Vector3f point){
        return getClosestPoint(point, new Vector3f());
    }

    public Vector3f reflect(Vector3f point, Vector3f store){
        if (store == null)
            store = new Vector3f();

        float d = pseudoDistance(point);
        store.set(normal).negateLocal().multLocal(d * 2f);
        store.addLocal(point);
        return store;
    }

    /**
     * pseudoDistance calculates the distance from this plane to
     * a provided point. If the point is on the negative side of the plane the
     * distance returned is negative, otherwise it is positive. If the point is
     * on the plane, it is zero.
     * 
     * @param point
     *            the point to check.
     * @return the signed distance from the plane to a point.
     */
    public float pseudoDistance(Vector3f point) {
        return normal.dot(point) - constant;
    }

    /**
     * whichSide returns the side at which a point lies on the
     * plane. The positive values returned are: NEGATIVE_SIDE, POSITIVE_SIDE and
     * NO_SIDE.
     * 
     * @param point
     *            the point to check.
     * @return the side at which the point lies.
     */
    public Side whichSide(Vector3f point) {
        float dis = pseudoDistance(point);
        if (dis < 0) {
            return Side.Negative;
        } else if (dis > 0) {
            return Side.Positive;
        } else {
            return Side.None;
        }
    }

    public boolean isOnPlane(Vector3f point){
        float dist = pseudoDistance(point);
        if (dist < FastMath.FLT_EPSILON && dist > -FastMath.FLT_EPSILON)
            return true;
        else
            return false;
    }

    /**
     * Initialize this plane using the three points of the given triangle.
     * 
     * @param t
     *            the triangle
     */
    public void setPlanePoints(AbstractTriangle t) {
        setPlanePoints(t.get1(), t.get2(), t.get3());
    }

    /**
     * Initialize this plane using a point of origin and a normal.
     *
     * @param origin
     * @param normal
     */
    public void setOriginNormal(Vector3f origin, Vector3f normal){
        this.normal.set(normal);
        this.constant = normal.x * origin.x + normal.y * origin.y + normal.z * origin.z;
    }

    /**
     * Initialize the Plane using the given 3 points as coplanar.
     * 
     * @param v1
     *            the first point
     * @param v2
     *            the second point
     * @param v3
     *            the third point
     */
    public void setPlanePoints(Vector3f v1, Vector3f v2, Vector3f v3) {
        normal.set(v2).subtractLocal(v1);
        normal.crossLocal(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z)
                .normalizeLocal();
        constant = normal.dot(v1);
    }

    /**
     * toString returns a string thta represents the string
     * representation of this plane. It represents the normal as a
     * Vector3f object, so the format is the following:
     * com.clockwork.math.Plane [Normal: org.CW.math.Vector3f [X=XX.XXXX, Y=YY.YYYY,
     * Z=ZZ.ZZZZ] - Constant: CC.CCCCC]
     * 
     * @return the string representation of this plane.
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " [Normal: " + normal + " - Constant: "
                + constant + "]";
    }

    public void write(CWExporter e) throws IOException {
        OutputCapsule capsule = e.getCapsule(this);
        capsule.write(normal, "normal", Vector3f.ZERO);
        capsule.write(constant, "constant", 0);
    }

    public void read(CWImporter e) throws IOException {
        InputCapsule capsule = e.getCapsule(this);
        normal = (Vector3f) capsule.readSavable("normal", Vector3f.ZERO.clone());
        constant = capsule.readFloat("constant", 0);
    }

    @Override
    public Plane clone() {
        try {
            Plane p = (Plane) super.clone();
            p.normal = normal.clone();
            return p;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
