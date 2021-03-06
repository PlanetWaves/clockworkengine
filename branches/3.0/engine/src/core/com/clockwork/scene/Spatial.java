
package com.clockwork.scene;

import com.clockwork.asset.AssetKey;
import com.clockwork.asset.CloneableSmartAsset;
import com.clockwork.bounding.BoundingVolume;
import com.clockwork.collision.Collidable;
import com.clockwork.export.*;
import com.clockwork.light.Light;
import com.clockwork.light.LightList;
import com.clockwork.material.Material;
import com.clockwork.math.*;
import com.clockwork.renderer.Camera;
import com.clockwork.renderer.RenderManager;
import com.clockwork.renderer.ViewPort;
import com.clockwork.renderer.queue.RenderQueue;
import com.clockwork.renderer.queue.RenderQueue.Bucket;
import com.clockwork.renderer.queue.RenderQueue.ShadowMode;
import com.clockwork.scene.control.Control;
import com.clockwork.util.SafeArrayList;
import com.clockwork.util.TempVars;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Spatial defines the base class for scene graph nodes. It
 * maintains a link to a parent, it's local transforms and the world's
 * transforms. All other scene graph elements, such as Node and
 * Geometry objects are subclasses of Spatial.
 */
public abstract class Spatial implements Savable, Cloneable, Collidable, CloneableSmartAsset {

    private static final Logger logger = Logger.getLogger(Spatial.class.getName());

    /**
     * Defines how frustum culling is to be handled by 
     * the spatial.
     */
    public enum CullHint {

        /** 
         * Inherit the parent's method. If no parent exists, it is set to Dynamic by default.
         */
        Inherit,
        /**
         * Do not draw if we are not at least partially within the view frustum
         * of the camera. This is determined via the defined
         * Camera planes whether or not this Spatial should be culled.
         */
        Dynamic,
        /** 
         * Always cull this from the view, throwing away this object
         * and any children from rendering commands.
         */
        Always,
        /**
         * Never cull this from view, always draw it. 
         * Note that we will still get culled if our parent is culled.
         */
        Never;
    }

    /**
     * Specifies if this spatial should be batched
     */
    public enum BatchHint {

        /** 
         * Do whatever our parent does. If no parent, default to #Always}.
         */
        Inherit,
        /** 
         * This spatial will always be batched when attached to a BatchNode.
         */
        Always,
        /** 
         * This spatial will never be batched when attached to a BatchNode.
         */
        Never;
    }
    /**
     * Refresh flag types
     */
    protected static final int RF_TRANSFORM = 0x01, // need light resort + combine transforms
                               RF_BOUND = 0x02,
                               RF_LIGHTLIST = 0x04; // changes in light lists 
    
    protected CullHint cullHint = CullHint.Inherit;
    protected BatchHint batchHint = BatchHint.Inherit;
    /** 
     * Spatial's bounding volume relative to the world.
     */
    protected BoundingVolume worldBound;
    /**
     * LightList
     */
    protected LightList localLights;
    protected transient LightList worldLights;
    /** 
     * This spatial's name.
     */
    protected String name;
    // scale values
    protected transient Camera.FrustumIntersect frustrumIntersects = Camera.FrustumIntersect.Intersects;
    protected RenderQueue.Bucket queueBucket = RenderQueue.Bucket.Inherit;
    protected ShadowMode shadowMode = RenderQueue.ShadowMode.Inherit;
    public transient float queueDistance = Float.NEGATIVE_INFINITY;
    protected Transform localTransform;
    protected Transform worldTransform;
    protected SafeArrayList<Control> controls = new SafeArrayList<Control>(Control.class);
    protected HashMap<String, Savable> userData = null;
    /**
     * Used for smart asset caching
     * 
     * see AssetKey#useSmartCache() 
     */
    protected AssetKey key;
    /** 
     * Spatial's parent, or null if it has none.
     */
    protected transient Node parent;
    /**
     * Refresh flags. Indicate what data of the spatial need to be
     * updated to reflect the correct state.
     */
    protected transient int refreshFlags = 0;

    /**
     * Serialisation only. Do not use.
     */
    public Spatial() {
        localTransform = new Transform();
        worldTransform = new Transform();

        localLights = new LightList(this);
        worldLights = new LightList(this);

        refreshFlags |= RF_BOUND;
    }

    /**
     * Constructor instantiates a new Spatial object setting the
     * rotation, translation and scale value to defaults.
     *
     * @param name
     *            the name of the scene element. This is required for
     *            identification and comparison purposes.
     */
    public Spatial(String name) {
        this();
        this.name = name;
    }

    public void setKey(AssetKey key) {
        this.key = key;
    }

    public AssetKey getKey() {
        return key;
    }

    /**
     * Indicate that the transform of this spatial has changed and that
     * a refresh is required.
     */
    protected void setTransformRefresh() {
        refreshFlags |= RF_TRANSFORM;
        setBoundRefresh();
    }

    protected void setLightListRefresh() {
        refreshFlags |= RF_LIGHTLIST;
    }

    /**
     * Indicate that the bounding of this spatial has changed and that
     * a refresh is required.
     */
    protected void setBoundRefresh() {
        refreshFlags |= RF_BOUND;

        Spatial p = parent;
        while (p != null) {
            if ((p.refreshFlags & RF_BOUND) != 0) {
                return;
            }

            p.refreshFlags |= RF_BOUND;
            p = p.parent;
        }
    }
    
    /**
     * (Internal use only) Forces a refresh of the given types of data.
     * 
     * @param transforms Refresh world transform based on parents'
     * @param bounds Refresh bounding volume data based on child nodes
     * @param lights Refresh light list based on parents'
     */
    public void forceRefresh(boolean transforms, boolean bounds, boolean lights) {
        if (transforms) {
            setTransformRefresh();
        }
        if (bounds) {
            setBoundRefresh();
        }
        if (lights) {
            setLightListRefresh();
        }
    }

    /**
     * checkCulling checks the spatial with the camera to see if it
     * should be culled.
     * 
     * This method is called by the renderer. Usually it should not be called
     * directly.
     *
     * @param cam The camera to check against.
     * @return true if inside or intersecting camera frustum
     * (should be rendered), false if outside.
     */
    public boolean checkCulling(Camera cam) {
        if (refreshFlags != 0) {
            throw new IllegalStateException("Scene graph is not properly updated for rendering.\n"
                    + "State was changed after rootNode.updateGeometricState() call. \n"
                    + "Make sure you do not modify the scene from another thread!\n"
                    + "Problem spatial name: " + getName());
        }

        CullHint cm = getCullHint();
        assert cm != CullHint.Inherit;
        if (cm == Spatial.CullHint.Always) {
            setLastFrustumIntersection(Camera.FrustumIntersect.Outside);
            return false;
        } else if (cm == Spatial.CullHint.Never) {
            setLastFrustumIntersection(Camera.FrustumIntersect.Intersects);
            return true;
        }

        // check to see if we can cull this node
        frustrumIntersects = (parent != null ? parent.frustrumIntersects
                : Camera.FrustumIntersect.Intersects);

        if (frustrumIntersects == Camera.FrustumIntersect.Intersects) {
            if (getQueueBucket() == Bucket.Gui) {
                return cam.containsGui(getWorldBound());
            } else {
                frustrumIntersects = cam.contains(getWorldBound());
            }
        }

        return frustrumIntersects != Camera.FrustumIntersect.Outside;
    }

    /**
     * Sets the name of this spatial.
     *
     * @param name
     *            The spatial's new name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this spatial.
     *
     * @return This spatial's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the local LightList}, which are the lights
     * that were directly attached to this Spatial through the
     * #addLight(com.clockwork.light.Light) } and 
     * #removeLight(com.clockwork.light.Light) } methods.
     * 
     * @return The local light list
     */
    public LightList getLocalLightList() {
        return localLights;
    }

    /**
     * Returns the world LightList}, containing the lights
     * combined from all this Spatial's parents up to and including
     * this Spatial's lights.
     * 
     * @return The combined world light list
     */
    public LightList getWorldLightList() {
        return worldLights;
    }

    /**
     * getWorldRotation retrieves the absolute rotation of the
     * Spatial.
     *
     * @return the Spatial's world rotation quaternion.
     */
    public Quaternion getWorldRotation() {
        checkDoTransformUpdate();
        return worldTransform.getRotation();
    }

    /**
     * getWorldTranslation retrieves the absolute translation of
     * the spatial.
     *
     * @return the Spatial's world tranlsation vector.
     */
    public Vector3f getWorldTranslation() {
        checkDoTransformUpdate();
        return worldTransform.getTranslation();
    }

    /**
     * getWorldScale retrieves the absolute scale factor of the
     * spatial.
     *
     * @return the Spatial's world scale factor.
     */
    public Vector3f getWorldScale() {
        checkDoTransformUpdate();
        return worldTransform.getScale();
    }

    /**
     * getWorldTransform retrieves the world transformation
     * of the spatial.
     *
     * @return the world transform.
     */
    public Transform getWorldTransform() {
        checkDoTransformUpdate();
        return worldTransform;
    }

    /**
     * rotateUpTo is a utility function that alters the
     * local rotation to point the Y axis in the direction given by newUp.
     *
     * @param newUp
     *            the up vector to use - assumed to be a unit vector.
     */
    public void rotateUpTo(Vector3f newUp) {
        TempVars vars = TempVars.get();

        Vector3f compVecA = vars.vect1;
        Quaternion q = vars.quat1;

        // First figure out the current up vector.
        Vector3f upY = compVecA.set(Vector3f.UNIT_Y);
        Quaternion rot = localTransform.getRotation();
        rot.multLocal(upY);

        // get angle between vectors
        float angle = upY.angleBetween(newUp);

        // figure out rotation axis by taking cross product
        Vector3f rotAxis = upY.crossLocal(newUp).normalizeLocal();

        // Build a rotation quat and apply current local rotation.
        q.fromAngleNormalAxis(angle, rotAxis);
        q.mult(rot, rot);

        vars.release();

        setTransformRefresh();
    }

    /**
     * lookAt is a convenience method for auto-setting the local
     * rotation based on a position in world space and an up vector. It computes the rotation
     * to transform the z-axis to point onto 'position' and the y-axis to 'up'.
     * Unlike Quaternion#lookAt(com.clockwork.math.Vector3f, com.clockwork.math.Vector3f) } 
     * this method takes a world position to look at and not a relative direction.
     *
     * Note : 28/01/2013 this method has been fixed as it was not taking into account the parent rotation.
     * This was resulting in improper rotation when the spatial had rotated parent nodes.
     * This method is intended to work in world space, so no matter what parent graph the 
     * spatial has, it will look at the given position in world space.
     * 
     * @param position
     *            where to look at in terms of world coordinates
     * @param upVector
     *            a vector indicating the (local) up direction. (typically {0,
     *            1, 0} in CW.)
     */
    public void lookAt(Vector3f position, Vector3f upVector) {
        Vector3f worldTranslation = getWorldTranslation();

        TempVars vars = TempVars.get();

        Vector3f compVecA = vars.vect4;
      
        compVecA.set(position).subtractLocal(worldTranslation);
        getLocalRotation().lookAt(compVecA, upVector);        
        
        if ( getParent() != null ) {
            Quaternion rot=vars.quat1;
            rot =  rot.set(parent.getWorldRotation()).inverseLocal().multLocal(getLocalRotation());
            rot.normalizeLocal();
            setLocalRotation(rot);
        }
        vars.release();
        setTransformRefresh();
    }

    /**
     * Should be overridden by Node and Geometry.
     */
    protected void updateWorldBound() {
        // the world bound of a leaf is the same as it's model bound
        // for a node, the world bound is a combination of all it's children
        // bounds
        // -> handled by subclass
        refreshFlags &= ~RF_BOUND;
    }

    protected void updateWorldLightList() {
        if (parent == null) {
            worldLights.update(localLights, null);
            refreshFlags &= ~RF_LIGHTLIST;
        } else {
            if ((parent.refreshFlags & RF_LIGHTLIST) == 0) {
                worldLights.update(localLights, parent.worldLights);
                refreshFlags &= ~RF_LIGHTLIST;
            } else {
                assert false;
            }
        }
    }

    /**
     * Should only be called from updateGeometricState().
     * In most cases should not be subclassed.
     */
    protected void updateWorldTransforms() {
        if (parent == null) {
            worldTransform.set(localTransform);
            refreshFlags &= ~RF_TRANSFORM;
        } else {
            // check if transform for parent is updated
            assert ((parent.refreshFlags & RF_TRANSFORM) == 0);
            worldTransform.set(localTransform);
            worldTransform.combineWithParent(parent.worldTransform);
            refreshFlags &= ~RF_TRANSFORM;
        }
    }

    /**
     * Computes the world transform of this Spatial in the most 
     * efficient manner possible.
     */
    void checkDoTransformUpdate() {
        if ((refreshFlags & RF_TRANSFORM) == 0) {
            return;
        }

        if (parent == null) {
            worldTransform.set(localTransform);
            refreshFlags &= ~RF_TRANSFORM;
        } else {
            TempVars vars = TempVars.get();

            Spatial[] stack = vars.spatialStack;
            Spatial rootNode = this;
            int i = 0;
            while (true) {
                Spatial hisParent = rootNode.parent;
                if (hisParent == null) {
                    rootNode.worldTransform.set(rootNode.localTransform);
                    rootNode.refreshFlags &= ~RF_TRANSFORM;
                    i--;
                    break;
                }

                stack[i] = rootNode;

                if ((hisParent.refreshFlags & RF_TRANSFORM) == 0) {
                    break;
                }

                rootNode = hisParent;
                i++;
            }

            vars.release();

            for (int j = i; j >= 0; j--) {
                rootNode = stack[j];
                //rootNode.worldTransform.set(rootNode.localTransform);
                //rootNode.worldTransform.combineWithParent(rootNode.parent.worldTransform);
                //rootNode.refreshFlags &= ~RF_TRANSFORM;
                rootNode.updateWorldTransforms();
            }
        }
    }

    /**
     * Computes this Spatial's world bounding volume in the most efficient
     * manner possible.
     */
    void checkDoBoundUpdate() {
        if ((refreshFlags & RF_BOUND) == 0) {
            return;
        }

        checkDoTransformUpdate();

        // Go to children recursively and update their bound
        if (this instanceof Node) {
            Node node = (Node) this;
            int len = node.getQuantity();
            for (int i = 0; i < len; i++) {
                Spatial child = node.getChild(i);
                child.checkDoBoundUpdate();
            }
        }

        // All children's bounds have been updated. Update my own now.
        updateWorldBound();
    }

    private void runControlUpdate(float tpf) {
        if (controls.isEmpty()) {
            return;
        }

        for (Control c : controls.getArray()) {
            c.update(tpf);
        }
    }

    /**
     * Called when the Spatial is about to be rendered, to notify
     * controls attached to this Spatial using the Control.render() method.
     *
     * @param rm The RenderManager rendering the Spatial.
     * @param vp The ViewPort to which the Spatial is being rendered to.
     *
     * see Spatial#addControl(com.clockwork.scene.control.Control)
     * see Spatial#getControl(java.lang.Class) 
     */
    public void runControlRender(RenderManager rm, ViewPort vp) {
        if (controls.isEmpty()) {
            return;
        }

        for (Control c : controls.getArray()) {
            c.render(rm, vp);
        }
    }

    /**
     * Add a control to the list of controls.
     * @param control The control to add.
     *
     * see Spatial#removeControl(java.lang.Class) 
     */
    public void addControl(Control control) {
        controls.add(control);
        control.setSpatial(this);
    }

    /**
     * Removes the first control that is an instance of the given class.
     *
     * see Spatial#addControl(com.clockwork.scene.control.Control) 
     */
    public void removeControl(Class<? extends Control> controlType) {
        for (int i = 0; i < controls.size(); i++) {
            if (controlType.isAssignableFrom(controls.get(i).getClass())) {
                Control control = controls.remove(i);
                control.setSpatial(null);
            }
        }
    }

    /**
     * Removes the given control from this spatial's controls.
     * 
     * @param control The control to remove
     * @return True if the control was successfuly removed. False if 
     * the control is not assigned to this spatial.
     * 
     * see Spatial#addControl(com.clockwork.scene.control.Control) 
     */
    public boolean removeControl(Control control) {
        boolean result = controls.remove(control);
        if (result) {
            control.setSpatial(null);
        }

        return result;
    }

    /**
     * Returns the first control that is an instance of the given class,
     * or null if no such control exists.
     *
     * @param controlType The superclass of the control to look for.
     * @return The first instance in the list of the controlType class, or null.
     *
     * see Spatial#addControl(com.clockwork.scene.control.Control) 
     */
    public <T extends Control> T getControl(Class<T> controlType) {
        for (Control c : controls.getArray()) {
            if (controlType.isAssignableFrom(c.getClass())) {
                return (T) c;
            }
        }
        return null;
    }

    /**
     * Returns the control at the given index in the list.
     *
     * @param index The index of the control in the list to find.
     * @return The control at the given index.
     *
     * @throws IndexOutOfBoundsException
     *      If the index is outside the range [0, getNumControls()-1]
     *
     * see Spatial#addControl(com.clockwork.scene.control.Control)
     */
    public Control getControl(int index) {
        return controls.get(index);
    }

    /**
     * @return The number of controls attached to this Spatial.
     * see Spatial#addControl(com.clockwork.scene.control.Control)
     * see Spatial#removeControl(java.lang.Class) 
     */
    public int getNumControls() {
        return controls.size();
    }

    /**
     * updateLogicalState calls the update() method
     * for all controls attached to this Spatial.
     *
     * @param tpf Time per frame.
     *
     * see Spatial#addControl(com.clockwork.scene.control.Control)
     */
    public void updateLogicalState(float tpf) {
        runControlUpdate(tpf);
    }

    /**
     * updateGeometricState updates the lightlist,
     * computes the world transforms, and computes the world bounds
     * for this Spatial.
     * Calling this when the Spatial is attached to a node
     * will cause undefined results. User code should only call this
     * method on Spatials having no parent.
     * 
     * see Spatial#getWorldLightList()
     * see Spatial#getWorldTransform()
     * see Spatial#getWorldBound()
     */
    public void updateGeometricState() {
        // assume that this Spatial is a leaf, a proper implementation
        // for this method should be provided by Node.

        // NOTE: Update world transforms first because
        // bound transform depends on them.
        if ((refreshFlags & RF_LIGHTLIST) != 0) {
            updateWorldLightList();
        }
        if ((refreshFlags & RF_TRANSFORM) != 0) {
            updateWorldTransforms();
        }
        if ((refreshFlags & RF_BOUND) != 0) {
            updateWorldBound();
        }

        assert refreshFlags == 0;
    }

    /**
     * Convert a vector (in) from this spatials' local coordinate space to world
     * coordinate space.
     *
     * @param in
     *            vector to read from
     * @param store
     *            where to write the result (null to create a new vector, may be
     *            same as in)
     * @return the result (store)
     */
    public Vector3f localToWorld(final Vector3f in, Vector3f store) {
        checkDoTransformUpdate();
        return worldTransform.transformVector(in, store);
    }

    /**
     * Convert a vector (in) from world coordinate space to this spatials' local
     * coordinate space.
     *
     * @param in
     *            vector to read from
     * @param store
     *            where to write the result
     * @return the result (store)
     */
    public Vector3f worldToLocal(final Vector3f in, final Vector3f store) {
        checkDoTransformUpdate();
        return worldTransform.transformInverseVector(in, store);
    }

    /**
     * getParent retrieves this node's parent. If the parent is
     * null this is the root node.
     *
     * @return the parent of this node.
     */
    public Node getParent() {
        return parent;
    }

    /**
     * Called by Node#attachChild(Spatial)} and
     * Node#detachChild(Spatial)} - don't call directly.
     * setParent sets the parent of this node.
     *
     * @param parent
     *            the parent of this node.
     */
    protected void setParent(Node parent) {
        this.parent = parent;
    }

    /**
     * removeFromParent removes this Spatial from it's parent.
     *
     * @return true if it has a parent and performed the remove.
     */
    public boolean removeFromParent() {
        if (parent != null) {
            parent.detachChild(this);
            return true;
        }
        return false;
    }

    /**
     * determines if the provided Node is the parent, or parent's parent, etc. of this Spatial.
     *
     * @param ancestor
     *            the ancestor object to look for.
     * @return true if the ancestor is found, false otherwise.
     */
    public boolean hasAncestor(Node ancestor) {
        if (parent == null) {
            return false;
        } else if (parent.equals(ancestor)) {
            return true;
        } else {
            return parent.hasAncestor(ancestor);
        }
    }

    /**
     * getLocalRotation retrieves the local rotation of this
     * node.
     *
     * @return the local rotation of this node.
     */
    public Quaternion getLocalRotation() {
        return localTransform.getRotation();
    }

    /**
     * setLocalRotation sets the local rotation of this node
     * by using a Matrix3f}.
     *
     * @param rotation
     *            the new local rotation.
     */
    public void setLocalRotation(Matrix3f rotation) {
        localTransform.getRotation().fromRotationMatrix(rotation);
        setTransformRefresh();
    }

    /**
     * setLocalRotation sets the local rotation of this node.
     *
     * @param quaternion
     *            the new local rotation.
     */
    public void setLocalRotation(Quaternion quaternion) {
        localTransform.setRotation(quaternion);
        setTransformRefresh();
    }

    /**
     * getLocalScale retrieves the local scale of this node.
     *
     * @return the local scale of this node.
     */
    public Vector3f getLocalScale() {
        return localTransform.getScale();
    }

    /**
     * setLocalScale sets the local scale of this node.
     *
     * @param localScale
     *            the new local scale, applied to x, y and z
     */
    public void setLocalScale(float localScale) {
        localTransform.setScale(localScale);
        setTransformRefresh();
    }

    /**
     * setLocalScale sets the local scale of this node.
     */
    public void setLocalScale(float x, float y, float z) {
        localTransform.setScale(x, y, z);
        setTransformRefresh();
    }

    /**
     * setLocalScale sets the local scale of this node.
     *
     * @param localScale
     *            the new local scale.
     */
    public void setLocalScale(Vector3f localScale) {
        localTransform.setScale(localScale);
        setTransformRefresh();
    }

    /**
     * getLocalTranslation retrieves the local translation of
     * this node.
     *
     * @return the local translation of this node.
     */
    public Vector3f getLocalTranslation() {
        return localTransform.getTranslation();
    }

    /**
     * setLocalTranslation sets the local translation of this
     * spatial.
     *
     * @param localTranslation
     *            the local translation of this spatial.
     */
    public void setLocalTranslation(Vector3f localTranslation) {
        this.localTransform.setTranslation(localTranslation);
        setTransformRefresh();
    }

    /**
     * setLocalTranslation sets the local translation of this
     * spatial.
     */
    public void setLocalTranslation(float x, float y, float z) {
        this.localTransform.setTranslation(x, y, z);
        setTransformRefresh();
    }

    /**
     * setLocalTransform sets the local transform of this
     * spatial.
     */
    public void setLocalTransform(Transform t) {
        this.localTransform.set(t);
        setTransformRefresh();
    }

    /**
     * getLocalTransform retrieves the local transform of
     * this spatial.
     *
     * @return the local transform of this spatial.
     */
    public Transform getLocalTransform() {
        return localTransform;
    }

    /**
     * Applies the given material to the Spatial, this will propagate the
     * material down to the geometries in the scene graph.
     *
     * @param material The material to set.
     */
    public void setMaterial(Material material) {
    }

    /**
     * addLight adds the given light to the Spatial; causing
     * all child Spatials to be effected by it.
     *
     * @param light The light to add.
     */
    public void addLight(Light light) {
        localLights.add(light);
        setLightListRefresh();
    }

    /**
     * removeLight removes the given light from the Spatial.
     * 
     * @param light The light to remove.
     * see Spatial#addLight(com.clockwork.light.Light) 
     */
    public void removeLight(Light light) {
        localLights.remove(light);
        setLightListRefresh();
    }

    /**
     * Translates the spatial by the given translation vector.
     *
     * @return The spatial on which this method is called, e.g this.
     */
    public Spatial move(float x, float y, float z) {
        this.localTransform.getTranslation().addLocal(x, y, z);
        setTransformRefresh();

        return this;
    }

    /**
     * Translates the spatial by the given translation vector.
     *
     * @return The spatial on which this method is called, e.g this.
     */
    public Spatial move(Vector3f offset) {
        this.localTransform.getTranslation().addLocal(offset);
        setTransformRefresh();

        return this;
    }

    /**
     * Scales the spatial by the given value
     *
     * @return The spatial on which this method is called, e.g this.
     */
    public Spatial scale(float s) {
        return scale(s, s, s);
    }

    /**
     * Scales the spatial by the given scale vector.
     *
     * @return The spatial on which this method is called, e.g this.
     */
    public Spatial scale(float x, float y, float z) {
        this.localTransform.getScale().multLocal(x, y, z);
        setTransformRefresh();

        return this;
    }

    /**
     * Rotates the spatial by the given rotation.
     *
     * @return The spatial on which this method is called, e.g this.
     */
    public Spatial rotate(Quaternion rot) {
        this.localTransform.getRotation().multLocal(rot);
        setTransformRefresh();

        return this;
    }

    /**
     * Rotates the spatial by the xAngle, yAngle and zAngle angles (in radians),
     * (aka pitch, yaw, roll) in the local coordinate space.
     *
     * @return The spatial on which this method is called, e.g this.
     */
    public Spatial rotate(float xAngle, float yAngle, float zAngle) {
        TempVars vars = TempVars.get();
        Quaternion q = vars.quat1;
        q.fromAngles(xAngle, yAngle, zAngle);
        rotate(q);
        vars.release();

        return this;
    }

    /**
     * Centers the spatial in the origin of the world bound.
     * @return The spatial on which this method is called, e.g this.
     */
    public Spatial center() {
        Vector3f worldTrans = getWorldTranslation();
        Vector3f worldCenter = getWorldBound().getCenter();

        Vector3f absTrans = worldTrans.subtract(worldCenter);
        setLocalTranslation(absTrans);

        return this;
    }

    /**
     * see #setCullHint(CullHint)
     * @return the cull mode of this spatial, or if set to CullHint.Inherit,
     * the cullmode of it's parent.
     */
    public CullHint getCullHint() {
        if (cullHint != CullHint.Inherit) {
            return cullHint;
        } else if (parent != null) {
            return parent.getCullHint();
        } else {
            return CullHint.Dynamic;
        }
    }

    public BatchHint getBatchHint() {
        if (batchHint != BatchHint.Inherit) {
            return batchHint;
        } else if (parent != null) {
            return parent.getBatchHint();
        } else {
            return BatchHint.Always;
        }
    }

    /**
     * Returns this spatial's renderqueue bucket. If the mode is set to inherit,
     * then the spatial gets its renderqueue bucket from its parent.
     *
     * @return The spatial's current renderqueue mode.
     */
    public RenderQueue.Bucket getQueueBucket() {
        if (queueBucket != RenderQueue.Bucket.Inherit) {
            return queueBucket;
        } else if (parent != null) {
            return parent.getQueueBucket();
        } else {
            return RenderQueue.Bucket.Opaque;
        }
    }

    /**
     * @return The shadow mode of this spatial, if the local shadow
     * mode is set to inherit, then the parent's shadow mode is returned.
     *
     * see Spatial#setShadowMode(com.clockwork.renderer.queue.RenderQueue.ShadowMode)
     * see ShadowMode
     */
    public RenderQueue.ShadowMode getShadowMode() {
        if (shadowMode != RenderQueue.ShadowMode.Inherit) {
            return shadowMode;
        } else if (parent != null) {
            return parent.getShadowMode();
        } else {
            return ShadowMode.Off;
        }
    }

    /**
     * Sets the level of detail to use when rendering this Spatial,
     * this call propagates to all geometries under this Spatial.
     *
     * @param lod The lod level to set.
     */
    public void setLodLevel(int lod) {
    }

    /**
     * updateModelBound recalculates the bounding object for this
     * Spatial.
     */
    public abstract void updateModelBound();

    /**
     * setModelBound sets the bounding object for this Spatial.
     *
     * @param modelBound
     *            the bounding object for this spatial.
     */
    public abstract void setModelBound(BoundingVolume modelBound);

    /**
     * @return The sum of all verticies under this Spatial.
     */
    public abstract int getVertexCount();

    /**
     * @return The sum of all triangles under this Spatial.
     */
    public abstract int getTriangleCount();

    /**
     * @return A clone of this Spatial, the scene graph in its entirety
     * is cloned and can be altered independently of the original scene graph.
     *
     * Note that meshes of geometries are not cloned explicitly, they
     * are shared if static, or specially cloned if animated.
     *
     * All controls will be cloned using the Control.cloneForSpatial method
     * on the clone.
     *
     * see Mesh#cloneForAnim() 
     */
    public Spatial clone(boolean cloneMaterial) {
        try {
            Spatial clone = (Spatial) super.clone();
            if (worldBound != null) {
                clone.worldBound = worldBound.clone();
            }
            clone.worldLights = worldLights.clone();
            clone.localLights = localLights.clone();

            // Set the new owner of the light lists
            clone.localLights.setOwner(clone);
            clone.worldLights.setOwner(clone);

            // No need to force cloned to update.
            // This node already has the refresh flags
            // set below so it will have to update anyway.
            clone.worldTransform = worldTransform.clone();
            clone.localTransform = localTransform.clone();

            if (clone instanceof Node) {
                Node node = (Node) this;
                Node nodeClone = (Node) clone;
                nodeClone.children = new SafeArrayList<Spatial>(Spatial.class);
                for (Spatial child : node.children) {
                    Spatial childClone = child.clone(cloneMaterial);
                    childClone.parent = nodeClone;
                    nodeClone.children.add(childClone);
                }
            }

            clone.parent = null;
            clone.setBoundRefresh();
            clone.setTransformRefresh();
            clone.setLightListRefresh();

            clone.controls = new SafeArrayList<Control>(Control.class);
            for (int i = 0; i < controls.size(); i++) {
                Control newControl = controls.get(i).cloneForSpatial(clone);
                newControl.setSpatial(clone);
                clone.controls.add(newControl);
            }

            if (userData != null) {
                clone.userData = (HashMap<String, Savable>) userData.clone();
            }

            return clone;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError();
        }
    }

    /**
     * @return A clone of this Spatial, the scene graph in its entirety
     * is cloned and can be altered independently of the original scene graph.
     *
     * Note that meshes of geometries are not cloned explicitly, they
     * are shared if static, or specially cloned if animated.
     *
     * All controls will be cloned using the Control.cloneForSpatial method
     * on the clone.
     *
     * see Mesh#cloneForAnim() 
     */
    @Override
    public Spatial clone() {
        return clone(true);
    }

    /**
     * @return Similar to Spatial.clone() except will create a deep clone
     * of all geometry's meshes, normally this method shouldn't be used
     * instead use Spatial.clone()
     *
     * see Spatial#clone()
     */
    public abstract Spatial deepClone();

    public void setUserData(String key, Object data) {
        if (userData == null) {
            userData = new HashMap<String, Savable>();
        }

        if(data == null){
            userData.remove(key);            
        }else if (data instanceof Savable) {
            userData.put(key, (Savable) data);
        } else {
            userData.put(key, new UserData(UserData.getObjectType(data), data));
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getUserData(String key) {
        if (userData == null) {
            return null;
        }

        Savable s = userData.get(key);
        if (s instanceof UserData) {
            return (T) ((UserData) s).getValue();
        } else {
            return (T) s;
        }
    }

    public Collection<String> getUserDataKeys() {
        if (userData != null) {
            return userData.keySet();
        }

        return Collections.EMPTY_SET;
    }

    /**
     * Note that we are <i>matching</i> the pattern, therefore the pattern
     * must match the entire pattern (i.e. it behaves as if it is sandwiched
     * between "^" and "$").
     * You can set regex modes, like case insensitivity, by using the (?X)
     * or (?X:Y) constructs.
     *
     * @param spatialSubclass Subclass which this must implement.
     *                        Null causes all Spatials to qualify.
     * @param nameRegex  Regular expression to match this name against.
     *                        Null causes all Names to qualify.
     * @return true if this implements the specified class and this's name
     *         matches the specified pattern.
     *
     * see java.util.regex.Pattern
     */
    public boolean matches(Class<? extends Spatial> spatialSubclass,
            String nameRegex) {
        if (spatialSubclass != null && !spatialSubclass.isInstance(this)) {
            return false;
        }

        if (nameRegex != null && (name == null || !name.matches(nameRegex))) {
            return false;
        }

        return true;
    }

    public void write(CWExporter ex) throws IOException {
        OutputCapsule capsule = ex.getCapsule(this);
        capsule.write(name, "name", null);
        capsule.write(worldBound, "world_bound", null);
        capsule.write(cullHint, "cull_mode", CullHint.Inherit);
        capsule.write(batchHint, "batch_hint", BatchHint.Inherit);
        capsule.write(queueBucket, "queue", RenderQueue.Bucket.Inherit);
        capsule.write(shadowMode, "shadow_mode", ShadowMode.Inherit);
        capsule.write(localTransform, "transform", Transform.IDENTITY);
        capsule.write(localLights, "lights", null);

        // Shallow clone the controls array to convert its type.
        capsule.writeSavableArrayList(new ArrayList(controls), "controlsList", null);
        capsule.writeStringSavableMap(userData, "user_data", null);
    }

    public void read(CWImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);

        name = ic.readString("name", null);
        worldBound = (BoundingVolume) ic.readSavable("world_bound", null);
        cullHint = ic.readEnum("cull_mode", CullHint.class, CullHint.Inherit);
        batchHint = ic.readEnum("batch_hint", BatchHint.class, BatchHint.Inherit);
        queueBucket = ic.readEnum("queue", RenderQueue.Bucket.class,
                RenderQueue.Bucket.Inherit);
        shadowMode = ic.readEnum("shadow_mode", ShadowMode.class,
                ShadowMode.Inherit);

        localTransform = (Transform) ic.readSavable("transform", Transform.IDENTITY);

        localLights = (LightList) ic.readSavable("lights", null);
        localLights.setOwner(this);

        //changed for backward compatibility with j3o files generated before the AnimControl/SkeletonControl split
        //the AnimControl creates the SkeletonControl for old files and add it to the spatial.
        //The SkeletonControl must be the last in the stack so we add the list of all other control before it.
        //When backward compatibility won't be needed anymore this can be replaced by : 
        //controls = ic.readSavableArrayList("controlsList", null));
        controls.addAll(0, ic.readSavableArrayList("controlsList", null));

        userData = (HashMap<String, Savable>) ic.readStringSavableMap("user_data", null);
    }

    /**
     * getWorldBound retrieves the world bound at this node
     * level.
     *
     * @return the world bound at this level.
     */
    public BoundingVolume getWorldBound() {
        checkDoBoundUpdate();
        return worldBound;
    }

    /**
     * setCullHint alters how view frustum culling will treat this
     * spatial.
     *
     * @param hint one of: CullHint.Dynamic,
     * CullHint.Always, CullHint.Inherit, or
     * CullHint.Never
     * 
     * The effect of the default value (CullHint.Inherit) may change if the
     * spatial gets re-parented.
     */
    public void setCullHint(CullHint hint) {
        cullHint = hint;
    }

    /**
     * setBatchHint alters how batching will treat this spatial.
     *
     * @param hint one of: BatchHint.Never,
     * BatchHint.Always, or BatchHint.Inherit
     * 
     * The effect of the default value (BatchHint.Inherit) may change if the
     * spatial gets re-parented.
     */
    public void setBatchHint(BatchHint hint) {
        batchHint = hint;
    }

    /**
     * @return the cullmode set on this Spatial
     */
    public CullHint getLocalCullHint() {
        return cullHint;
    }

    /**
     * @return the batchHint set on this Spatial
     */
    public BatchHint getLocalBatchHint() {
        return batchHint;
    }

    /**
     * setQueueBucket determines at what phase of the
     * rendering process this Spatial will rendered. See the
     * Bucket} enum for an explanation of the various 
     * render queue buckets.
     * 
     * @param queueBucket
     *            The bucket to use for this Spatial.
     */
    public void setQueueBucket(RenderQueue.Bucket queueBucket) {
        this.queueBucket = queueBucket;
    }

    /**
     * Sets the shadow mode of the spatial
     * The shadow mode determines how the spatial should be shadowed,
     * when a shadowing technique is used. See the
     * documentation for the class ShadowMode} for more information.
     *
     * see ShadowMode
     *
     * @param shadowMode The local shadow mode to set.
     */
    public void setShadowMode(RenderQueue.ShadowMode shadowMode) {
        this.shadowMode = shadowMode;
    }

    /**
     * @return The locally set queue bucket mode
     *
     * see Spatial#setQueueBucket(com.clockwork.renderer.queue.RenderQueue.Bucket)
     */
    public RenderQueue.Bucket getLocalQueueBucket() {
        return queueBucket;
    }

    /**
     * @return The locally set shadow mode
     *
     * see Spatial#setShadowMode(com.clockwork.renderer.queue.RenderQueue.ShadowMode)
     */
    public RenderQueue.ShadowMode getLocalShadowMode() {
        return shadowMode;
    }

    /**
     * Returns this spatial's last frustum intersection result. This int is set
     * when a check is made to determine if the bounds of the object fall inside
     * a camera's frustum. If a parent is found to fall outside the frustum, the
     * value for this spatial will not be updated.
     *
     * @return The spatial's last frustum intersection result.
     */
    public Camera.FrustumIntersect getLastFrustumIntersection() {
        return frustrumIntersects;
    }

    /**
     * Overrides the last intersection result. This is useful for operations
     * that want to start rendering at the middle of a scene tree and don't want
     * the parent of that node to influence culling.
     *
     * @param intersects
     *            the new value
     */
    public void setLastFrustumIntersection(Camera.FrustumIntersect intersects) {
        frustrumIntersects = intersects;
    }

    /**
     * Returns the Spatial's name followed by the class of the spatial 
     * Example: "MyNode (com.clockwork.scene.Spatial)
     *
     * @return Spatial's name followed by the class of the Spatial
     */
    @Override
    public String toString() {
        return name + " (" + this.getClass().getSimpleName() + ')';
    }

    /**
     * Creates a transform matrix that will convert from this spatials'
     * local coordinate space to the world coordinate space
     * based on the world transform.
     *
     * @param store Matrix where to store the result, if null, a new one
     * will be created and returned.
     *
     * @return store if not null, otherwise, a new matrix containing the result.
     *
     * see Spatial#getWorldTransform() 
     */
    public Matrix4f getLocalToWorldMatrix(Matrix4f store) {
        if (store == null) {
            store = new Matrix4f();
        } else {
            store.loadIdentity();
        }
        // multiply with scale first, then rotate, finally translate (cf.
        // Eberly)
        store.scale(getWorldScale());
        store.multLocal(getWorldRotation());
        store.setTranslation(getWorldTranslation());
        return store;
    }

    /**
     * Visit each scene graph element ordered by DFS
     * @param visitor
     */
    public abstract void depthFirstTraversal(SceneGraphVisitor visitor);

    /**
     * Visit each scene graph element ordered by BFS
     * @param visitor
     */
    public void breadthFirstTraversal(SceneGraphVisitor visitor) {
        Queue<Spatial> queue = new LinkedList<Spatial>();
        queue.add(this);

        while (!queue.isEmpty()) {
            Spatial s = queue.poll();
            visitor.visit(s);
            s.breadthFirstTraversal(visitor, queue);
        }
    }

    protected abstract void breadthFirstTraversal(SceneGraphVisitor visitor, Queue<Spatial> queue);
}
