package com.example.evotingapp.ui;

import com.example.evotingapp.role.User;

/** Menyimpan pengguna yang sedang login selama aplikasi berjalan. */
public final class Session {
    private static User currentUser;

    private Session() {
    }

    public static User get() {
        return currentUser;
    }

    public static void set(User user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
    }
}
