package com.xusu.sshlinkterminal

import android.app.Application
import com.xusu.sshlinkterminal.data.AppRepository
import com.xusu.sshlinkterminal.shizuku.ShizukuManager
import com.xusu.sshlinkterminal.ssh.SshSessionManager

class LinkTerminalApp : Application() {

    lateinit var repository: AppRepository
        private set

    lateinit var shizukuManager: ShizukuManager
        private set

    lateinit var sshManager: SshSessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        repository = AppRepository(this)
        shizukuManager = ShizukuManager(this, repository)
        sshManager = SshSessionManager(this, repository)
    }
}
