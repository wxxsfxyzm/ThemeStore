package android.app;

public class AppOpsManager {
    public static final int MODE_ALLOWED = 0;
    public static final int MODE_IGNORED = 1;
    public static final int MODE_ERRORED = 2;

    public int checkOpNoThrow(String op, int uid, String packageName) {
        throw new RuntimeException("Stub!");
    }

    public static int strOpToOp(String op) {
        throw new RuntimeException("Stub!");
    }
}
