package org.geant.maat.common;

public interface CurrentUserResolver {
    String UNKNOWN_USER = "unknown";
    String AUTHENTICATED_USER_HEADER = "X-Authenticated-User-Id";
    String CURRENT_USER_REQUEST_ATTRIBUTE = CurrentUserResolver.class.getName() + ".currentUser";
    String MDC_KEY = "maat.user";

    String resolveCurrentUser();
}
