
package com.clockwork.animation;

import com.clockwork.export.*;
import com.clockwork.math.*;
import com.clockwork.scene.Node;
import com.clockwork.util.TempVars;
import java.io.IOException;
import java.util.ArrayList;

/**
 * <code>Bone</code> describes a bone in the bone-weight skeletal animation
 * system. A bone contains a name and an index, as well as relevant
 * transformation data.
 *
 */
public final class Bone implements Savable {

    private String name;
    private Bone parent;
    private final ArrayList<Bone> children = new ArrayList<Bone>();
    /**
     * If enabled, user can control bone transform with setUserTransforms.
     * Animation transforms are not applied to this bone when enabled.
     */
    private boolean userControl = false;
    /**
     * The attachment node.
     */
    private Node attachNode;
    /**
     * Initial transform is the local bind transform of this bone.
     * PARENT SPACE -> BONE SPACE
     */
    private Vector3f initialPos;
    private Quaternion initialRot;
    private Vector3f initialScale;
    /**
     * The inverse world bind transform.
     * BONE SPACE -> MODEL SPACE
     */
    private Vector3f worldBindInversePos;
    private Quaternion worldBindInverseRot;
    private Vector3f worldBindInverseScale;
    /**
     * The local animated transform combined with the local bind transform and parent world transform
     */
    private Vector3f localPos = new Vector3f();
    private Quaternion localRot = new Quaternion();
    private Vector3f localScale = new Vector3f(1.0f, 1.0f, 1.0f);
    /**
     * MODEL SPACE -> BONE SPACE (in animated state)
     */
    private Vector3f worldPos = new Vector3f();
    private Quaternion worldRot = new Quaternion();
    private Vector3f worldScale = new Vector3f();
    
    // Used for getCombinedTransform
    private Transform tmpTransform = new Transform();
    
    /**
     * Used to handle blending from one animation to another.
     * See {@link #blendAnimTransforms(com.clockwork.math.Vector3f, com.clockwork.math.Quaternion, com.clockwork.math.Vector3f, float)}
     * on how this variable is used.
     */
    private transient float currentWeightSum = -1;

    /**
     * Creates a new bone with the given name.
     * 
     * @param name Name to give to this bone
     */
    public Bone(String name) {
        if (name == null)
            throw new IllegalArgumentException("Name cannot be null");
        
        this.name = name;

        initialPos = new Vector3f();
        initialRot = new Quaternion();
        initialScale = new Vector3f(1, 1, 1);

        worldBindInversePos = new Vector3f();
        worldBindInverseRot = new Quaternion();
        worldBindInverseScale = new Vector3f();
    }

    /**
     * Special-purpose copy constructor. 
     * <p>
     * Only copies the name and bind pose from the original.
     * <p>
     * WARNING: Local bind pose and world inverse bind pose transforms shallow 
     * copied. Modifying that data on the original bone will cause it to
     * be recomputed on any cloned bones.
     * <p>
     * The rest of the data is <em>NOT</em> copied, as it will be
     * generated automatically when the bone is animated.
     * 
     * @param source The bone from which to copy the data.
     */
    Bone(Bone source) {
        this.name = source.name;

        userControl = source.userControl;

        initialPos = source.initialPos;
        initialRot = source.initialRot;
        initialScale = source.initialScale;

        worldBindInversePos = source.worldBindInversePos;
        worldBindInverseRot = source.worldBindInverseRot;
        worldBindInverseScale = source.worldBindInverseScale;

        // parent and children will be assigned manually..
    }

    /**
     * Serialisation only. Do not use.
     */
    public Bone() {
    }

    /**
     * Returns the name of the bone, set in the constructor.
     * 
     * @return The name of the bone, set in the constructor.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns parent bone of this bone, or null if it is a root bone.
     * @return The parent bone of this bone, or null if it is a root bone.
     */
    public Bone getParent() {
        return parent;
    }

    /**
     * Returns all the children bones of this bone.
     * 
     * @return All the children bones of this bone.
     */
    public ArrayList<Bone> getChildren() {
        return children;
    }

    /**
     * Returns the local position of the bone, relative to the parent bone.
     * 
     * @return The local position of the bone, relative to the parent bone.
     */
    public Vector3f getLocalPosition() {
        return localPos;
    }

    /**
     * Returns the local rotation of the bone, relative to the parent bone.
     * 
     * @return The local rotation of the bone, relative to the parent bone.
     */
    public Quaternion getLocalRotation() {
        return localRot;
    }

    /**
     * Returns the local scale of the bone, relative to the parent bone.
     * 
     * @return The local scale of the bone, relative to the parent bone.
     */
    public Vector3f getLocalScale() {
        return localScale;
    }

    /**
     * Returns the position of the bone in model space.
     * 
     * @return The position of the bone in model space.
     */
    public Vector3f getModelSpacePosition() {
        return worldPos;
    }

    /**
     * Returns the rotation of the bone in model space.
     * 
     * @return The rotation of the bone in model space.
     */
    public Quaternion getModelSpaceRotation() {
        return worldRot;
    }

    /**
     * Returns the scale of the bone in model space.
     * 
     * @return The scale of the bone in model space.
     */
    public Vector3f getModelSpaceScale() {
        return worldScale;
    }

    /**
     * Returns the inverse world bind pose position.
     * <p>
     * The bind pose transform of the bone is its "default"
     * transform with no animation applied.
     * 
     * @return the inverse world bind pose position.
     */
    public Vector3f getWorldBindInversePosition() {
        return worldBindInversePos;
    }

    /**
     * Returns the inverse world bind pose rotation.
     * <p>
     * The bind pose transform of the bone is its "default"
     * transform with no animation applied.
     * 
     * @return the inverse world bind pose rotation.
     */
    public Quaternion getWorldBindInverseRotation() {
        return worldBindInverseRot;
    }

    /**
     * Returns the inverse world bind pose scale.
     * <p>
     * The bind pose transform of the bone is its "default"
     * transform with no animation applied.
     * 
     * @return the inverse world bind pose scale.
     */
    public Vector3f getWorldBindInverseScale() {
        return worldBindInverseScale;
    }

    /**
     * Returns the world bind pose position.
     * <p>
     * The bind pose transform of the bone is its "default"
     * transform with no animation applied.
     * 
     * @return the world bind pose position.
     */
    public Vector3f getWorldBindPosition() {
        return initialPos;
    }

    /**
     * Returns the world bind pose rotation.
     * <p>
     * The bind pose transform of the bone is its "default"
     * transform with no animation applied.
     * 
     * @return the world bind pose rotation.
     */
    public Quaternion getWorldBindRotation() {
        return initialRot;
    }

    /**
     * Returns the world bind pose scale.
     * <p>
     * The bind pose transform of the bone is its "default"
     * transform with no animation applied.
     * 
     * @return the world bind pose scale.
     */
    public Vector3f getWorldBindScale() {
        return initialScale;
    }

    /**
     * If enabled, user can control bone transform with setUserTransforms.
     * Animation transforms are not applied to this bone when enabled.
     */
    public void setUserControl(boolean enable) {
        userControl = enable;
    }

    /**
     * Add a new child to this bone. Shouldn't be used by user code.
     * Can corrupt skeleton.
     * 
     * @param bone The bone to add
     */
    public void addChild(Bone bone) {
        children.add(bone);
        bone.parent = this;
    }

    /**
     * Updates the world transforms for this bone, and, possibly the attach node
     * if not null.
     * <p>
     * The world transform of this bone is computed by combining the parent's
     * world transform with this bones' local transform.
     */
    public final void updateWorldVectors() {
        if (currentWeightSum == 1f) {
            currentWeightSum = -1;
        } else if (currentWeightSum != -1f) {
            // Apply the weight to the local transform
            if (currentWeightSum == 0) {
                localRot.set(initialRot);
                localPos.set(initialPos);
                localScale.set(initialScale);
            } else {
                float invWeightSum = 1f - currentWeightSum;
                localRot.nlerp(initialRot, invWeightSum);
                localPos.interpolate(initialPos, invWeightSum);
                localScale.interpolate(initialScale, invWeightSum);
            }
            
            // Future invocations of transform blend will start over.
            currentWeightSum = -1;
        }
        
        if (parent != null) {
            //rotation
            parent.worldRot.mult(localRot, worldRot);

            //scale
            //For scale parent scale is not taken into account!
            // worldScale.set(localScale);
            parent.worldScale.mult(localScale, worldScale);

            //translation
            //scale and rotation of parent affect bone position            
            parent.worldRot.mult(localPos, worldPos);
            worldPos.multLocal(parent.worldScale);
            worldPos.addLocal(parent.worldPos);
        } else {
            worldRot.set(localRot);
            worldPos.set(localPos);
            worldScale.set(localScale);
        }

        if (attachNode != null) {
            attachNode.setLocalTranslation(worldPos);
            attachNode.setLocalRotation(worldRot);
            attachNode.setLocalScale(worldScale);
        }
    }

    /**
     * Updates world transforms for this bone and it's children.
     */
    final void update() {
        this.updateWorldVectors();

        for (int i = children.size() - 1; i >= 0; i--) {
            children.get(i).update();
        }
    }

    /**
     * Saves the current bone state as its binding pose, including its children.
     */
    void setBindingPose() {
        initialPos.set(localPos);
        initialRot.set(localRot);
        initialScale.set(localScale);

        if (worldBindInversePos == null) {
            worldBindInversePos = new Vector3f();
            worldBindInverseRot = new Quaternion();
            worldBindInverseScale = new Vector3f();
        }

        // Save inverse derived position/scale/orientation, used for calculate offset transform later
        worldBindInversePos.set(worldPos);
        worldBindInversePos.negateLocal();

        worldBindInverseRot.set(worldRot);
        worldBindInverseRot.inverseLocal();

        worldBindInverseScale.set(Vector3f.UNIT_XYZ);
        worldBindInverseScale.divideLocal(worldScale);

        for (Bone b : children) {
            b.setBindingPose();
        }
    }

    /**
     * Reset the bone and it's children to bind pose.
     */
    final void reset() {
        if (!userControl) {
            localPos.set(initialPos);
            localRot.set(initialRot);
            localScale.set(initialScale);
        }

        for (int i = children.size() - 1; i >= 0; i--) {
            children.get(i).reset();
        }
    }

     /**
     * Stores the skinning transform in the specified Matrix4f.
     * The skinning transform applies the animation of the bone to a vertex.
     * 
     * This assumes that the world transforms for the entire bone hierarchy
     * have already been computed, otherwise this method will return undefined
     * results.
     * 
     * @param outTransform
     */
    void getOffsetTransform(Matrix4f outTransform, Quaternion tmp1, Vector3f tmp2, Vector3f tmp3, Matrix3f tmp4) {
        // Computing scale
        Vector3f scale = worldScale.mult(worldBindInverseScale, tmp3);

        // Computing rotation
        Quaternion rotate = worldRot.mult(worldBindInverseRot, tmp1);

        // Computing translation
        // Translation depend on rotation and scale
        Vector3f translate = worldPos.add(rotate.mult(scale.mult(worldBindInversePos, tmp2), tmp2), tmp2);

        // Populating the matrix
        outTransform.loadIdentity();
        outTransform.setTransform(translate, scale, rotate.toRotationMatrix(tmp4));
    }

    /**
     * Sets user transform.
     */
    public void setUserTransforms(Vector3f translation, Quaternion rotation, Vector3f scale) {
        if (!userControl) {
            throw new IllegalStateException("User control must be on bone to allow user transforms");
        }

        localPos.set(initialPos);
        localRot.set(initialRot);
        localScale.set(initialScale);

        localPos.addLocal(translation);
        localRot = localRot.mult(rotation);
        localScale.multLocal(scale);
    }

    /**
     * Must update all bones in skeleton for this to work.
     * @param translation
     * @param rotation
     */
    public void setUserTransformsWorld(Vector3f translation, Quaternion rotation) {
        if (!userControl) {
            throw new IllegalStateException("User control must be on bone to allow user transforms");
        }

        // TODO: add scale here ???
        worldPos.set(translation);
        worldRot.set(rotation);
        
        //if there is an attached Node we need to set it's local transforms too.
        if(attachNode != null){
            attachNode.setLocalTranslation(translation);
            attachNode.setLocalRotation(rotation);
        }
    }

    /**
     * Returns the local transform of this bone combined with the given position and rotation
     * @param position a position
     * @param rotation a rotation
     */
    public Transform getCombinedTransform(Vector3f position, Quaternion rotation) {
        rotation.mult(localPos, tmpTransform.getTranslation()).addLocal(position);
        tmpTransform.setRotation(rotation).getRotation().multLocal(localRot);
        return tmpTransform;
    }

    /**
     * Returns the attachment node.
     * Attach models and effects to this node to make
     * them follow this bone's motions.
     */
    Node getAttachmentsNode() {
        if (attachNode == null) {
            attachNode = new Node(name + "_attachnode");
            attachNode.setUserData("AttachedBone", this);
        }
        return attachNode;
    }

    /**
     * Used internally after model cloning.
     * @param attachNode
     */
    void setAttachmentsNode(Node attachNode) {
        this.attachNode = attachNode;
    }

    /**
     * Sets the local animation transform of this bone.
     * Bone is assumed to be in bind pose when this is called.
     */
    void setAnimTransforms(Vector3f translation, Quaternion rotation, Vector3f scale) {
        if (userControl) {
            return;
        }

//        localPos.addLocal(translation);
//        localRot.multLocal(rotation);
        //localRot = localRot.mult(rotation);

        localPos.set(initialPos).addLocal(translation);
        localRot.set(initialRot).multLocal(rotation);

        if (scale != null) {
            localScale.set(initialScale).multLocal(scale);
        }
    }

    /**
     * Blends the given animation transform onto the bone's local transform.
     * <p>
     * Subsequent calls of this method stack up, with the final transformation
     * of the bone computed at {@link #updateWorldVectors() } which resets
     * the stack.
     * <p>
     * E.g. a single transform blend with weight = 0.5 followed by an
     * updateWorldVectors() call will result in final transform = transform * 0.5.
     * Two transform blends with weight = 0.5 each will result in the two
     * transforms blended together (nlerp) with blend = 0.5.
     * 
     * @param translation The translation to blend in
     * @param rotation The rotation to blend in
     * @param scale The scale to blend in
     * @param weight The weight of the transform to apply. Set to 1.0 to prevent
     * any other transform from being applied until updateWorldVectors().
     */
    void blendAnimTransforms(Vector3f translation, Quaternion rotation, Vector3f scale, float weight) {
        if (userControl) {
            return;
        }
        
        if (weight == 0) {
            // Do not apply this transform at all.
            return;
        }

        if (currentWeightSum == 1){
            return; // More than 2 transforms are being blended
        } else if (currentWeightSum == -1 || currentWeightSum == 0) {
            // Set the transform fully
            localPos.set(initialPos).addLocal(translation);
            localRot.set(initialRot).multLocal(rotation);
            if (scale != null) {
                localScale.set(initialScale).multLocal(scale);
            }
            // Set the weight. It will be applied in updateWorldVectors().
            currentWeightSum = weight;
        } else {
            // The weight is already set. 
            // Blend in the new transform.
            TempVars vars = TempVars.get();

            Vector3f tmpV = vars.vect1;
            Vector3f tmpV2 = vars.vect2;
            Quaternion tmpQ = vars.quat1;
            
            tmpV.set(initialPos).addLocal(translation);
            localPos.interpolate(tmpV, weight);

            tmpQ.set(initialRot).multLocal(rotation);
            localRot.nlerp(tmpQ, weight);

            if (scale != null) {
                tmpV2.set(initialScale).multLocal(scale);
                localScale.interpolate(tmpV2, weight);
            }
        
            // Ensures no new weights will be blended in the future.
            currentWeightSum = 1;
            
            vars.release();
        }
    }

    /**
     * Sets local bind transform for bone.
     * Call setBindingPose() after all of the skeleton bones' bind transforms are set to save them.
     */
    public void setBindTransforms(Vector3f translation, Quaternion rotation, Vector3f scale) {
        initialPos.set(translation);
        initialRot.set(rotation);
        //ogre.xml can have null scale values breaking this if the check is removed
        if (scale != null) {
            initialScale.set(scale);
        }

        localPos.set(translation);
        localRot.set(rotation);
        if (scale != null) {
            localScale.set(scale);
        }
    }

    private String toString(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            sb.append('-');
        }

        sb.append(name).append(" bone\n");
        for (Bone child : children) {
            sb.append(child.toString(depth + 1));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return this.toString(0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void read(JmeImporter im) throws IOException {
        InputCapsule input = im.getCapsule(this);

        name = input.readString("name", null);
        initialPos = (Vector3f) input.readSavable("initialPos", null);
        initialRot = (Quaternion) input.readSavable("initialRot", null);
        initialScale = (Vector3f) input.readSavable("initialScale", new Vector3f(1.0f, 1.0f, 1.0f));
        attachNode = (Node) input.readSavable("attachNode", null);

        localPos.set(initialPos);
        localRot.set(initialRot);

        ArrayList<Bone> childList = input.readSavableArrayList("children", null);
        for (int i = childList.size() - 1; i >= 0; i--) {
            this.addChild(childList.get(i));
        }

        // NOTE: Parent skeleton will call update() then setBindingPose()
        // after Skeleton has been de-serialised.
        // Therefore, worldBindInversePos and worldBindInverseRot
        // will be reconstructed based on that information.
    }

    @Override
    public void write(JmeExporter ex) throws IOException {
        OutputCapsule output = ex.getCapsule(this);

        output.write(name, "name", null);
        output.write(attachNode, "attachNode", null);
        output.write(initialPos, "initialPos", null);
        output.write(initialRot, "initialRot", null);
        output.write(initialScale, "initialScale", new Vector3f(1.0f, 1.0f, 1.0f));
        output.writeSavableArrayList(children, "children", null);
    }
}
