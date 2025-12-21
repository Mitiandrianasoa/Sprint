package com.sprint.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelView {
    private String view;
    private Map<String, Object> data;
    private String dataType = "string"; // type de données par défaut

    public ModelView(String view) {
        this.view = view;
        this.data = new HashMap<>();
    }

    // Constructeur avec données
    public ModelView(String view, Map<String, Object> data) {
        this.view = view;
        this.data = data != null ? data : new HashMap<>();
        detectDataType(); // NOUVEAU: détection automatique du type
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public ModelView addObject(String key, Object value) {
        this.data.put(key, value);
        detectDataType(); // re-détecter le type après ajout
        return this;
    }

    public Map<String, Object> getData() {
        return this.data;
    }

    public Object getObject(String key) {
        return this.data.get(key);
    }

    // Getter pour le type de données
    public String getDataType() {
        return dataType;
    }

    // Setter pour le type de données (si besoin de forcer)
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    // Méthode pour détecter le type de données
    private void detectDataType() {
        if (data == null || data.isEmpty()) {
            this.dataType = "string";
            return;
        }

        // Parcourir les valeurs pour détecter le type dominant
        for (Object value : data.values()) {
            if (value instanceof Map) {
                this.dataType = "map";
                return;
            } else if (value instanceof List) {
                this.dataType = "list";
                return;
            } else if (value instanceof String) {
                this.dataType = "string";
                // Continue pour voir s'il y a des types plus complexes
            } else if (value != null) {
                // Pour les autres types d'objets
                this.dataType = "object";
                return;
            }
        }
    }

    public boolean hasData() {
        return data != null && !data.isEmpty();
    }

    public int getDataCount() {
        return data != null ? data.size() : 0;
    }

    @Override
    public String toString() {
        return "ModelView{" +
                "view='" + view + '\'' +
                ", dataType='" + dataType + '\'' +
                ", dataCount=" + getDataCount() +
                '}';
    }
}