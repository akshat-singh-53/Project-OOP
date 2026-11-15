package com.itsm.strategy;

/** Common contract for every pluggable algorithm so they can be listed and selected by key. */
public interface NamedStrategy {

    String key();

    String displayName();

    String description();
}
