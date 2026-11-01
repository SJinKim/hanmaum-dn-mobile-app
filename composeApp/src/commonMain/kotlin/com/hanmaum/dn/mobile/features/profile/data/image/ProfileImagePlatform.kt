package com.hanmaum.dn.mobile.features.profile.data.image

import androidx.compose.runtime.Composable
import com.hanmaum.dn.mobile.features.profile.domain.ProfileImagePicker

/**
 * The system picker, bound to the current Activity / view controller like
 * [com.hanmaum.dn.mobile.core.security.rememberBrowserAuthentication] — a
 * picker in a singleton would hold on to a destroyed Activity.
 */
@Composable
expect fun rememberProfileImagePicker(): ProfileImagePicker
