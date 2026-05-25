package com.plog.api.common;

public final class UserContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private UserContext() {}

    public static void set(long userId) {
        CURRENT.set(userId);
    }

    public static long get() {
        Long id = CURRENT.get();
        if (id == null) {
            throw new IllegalStateException("UserContext가 비어있습니다 (인터셉터 통과 후 호출되어야 함)");
        }
        return id;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
