package com.mygolfswing.app.data.repo

import android.content.Context
import androidx.core.content.edit
import com.mygolfswing.app.data.net.V2NetClient
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class V2AppUpdatePolicy(
    val enabled: Boolean,
    val latestVersion: String,
    val minimumSupportedVersion: String,
    val title: String,
    val message: String,
    val releaseNotes: String,
    val storeUrl: String,
    val remindIntervalHours: Int,
    val rolloutPercentage: Int,
    val forceAfter: String,
    val allowLocalAccess: Boolean = true,
)

enum class V2AppUpdateKind { RECOMMENDED, REQUIRED }

data class V2AppUpdatePresentation(
    val policy: V2AppUpdatePolicy,
    val kind: V2AppUpdateKind,
    val currentVersion: String,
)

object V2AppVersionPolicy {
    fun matchesPlatform(responsePlatform: String?, expectedPlatform: String): Boolean =
        responsePlatform?.trim()?.equals(expectedPlatform.trim(), ignoreCase = true) == true

    fun compare(left: String, right: String): Int {
        val a = numericParts(left)
        val b = numericParts(right)
        repeat(maxOf(a.size, b.size)) { index ->
            val delta = (a.getOrNull(index) ?: 0) - (b.getOrNull(index) ?: 0)
            if (delta != 0) return if (delta < 0) -1 else 1
        }
        return 0
    }

    fun rolloutBucket(identifier: String): Int {
        var hash = 2_166_136_261u
        identifier.encodeToByteArray().forEach { byte ->
            hash = hash xor byte.toUByte().toUInt()
            hash *= 16_777_619u
        }
        return (hash % 100u).toInt()
    }

    private fun numericParts(value: String): List<Int> = value.split('.').map { part ->
        part.takeWhile(Char::isDigit).toIntOrNull() ?: 0
    }
}

class V2AppUpdateRepository(
    context: Context,
    private val net: V2NetClient,
) {
    private val preferences = context.getSharedPreferences("v2_app_update", Context.MODE_PRIVATE)

    suspend fun check(currentVersion: String, now: Instant = Instant.now()): Result<V2AppUpdatePresentation?> {
        return try {
            val response = net.cloudApi.videoPlaza(
            JsonObject(
                mapOf(
                    "action" to JsonPrimitive("getAppUpdatePolicy"),
                    "platform" to JsonPrimitive("android"),
                ),
            ),
        )
            if (response.bool("success") != true) error("版本策略暂时不可用")
            val policyObject = response["policy"] as? JsonObject ?: return Result.success(null)
            // The deployed legacy function historically ignored the request platform and
            // returned the iOS App Store policy. Fail closed until the backend explicitly
            // echoes Android, otherwise a fresh Android install can advertise an iOS release.
            if (!V2AppVersionPolicy.matchesPlatform(policyObject.string("platform"), "android")) {
                return Result.success(null)
            }
            val policy = V2AppUpdatePolicy(
            enabled = policyObject.bool("enabled") == true,
            latestVersion = policyObject.string("latestVersion").orEmpty(),
            minimumSupportedVersion = policyObject.string("minimumSupportedVersion").orEmpty(),
            title = policyObject.string("title") ?: "发现新版本",
            message = policyObject.string("message") ?: "新版本已经可以下载。",
            releaseNotes = policyObject.string("releaseNotes").orEmpty(),
            storeUrl = policyObject.string("storeURL", "storeUrl").orEmpty(),
            remindIntervalHours = policyObject.int("remindIntervalHours")?.coerceIn(1, 720) ?: 24,
            rolloutPercentage = policyObject.int("rolloutPercentage")?.coerceIn(0, 100) ?: 100,
            forceAfter = policyObject.string("forceAfter").orEmpty(),
            allowLocalAccess = policyObject.bool("allowLocalAccess") ?: true,
        )
            if (!policy.enabled || policy.latestVersion.isBlank() || policy.minimumSupportedVersion.isBlank() ||
                policy.storeUrl.isBlank() || V2AppVersionPolicy.compare(currentVersion, policy.latestVersion) >= 0
            ) return Result.success(null)

            val forceReached = policy.forceAfter.takeIf(String::isNotBlank)?.let {
                runCatching { !now.isBefore(Instant.parse(it)) }.getOrDefault(false)
            } ?: false
            val required = V2AppVersionPolicy.compare(currentVersion, policy.minimumSupportedVersion) < 0 || forceReached
            if (!required) {
                val installationId = installationId()
                if (V2AppVersionPolicy.rolloutBucket(installationId) >= policy.rolloutPercentage) {
                    return Result.success(null)
                }
                val lastVersion = preferences.getString(KEY_LAST_VERSION, null)
                val lastEpoch = preferences.getLong(KEY_LAST_TIME, 0L)
                if (lastVersion == policy.latestVersion && lastEpoch > 0L) {
                    val nextReminder = Instant.ofEpochMilli(lastEpoch).plus(policy.remindIntervalHours.toLong(), ChronoUnit.HOURS)
                    if (now.isBefore(nextReminder)) return Result.success(null)
                }
            }
            Result.success(
                V2AppUpdatePresentation(
                    policy = policy,
                    kind = if (required) V2AppUpdateKind.REQUIRED else V2AppUpdateKind.RECOMMENDED,
                    currentVersion = currentVersion,
                ),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }

    fun remindLater(presentation: V2AppUpdatePresentation) {
        if (presentation.kind == V2AppUpdateKind.REQUIRED) return
        preferences.edit {
            putString(KEY_LAST_VERSION, presentation.policy.latestVersion)
            putLong(KEY_LAST_TIME, System.currentTimeMillis())
        }
    }

    private fun installationId(): String {
        preferences.getString(KEY_INSTALLATION_ID, null)?.let { return it }
        return UUID.randomUUID().toString().also { value ->
            preferences.edit { putString(KEY_INSTALLATION_ID, value) }
        }
    }

    private companion object {
        const val KEY_INSTALLATION_ID = "installation_id"
        const val KEY_LAST_VERSION = "last_reminder_version"
        const val KEY_LAST_TIME = "last_reminder_time"
    }
}

private fun JsonObject.bool(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull
private fun JsonObject.string(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
    this[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf(String::isNotEmpty)
}
private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.let {
    it.intOrNull ?: it.contentOrNull?.toIntOrNull()
}
