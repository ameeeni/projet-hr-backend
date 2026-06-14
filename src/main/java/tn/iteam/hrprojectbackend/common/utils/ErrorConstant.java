package tn.iteam.hrprojectbackend.common.utils;

public class ErrorConstant {
    private ErrorConstant() {}

    // User
    public static final String DUPLICATE_EMAIL       = "DUPLICATE_EMAIL";
    public static final String DUPLICATE_MATRICULE   = "DUPLICATE_MATRICULE";
    public static final String HR_CANNOT_HAVE_TEAM   = "HR_CANNOT_HAVE_TEAM";
    public static final String USER_NOT_FOUND        = "USER_NOT_FOUND";      // ← ajouté

    // Team
    public static final String DUPLICATE_TEAM_NAME   = "DUPLICATE_TEAM_NAME";
    public static final String USER_ALREADY_IN_TEAM  = "USER_ALREADY_IN_TEAM";
    public static final String USER_NOT_IN_TEAM      = "USER_NOT_IN_TEAM";
    public static final String TEAM_NOT_FOUND        = "TEAM_NOT_FOUND";      // ← ajouté

    // Leave
    public static final String OVERLAPPING_REQUEST        = "OVERLAPPING_REQUEST";
    public static final String INSUFFICIENT_BALANCE       = "INSUFFICIENT_BALANCE";
    public static final String INVALID_DATE_RANGE         = "INVALID_DATE_RANGE";
    public static final String REQUEST_ALREADY_PROCESSED  = "REQUEST_ALREADY_PROCESSED";
    public static final String CANNOT_CANCEL_REQUEST      = "CANNOT_CANCEL_REQUEST";
    public static final String REQUEST_NOT_FOUND          = "REQUEST_NOT_FOUND";
    public static final String VALIDATOR_NOT_FOUND        = "VALIDATOR_NOT_FOUND";
    public static final String NOT_VALID_VALIDATOR        = "INVALID_VALIDATOR_ROLE";
    public static final String TREATED_REQUEST            = "TREATED_REQUEST";
}
