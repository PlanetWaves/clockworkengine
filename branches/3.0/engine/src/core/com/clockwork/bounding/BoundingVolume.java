
package com.clockwork.bounding;

import com.clockwork.collision.Collidable;
import com.clockwork.export.CWExporter;
import com.clockwork.export.CWImporter;
import com.clockwork.export.Savable;
import com.clockwork.math.*;
import java.io.IOException;
import java.nio.FloatBuffer;

/**
 * BoundingVolume defines an interface for dealing with
 * containment of a collection of points.
 * 
 * 
 * @version $Id: BoundingVolume.java,v 1.24 2007/09/21 15:45:32 nca Exp $
 */
public abstract class BoundingVolume implements Savable, Cloneable, Collidable {

    /**
     * The type of bounding volume being used.
     */
    public enum Type {
        /**
         * BoundingSphere}
         */
        Sphere, 
        
        /**
         * BoundingBox}.
         */
        AABB, 
        
        /**
         * Currently unsupported by CW.
         */
        Capsule;
    }

    protected int checkPlane = 0;
    protected Vector3f center = new Vector3f();

    public BoundingVolume() {
    }

    public BoundingVolume(Vector3f center) {
        this.center.set(center);
    }

    /**
     * Grabs the checkplane we should check first.
     *
     */
    public int getCheckPlane() {
        return checkPlane;
    }

    /**
     * Sets the index of the plane that should be first checked during rendering.
     *
     * @param value
     */
    public final void setCheckPlane(int value) {
        checkPlane = value;
    }

    /**
     * getType returns the type of bounding volume this is.
     */
    public abstract Type getType();

    /**
     *
     * transform alters the location of the bounding volume by a
     * rotation, translation and a scalar.
     *
     * @param trans
     *            the transform to affect the bound.
     * @return the new bounding volume.
     */
    public final BoundingVolume transform(Transform trans) {
        return transform(trans, null);
    }

    /**
     *
     * transform alters the location of the bounding volume by a
     * rotation, translation and a scalar.
     *
     * @param trans
     *            the transform to affect the bound.
     * @param store
     *            sphere to store result in
     * @return the new bounding volume.
     */
    public abstract BoundingVolume transform(Transform trans, BoundingVolume store);

    public abstract BoundingVolume transform(Matrix4f trans, BoundingVolume store);

    /**
     *
     * whichSide returns the side on which the bounding volume
     * lies on a plane. Possible values are POSITIVE_SIDE, NEGATIVE_SIDE, and
     * NO_SIDE.
     *
     * @param plane
     *            the plane to check against this bounding volume.
     * @return the side on which this bounding volume lies.
     */
    public abstract Plane.Side whichSide(Plane plane);

    /**
     *
     * computeFromPoints generates a bounding volume that
     * encompasses a collection of points.
     *
     * @param points
     *            the points to contain.
     */
    public abstract void computeFromPoints(FloatBuffer points);

    /**
     * merge combines two bounding volumes into a single bounding
     * volume that contains both this bounding volume and the parameter volume.
     *
     * @param volume
     *            the volume to combine.
     * @return the new merged bounding volume.
     */
    public abstract BoundingVolume merge(BoundingVolume volume);

    /**
     * mergeLocal combines two bounding volumes into a single
     * bounding volume that contains both this bounding volume and the parameter
     * volume. The result is stored locally.
     *
     * @param volume
     *            the volume to combine.
     * @return this
     */
    public abstract BoundingVolume mergeLocal(BoundingVolume volume);

    /**
     * clone creates a new BoundingVolume object containing the
     * same data as this one.
     *
     * @param store
     *            where to store the cloned information. if null or wrong class,
     *            a new store is created.
     * @return the new BoundingVolume
     */
    public abstract BoundingVolume clone(BoundingVolume store);

    public final Vector3f getCenter() {
        return center;
    }

    public final Vector3f getCenter(Vector3f store) {
        store.set(center);
        return store;
    }

    public final void setCenter(Vector3f newCenter) {
        center.set(newCenter);
    }

    /**
     * Find the distance from the center of this Bounding Volume to the given
     * point.
     * 
     * @param point
     *            The point to get the distance to
     * @return distance
     */
    public final float distanceTo(Vector3f point) {
        return center.distance(point);
    }

    /**
     * Find the squared distance from the center of this Bounding Volume to the
     * given point.
     * 
     * @param point
     *            The point to get the distance to
     * @return distance
     */
    public final float distanceSquaredTo(Vector3f point) {
        return center.distanceSquared(point);
    }

    /**
     * Find the distance from the nearest edge of this Bounding Volume to the given
     * point.
     * 
     * @param point
     *            The point to get the distance to
     * @return distance
     */
    public abstract float distanceToEdge(Vector3f point);

    /**
     * determines if this bounding volume and a second given volume are
     * intersecting. Intersecting being: one volume contains another, one volume
     * overlaps another or one volume touches another.
     *
     * @param bv
     *            the second volume to test against.
     * @return true if this volume intersects the given volume.
     */
    public abstract boolean intersects(BoundingVolume bv);

    /**
     * determines if a ray intersects this bounding volume.
     *
     * @param ray
     *            the ray to test.
     * @return true if this volume is intersected by a given ray.
     */
    public abstract boolean intersects(Ray ray);


    /**
     * determines if this bounding volume and a given bounding sphere are
     * intersecting.
     *
     * @param bs
     *            the bounding sphere to test against.
     * @return true if this volume intersects the given bounding sphere.
     */
    public abstract boolean intersectsSphere(BoundingSphere bs);

    /**
     * determines if this bounding volume and a given bounding box are
     * intersecting.
     *
     * @param bb
     *            the bounding box to test against.
     * @return true if this volume intersects the given bounding box.
     */
    public abstract boolean intersectsBoundingBox(BoundingBox bb);

    /**
     * determines if this bounding volume and a given bounding box are
     * intersecting.
     *
     * @param bb
     *            the bounding box to test against.
     * @return true if this volume intersects the given bounding box.
     */
//	public abstract boolean intersectsOrientedBoundingBox(OrientedBoundingBox bb);
    /**
     * 
     * determines if a given point is contained within this bounding volume.
     * If the point is on the edge of the bounding volume, this method will
     * return false. Use intersects(Vector3f) to check for edge intersection.
     * 
     * @param point
     *            the point to check
     * @return true if the point lies within this bounding volume.
     */
    public abstract boolean contains(Vector3f point);

    /**
     * Determines if a given point intersects (touches or is inside) this bounding volume.
     * @param point the point to check
     * @return true if the point lies within this bounding volume.
     */
    public abstract boolean intersects(Vector3f point);

    public abstract float getVolume();

    @Override
    public BoundingVolume clone() {
        try{
            BoundingVolume clone = (BoundingVolume) super.clone();
            clone.center = center.clone();
            return clone;
        }catch (CloneNotSupportedException ex){
            throw new AssertionError();
        }
    }

    public void write(CWExporter e) throws IOException {
        e.getCapsule(this).write(center, "center", Vector3f.ZERO);
    }

    public void read(CWImporter e) throws IOException {
        center = (Vector3f) e.getCapsule(this).readSavable("center", Vector3f.ZERO.clone());
    }
 
}

