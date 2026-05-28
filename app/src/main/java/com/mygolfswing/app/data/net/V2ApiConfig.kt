package com.mygolfswing.app.data.net

/**
 * 对齐 iOS `TencentCloudConfig` 的常量集合。
 */
object V2ApiConfig {
    const val CLOUD_BASE_HOST = "https://mygolfswingapp-9g2izywqa8ac3f5b-1259543736.ap-shanghai.app.tcloudbase.com/"
    const val DOWNLOAD_SERVER_BASE = "https://download.mygolfai.com.cn/api/"

    const val IM_SDK_APP_ID: Int = 1600125336

    /** 默认 builtin AI 服务（挥呗 AI），baseURL 复用 download 域名 */
    const val BUILTIN_AI_BASE = "https://download.mygolfai.com.cn/api/ai/"
}
