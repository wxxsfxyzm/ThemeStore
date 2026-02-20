// IUserService.aidl
package com.merak.x;

interface IUserService {
    /**
     * Executes a shell command with arguments and returns the result synchronously.
     *
     * @param command an array of strings representing the command and its arguments
     * @return the standard output of the executed command
     */
    String execArr(in String[] command) = 1;

    void setAccessibilityServiceState(in ComponentName componentName, boolean enabled) = 2;

    void grantRuntimePermission(String packageName, String permission, int userId) = 3;

    void setAppOpsMode(int code, int uid, String packageName, int mode) = 4;

    oneway void destroy() = 16777114;
}