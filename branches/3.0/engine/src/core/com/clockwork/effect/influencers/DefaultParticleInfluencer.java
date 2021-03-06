
package com.clockwork.effect.influencers;

import com.clockwork.effect.Particle;
import com.clockwork.effect.shapes.EmitterShape;
import com.clockwork.export.InputCapsule;
import com.clockwork.export.CWExporter;
import com.clockwork.export.CWImporter;
import com.clockwork.export.OutputCapsule;
import com.clockwork.math.FastMath;
import com.clockwork.math.Vector3f;
import java.io.IOException;

/**
 * This emitter influences the particles so that they move all in the same direction.
 * The direction may vary a little if the velocity variation is non zero.
 * This influencer is default for the particle emitter.
 */
public class DefaultParticleInfluencer implements ParticleInfluencer {

    //Version #1 : changed startVelocity to initialvelocity for consistency with accessors 
    //and also changed it in serialisation
    public static final int SAVABLE_VERSION = 1;
    /** Temporary variable used to help with calculations. */
    protected transient Vector3f temp = new Vector3f();
    /** The initial velocity of the particles. */
    protected Vector3f initialVelocity = new Vector3f();
    /** The velocity's variation of the particles. */
    protected float velocityVariation = 0.2f;

    @Override
    public void influenceParticle(Particle particle, EmitterShape emitterShape) {
        emitterShape.getRandomPoint(particle.position);
        this.applyVelocityVariation(particle);
    }

    /**
     * This method applies the variation to the particle with already set velocity.
     * @param particle
     *        the particle to be affected
     */
    protected void applyVelocityVariation(Particle particle) {
    	particle.velocity.set(initialVelocity);
        temp.set(FastMath.nextRandomFloat(), FastMath.nextRandomFloat(), FastMath.nextRandomFloat());
        temp.multLocal(2f);
        temp.subtractLocal(1f, 1f, 1f);
        temp.multLocal(initialVelocity.length());
        particle.velocity.interpolate(temp, velocityVariation);
    }

    @Override
    public void write(CWExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(initialVelocity, "initialVelocity", Vector3f.ZERO);
        oc.write(velocityVariation, "variation", 0.2f);
    }

    @Override
    public void read(CWImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);
        // NOTE: In previous versions of CW, initialVelocity was called startVelocity
        if (ic.getSavableVersion(DefaultParticleInfluencer.class) == 0){
            initialVelocity = (Vector3f) ic.readSavable("startVelocity", Vector3f.ZERO.clone());
        }else{
            initialVelocity = (Vector3f) ic.readSavable("initialVelocity", Vector3f.ZERO.clone());
        }       
        velocityVariation = ic.readFloat("variation", 0.2f);
    }

    @Override
    public ParticleInfluencer clone() {
        try {
            DefaultParticleInfluencer clone = (DefaultParticleInfluencer) super.clone();
            clone.initialVelocity = initialVelocity.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public void setInitialVelocity(Vector3f initialVelocity) {
        this.initialVelocity.set(initialVelocity);
    }

    @Override
    public Vector3f getInitialVelocity() {
        return initialVelocity;
    }

    @Override
    public void setVelocityVariation(float variation) {
        this.velocityVariation = variation;
    }

    @Override
    public float getVelocityVariation() {
        return velocityVariation;
    }
}
