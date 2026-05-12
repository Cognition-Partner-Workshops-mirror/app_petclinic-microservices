package com.resumeai.resume.exception;

/**
 * Thrown when a requested resource (resume, job description) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
