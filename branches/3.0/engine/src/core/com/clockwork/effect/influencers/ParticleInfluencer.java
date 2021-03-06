
package com.clockwork.effect.influencers;

import com.clockwork.effect.Particle;
import com.clockwork.effect.ParticleEmitter;
import com.clockwork.effect.shapes.EmitterShape;
import com.clockwork.export.Savable;
import com.clockwork.math.Vector3f;

/**
 * An interface that defines the methods to affect initial velocity of the particles.
 */
public interface ParticleInfluencer extends Savable, Cloneable {

    /**
     * This method influences the particle.
     * @param particle
     *        particle to be influenced
     * @param emitterShape
     *        the shape of it emitter
     */
    void influenceParticle(Particle particle, EmitterShape emitterShape);

    /**
     * This method clones the influencer instance.
     * @return cloned instance
     */
    public ParticleInfluencer clone();

    /**
     * @param initialVelocity
     *        Set the initial velocity a particle is spawned with,
     *        the initial velocity given in the parameter will be varied according
     *        to the velocity variation set in ParticleEmitter#setVelocityVariation(float) }.
     *        A particle will move toward its velocity unless it is effected by the
     *        gravity.
     */
    void setInitialVelocity(Vector3f initialVelocity);

    /**
     * This method returns the initial velocity.
     * @return the initial velocity
     */
    Vector3f getInitialVelocity();

    /**
     * @param variation
     *        Set the variation by which the initial velocity
     *        of the particle is determined. variation should be a value
     *        from 0 to 1, where 0 means particles are to spawn with exactly
     *        the velocity given in ParticleEmitter#setInitialVelocity(com.clockwork.math.Vector3f) },
     *        and 1 means particles are to spawn with a completely random velocity.
     */
    void setVelocityVariation(float variation);

    /**
     * This method returns the velocity variation.
     * @return the velocity variation
     */
    float getVelocityVariation();
}
