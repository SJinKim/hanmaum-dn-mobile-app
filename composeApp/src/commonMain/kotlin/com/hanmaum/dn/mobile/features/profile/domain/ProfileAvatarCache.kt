package com.hanmaum.dn.mobile.features.profile.domain

/** Drops a superseded avatar from the image caches, so a replaced or deleted photo never reappears. */
interface ProfileAvatarCache {
    fun evict(cacheKey: String)
}
