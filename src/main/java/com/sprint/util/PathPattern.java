package com.sprint.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathPattern {
    private final String pattern;
    private final Pattern regexPattern;
    private final List<String> parameterNames;

    public PathPattern(String pattern) {
        this.pattern = pattern;
        this.parameterNames = new ArrayList<>();
        this.regexPattern = compilePattern(pattern);
        extractParameterNames(pattern); // Ajout de l'appel manquant
    }

    private Pattern compilePattern(String pattern) {
        // Remplace les {param} par des groupes de capture nommés
        String regex = pattern.replaceAll("\\{([^}]+)\\}", "(?<$1>[^/]+)");
        return Pattern.compile("^" + regex + "$");
    }

    public boolean matches(String path) {
        return regexPattern.matcher(path).matches();
    }

    public Map<String, String> extractParameters(String path) {
        Map<String, String> params = new HashMap<>();
        Matcher matcher = regexPattern.matcher(path);
        if (matcher.matches()) {
            for (String paramName : parameterNames) {
                params.put(paramName, matcher.group(paramName));
            }
        }
        return params;
    }

    private void extractParameterNames(String pattern) {
        Matcher m = Pattern.compile("\\{([^}]+)\\}").matcher(pattern);
        while (m.find()) {
            parameterNames.add(m.group(1));
        }
    }
}