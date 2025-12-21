package com.sprint.util;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;

/**
 * Classe utilitaire pour le binding automatique des entités
 * Sprint 8 bis
 */
public class EntityBinder {

    /**
     * Bind les paramètres de la requête HTTP vers une instance d'entité
     * 
     * @param request La requête HTTP contenant les paramètres
     * @param entityClass La classe de l'entité à instancier
     * @return Une instance de l'entité avec les champs remplis
     * @throws Exception Si une erreur survient lors du binding
     */
    public static Object bindEntity(HttpServletRequest request, Class<?> entityClass) 
            throws Exception {
        
        // Créer une nouvelle instance de l'entité
        Object entity = entityClass.getDeclaredConstructor().newInstance();
        
        // Récupérer tous les champs de la classe
        Field[] fields = entityClass.getDeclaredFields();
        
        for (Field field : fields) {
            // Rendre le champ accessible (même s'il est privé)
            field.setAccessible(true);
            
            // Récupérer la valeur du paramètre correspondant au nom du champ
            String paramValue = request.getParameter(field.getName());
            
            // Si le paramètre existe et n'est pas vide
            if (paramValue != null && !paramValue.isEmpty()) {
                try {
                    // Convertir la valeur au bon type et l'assigner au champ
                    Object convertedValue = convertValue(paramValue, field.getType());
                    field.set(entity, convertedValue);
                } catch (Exception e) {
                    System.err.println("Erreur lors de la conversion du champ " + 
                                     field.getName() + ": " + e.getMessage());
                    // Continuer avec les autres champs
                }
            }
        }
        
        return entity;
    }

    /**
     * Convertit une valeur String vers le type cible
     * 
     * @param value La valeur à convertir
     * @param targetType Le type cible
     * @return La valeur convertie
     */
    private static Object convertValue(String value, Class<?> targetType) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        try {
            // String
            if (targetType == String.class) {
                return value;
            }
            
            // Integer
            if (targetType == int.class || targetType == Integer.class) {
                return Integer.parseInt(value.trim());
            }
            
            // Long
            if (targetType == long.class || targetType == Long.class) {
                return Long.parseLong(value.trim());
            }
            
            // Double
            if (targetType == double.class || targetType == Double.class) {
                return Double.parseDouble(value.trim());
            }
            
            // Float
            if (targetType == float.class || targetType == Float.class) {
                return Float.parseFloat(value.trim());
            }
            
            // Boolean
            if (targetType == boolean.class || targetType == Boolean.class) {
                return Boolean.parseBoolean(value.trim());
            }
            
            // Byte
            if (targetType == byte.class || targetType == Byte.class) {
                return Byte.parseByte(value.trim());
            }
            
            // Short
            if (targetType == short.class || targetType == Short.class) {
                return Short.parseShort(value.trim());
            }
            
            // Character
            if (targetType == char.class || targetType == Character.class) {
                return value.charAt(0);
            }
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Impossible de convertir '" + value + "' en " + targetType.getSimpleName(), e
            );
        }
        
        // Si le type n'est pas supporté, retourner la valeur String
        return value;
    }

    /**
     * Vérifie si une classe est une entité
     * Une classe est considérée comme une entité si :
     * - Elle est dans un package contenant "entity" ou "model"
     * - Elle a l'annotation @Entity (si disponible)
     * 
     * @param clazz La classe à vérifier
     * @return true si c'est une entité, false sinon
     */
    public static boolean isEntity(Class<?> clazz) {
        // Vérifier le package
        String packageName = clazz.getPackage().getName().toLowerCase();
        if (packageName.contains("entity") || packageName.contains("model")) {
            // Exclure ModelView qui est dans le package model
            if (clazz.getSimpleName().equals("ModelView") || 
                clazz.getSimpleName().equals("JsonResponse")) {
                return false;
            }
            return true;
        }
        
        // Vérifier l'annotation @Entity si elle existe
        try {
            // Pour JPA
            if (clazz.isAnnotationPresent(
                Class.forName("jakarta.persistence.Entity").asSubclass(java.lang.annotation.Annotation.class)
            )) {
                return true;
            }
        } catch (ClassNotFoundException e) {
            // L'annotation @Entity n'est pas disponible, continuer
        }
        
        return false;
    }

    /**
     * Vérifie si une classe est un type simple (String, Integer, etc.)
     * 
     * @param clazz La classe à vérifier
     * @return true si c'est un type simple, false sinon
     */
    public static boolean isSimpleType(Class<?> clazz) {
        return clazz == String.class ||
               clazz == Integer.class || clazz == int.class ||
               clazz == Long.class || clazz == long.class ||
               clazz == Double.class || clazz == double.class ||
               clazz == Float.class || clazz == float.class ||
               clazz == Boolean.class || clazz == boolean.class ||
               clazz == Byte.class || clazz == byte.class ||
               clazz == Short.class || clazz == short.class ||
               clazz == Character.class || clazz == char.class;
    }
}