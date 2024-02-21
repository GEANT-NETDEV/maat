package org.geant.maat.notification;

//TODO Should I split that enum to ResourceEvent and ServiceEvent?
public enum EventType {
    ResourceCreateEvent,
    ResourceAttributeValueChangeEvent,
    ResourceStateChangeEvent,
    ResourceDeleteEvent,
    ServiceCreateEvent,
    ServiceAttributeValueChangeEvent,
    ServiceStateChangeEvent,
    ServiceDeleteEvent
}
