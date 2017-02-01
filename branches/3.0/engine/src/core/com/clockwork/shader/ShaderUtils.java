
package com.clockwork.shader;

public class ShaderUtils {

    public static String convertToGLSL130(String input, boolean isFrag) {
        StringBuilder sb = new StringBuilder();
        sb.append("#version 130\n");
        if (isFrag) {
            input = input.replaceAll("varying", "in");
        } else {
            input = input.replaceAll("attribute", "in");
            input = input.replaceAll("varying", "out");
        }
        sb.append(input);
        return sb.toString();
    }

    /**
     * Check if a mapping is valid by checking the types and swizzle of both of
     * the variables
     *
     * @param mapping the mapping
     * @return true if this mapping is valid
     */
    public static boolean typesMatch(VariableMapping mapping) {
        String leftType = mapping.getLeftVariable().getType();
        String rightType = mapping.getRightVariable().getType();
        String leftSwizzling = mapping.getLeftSwizzling();
        String rightSwizzling = mapping.getRightSwizzling();

        //types match : no error
        if (leftType.equals(rightType) && leftSwizzling.length() == rightSwizzling.length()) {
            return true;
        }
        if (isSwizzlable(leftType) && isSwizzlable(rightType)) {
            if (getCardinality(leftType, leftSwizzling) == getCardinality(rightType, rightSwizzling)) {
                return true;
            }
        }

        return false;
    }
    
     /**
     * Check if a mapping is valid by checking the multiplicity of both of
     * the variables if they are arrays
     *
     * @param mapping the mapping
     * @return true if this mapping is valid
     */
    public static boolean multiplicityMatch(VariableMapping mapping) {
        String leftMult = mapping.getLeftVariable().getMultiplicity();
        String rightMult = mapping.getRightVariable().getMultiplicity();
        
        if(leftMult == null){
            if(rightMult != null){
                return false;
            }
        }else{
            if(rightMult == null){
                return false;
            }else{
                if(!leftMult.equalsIgnoreCase(rightMult)){
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * return the cardinality of a type and a swizzle example : vec4 cardinality
     * is 4 float cardinality is 1 vec4.xyz cardinality is 3. sampler2D
     * cardinality is 0
     *
     * @param type the glsl type
     * @param swizzling the swizzling of a variable
     * @return the cardinality
     */
    public static int getCardinality(String type, String swizzling) {
        int card = 0;
        if (isSwizzlable(type)) {
            if (type.equals("float")) {
                card = 1;
                if (swizzling.length() != 0) {
                    card = 0;
                }
            } else {
                card = Integer.parseInt(type.replaceAll("vec", ""));

                if (swizzling.length() > 0) {
                    if (card >= swizzling.length()) {
                        card = swizzling.length();
                    } else {
                        card = 0;
                    }
                }
            }
        }
        return card;
    }

    /**
     * returns true if a variable of the given type can have a swizzle
     *
     * @param type the glsl type
     * @return true if a variable of the given type can have a swizzle
     */
    public static boolean isSwizzlable(String type) {
        return type.equals("vec4") || type.equals("vec3") || type.equals("vec2") || type.equals("float");
    }
}