package com.xusu.sshlinkterminal

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.xusu.sshlinkterminal.data.AppRepository
import com.xusu.sshlinkterminal.shizuku.ShizukuManager
import com.xusu.sshlinkterminal.ssh.SshSessionManager
import java.lang.ref.WeakReference

class LinkTerminalApp : Application() {

    lateinit var repository: AppRepository
        private set

    lateinit var shizukuManager: ShizukuManager
        private set

    lateinit var sshManager: SshSessionManager
        private set

    @Volatile
    private var resumedActivityRef: WeakReference<Activity>? = null

    fun currentResumedActivity(): Activity? = resumedActivityRef?.get()

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                resumedActivityRef = WeakReference(activity)
            }

            override fun onActivityPaused(activity: Activity) {
                if (resumedActivityRef?.get() === activity) {
                    resumedActivityRef = null
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        repository = AppRepository(this)
        shizukuManager = ShizukuManager(this, repository)
        sshManager = SshSessionManager(this, repository)
    }
}
