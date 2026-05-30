package io.github.adulescentia.LUMOS_lib;

import android.util.Log;

/**
 * Android 런타임과 JVM unit test 환경 모두에서 안전하게 동작하는 내부 로그 어댑터.
 *
 * <p>로컬 JVM unit test에서는 android.util.Log가 mock 처리되지 않아 RuntimeException을 던질 수 있다.
 * 라이브러리 핵심 로직이 로그 호출 때문에 실패하지 않도록 Android Log 실패 시 표준 출력으로 폴백한다.</p>
 */
final class LumosLog {
    private LumosLog() {}

    static void d(String tag, String message) {
        try {
            Log.d(tag, message);
        } catch (RuntimeException ignored) {
            System.out.println(format(tag, message));
        }
    }

    static void w(String tag, String message) {
        try {
            Log.w(tag, message);
        } catch (RuntimeException ignored) {
            System.out.println(format(tag, message));
        }
    }

    static void e(String tag, String message, Throwable throwable) {
        try {
            Log.e(tag, message, throwable);
        } catch (RuntimeException ignored) {
            System.err.println(format(tag, message));
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        }
    }

    private static String format(String tag, String message) {
        return tag + ": " + message;
    }
}
