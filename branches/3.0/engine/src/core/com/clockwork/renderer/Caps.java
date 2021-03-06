
package com.clockwork.renderer;

import com.clockwork.shader.Shader;
import com.clockwork.shader.Shader.ShaderSource;
import com.clockwork.texture.FrameBuffer;
import com.clockwork.texture.FrameBuffer.RenderBuffer;
import com.clockwork.texture.Image;
import com.clockwork.texture.Image.Format;
import com.clockwork.texture.Texture;
import java.util.Collection;

/**
 * Caps is an enum specifying a capability that the Renderer}
 * supports.
 * 
 */
public enum Caps {

    /**
     * Supports FrameBuffer FrameBuffers}.
     * 
     * OpenGL: Renderer exposes the GL_EXT_framebuffer_object extension.
     * OpenGL ES: Renderer supports OpenGL ES 2.0.
     */
    FrameBuffer,

    /**
     * Supports framebuffer Multiple Render Targets (MRT)
     * 
     * OpenGL: Renderer exposes the GL_ARB_draw_buffers extension
     */
    FrameBufferMRT,

    /**
     * Supports framebuffer multi-sampling
     * 
     * OpenGL: Renderer exposes the GL EXT framebuffer multisample extension
     * OpenGL ES: Renderer exposes GL_APPLE_framebuffer_multisample or
     * GL_ANGLE_framebuffer_multisample.
     */
    FrameBufferMultisample,

    /**
     * Supports texture multi-sampling
     * 
     * OpenGL: Renderer exposes the GL_ARB_texture_multisample extension
     * OpenGL ES: Renderer exposes the GL_IMG_multisampled_render_to_texture
     * extension.
     */
    TextureMultisample,

    /**
     * Supports OpenGL 2.0 or OpenGL ES 2.0.
     */
    OpenGL20,
    
    /**
     * Supports OpenGL 2.1
     */
    OpenGL21,
    
    /**
     * Supports OpenGL 3.0
     */
    OpenGL30,
    
    /**
     * Supports OpenGL 3.1
     */
    OpenGL31,
    
    /**
     * Supports OpenGL 3.2
     */
    OpenGL32,

    /**
     * Supports OpenGL ARB program.
     * 
     * OpenGL: Renderer exposes ARB_vertex_program and ARB_fragment_program
     * extensions.
     */
    ARBprogram,
    
    /**
     * Supports GLSL 1.0
     */
    GLSL100,
    
    /**
     * Supports GLSL 1.1
     */
    GLSL110,
    
    /**
     * Supports GLSL 1.2
     */
    GLSL120,
    
    /**
     * Supports GLSL 1.3
     */
    GLSL130,
    
    /**
     * Supports GLSL 1.4
     */
    GLSL140,
    
    /**
     * Supports GLSL 1.5
     */
    GLSL150,
    
    /**
     * Supports GLSL 3.3
     */
    GLSL330,

    /**
     * Supports reading from textures inside the vertex shader.
     */
    VertexTextureFetch,

    /**
     * Supports geometry shader.
     */
    GeometryShader,

    /**
     * Supports texture arrays
     */
    TextureArray,

    /**
     * Supports texture buffers
     */
    TextureBuffer,

    /**
     * Supports floating point textures (Format.RGB16F)
     */
    FloatTexture,

    /**
     * Supports floating point FBO color buffers (Format.RGB16F)
     */
    FloatColorBuffer,

    /**
     * Supports floating point depth buffer
     */
    FloatDepthBuffer,

    /**
     * Supports Format.RGB111110F for textures
     */
    PackedFloatTexture,

    /**
     * Supports Format.RGB9E5 for textures
     */
    SharedExponentTexture,

    /**
     * Supports Format.RGB111110F for FBO color buffers
     */
    PackedFloatColorBuffer,

    /**
     * Supports Format.RGB9E5 for FBO color buffers
     */
    SharedExponentColorBuffer,
    
    /**
     * Supports Format.LATC for textures, this includes
     * support for ATI's 3Dc texture compression.
     */
    TextureCompressionLATC,

    /**
     * Supports Non-Power-Of-Two (NPOT) textures and framebuffers
     */
    NonPowerOfTwoTextures,

    /// Vertex Buffer features
    MeshInstancing,

    /**
     * Supports VAO, or vertex buffer arrays
     */
    VertexBufferArray,

    /**
     * Supports multisampling on the screen
     */
    Multisample,
    
    /**
     * Supports FBO with Depth24Stencil8 image format
     */
    PackedDepthStencilBuffer;

    /**
     * Returns true if given the renderer capabilities, the texture
     * can be supported by the renderer.
     * 
     * This only checks the format of the texture, non-power-of-2
     * textures are scaled automatically inside the renderer 
     * if are not supported natively.
     * 
     * @param caps The collection of renderer capabilities Renderer#getCaps() }.
     * @param tex The texture to check
     * @return True if it is supported, false otherwise.
     */
    public static boolean supports(Collection<Caps> caps, Texture tex){
        if (tex.getType() == Texture.Type.TwoDimensionalArray
         && !caps.contains(Caps.TextureArray))
            return false;

        Image img = tex.getImage();
        if (img == null)
            return true;

        Format fmt = img.getFormat();
        switch (fmt){
            case Depth24Stencil8:
                return caps.contains(Caps.PackedDepthStencilBuffer);
            case Depth32F:
                return caps.contains(Caps.FloatDepthBuffer);
            case LATC:
                return caps.contains(Caps.TextureCompressionLATC);
            case RGB16F_to_RGB111110F:
            case RGB111110F:
                return caps.contains(Caps.PackedFloatTexture);
            case RGB16F_to_RGB9E5:
            case RGB9E5:
                return caps.contains(Caps.SharedExponentTexture);
            default:
                if (fmt.isFloatingPont())
                    return caps.contains(Caps.FloatTexture);
                        
                return true;
        }
    }
    
    private static boolean supportsColorBuffer(Collection<Caps> caps, RenderBuffer colorBuf){
        Format colorFmt = colorBuf.getFormat();
        if (colorFmt.isDepthFormat())
            return false;

        if (colorFmt.isCompressed())
            return false;

        switch (colorFmt){
            case RGB111110F:
                return caps.contains(Caps.PackedFloatColorBuffer);
            case RGB16F_to_RGB111110F:
            case RGB16F_to_RGB9E5:
            case RGB9E5:
                return false;
            default:
                if (colorFmt.isFloatingPont())
                    return caps.contains(Caps.FloatColorBuffer);

                return true;
        }
    }

    /**
     * Returns true if given the renderer capabilities, the framebuffer
     * can be supported by the renderer.
     * 
     * @param caps The collection of renderer capabilities Renderer#getCaps() }.
     * @param fb The framebuffer to check
     * @return True if it is supported, false otherwise.
     */
    public static boolean supports(Collection<Caps> caps, FrameBuffer fb){
        if (!caps.contains(Caps.FrameBuffer))
            return false;

        if (fb.getSamples() > 1
         && !caps.contains(Caps.FrameBufferMultisample))
            return false;

        RenderBuffer depthBuf = fb.getDepthBuffer();
        if (depthBuf != null){
            Format depthFmt = depthBuf.getFormat();
            if (!depthFmt.isDepthFormat()){
                return false;
            }else{
                if (depthFmt == Format.Depth32F
                 && !caps.contains(Caps.FloatDepthBuffer))
                    return false;
                
                if (depthFmt == Format.Depth24Stencil8
                 && !caps.contains(Caps.PackedDepthStencilBuffer))
                    return false;
            }
        }
        for (int i = 0; i < fb.getNumColorBuffers(); i++){
            if (!supportsColorBuffer(caps, fb.getColorBuffer(i))){
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if given the renderer capabilities, the shader
     * can be supported by the renderer.
     * 
     * @param caps The collection of renderer capabilities Renderer#getCaps() }.
     * @param shader The shader to check
     * @return True if it is supported, false otherwise.
     */
    public static boolean supports(Collection<Caps> caps, Shader shader){
        for (ShaderSource source : shader.getSources()) {
            if (source.getLanguage().startsWith("GLSL")) {
                int ver = Integer.parseInt(source.getLanguage().substring(4));
                switch (ver) {
                    case 100:
                        if (!caps.contains(Caps.GLSL100)) return false;
                    case 110:
                        if (!caps.contains(Caps.GLSL110)) return false;
                    case 120:
                        if (!caps.contains(Caps.GLSL120)) return false;
                    case 130:
                        if (!caps.contains(Caps.GLSL130)) return false;
                    case 140:
                        if (!caps.contains(Caps.GLSL140)) return false;
                    case 150:
                        if (!caps.contains(Caps.GLSL150)) return false;
                    case 330:
                        if (!caps.contains(Caps.GLSL330)) return false;
                    default:
                        return false;
                }
            }
        }
        return true;
    }

}
