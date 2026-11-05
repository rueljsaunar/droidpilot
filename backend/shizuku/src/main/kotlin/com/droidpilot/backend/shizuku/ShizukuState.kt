package com.droidpilot.backend.shizuku

public sealed class ShizukuState {
    /** Shizuku is not installed or its binder has not arrived yet. */
    public object Unavailable : ShizukuState()

    /** Shizuku binder is live but DroidPilot has not been granted permission. */
    public object PermissionRequired : ShizukuState()

    /** Permission was denied by the user. */
    public object PermissionDenied : ShizukuState()

    /** Binding the privileged user service. */
    public object Connecting : ShizukuState()

    /** User service is connected; shell operations are ready. */
    public object Available : ShizukuState()
}
