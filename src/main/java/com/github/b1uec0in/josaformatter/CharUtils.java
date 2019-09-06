package com.github.b1uec0in.josaformatter;

/**
 * Created by yjbae@sk.com on 2017/05/24.
 */

class CharUtils {
    public static boolean isAlpha(char ch) {
        return isAlphaLowerCase(ch) || isAlphaUpperCase(ch);
    }

    public static boolean isAlphaLowerCase(char ch) {
        return ch >= 'a' && ch <= 'z';
    }

    public static boolean isAlphaUpperCase(char ch) {
        return ch >= 'A' && ch <= 'Z';
    }

    public static boolean isNumber(char ch) {
        return ch >= '0' && ch <= '9';
    }

    public static boolean isHangulSyllables(char ch) {
        return ch >= 0xac00 && ch <= 0xd7af;
    }

    public static int getHangulJongSungType(char ch) {
        int result = 0;
        if (isHangulSyllables(ch)) {
            int code = (ch - 0xAC00) % 28;
            if (code > 0) ++result;
            if (code == 8) ++result;
        }

        return result;
    }

    public static boolean isHiragana(char ch) {
        return ch >= 0x3040 && ch <= 0x309F;
    }

    public static boolean isKatakana(char ch) {
        return ch >= 0x30A0 && ch <= 0x30FF;
    }

    public static boolean isJapanese(char ch) {
        return isHiragana(ch) || isKatakana(ch);
    }

    // String manipulation
    public static char lastChar(CharSequence charSequence) {
        if (charSequence == null) {
            return '\0';
        }
        int length = charSequence.length();
        if (length == 0) {
            return '\0';
        }

        return charSequence.charAt(length - 1);
    }

}
