package com.hanmaum.dn.mobile.features.profile.data.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.hanmaum.dn.mobile.BuildKonfig
import com.hanmaum.dn.mobile.features.profile.domain.ProfileAvatarCache
import io.ktor.client.HttpClient
import io.ktor.http.Url
import okio.Path.Companion.toPath

/**
 * The loader for avatars only — album and announcement images keep Coil's
 * default. It fetches through the shared [HttpClient], whose host guard keeps
 * the bearer off the storage host even on a challenge, instead of a second
 * client. No logger is attached: the URLs are signed.
 */
class ProfileImageLoader(val imageLoader: ImageLoader)

fun buildProfileImageLoader(context: PlatformContext, client: HttpClient, cacheRoot: String): ProfileImageLoader {
    // One folder per backend, so profile:{id}:{version} on dev never answers for st or prod.
    val environment = Url(BuildKonfig.BACKEND_URL).host.replace(Regex("[^A-Za-z0-9.-]"), "_")
    val loader = ImageLoader.Builder(context)
        .components { add(KtorNetworkFetcherFactory(httpClient = { client })) }
        .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.05).build() }
        .diskCache {
            DiskCache.Builder()
                .directory("${cacheRoot.trimEnd('/')}/avatars-$environment".toPath())
                .maxSizeBytes(AVATAR_DISK_CACHE_BYTES)
                .build()
        }
        .build()
    return ProfileImageLoader(loader)
}

private const val AVATAR_DISK_CACHE_BYTES = 20L * 1024 * 1024

class CoilProfileAvatarCache(private val loader: ProfileImageLoader) : ProfileAvatarCache {
    override fun evict(cacheKey: String) {
        loader.imageLoader.memoryCache?.let { cache ->
            cache.keys.filter { it.key == cacheKey }.forEach(cache::remove)
        }
        loader.imageLoader.diskCache?.remove(cacheKey)
    }
}
