package tn.iteam.hrprojectbackend.common.exception;

public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final Long resourceId;

    public ResourceNotFoundException(String resourceName, Long resourceId) {
        super(resourceName + " introuvable avec l'id : " + resourceId);
        this.resourceName = resourceName;
        this.resourceId = resourceId;
    }

    public ResourceNotFoundException(String resourceName, String field, String value) {
        super(resourceName + " introuvable avec " + field + " : " + value);
        this.resourceName = resourceName;
        this.resourceId = null;
    }
}
