# Android 官网分发

- 固定下载页：`https://mygolfai.com.cn/android/`
- 固定 APK 地址：`https://mygolfai.com.cn/android/latest.apk`
- 版本元数据：`https://mygolfai.com.cn/android/latest.json`
- 二维码始终指向固定下载页，后续发版无需更换二维码。
- 正式 APK 必须始终使用 `/Users/huxiaoran/.mygolfswing/release/mygolfswing-release.jks` 中的同一把密钥签名。
- 密钥密码保存在 macOS Keychain 的 `com.mygolfswing.android.release.store` 和 `com.mygolfswing.android.release.key` 项中，不得提交到 Git。
