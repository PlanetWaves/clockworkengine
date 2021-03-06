
package com.clockwork.animation;

import com.clockwork.export.*;
import com.clockwork.math.Matrix4f;
import com.clockwork.util.TempVars;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Skeleton is a convenience class for managing a bone hierarchy.
 * Skeleton updates the world transforms to reflect the current local
 * animated matrixes.
 * 
 */
public final class Skeleton implements Savable {

    private Bone[] rootBones;
    private Bone[] boneList;
    
    /**
     * Contains the skinning matrices, multiplying it by a vertex effected by a bone
     * will cause it to go to the animated position.
     */
    private transient Matrix4f[] skinningMatrixes;

    /**
     * Creates a skeleton from a bone list. 
     * The root bones are found automatically.
     * 
     * Note that using this constructor will cause the bones in the list
     * to have their bind pose recomputed based on their local transforms.
     * 
     * @param boneList The list of bones to manage by this Skeleton
     */
    public Skeleton(Bone[] boneList) {
        this.boneList = boneList;

        List<Bone> rootBoneList = new ArrayList<Bone>();
        for (int i = boneList.length - 1; i >= 0; i--) {
            Bone b = boneList[i];
            if (b.getParent() == null) {
                rootBoneList.add(b);
            }
        }
        rootBones = rootBoneList.toArray(new Bone[rootBoneList.size()]);

        createSkinningMatrices();

        for (int i = rootBones.length - 1; i >= 0; i--) {
            Bone rootBone = rootBones[i];
            rootBone.update();
            rootBone.setBindingPose();
        }
    }

    /**
     * Special-purpose copy constructor.
     * 
     * Shallow copies bind pose data from the source skeleton, does not
     * copy any other data.
     * 
     * @param source The source Skeleton to copy from
     */
    public Skeleton(Skeleton source) {
        Bone[] sourceList = source.boneList;
        boneList = new Bone[sourceList.length];
        for (int i = 0; i < sourceList.length; i++) {
            boneList[i] = new Bone(sourceList[i]);
        }

        rootBones = new Bone[source.rootBones.length];
        for (int i = 0; i < rootBones.length; i++) {
            rootBones[i] = recreateBoneStructure(source.rootBones[i]);
        }
        createSkinningMatrices();

        for (int i = rootBones.length - 1; i >= 0; i--) {
            rootBones[i].update();
        }
    }

    /**
     * Serialisation only. Do not use.
     */
    public Skeleton() {
    }

    private void createSkinningMatrices() {
        skinningMatrixes = new Matrix4f[boneList.length];
        for (int i = 0; i < skinningMatrixes.length; i++) {
            skinningMatrixes[i] = new Matrix4f();
        }
    }

    private Bone recreateBoneStructure(Bone sourceRoot) {
        Bone targetRoot = getBone(sourceRoot.getName());
        List<Bone> children = sourceRoot.getChildren();
        for (int i = 0; i < children.size(); i++) {
            Bone sourceChild = children.get(i);
            // find my version of the child
            Bone targetChild = getBone(sourceChild.getName());
            targetRoot.addChild(targetChild);
            recreateBoneStructure(sourceChild);
        }

        return targetRoot;
    }

    /**
     * Updates world transforms for all bones in this skeleton.
     * Typically called after setting local animation transforms.
     */
    public void updateWorldVectors() {
        for (int i = rootBones.length - 1; i >= 0; i--) {
            rootBones[i].update();
        }
    }

    /**
     * Saves the current skeleton state as it's binding pose.
     */
    public void setBindingPose() {
        for (int i = rootBones.length - 1; i >= 0; i--) {
            rootBones[i].setBindingPose();
        }
    }

    /**
     * Reset the skeleton to bind pose.
     */
    public final void reset() {
        for (int i = rootBones.length - 1; i >= 0; i--) {
            rootBones[i].reset();
        }
    }

    /**
     * Reset the skeleton to base pose and updates the bones
     */
    public final void resetAndUpdate() {
        for (int i = rootBones.length - 1; i >= 0; i--) {
            Bone rootBone = rootBones[i];
            rootBone.reset();
            rootBone.update();
        }
    }

    /**
     * returns the array of all root bones of this skeleton
     * @return 
     */
    public Bone[] getRoots() {
        return rootBones;
    }

    /**
     * return a bone for the given index
     * @param index
     * @return 
     */
    public Bone getBone(int index) {
        return boneList[index];
    }

    /**
     * returns the bone with the given name
     * @param name
     * @return 
     */
    public Bone getBone(String name) {
        for (int i = 0; i < boneList.length; i++) {
            if (boneList[i].getName().equals(name)) {
                return boneList[i];
            }
        }
        return null;
    }

    /**
     * returns the bone index of the given bone
     * @param bone
     * @return 
     */
    public int getBoneIndex(Bone bone) {
        for (int i = 0; i < boneList.length; i++) {
            if (boneList[i] == bone) {
                return i;
            }
        }

        return -1;
    }

    /**
     * returns the bone index of the bone that has the given name
     * @param name
     * @return 
     */
    public int getBoneIndex(String name) {
        for (int i = 0; i < boneList.length; i++) {
            if (boneList[i].getName().equals(name)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Compute the skining matrices for each bone of the skeleton that would be used to transform vertices of associated meshes
     * @return 
     */
    public Matrix4f[] computeSkinningMatrices() {
        TempVars vars = TempVars.get();
        for (int i = 0; i < boneList.length; i++) {
            boneList[i].getOffsetTransform(skinningMatrixes[i], vars.quat1, vars.vect1, vars.vect2, vars.tempMat3);
        }
        vars.release();
        return skinningMatrixes;
    }

    /**
     * returns the number of bones of this skeleton
     * @return 
     */
    public int getBoneCount() {
        return boneList.length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Skeleton - ").append(boneList.length).append(" bones, ").append(rootBones.length).append(" roots\n");
        for (Bone rootBone : rootBones) {
            sb.append(rootBone.toString());
        }
        return sb.toString();
    }

    public void read(CWImporter im) throws IOException {
        InputCapsule input = im.getCapsule(this);

        Savable[] boneRootsAsSav = input.readSavableArray("rootBones", null);
        rootBones = new Bone[boneRootsAsSav.length];
        System.arraycopy(boneRootsAsSav, 0, rootBones, 0, boneRootsAsSav.length);

        Savable[] boneListAsSavable = input.readSavableArray("boneList", null);
        boneList = new Bone[boneListAsSavable.length];
        System.arraycopy(boneListAsSavable, 0, boneList, 0, boneListAsSavable.length);

        createSkinningMatrices();

        for (Bone rootBone : rootBones) {
            rootBone.update();
            rootBone.setBindingPose();
        }
    }

    public void write(CWExporter ex) throws IOException {
        OutputCapsule output = ex.getCapsule(this);
        output.write(rootBones, "rootBones", null);
        output.write(boneList, "boneList", null);
    }
}
