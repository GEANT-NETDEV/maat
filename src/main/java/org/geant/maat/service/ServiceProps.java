package org.geant.maat.service;

enum ServiceProps {
    TYPE("@type"),
    SCHEMA_LOCATION("@schemaLocation");
    public final String name;

    ServiceProps(String name) {
        this.name = name;
    }
}
