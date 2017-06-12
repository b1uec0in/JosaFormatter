# Josa Formatter
받침에 따라 조사(은,는,이,가,을,를 등)를 교정할 수 있는 String.format과 유사한 함수를 제공합니다.

[![CI Status](http://img.shields.io/travis/b1uec0in/JosaFormatter.svg?style=flat)](https://travis-ci.org/b1uec0in/JosaFormatter)
[![Release](https://jitpack.io/v/b1uec0in/JosaFormatter.svg)](https://jitpack.io/#b1uec0in/JosaFormatter)
[![license: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![language: java|swift](https://img.shields.io/badge/language-java|swift-84acfe.svg)](#repositories)


### Sample

```java
KoreanUtils.format("%s를 %s으로 변경할까요?", "아이폰", "Galaxy");

아이폰을 Galaxy로 변경할까요?
```


### Setup
build.gradle
```diff
apply plugin: 'java'  // or 'com.android.application'

+ repositories {
+    maven { url "https://jitpack.io" }
+ }

dependencies {
...
+ compile 'com.github.b1uec0in:JosaFormatter:+'
}
```

### Features
* 앞 글자의 종성(받침) 여부에 따라 조사(은,는,이,가,을,를 등)를 교정합니다.
* 한글 뿐만 아니라 영어, 숫자, 한자, 일본어 등도 처리가 가능합니다.
* 조사 앞에 인용 부호나 괄호가 있어도 동작합니다.
```java
KoreanUtils.format("'%s'는 사용중인 닉네임 입니다.", nickName);
```
* Detector를 직접 등록하거나 우선 순위 등을 조정할 수 있습니다. (JongSungDetector 클래스 순서 참고)

### JongSungDetector 기본 우선 순위
* 한글 (HangulJongSungDetector)<br/>
: '홍길동'은
* 영문 대문자 약어 (EnglishCapitalJongSungDetector)<br/>
: 'IBM'이(아이비엠이)
* 일반 영문 (EnglishJongSungDetector)<br/>
: 'Google'을(구글을)
* 영문+숫자 (EnglishNumberJongSungDetector)<br/>
: 'WD40'는(더블유디포티는) - 이렇게 읽는 경우는 드물어 기본으로는 등록되어 있지 않습니다. (예외 처리 참고)
* 영문+10이하 숫자 (EnglishNumberKorStyleJongSungDetector)<br/>
: 'MP3'는(엠피쓰리는), 'WD40'은(더블유디사십은)
* 숫자 (NumberJongSungDetector)<br/>
: '1'과 '2'는(일과 이는)
* 한자 (HanjaJongSungDetector)<br/>
: '6月'은(유월은)
* 일본어 JapaneseJongSungDetector<br/>
: 'あゆみ'는(아유미는)

### 예외 처리
* '영문+숫자'는 경우 10 이하만 영어로 읽도록 되어 있습니다.<br/>
보통 'MP3'는 '엠피쓰리'로 읽지만, 'Office 2000'은 '오피스 이천'으로 읽습니다.<br/>
만약 '영문+숫자'를 항상 영어로 읽도록 하기 위해서는 직접 EnglishNumberJongSungDetector 를 등록해야 합니다.

```java
// 기본 설정
String text = josaFormatter.format("%s을 구매하시겠습니까?", "Office 2000"));
// Office 2000을 구매하시겠습니까? -> '오피스 이천'으로 읽음


// EnglishNumberKorStyleJongSungDetector 대신 EnglishNumberJongSungDetector를 등록
JosaFormatter josaFormatter = new JosaFormatter();
ArrayList<JosaFormatter.JongSungDetector> jongSungDetectors = josaFormatter.getJongSungDetectors();
for (int i = 0; i < jongSungDetectors.size(); ++i) {
    JosaFormatter.JongSungDetector jongSungDetector = jongSungDetectors.get(i);
    if (jongSungDetector instanceof JosaFormatter.EnglishNumberKorStyleJongSungDetector) {
        jongSungDetectors.set(i, new JosaFormatter.EnglishNumberJongSungDetector());
        break;
    }
}

String text = josaFormatter.format("%s을 구매하시겠습니까?", "Office 2000"));
// Office 2000를 구매하시겠습니까? -> '오피스 투싸우전드'로 읽음

```

* '한글+숫자'인 경우 숫자는 한글로 읽도록 되어 있습니다.<br/>
하지만, 영어를 한글로 쓴 경우 숫자도 영어로 읽어야 해서 오동작하는 경우가 있습니다.
현재는 읽는 규칙을 직접 추가해줘서 영어로 간주하도록 할 수 있습니다.
```java
KoreanUtils.getDefaultJosaFormatter().addReadRule("베타", "beta");
String text = KoreanUtils.format("%s을 구매하시겠습니까?", "베가 베타 3"));
// 베가 베타 3를 구매하시겠습니까?
```

### Repositories
* Java version<br/>
https://github.com/b1uec0in/JosaFormatter

* Swift version<br/>
https://github.com/b1uec0in/SwiftJosaFormatter

* Android Sample<br/>
https://github.com/b1uec0in/AndroidJosaFormatter

### Reference
* 한국어 속 영어 읽기<br/>
http://blog.naver.com/b1uec0in/221025080633

* 한글 받침에 따라 '을/를' 구분 <br/>
http://gun0912.tistory.com/65

* 한글, 영어 받침 처리 (iOS) <br/>
https://github.com/trilliwon/JNaturalKorean

* 한자를 한글로 변환 <br/>
http://kangwoo.tistory.com/33

* suffix로 영어 단어 찾기 <br/>
http://www.litscape.com/word_tools/ends_with.php

