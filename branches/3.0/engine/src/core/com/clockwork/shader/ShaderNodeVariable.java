
package com.clockwork.shader;

import com.clockwork.export.InputCapsule;
import com.clockwork.export.CWExporter;
import com.clockwork.export.CWImporter;
import com.clockwork.export.OutputCapsule;
import com.clockwork.export.Savable;
import java.io.IOException;

/**
 * A shader node variable
 *
 * 
 */
public class ShaderNodeVariable implements Savable, Cloneable {

    private String name;
    private String type;
    private String nameSpace;
    private String condition;
    private boolean shaderOutput = false;
    private String multiplicity;

    /**
     * creates a ShaderNodeVariable
     *
     * @param type the glsl type of the variable
     * @param name the name of the variable
     */
    public ShaderNodeVariable(String type, String name) {
        this.name = name;
        this.type = type;
    }
    
    
    /**
     * creates a ShaderNodeVariable
     *
     * @param type the glsl type of the variable
     * @param nameSpace the nameSpace (can be the name of the shaderNode or
     * Globel,Attr,MatParam,WorldParam)
     * @param name the name of the variable
     * @param multiplicity the number of element if this variable is an array. Can be an Int of a declared material parameter
     */
    public ShaderNodeVariable(String type, String nameSpace, String name, String multiplicity) {
        this.name = name;
        this.nameSpace = nameSpace;
        this.type = type;
        this.multiplicity = multiplicity;
    }

    /**
     * creates a ShaderNodeVariable
     *
     * @param type the glsl type of the variable
     * @param nameSpace the nameSpace (can be the name of the shaderNode or
     * Globel,Attr,MatParam,WorldParam)
     * @param name the name of the variable
     */
    public ShaderNodeVariable(String type, String nameSpace, String name) {
        this.name = name;
        this.nameSpace = nameSpace;
        this.type = type;
    }

    /**
     * returns the name
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * sets the name
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     * @return the glsl type
     */
    public String getType() {
        return type;
    }

    /**
     * sets the glsl type
     *
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     *
     * @return the name space (can be the name of the shaderNode or
     * Globel,Attr,MatParam,WorldParam)
     */
    public String getNameSpace() {
        return nameSpace;
    }

    /**
     * sets the nameSpace (can be the name of the shaderNode or
     * Globel,Attr,MatParam,WorldParam)
     *
     * @param nameSpace
     */
    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ShaderNodeVariable other = (ShaderNodeVariable) obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if ((this.type == null) ? (other.type != null) : !this.type.equals(other.type)) {
            return false;
        }
        if ((this.nameSpace == null) ? (other.nameSpace != null) : !this.nameSpace.equals(other.nameSpace)) {
            return false;
        }
        return true;
    }

    /**
     * CW seralization (not used)
     *
     * @param ex the exporter
     * @throws IOException
     */
    public void write(CWExporter ex) throws IOException {
        OutputCapsule oc = (OutputCapsule) ex.getCapsule(this);
        oc.write(name, "name", "");
        oc.write(type, "type", "");
        oc.write(nameSpace, "nameSpace", "");
        oc.write(condition, "condition", null);
        oc.write(shaderOutput, "shaderOutput", false);
        oc.write(multiplicity, "multiplicity", null);

    }

    /**
     * CW seralization (not used)
     *
     * @param im the importer
     * @throws IOException
     */
    public void read(CWImporter im) throws IOException {
        InputCapsule ic = (InputCapsule) im.getCapsule(this);
        name = ic.readString("name", "");
        type = ic.readString("type", "");
        nameSpace = ic.readString("nameSpace", "");
        condition = ic.readString("condition", null);        
        shaderOutput = ic.readBoolean("shaderOutput", false);
        multiplicity = ic.readString("multiplicity", null);
    }

    /**
     *
     * @return the condition for this variable to be declared
     */
    public String getCondition() {
        return condition;
    }

    /**
     * sets the condition
     *
     * @param condition the condition
     */
    public void setCondition(String condition) {
        this.condition = condition;
    }

    @Override
    public String toString() {
        return "\n" + type + ' ' + (nameSpace != null ? (nameSpace + '.') : "") + name;
    }

    @Override
    public ShaderNodeVariable clone() {
        return new ShaderNodeVariable(type, nameSpace, name);
    }

    /**
     *
     * @return true if this variable is a shader output
     */
    public boolean isShaderOutput() {
        return shaderOutput;
    }

    /**
     * sets to true if this variable is a shader output
     *
     * @param shaderOutput true if this variable is a shader output
     */
    public void setShaderOutput(boolean shaderOutput) {
        this.shaderOutput = shaderOutput;
    }

    /**
     * 
     * @return the number of elements if this variable is an array
     */
    public String getMultiplicity() {
        return multiplicity;
    }

    /**
     * sets the number of elements of this variable making it an array
     * this value can be a number of can be a define
     * @param multiplicity 
     */
    public void setMultiplicity(String multiplicity) {
        this.multiplicity = multiplicity;
    }
    
    
}
