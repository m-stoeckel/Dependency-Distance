package org.texttechnologylab.dependency.graph;

public class InvalidDependencyGraphException extends Exception {

    public InvalidDependencyGraphException(String message) {
        super(message);
    }

    public InvalidDependencyGraphException(Exception cause) {
        super(cause);
    }

    public InvalidDependencyGraphException(String message, Exception cause) {
        super(message, cause);
    }
    
}
