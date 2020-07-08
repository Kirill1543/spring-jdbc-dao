package com.kappadrive.dao.gen.tool;

import javax.annotation.Nonnull;

public final class DbNameUtil {

    private DbNameUtil() {
    }

    @Nonnull
    public static String convertToDbName(@Nonnull String name) {
        StringBuilder dbName = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isUpperCase(c)) {
                dbName.append('_').append(Character.toLowerCase(c));
            } else {
                dbName.append(c);
            }
        }
        if (dbName.charAt(0) == '_') {
            dbName.deleteCharAt(0);
        }
        return dbName.toString();
    }
}
