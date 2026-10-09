package com.hanmaum.dn.mobile.features.bulletin.domain.repository

import com.hanmaum.dn.mobile.features.bulletin.domain.model.BulletinPage
import com.hanmaum.dn.mobile.features.bulletin.domain.model.BulletinRead
import kotlinx.datetime.LocalDate

class BulletinAccessDenied : Exception("Bulletin access denied")

interface BulletinRepository {
    suspend fun getCurrent(): Result<BulletinRead?>
    suspend fun getByDate(date: LocalDate): Result<BulletinRead?>
    suspend fun getHistory(page: Int): Result<BulletinPage>
}
