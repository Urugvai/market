package org.morozov.market.util;

import org.jetbrains.annotations.Nullable;

/**
 * Created by Morozov on 5/22/2017.
 */
public class StringUtils {

    public static boolean isBlank(@Nullable final String string) {
        return string == null || "".equals(string);
    }
}
