package com.github.b1uec0in.josaformatter;

import java.util.Locale;

/**
 * Created by yjbae@sk.com on 2017/05/23.
 */

public class KoreanUtils {

    private static JosaFormatter defaultJosaFormatter;

    public static JosaFormatter getDefaultJosaFormatter() {
        if (defaultJosaFormatter == null) {
            defaultJosaFormatter = createDefaultJosaFormatter();
        }

        return defaultJosaFormatter;
    }

    public static JosaFormatter createDefaultJosaFormatter() {
        JosaFormatter josaFormatter = new JosaFormatter();
        return josaFormatter;
    }


    public static String format(String format, Object... args) {
        return format(Locale.getDefault(), format, args);
    }

    public static String format(Locale l, String format, Object... args) {
        return getDefaultJosaFormatter().format(l, format, args);
    }
}
