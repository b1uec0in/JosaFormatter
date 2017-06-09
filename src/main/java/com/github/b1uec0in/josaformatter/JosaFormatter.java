package com.github.b1uec0in.josaformatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by yjbae@sk.com on 2017/05/24.
 */

public class JosaFormatter {
    // 조사들을 종성이 있을 때와 없을 때 순서로 나열.
    private static List<Pair<String, String>> josaPairs = Arrays.asList(
            new Pair<>("은", "는"),
            new Pair<>("이", "가"),
            new Pair<>("을", "를"),
            new Pair<>("과", "와"),
            new Pair<>("으로", "로")
    );

    // 종성(받침) 검사 필터. 순서대로 동작함.
    private ArrayList<JongSungDetector> jongSungDetectors = new ArrayList<>(Arrays.asList(
            new HangulJongSungDetector(),
            new EnglishCapitalJongSungDetector(),
            new EnglishJongSungDetector(),
            //new EnglishNumberJongSungDetector(), // 일반인이 영어+숫자인 경우 항상 숫자를 영어로 읽는 경우는 드물기 때문에 사용하지 않음.
            new EnglishNumberKorStyleJongSungDetector(),
            new NumberJongSungDetector(),
            new HanjaJongSungDetector(),
            new JapaneseJongSungDetector()
    ));

    // 사용자 추가 읽기 규칙. 주로 한글+숫자인 경우 한글로 쓴 외국어를 영어로 인식하기 위해 필요함.
    // ex) 아이폰3는 한글뒤의 숫자를 '아이폰삼'이 아니라 '아이폰쓰리'로 읽기 위해서는 '아이폰' 한글을 'iPhone' 영어로 인식해야 한다.
    // 특히 숫자 0,3,6이 사용될 가능성이 있는 경우에 유용함. (0,3,6은 영어로 발음할 때 종성 유무가 한글과 다르다.
    // 반대로 '포르쉐911' 처럼 '포르쉐구일일'이나 '포르쉐나인원원'로 읽어도 종성 유무가 동일한 경우는 규칙에 넣을 필요가 없다.
    private ArrayList<Pair<String, String>> readingRules = new ArrayList<>(Arrays.asList(
            new Pair<>("아이폰", "iPhone"),
            new Pair<>("갤럭시", "Galaxy"),
            new Pair<>("넘버", "number")
    ));

    public ArrayList<JongSungDetector> getJongSungDetectors() {
        return jongSungDetectors;
    }

    public void setJongSungDetectors(ArrayList<JongSungDetector> jongSungDetectors) {
        this.jongSungDetectors = jongSungDetectors;
    }

    public String format(String format, Object... args) {
        return format(Locale.getDefault(), format, args);
    }

    public static class FormattedString {
        private String s;
        private boolean isFormatString;

        public FormattedString(String s, boolean isFormatString) {
            this.s = s;
            this.isFormatString = isFormatString;
        }

        public String toString() {
            return s;
        }

        public boolean isFormatString() {
            return isFormatString;
        }
    }


    // 't', 'T' (date/time) conversion are not supported
    private static final String simpleFormatRegex = "%(\\d+\\$?\\<?)?([-#+ 0,(]*)?(\\d+)?(\\.\\d+)?([bBhHsScCdoxXeEfgGaAtT%])";

    private static Pattern simpleFormatPattern = Pattern.compile(simpleFormatRegex);

    public static ArrayList<FormattedString> parseFormat(Locale locale, String format, Object[] args) {
        ArrayList<FormattedString> formattedStrings = new ArrayList<>();

        Matcher matcher = simpleFormatPattern.matcher(format);

        int prevMatcherEnd = 0;
        int argIndex = 0;
        int lastArgIndex = -1;
        while (matcher.find()) {
            int start = matcher.start();

            if (start > prevMatcherEnd) {
                String prevText = format.substring(prevMatcherEnd, start);
                formattedStrings.add(new FormattedString(prevText, false));
            }
            String singleFormat = matcher.group();

            int groupCount = matcher.groupCount();

            if (groupCount == 5) {
                String indexString = matcher.group(1);
                String conversion = matcher.group(5);

                if (conversion.equals("%")) {
                    formattedStrings.add(new FormattedString(singleFormat, false));
                } else {
                    int index = -1;
                    if (indexString != null && indexString.length() > 0) {

                        if (indexString.equals("<")) { // previous format specifier index
                            index = lastArgIndex;
                        } else if (indexString.endsWith("$")) { // argument position indexing
                            try {
                                index = Integer.parseInt(indexString.substring(0, indexString.length() - 1)) - 1;
                            } catch (Exception e) {
                                index = 0;
                            }
                            lastArgIndex = index;
                        }

                        // remove indexString
                        singleFormat = format.substring(matcher.start(0), matcher.start(1)) + format.substring(matcher.end(1), matcher.end(0));

                    } else { // relative indexing
                        index = argIndex++;
                        lastArgIndex = index;
                    }

                    String singleFormattedString = String.format(locale, singleFormat, args[index]);
                    formattedStrings.add(new FormattedString(singleFormattedString, true));
                }
                prevMatcherEnd = matcher.end();
            }

        }

        if (format.length() > prevMatcherEnd) {
            String prevText = format.substring(prevMatcherEnd);
            formattedStrings.add(new FormattedString(prevText, false));
        }

        return formattedStrings;
    }

    public String format(Locale l, String format, Object... args) {
        ArrayList<FormattedString> formattedStrings = parseFormat(l, format, args);

        int count = formattedStrings.size();
        StringBuilder sb = new StringBuilder(formattedStrings.get(0).toString());

        if (count == 1) {
            return sb.toString();
        }

        for (int i = 1; i < formattedStrings.size(); ++i) {
            FormattedString formattedString = formattedStrings.get(i);

            String str;

            if (!formattedString.isFormatString()) {
                str = getJosaModifiedString(formattedStrings.get(i - 1).toString(), formattedString.toString());
            } else {
                str = formattedString.toString();
            }

            sb.append(str);
        }

        return sb.toString();
    }

    private static int indexOfJosa(String str, String josa) {
        int index;
        int searchFromIndex = 0;
        int strLength = str.length();
        int josaLength = josa.length();
        do {
            index = str.indexOf(josa, searchFromIndex);

            if (index >=0) {
                int josaNext = index + josaLength;

                // 조사로 끝나거나 뒤에 공백이 있어야 함.
                if (josaNext < strLength) {
                    if (Character.isWhitespace(str.charAt(josaNext))) {
                        return index;
                    }
                } else {
                    return index;
                }
            } else {
               return -1;
            }
            searchFromIndex = index + josaLength;
        } while(searchFromIndex < strLength);

        return -1;
    }

    public String getJosaModifiedString(String previous, String str) {

        if (previous == null || previous.length() == 0) {
            return str;
        }

        Pair<String, String> matchedJosaPair = null;
        int josaIndex = -1;

        String searchStr = null;
        for (Pair<String, String> josaPair : josaPairs) {
            int firstIndex = indexOfJosa(str, josaPair.first);
            int secondIndex = indexOfJosa(str, josaPair.second);

            if (firstIndex >= 0 && secondIndex >= 0) {
                if (firstIndex < secondIndex) {
                    josaIndex = firstIndex;
                    searchStr = josaPair.first;
                } else {
                    josaIndex = secondIndex;
                    searchStr = josaPair.second;
                }
            } else if (firstIndex >= 0) {
                josaIndex = firstIndex;
                searchStr = josaPair.first;
            } else if (secondIndex >= 0) {
                josaIndex = secondIndex;
                searchStr = josaPair.second;
            }

            if (josaIndex >= 0 && isEndSkipText(str, 0, josaIndex)) {
                matchedJosaPair = josaPair;
                break;
            }
        }

        if (matchedJosaPair != null) {

            String readText = getReadText(previous);

            ArrayList<JongSungDetector> jongSungDetectors = getJongSungDetectors();
            for (JongSungDetector jongSungDetector : jongSungDetectors) {
                if (jongSungDetector.canHandle(readText)) {
                    return replaceStringByJongSung(str, matchedJosaPair, jongSungDetector.hasJongSung(readText));
                }
            }

            // 없으면 괄호 표현식을 사용한다. ex) "???을(를) 찾을 수 없습니다."

            String replaceStr = matchedJosaPair.first + "(" + matchedJosaPair.second + ")";
            return str.substring(0, josaIndex) + replaceStr + str.substring(josaIndex + searchStr.length());
        }

        return str;
    }

    public String replaceStringByJongSung(String str, Pair<String, String> josaPair, boolean hasJongSung) {
        if (josaPair != null) {
            // 잘못된 것을 찾아야 하므로 반대로 찾는다. 종성이 있으면 종성이 없을 때 사용하는 조사가 사용 되었는지 찾는다.
            String searchStr = hasJongSung ? josaPair.second : josaPair.first;
            String replaceStr = hasJongSung ? josaPair.first : josaPair.second;
            int josaIndex = str.indexOf(searchStr);

            if (josaIndex >= 0 && isEndSkipText(str, 0, josaIndex)) {
                return str.substring(0, josaIndex) + replaceStr + str.substring(josaIndex + searchStr.length());
            }
        }

        return str;
    }

    public boolean isEndSkipText(String str, int begin, int end) {
        for (int i = begin; i < end; ++i) {
            if (!isEndSkipText(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    // 조사 앞에 붙는 문자중 무시할 문자들. ex) "(%s)으로"
    public boolean isEndSkipText(char ch) {
        String skipChars = "\"')]}>";
        return skipChars.indexOf(ch) >= 0;
    }

    public String getReadText(String str) {
        for (Pair<String, String> readingRule : readingRules) {
            str = str.replace(readingRule.first, readingRule.second);
        }

        int skipCount = 0;

        int i;
        for (i = str.length() - 1; i >= 0; --i) {
            char ch = str.charAt(i);

            if (!isEndSkipText(ch)) {
                break;
            }
        }

        return str.substring(0, i + 1);
    }

    public void addReadRule(String originalText, String replaceText) {
        for (Pair<String, String> readingRule : readingRules) {
            if (readingRule.first.equals(originalText)) {
                readingRules.remove(readingRule);
                break;
            }
        }
        readingRules.add(new Pair<>(originalText, replaceText));
    }

    interface JongSungDetector {
        boolean canHandle(String str);

        boolean hasJongSung(String str);
    }


    public static class HangulJongSungDetector implements JongSungDetector {

        @Override
        public boolean canHandle(String str) {
            return CharUtils.isHangulSyllables(CharUtils.lastChar(str));
        }

        @Override
        public boolean hasJongSung(String str) {
            return CharUtils.hasHangulJongSung(CharUtils.lastChar(str));
        }
    }

    public static class EnglishCapitalJongSungDetector implements JongSungDetector {

        @Override
        public boolean canHandle(String str) {
            char ch = CharUtils.lastChar(str);
            if (CharUtils.isAlphaUpperCase(ch)) {
                return true;
            }

            return false;
        }

        @Override
        public boolean hasJongSung(String str) {
            String jongSungChars = "LMNR";
            char lastChar = CharUtils.lastChar(str);
            return jongSungChars.indexOf(lastChar) >= 0;
        }
    }


    public static class EnglishJongSungDetector implements JongSungDetector {

        private ArrayList<Pair<String, Boolean>> customRules = new ArrayList<>(Arrays.asList(
                new Pair<>("app", true),
                new Pair<>("god", true),
                new Pair<>("good", true),
                new Pair<>("pod", true),
                new Pair<>("bag", true),
                new Pair<>("big", true),
                new Pair<>("gig", true),
                new Pair<>("chocolate", true),
                new Pair<>("root", false),
                new Pair<>("boot", false),
                new Pair<>("check", false)
        ));
        @Override
        public boolean canHandle(String str) {
            char lastChar  = CharUtils.lastChar(str);

            // q, j 등으로 끝나는 단어는 알려지지 않음.
            String unknownWordSuffixs = "qj";

            if (unknownWordSuffixs.indexOf(lastChar) >= 0) {
                return false;
            }

            return CharUtils.isAlpha(lastChar);
        }

        public void addCustomRule(String suffix, boolean hasJongSung) {
            customRules.add(new Pair<>(suffix, hasJongSung));
        }

        @Override
        public boolean hasJongSung(String str) {
            str = str.toLowerCase();

            for (Pair<String, Boolean> rule : customRules) {
                if (str.endsWith(rule.first)) {
                    return rule.second;
                }
            }

            int length = str.length();
            char lastChar1 = str.charAt(length - 1);

            // 3자 이상인 경우만 마지막 2자만 suffix로 간주.
            String suffix = null;
            char lastChar2 = '\0';
            char lastChar3 = '\0';
            if (str.length() >= 3) {
                lastChar2 = str.charAt(length - 2);
                lastChar3 = str.charAt(length - 3);

                if (CharUtils.isAlpha(lastChar2) && CharUtils.isAlpha(lastChar3)) {
                    suffix = String.valueOf(lastChar2) + String.valueOf(lastChar1);
                }
            }

            if (suffix != null) {
                // 끝나는 문자들로 종성 여부를 확인할 때 qj를 제외한 알파벳 22자를 기준으로 분류하면 아래와 같다.
                String jongSungChars = "lmn"; // 1. 항상 받침으로 읽음
                String notJongSungChars = "afhiorsuvwxyz"; // 2. 항상 받침으로 읽지 않음
                String jongSungCandidateChars = "bckpt"; // 3. 대체로 받침으로 읽음
                String notJongSungCandidateChars = "deg";  // 4. 대체로 받침으로 읽지 않음

                if (jongSungChars.indexOf(lastChar1) >= 0) {
                    // 마지막 1문자 lmn은 항상 받침으로 읽음
                    return true;
                } else if (notJongSungChars.indexOf(lastChar1) >= 0) {
                    // 마지막 1문자 afhiorsuvwxyz는 항상 받침으로 읽지 않음
                    return false;
                }

                if (jongSungCandidateChars.indexOf(lastChar1) >= 0) {
                    // 예외 처리
                    switch (suffix) {
                        case "ck":
                        case "mb": // b 묵음
                            return true;
                    }

                    // 마지막 1문자 bckpt는 모음 뒤에서는 받침으로 읽는다.
                    String vowelChars = "aeiou";
                    return vowelChars.indexOf(lastChar2) >= 0;
                } else if (notJongSungCandidateChars.indexOf(lastChar1) >= 0) {
                    // 마지막 1문자 deg는 대체로 받침으로 읽지 않지만, 아래의 경우는 받침으로 읽음.
                    switch (suffix) {
                        case "le": // ㄹ
                        case "me": // ㅁ
                        case "ne": // ㄴ
                        case "ng": // ㅇ
                            return true;
                        default:
                            return false;
                    }
                } else {
                    // unreachable condition
                }
            } else {
                // 1자, 2자는 약자로 간주하고 알파벳 그대로 읽음. (엘엠엔알)만 종성이 있음.
                String jongSungChars = "lmnr";
                if (jongSungChars.indexOf(lastChar1) >= 0) {
                    return true;
                }
            }
            return false;
        }
    }

    // 영문+숫자를 미국식으로 읽기 ex) MP3, iPhone4, iOS8.3 (iOS eight point three), Office2003 (Office two thousand three)
    // 일반적으로 영문+숫자라도 11 이상은 그냥 한글로 읽는 경우가 많아서 적합하지 않을 수 있음.
    public static class EnglishNumberJongSungDetector implements JongSungDetector {
        public static ParseResult parse(String str) {
            ParseResult parseResult = new ParseResult();
            int i;
            boolean isSpaceFound = false;
            int numberPartBeiginIndex = 0;
            boolean isNumberCompleted = false;
            // 뒤에서부터 숫자, 영어 순서로 찾는다.
            for (i = str.length() - 1; i >= 0; --i) {
                char ch = str.charAt(i);

                if (!isNumberCompleted && !isSpaceFound && CharUtils.isNumber(ch)) {
                    parseResult.isNumberFound = true;
                    numberPartBeiginIndex = i;
                    continue;
                }

                if (ch == ',') {
                    continue;
                }

                if (!isNumberCompleted && parseResult.isNumberFound && !parseResult.isFloat && ch == '.') {
                    parseResult.isFloat = true;
                    continue;
                }

                // 공백은 숫자가 찾아진 이후 한번만 허용
                if (!isNumberCompleted && parseResult.isNumberFound && !isSpaceFound && ch == ' ') {
                    isSpaceFound = true;
                    isNumberCompleted = true;
                    continue;
                }

                // - 는 음수나 dash 용도로 사용될 수 있음.
                if (!isNumberCompleted && parseResult.isNumberFound && !isSpaceFound && ch == '-') {
                    isNumberCompleted = true;
                    continue;
                }

                // 영어는 숫자가 찾아진 이후에만 허용
                if (parseResult.isNumberFound && CharUtils.isAlpha(ch)) {
                    parseResult.isEnglishFound = true;
                    isNumberCompleted = true;
                    break;
                }

                break;
            }

            if (parseResult.isNumberFound) {
                parseResult.numberPart = str.substring(numberPartBeiginIndex);
                parseResult.prefixPart = str.substring(0, numberPartBeiginIndex);

                try {
                    parseResult.number = Double.parseDouble(parseResult.numberPart);
                } catch (Exception ignore) {
                }
            }

            return parseResult;
        }

        @Override
        public boolean canHandle(String str) {
            EnglishNumberJongSungDetector.ParseResult parseResult = EnglishNumberJongSungDetector.parse(str);

            return parseResult.isNumberFound && parseResult.isEnglishFound;

        }

        @Override
        public boolean hasJongSung(String str) {
            EnglishNumberJongSungDetector.ParseResult parseResult = EnglishNumberJongSungDetector.parse(str);

            if (!parseResult.isFloat) {
                long number = (long) (parseResult.number);

                if (number == 0) {
                    return false;
                }

                // 두자리 예외 처리
                int twoDigit = (int) (number % 100);
                switch (twoDigit) {
                    case 10:
                    case 13:
                    case 14:
                    case 16:
                        return true;
                }

                // million 이상 예외 처리
                // 100 : hundred (x)
                // 1000 : thousand (x)
                // 1000000... : million, billion, trillion (o)
                if (number % 100000 == 0) {
                    return true;
                }
            }

            // 마지막 한자리 (소수 포함)
            int oneDigit = CharUtils.lastChar(parseResult.numberPart) - '0';

            switch (oneDigit) {
                case 1:
                case 7:
                case 8:
                case 9:
                    return true;
            }

            return false;
        }

        public static class ParseResult {
            public boolean isNumberFound;

            // valid only if isNumberFound
            public boolean isEnglishFound;
            public double number;
            public boolean isFloat;
            public String prefixPart;
            public String numberPart;
        }
    }

    // 영문+숫자 10이하만 영어로 읽기 ex) MP3, iPhone4
    // 다른 경우에는 숫자를 한글로 읽기 위해서는 EnglishNumberJongSungDetector 와 같이 사용하면 안된다.
    public static class EnglishNumberKorStyleJongSungDetector implements JongSungDetector {

        @Override
        public boolean canHandle(String str) {
            EnglishNumberJongSungDetector.ParseResult parseResult = EnglishNumberJongSungDetector.parse(str);

            return parseResult.isNumberFound && parseResult.isEnglishFound && !parseResult.isFloat && parseResult.number <= 10;

        }

        @Override
        public boolean hasJongSung(String str) {
            EnglishNumberJongSungDetector.ParseResult parseResult = EnglishNumberJongSungDetector.parse(str);
            int number = (int) (parseResult.number);
            switch (number) {
                case 1:
                case 7:
                case 8:
                case 9:
                case 10:
                    return true;
            }

            return false;
        }
    }

    // 숫자를 한국식으로 읽기
    public static class NumberJongSungDetector implements JongSungDetector {
        @Override
        public boolean canHandle(String str) {
            EnglishNumberJongSungDetector.ParseResult parseResult = EnglishNumberJongSungDetector.parse(str);

            return parseResult.isNumberFound;

        }

        @Override
        public boolean hasJongSung(String str) {
            EnglishNumberJongSungDetector.ParseResult parseResult = EnglishNumberJongSungDetector.parse(str);

            if (!parseResult.isFloat) {
                long number = (long) (parseResult.number);
                // 조 예외 처리 : 조(받침 없음), 십, 백, 천, 만, 억, 경, 현
                if (number % 1000000000000L == 0) {
                    return false;
                }
            }

            // 마지막 한자리 (소수 포함)
            int oneDigit = CharUtils.lastChar(parseResult.numberPart) - '0';
            switch (oneDigit) {
                case 0:
                case 1:
                case 3:
                case 6:
                case 7:
                case 8:
                    return true;
            }

            return false;
        }
    }


    // 한자는 한글 코드로 변경해서 판단
    public static class HanjaJongSungDetector implements JongSungDetector {

        @Override
        public boolean canHandle(String str) {
            return HanjaMap.canHandle(CharUtils.lastChar(str));
        }

        @Override
        public boolean hasJongSung(String str) {
            char hangulChar = HanjaMap.toHangul(CharUtils.lastChar(str));
            return CharUtils.hasHangulJongSung(hangulChar);
        }
    }

    // 일본어
    public static class JapaneseJongSungDetector implements JongSungDetector {

        @Override
        public boolean canHandle(String str) {
            return CharUtils.isJapanese(CharUtils.lastChar(str));
        }

        @Override
        public boolean hasJongSung(String str) {
            char lastChar = CharUtils.lastChar(str);

            return lastChar == 0x30f3 || lastChar == 0x3093;
        }
    }
}
