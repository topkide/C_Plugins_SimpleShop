package com.dotorimaru.simpleshop.util;

/** 금액 표시 포맷 (천 단위 콤마, 정수면 소수점 생략). */
public final class Money {
    private Money() {}

    public static String fmt(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) {
            return String.format("%,d", (long) v);
        }
        return String.format("%,.2f", v);
    }
}
