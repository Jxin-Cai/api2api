package com.api2api.ohs.http;

import com.api2api.domain.user.model.UserAccountId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

/**
 * Resolves current user identity from the server-side HTTP session.
 */
@Component
public class CurrentUserContextResolver {

    public static final String CURRENT_USER_ID_SESSION_KEY = "api2api.currentUserId";

    public UserAccountId resolveCurrentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new AuthenticationRequiredException("Authentication required");
        }
        Object sessionValue = session.getAttribute(CURRENT_USER_ID_SESSION_KEY);
        if (sessionValue == null) {
            throw new AuthenticationRequiredException("Authentication required");
        }
        try {
            if (sessionValue instanceof Number number) {
                return UserAccountId.of(number.longValue());
            }
            return UserAccountId.of(Long.valueOf(String.valueOf(sessionValue).trim()));
        } catch (NumberFormatException exception) {
            session.invalidate();
            throw new AuthenticationRequiredException("Invalid authenticated session");
        }
    }

    public void bindCurrentUser(HttpServletRequest request, UserAccountId userAccountId) {
        HttpSession session = request.getSession(true);
        session.setAttribute(CURRENT_USER_ID_SESSION_KEY, userAccountId.getValue());
    }

    public void clearCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    public UserAccountId resolveOperatorUserId(HttpServletRequest request) {
        return resolveCurrentUserId(request);
    }
}
