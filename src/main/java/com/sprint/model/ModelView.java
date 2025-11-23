package com.sprint.model;

import java.util.HashMap;
import java.util.Map;

public class ModelView {
    private String view;
    private Map<String, Object> data;

    public ModelView(String view) {
        this.view = view;
        this.data = new HashMap<>();
    }

    public String getView() {
        return view;
    }

    public void setView(String view) {
        this.view = view;
    }

    public ModelView addObject(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    public Map<String, Object> getData() {
        return this.data;
    }

    public Object getObject(String key) {
        return this.data.get(key);
    }
}