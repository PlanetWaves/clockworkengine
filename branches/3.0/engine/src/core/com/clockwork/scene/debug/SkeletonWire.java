
package com.clockwork.scene.debug;

import com.clockwork.animation.Bone;
import com.clockwork.animation.Skeleton;
import com.clockwork.math.Vector3f;
import com.clockwork.scene.Mesh;
import com.clockwork.scene.VertexBuffer;
import com.clockwork.scene.VertexBuffer.Format;
import com.clockwork.scene.VertexBuffer.Type;
import com.clockwork.scene.VertexBuffer.Usage;
import com.clockwork.util.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class SkeletonWire extends Mesh {

    private int numConnections = 0;
    private Skeleton skeleton;

    private void countConnections(Bone bone){
        for (Bone child : bone.getChildren()){
            numConnections ++;
            countConnections(child);
        }
    }

    private void writeConnections(ShortBuffer indexBuf, Bone bone){
        for (Bone child : bone.getChildren()){
            // write myself
            indexBuf.put( (short) skeleton.getBoneIndex(bone) );
            // write the child
            indexBuf.put( (short) skeleton.getBoneIndex(child) );

            writeConnections(indexBuf, child);
        }
    }

    public SkeletonWire(Skeleton skeleton){
        this.skeleton = skeleton;
        for (Bone bone : skeleton.getRoots())
            countConnections(bone);

        setMode(Mode.Lines);

        VertexBuffer pb = new VertexBuffer(Type.Position);
        FloatBuffer fpb = BufferUtils.createFloatBuffer(skeleton.getBoneCount() * 3);
        pb.setupData(Usage.Stream, 3, Format.Float, fpb);
        setBuffer(pb);

        VertexBuffer ib = new VertexBuffer(Type.Index);
        ShortBuffer sib = BufferUtils.createShortBuffer(numConnections * 2);
        ib.setupData(Usage.Static, 2, Format.UnsignedShort, sib);
        setBuffer(ib);

        for (Bone bone : skeleton.getRoots())
            writeConnections(sib, bone);
        sib.flip();

        updateCounts();
    }

    public void updateGeometry(){
        VertexBuffer vb = getBuffer(Type.Position);
        FloatBuffer posBuf = getFloatBuffer(Type.Position);
        posBuf.clear();
        for (int i = 0; i < skeleton.getBoneCount(); i++){
            Bone bone = skeleton.getBone(i);
            Vector3f bonePos = bone.getModelSpacePosition();

            posBuf.put(bonePos.getX()).put(bonePos.getY()).put(bonePos.getZ());
        }
        posBuf.flip();
        vb.updateData(posBuf);

        updateBound();
    }
}
