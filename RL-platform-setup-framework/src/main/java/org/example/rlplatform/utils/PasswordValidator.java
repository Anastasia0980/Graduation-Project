package org.example.rlplatform.utils;

public final class PasswordValidator {

    public static final String MESSAGE = "密码需为8~16位，且同时包含大写字母、小写字母和数字";

    private static final String PASSWORD_PATTERN = "^(?=\\S{8,16}$)(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).*$";

    private PasswordValidator() {
    }

    public static boolean isValid(String password) {
        return password != null && password.matches(PASSWORD_PATTERN);
    }
}
