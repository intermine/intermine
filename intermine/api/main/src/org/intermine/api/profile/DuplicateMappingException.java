package org.intermine.api.profile;

public class DuplicateMappingException extends RuntimeException {
    private static final long serialVersionUID = -7950202126281601571L;
    private static final String MESSAGE_FMT =
            "No two users may have the same value for '%s', but the value '%s' is already taken";

    public DuplicateMappingException(String key, String value) {
        super(String.format(MESSAGE_FMT, key, value));
    }
}