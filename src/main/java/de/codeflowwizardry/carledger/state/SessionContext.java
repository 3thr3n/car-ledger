package de.codeflowwizardry.carledger.state;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SessionContext {
    private static final ThreadLocal<SessionUser> USER_HOLDER = new ThreadLocal<>();

    public void set(SessionUser sessionUser) {
        USER_HOLDER.set(sessionUser);
    }

    public SessionUser get() {
        return USER_HOLDER.get();
    }

    public void clear() {
        USER_HOLDER.remove();
    }
}