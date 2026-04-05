package io.ylab.chat.context;

/**
 * Utility class for storing and accessing user-related information in a thread-local context.
 */
public class UserContext {

    /**
     * Thread-local storage for {@link UserInfo} associated with the current thread.
     */
    private static final ThreadLocal<UserInfo> contextHolder = new ThreadLocal<>();

    /**
     * Sets the {@link UserInfo} for the current thread.
     *
     * @param userInfo the user information to associate with the current thread
     */
    public static void setUserInfo(UserInfo userInfo) {
        contextHolder.set(userInfo);
    }

    /**
     * Retrieves the {@link UserInfo} associated with the current thread.
     *
     * @return the current thread's user information, or {@code null} if none is set
     */
    public static UserInfo getUserInfo() {
        return contextHolder.get();
    }

    /**
     * Clears the {@link UserInfo} associated with the current thread.
     */
    public static void clear() {
        contextHolder.remove();
    }

    /**
     * Retrieves the username of the current user from the thread-local context.
     *
     * @return the username if {@link UserInfo} is set; otherwise, {@code null}
     */
    public static String getCurrentUsername() {
        UserInfo userInfo = getUserInfo();
        return userInfo != null ? userInfo.getUsername() : null;
    }
}