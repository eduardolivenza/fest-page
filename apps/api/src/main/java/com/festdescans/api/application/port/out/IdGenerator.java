package com.festdescans.api.application.port.out;

/** New primary keys, in the same format as the existing rows (cuid). */
public interface IdGenerator {

    String newId();
}
