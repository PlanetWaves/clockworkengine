package com.clockwork.scene.plugins.blender.materials;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.clockwork.material.Material;
import com.clockwork.material.RenderState.BlendMode;
import com.clockwork.material.RenderState.FaceCullMode;
import com.clockwork.math.ColorRGBA;
import com.clockwork.math.Vector2f;
import com.clockwork.renderer.queue.RenderQueue.Bucket;
import com.clockwork.scene.Geometry;
import com.clockwork.scene.VertexBuffer;
import com.clockwork.scene.VertexBuffer.Format;
import com.clockwork.scene.VertexBuffer.Usage;
import com.clockwork.scene.plugins.blender.BlenderContext;
import com.clockwork.scene.plugins.blender.exceptions.BlenderFileException;
import com.clockwork.scene.plugins.blender.file.DynamicArray;
import com.clockwork.scene.plugins.blender.file.Pointer;
import com.clockwork.scene.plugins.blender.file.Structure;
import com.clockwork.scene.plugins.blender.materials.MaterialHelper.DiffuseShader;
import com.clockwork.scene.plugins.blender.materials.MaterialHelper.SpecularShader;
import com.clockwork.scene.plugins.blender.textures.CombinedTexture;
import com.clockwork.scene.plugins.blender.textures.TextureHelper;
import com.clockwork.scene.plugins.blender.textures.blending.TextureBlender;
import com.clockwork.scene.plugins.blender.textures.blending.TextureBlenderFactory;
import com.clockwork.texture.Texture;
import com.clockwork.util.BufferUtils;

/**
 * This class holds the data about the material.
 */
public final class MaterialContext {
    private static final Logger                     LOGGER     = Logger.getLogger(MaterialContext.class.getName());

    // texture mapping types
    public static final int                         MTEX_COL   = 0x01;
    public static final int                         MTEX_NOR   = 0x02;
    public static final int                         MTEX_SPEC  = 0x04;
    public static final int                         MTEX_EMIT  = 0x40;
    public static final int                         MTEX_ALPHA = 0x80;
    public static final int                         MTEX_AMB   = 0x800;

    /* package */final String                       name;
    /* package */final Map<Number, CombinedTexture> loadedTextures;

    /* package */final ColorRGBA                    diffuseColor;
    /* package */final DiffuseShader                diffuseShader;
    /* package */final SpecularShader               specularShader;
    /* package */final ColorRGBA                    specularColor;
    /* package */final ColorRGBA                    ambientColor;
    /* package */final float                        shininess;
    /* package */final boolean                      shadeless;
    /* package */final boolean                      vertexColor;
    /* package */final boolean                      transparent;
    /* package */final boolean                      vTangent;
    /* package */FaceCullMode                       faceCullMode;

    @SuppressWarnings("unchecked")
    /* package */MaterialContext(Structure structure, BlenderContext blenderContext) throws BlenderFileException {
        name = structure.getName();

        int mode = ((Number) structure.getFieldValue("mode")).intValue();
        shadeless = (mode & 0x4) != 0;
        vertexColor = (mode & 0x80) != 0;
        vTangent = (mode & 0x4000000) != 0; // NOTE: Requires tangents

        int diff_shader = ((Number) structure.getFieldValue("diff_shader")).intValue();
        diffuseShader = DiffuseShader.values()[diff_shader];

        if (this.shadeless) {
            float r = ((Number) structure.getFieldValue("r")).floatValue();
            float g = ((Number) structure.getFieldValue("g")).floatValue();
            float b = ((Number) structure.getFieldValue("b")).floatValue();
            float alpha = ((Number) structure.getFieldValue("alpha")).floatValue();

            diffuseColor = new ColorRGBA(r, g, b, alpha);
            specularShader = null;
            specularColor = ambientColor = null;
            shininess = 0.0f;
        } else {
            diffuseColor = this.readDiffuseColor(structure, diffuseShader);

            int spec_shader = ((Number) structure.getFieldValue("spec_shader")).intValue();
            specularShader = SpecularShader.values()[spec_shader];
            specularColor = this.readSpecularColor(structure, specularShader);

            float r = ((Number) structure.getFieldValue("ambr")).floatValue();
            float g = ((Number) structure.getFieldValue("ambg")).floatValue();
            float b = ((Number) structure.getFieldValue("ambb")).floatValue();
            float alpha = ((Number) structure.getFieldValue("alpha")).floatValue();
            ambientColor = new ColorRGBA(r, g, b, alpha);

            float shininess = ((Number) structure.getFieldValue("emit")).floatValue();
            this.shininess = shininess > 0.0f ? shininess : MaterialHelper.DEFAULT_SHININESS;
        }

        DynamicArray<Pointer> mtexsArray = (DynamicArray<Pointer>) structure.getFieldValue("mtex");
        int separatedTextures = ((Number) structure.getFieldValue("septex")).intValue();
        List<TextureData> texturesList = new ArrayList<TextureData>();
        for (int i = 0; i < mtexsArray.getTotalSize(); ++i) {
            Pointer p = mtexsArray.get(i);
            if (p.isNotNull() && (separatedTextures & 1 << i) == 0) {
                TextureData textureData = new TextureData();
                textureData.mtex = p.fetchData(blenderContext.getInputStream()).get(0);
                textureData.uvCoordinatesType = ((Number) textureData.mtex.getFieldValue("texco")).intValue();
                textureData.projectionType = ((Number) textureData.mtex.getFieldValue("mapping")).intValue();
                textureData.uvCoordinatesName = textureData.mtex.getFieldValue("uvName").toString();
                if(textureData.uvCoordinatesName != null && textureData.uvCoordinatesName.trim().length() == 0) {
                    textureData.uvCoordinatesName = null;
                }
                
                Pointer pTex = (Pointer) textureData.mtex.getFieldValue("tex");
                if (pTex.isNotNull()) {
                    Structure tex = pTex.fetchData(blenderContext.getInputStream()).get(0);
                    textureData.textureStructure = tex;
                    texturesList.add(textureData);
                }
            }
        }

        // loading the textures and merging them
        Map<Number, List<TextureData>> textureDataMap = this.sortAndFilterTextures(texturesList);
        loadedTextures = new HashMap<Number, CombinedTexture>();
        float[] diffuseColorArray = new float[] { diffuseColor.r, diffuseColor.g, diffuseColor.b, diffuseColor.a };
        TextureHelper textureHelper = blenderContext.getHelper(TextureHelper.class);
        for (Entry<Number, List<TextureData>> entry : textureDataMap.entrySet()) {
            if (entry.getValue().size() > 0) {
                CombinedTexture combinedTexture = new CombinedTexture(entry.getKey().intValue());
                for (TextureData textureData : entry.getValue()) {
                    int texflag = ((Number) textureData.mtex.getFieldValue("texflag")).intValue();
                    boolean negateTexture = (texflag & 0x04) != 0;
                    Texture texture = textureHelper.getTexture(textureData.textureStructure, textureData.mtex, blenderContext);
                    if (texture != null) {
                        int blendType = ((Number) textureData.mtex.getFieldValue("blendtype")).intValue();
                        float[] color = new float[] { ((Number) textureData.mtex.getFieldValue("r")).floatValue(), ((Number) textureData.mtex.getFieldValue("g")).floatValue(), ((Number) textureData.mtex.getFieldValue("b")).floatValue() };
                        float colfac = ((Number) textureData.mtex.getFieldValue("colfac")).floatValue();
                        TextureBlender textureBlender = TextureBlenderFactory.createTextureBlender(texture.getImage().getFormat(), texflag, negateTexture, blendType, diffuseColorArray, color, colfac);
                        combinedTexture.add(texture, textureBlender, textureData.uvCoordinatesType, textureData.projectionType,
                        					textureData.textureStructure, textureData.uvCoordinatesName, blenderContext);
                    }
                }
                if (combinedTexture.getTexturesCount() > 0) {
                    loadedTextures.put(entry.getKey(), combinedTexture);
                }
            }
        }

        // veryfying if the transparency is present
        // (in blender transparent mask is 0x10000 but its better to verify it because blender can indicate transparency when
        // it is not required
        boolean transparent = false;
        if (diffuseColor != null) {
            transparent = diffuseColor.a < 1.0f;
            if (textureDataMap.size() > 0) {// texutre covers the material color
                diffuseColor.set(1, 1, 1, 1);
            }
        }
        if (specularColor != null) {
            transparent = transparent || specularColor.a < 1.0f;
        }
        if (ambientColor != null) {
            transparent = transparent || ambientColor.a < 1.0f;
        }
        this.transparent = transparent;
    }

    /**
     * Applies material to a given geometry.
     * 
     * @param geometry
     *            the geometry
     * @param geometriesOMA
     *            the geometries OMA
     * @param userDefinedUVCoordinates
     *            UV coords defined by user
     * @param blenderContext
     *            the blender context
     */
    public void applyMaterial(Geometry geometry, Long geometriesOMA, LinkedHashMap<String, List<Vector2f>> userDefinedUVCoordinates, BlenderContext blenderContext) {
        Material material = null;
        if (shadeless) {
            material = new Material(blenderContext.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");

            if (!transparent) {
                diffuseColor.a = 1;
            }

            material.setColor("Color", diffuseColor);
        } else {
            material = new Material(blenderContext.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
            material.setBoolean("UseMaterialColors", Boolean.TRUE);

            // setting the colors
            material.setBoolean("Minnaert", diffuseShader == DiffuseShader.MINNAERT);
            if (!transparent) {
                diffuseColor.a = 1;
            }
            material.setColor("Diffuse", diffuseColor);

            material.setBoolean("WardIso", specularShader == SpecularShader.WARDISO);
            material.setColor("Specular", specularColor);

            material.setColor("Ambient", ambientColor);
            material.setFloat("Shininess", shininess);
        }

        // applying textures
        if (loadedTextures != null && loadedTextures.size() > 0) {
            int textureIndex = 0;
            if(loadedTextures.size() > 8) {
                LOGGER.log(Level.WARNING, "The blender file has defined more than {0} different textures. CW supports only {0} UV mappings.", TextureHelper.TEXCOORD_TYPES.length);
            }
            for (Entry<Number, CombinedTexture> entry : loadedTextures.entrySet()) {
                if(textureIndex < TextureHelper.TEXCOORD_TYPES.length) {
                    CombinedTexture combinedTexture = entry.getValue();
                    combinedTexture.flatten(geometry, geometriesOMA, userDefinedUVCoordinates, blenderContext);

                    this.setTexture(material, entry.getKey().intValue(), combinedTexture.getResultTexture());
                    List<Vector2f> uvs = entry.getValue().getResultUVS();
                    VertexBuffer uvCoordsBuffer = new VertexBuffer(TextureHelper.TEXCOORD_TYPES[textureIndex++]);
                    uvCoordsBuffer.setupData(Usage.Static, 2, Format.Float, BufferUtils.createFloatBuffer(uvs.toArray(new Vector2f[uvs.size()])));
                    geometry.getMesh().setBuffer(uvCoordsBuffer);
                } else {
                    LOGGER.log(Level.WARNING, "The texture could not be applied because CW only supports up to {0} different UV's.", TextureHelper.TEXCOORD_TYPES.length);
                }
            }
        }

        // applying additional data
        material.setName(name);
        if (vertexColor) {
            material.setBoolean(shadeless ? "VertexColor" : "UseVertexColor", true);
        }
        material.getAdditionalRenderState().setFaceCullMode(faceCullMode != null ? faceCullMode : blenderContext.getBlenderKey().getFaceCullMode());
        if (transparent) {
            material.setTransparent(true);
            material.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            geometry.setQueueBucket(Bucket.Transparent);
        }

        geometry.setMaterial(material);
    }

    /**
     * Sets the texture to the given material.
     * 
     * @param material
     *            the material that we add texture to
     * @param mapTo
     *            the texture mapping type
     * @param texture
     *            the added texture
     */
    private void setTexture(Material material, int mapTo, Texture texture) {
        switch (mapTo) {
            case MTEX_COL:
                material.setTexture(shadeless ? MaterialHelper.TEXTURE_TYPE_COLOR : MaterialHelper.TEXTURE_TYPE_DIFFUSE, texture);
                break;
            case MTEX_NOR:
                material.setTexture(MaterialHelper.TEXTURE_TYPE_NORMAL, texture);
                break;
            case MTEX_SPEC:
                material.setTexture(MaterialHelper.TEXTURE_TYPE_SPECULAR, texture);
                break;
            case MTEX_EMIT:
                material.setTexture(MaterialHelper.TEXTURE_TYPE_GLOW, texture);
                break;
            case MTEX_ALPHA:
                if (!shadeless) {
                    material.setTexture(MaterialHelper.TEXTURE_TYPE_ALPHA, texture);
                } else {
                    LOGGER.warning("CW does not support alpha map on unshaded material. Material name is " + name);
                }
                break;
            case MTEX_AMB:
                material.setTexture(MaterialHelper.TEXTURE_TYPE_LIGHTMAP, texture);
                break;
            default:
                LOGGER.severe("Unknown mapping type: " + mapTo);
        }
    }

    /**
     * @return true if the material has at least one generated texture and false otherwise
     */
    public boolean hasGeneratedTextures() {
        if (loadedTextures != null) {
            for (Entry<Number, CombinedTexture> entry : loadedTextures.entrySet()) {
                if (entry.getValue().hasGeneratedTextures()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This method sorts the textures by their mapping type. In each group only
     * textures of one type are put (either two- or three-dimensional). If the
     * mapping type is MTEX_COL then if the texture has no alpha channel then
     * all textures before it are discarded and will not be loaded and merged
     * because texture with no alpha will cover them anyway.
     * 
     * @return a map with sorted and filtered textures
     */
    private Map<Number, List<TextureData>> sortAndFilterTextures(List<TextureData> textures) {
        int[] mappings = new int[] { MTEX_COL, MTEX_NOR, MTEX_EMIT, MTEX_SPEC, MTEX_ALPHA, MTEX_AMB };
        Map<Number, List<TextureData>> result = new HashMap<Number, List<TextureData>>();
        for (TextureData data : textures) {
            Number mapto = (Number) data.mtex.getFieldValue("mapto");
            for (int i = 0; i < mappings.length; ++i) {
                if ((mappings[i] & mapto.intValue()) != 0) {
                    List<TextureData> datas = result.get(mappings[i]);
                    if (datas == null) {
                        datas = new ArrayList<TextureData>();
                        result.put(mappings[i], datas);
                    }
                    datas.add(data);
                }
            }
        }
        return result;
    }

    /**
     * This method sets the face cull mode.
     * @param faceCullMode
     *            the face cull mode
     */
    public void setFaceCullMode(FaceCullMode faceCullMode) {
        this.faceCullMode = faceCullMode;
    }

    /**
     * This method returns the diffuse color.
     * 
     * @param materialStructure
     *            the material structure
     * @param diffuseShader
     *            the diffuse shader
     * @return the diffuse color
     */
    private ColorRGBA readDiffuseColor(Structure materialStructure, DiffuseShader diffuseShader) {
        // bitwise 'or' of all textures mappings
        int commonMapto = ((Number) materialStructure.getFieldValue("mapto")).intValue();

        // diffuse color
        float r = ((Number) materialStructure.getFieldValue("r")).floatValue();
        float g = ((Number) materialStructure.getFieldValue("g")).floatValue();
        float b = ((Number) materialStructure.getFieldValue("b")).floatValue();
        float alpha = ((Number) materialStructure.getFieldValue("alpha")).floatValue();
        if ((commonMapto & 0x01) == 0x01) {// Col
            return new ColorRGBA(r, g, b, alpha);
        } else {
            switch (diffuseShader) {
                case FRESNEL:
                case ORENNAYAR:
                case TOON:
                    break;// TODO: find what is the proper modification
                case MINNAERT:
                case LAMBERT:// TODO: check if that is correct
                    float ref = ((Number) materialStructure.getFieldValue("ref")).floatValue();
                    r *= ref;
                    g *= ref;
                    b *= ref;
                    break;
                default:
                    throw new IllegalStateException("Unknown diffuse shader type: " + diffuseShader.toString());
            }
            return new ColorRGBA(r, g, b, alpha);
        }
    }

    /**
     * This method returns a specular color used by the material.
     * 
     * @param materialStructure
     *            the material structure filled with data
     * @return a specular color used by the material
     */
    private ColorRGBA readSpecularColor(Structure materialStructure, SpecularShader specularShader) {
        float r = ((Number) materialStructure.getFieldValue("specr")).floatValue();
        float g = ((Number) materialStructure.getFieldValue("specg")).floatValue();
        float b = ((Number) materialStructure.getFieldValue("specb")).floatValue();
        float alpha = ((Number) materialStructure.getFieldValue("alpha")).floatValue();
        switch (specularShader) {
            case BLINN:
            case COOKTORRENCE:
            case TOON:
            case WARDISO:// TODO: find what is the proper modification
                break;
            case PHONG:// TODO: check if that is correct
                float spec = ((Number) materialStructure.getFieldValue("spec")).floatValue();
                r *= spec * 0.5f;
                g *= spec * 0.5f;
                b *= spec * 0.5f;
                break;
            default:
                throw new IllegalStateException("Unknown specular shader type: " + specularShader.toString());
        }
        return new ColorRGBA(r, g, b, alpha);
    }

    private static class TextureData {
        Structure mtex;
        Structure textureStructure;
        int       uvCoordinatesType;
        int       projectionType;
        /** The name of the user's UV coordinates that are used for this texture. */
        String	  uvCoordinatesName;
    }
}
