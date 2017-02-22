
package com.clockwork.material.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An utility class that allows to parse a define condition in a glsl language
 * style.
 *
 * extractDefines is able to get a list of defines in an expression and update
 * the formatter expression with upercased defines
 *
 * 
 */
public class ConditionParser {

    private String formattedExpression = "";

    public static void main(String argv[]) {
        ConditionParser parser = new ConditionParser();
        List<String> defines = parser.extractDefines("(LightMap && SeparateTexCoord) || !ColorMap");

        for (String string : defines) {
            System.err.println(string);
        }
        System.err.println(parser.formattedExpression);

        defines = parser.extractDefines("#if (defined(LightMap) && defined(SeparateTexCoord)) || !defined(ColorMap)");

        for (String string : defines) {
            System.err.println(string);
        }
        System.err.println(parser.formattedExpression);


//        System.err.println(parser.getFormattedExpression());
//        
//        parser.parse("ShaderNode.var.xyz");
//        parser.parse("var.xyz");
//        parser.parse("ShaderNode.var");
//        parser.parse("var");
    }

    /**
     * parse a condition and returns the list of defines of this condition.
     * additionally this methods updates the formattedExpression with uppercased
     * defines names
     * 
     * supported expression syntax example: 
     * "(LightMap && SeparateTexCoord) || !ColorMap"
     * "#if (defined(LightMap) && defined(SeparateTexCoord)) || !defined(ColorMap)"
     * "#ifdef LightMap"
     * "#ifdef (LightMap && SeparateTexCoord) || !ColorMap"
     *
     * @param expression the expression to parse
     * @return the list of defines
     */
    public List<String> extractDefines(String expression) {
        List<String> defines = new ArrayList<String>();
        expression = expression.replaceAll("#ifdef", "").replaceAll("#if", "").replaceAll("defined", "");
        Pattern pattern = Pattern.compile("(\\w+)");
        formattedExpression = expression;
        Matcher m = pattern.matcher(expression);
        while (m.find()) {
            String match = m.group();
            defines.add(match);
            formattedExpression = formattedExpression.replaceAll(match, "defined(" + match.toUpperCase() + ")");
        }
        return defines;
    }

    /**
     * 
     * @return the formatted expression previously updated by extractDefines
     */
    public String getFormattedExpression() {
        return formattedExpression;
    }
}