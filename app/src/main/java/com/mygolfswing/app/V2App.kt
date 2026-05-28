package com.mygolfswing.app

import android.app.Application
import com.mygolfswing.app.data.ServiceLocator

/**
 * 入口 Application：负责构造 [ServiceLocator]（数据库 / DataStore / Retrofit / Repo 实例）。
 *
 * 选择 ServiceLocator 而非 Hilt：避免 KSP 二次配置 + 减小学习成本，
 * 之后若工程规模上来可平滑迁移到 Hilt。
 */
class V2App : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
    }
}
