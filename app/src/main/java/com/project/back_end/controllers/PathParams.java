package com.project.back_end.controllers;

/** The frontend sends the literal "null" in a path segment when a filter is not set. */
final class PathParams {

    private PathParams() {
    }

    static String optional(String value) {
        return value == null || value.isBlank() || "null".equalsIgnoreCase(value) ? null : value;
    }
}
