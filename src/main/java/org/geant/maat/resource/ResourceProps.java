package org.geant.maat.resource;

enum ResourceProps {
    TYPE("@type"),
    SCHEMA_LOCATION("@schemaLocation");
    public final String name;

    ResourceProps(String name) {
        this.name = name;
    }
}
