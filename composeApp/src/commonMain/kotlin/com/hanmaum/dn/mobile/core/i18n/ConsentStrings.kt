package com.hanmaum.dn.mobile.core.i18n

import com.hanmaum.dn.mobile.features.consent.domain.model.ConsentProblem

data class ConsentStrings(
    val title: String,
    val label: String,
    val detail: String,
    val policy: String,
    val withdrawalHint: String,
    val missingTitle: String,
    val missingBody: String,
    val outdatedTitle: String,
    val outdatedBody: String,
    val loading: String,
    val loadFailed: String,
    val saveFailed: String,
    val unavailable: String,
    val linkFailed: String,
    val retry: String,
    val continueLabel: String,
    val saving: String,
    val granted: String,
    val record: String,
    val contact: String,
    val controller: String,
    val withdraw: String,
    val withdrawTitle: String,
    val withdrawBody: String,
    val withdrawFailed: String,
    val version: String,
    val newVersion: String,
    val previousVersion: String,
) {
    fun problem(problem: ConsentProblem): String = when (problem) {
        ConsentProblem.LOAD -> loadFailed
        ConsentProblem.SAVE -> saveFailed
        ConsentProblem.VERSION_OUTDATED -> outdatedBody
        ConsentProblem.REQUIRED -> missingBody
        ConsentProblem.POLICY_UNAVAILABLE -> unavailable
        ConsentProblem.WITHDRAW -> withdrawFailed
    }

    fun record(date: String, version: String): String = record.replace("{date}", date).replace("{version}", version)
    fun version(value: String): String = version.replace("{version}", value)
}

val EnConsentStrings = ConsentStrings(
    title = "Privacy information",
    label = "I consent to processing my personal data for church activities",
    detail = "This includes religious information, such as church affiliation and baptism. See the privacy policy for the purposes and scope.",
    policy = "Read privacy policy",
    withdrawalHint = "You can withdraw your consent. See the privacy policy for details and contact information.",
    missingTitle = "Please review the privacy information",
    missingBody = "There is no consent record for this account. Review the purposes and scope before deciding.",
    outdatedTitle = "The privacy information has changed",
    outdatedBody = "Please read the current version and make a new choice. Your previous selection has been cleared.",
    loading = "Loading privacy information…",
    loadFailed = "The privacy information could not be loaded. Check your connection and try again.",
    saveFailed = "Your consent has not been saved. Please try again.",
    unavailable = "The privacy policy is not available yet. Please contact the church team.",
    linkFailed = "The link could not be opened. Please try again or contact the church team.",
    retry = "Try again",
    continueLabel = "Agree and continue",
    saving = "Saving…",
    granted = "Privacy consent",
    record = "Agreed on {date} · Version {version}",
    contact = "Privacy contact",
    controller = "Hanmaum Church",
    withdraw = "Withdraw consent",
    withdrawTitle = "Withdraw your privacy consent?",
    withdrawBody = "You stay signed in, but you cannot use the app until you agree again. This does not affect processing before withdrawal or delete your account.",
    withdrawFailed = "Consent could not be withdrawn. Please try again.",
    version = "Version {version}",
    newVersion = "New version {version}",
    previousVersion = "Previous consent: version {version}",
)

val KoConsentStrings = ConsentStrings(
    title = "개인정보 처리 안내",
    label = "교회 활동을 위한 개인정보 처리에 동의합니다",
    detail = "교회 소속·세례 등 종교 관련 정보가 포함됩니다. 목적과 범위는 개인정보 처리방침에서 확인해 주세요.",
    policy = "개인정보 처리방침 보기",
    withdrawalHint = "동의는 철회할 수 있습니다. 자세한 내용과 연락처는 처리방침을 확인해 주세요.",
    missingTitle = "개인정보 처리 내용을 확인해 주세요",
    missingBody = "이 계정에 동의 기록이 없습니다. 처리 목적과 범위를 먼저 확인해 주세요.",
    outdatedTitle = "개인정보 처리 안내가 변경되었습니다",
    outdatedBody = "최신 안내를 읽고 다시 선택해 주세요. 이전 선택은 해제되었습니다.",
    loading = "안내를 불러오는 중…",
    loadFailed = "안내를 불러오지 못했습니다. 인터넷 연결을 확인하고 다시 시도해 주세요.",
    saveFailed = "동의를 저장하지 못했습니다. 아직 저장되지 않았습니다. 다시 시도해 주세요.",
    unavailable = "개인정보 처리방침이 아직 준비되지 않았습니다. 교회 담당자에게 문의해 주세요.",
    linkFailed = "링크를 열지 못했습니다. 다시 시도하거나 교회 담당자에게 문의해 주세요.",
    retry = "다시 시도하기",
    continueLabel = "동의하고 계속하기",
    saving = "저장 중…",
    granted = "개인정보 처리 동의",
    record = "동의일 {date} · 버전 {version}",
    contact = "개인정보 문의",
    controller = "한마음교회",
    withdraw = "동의 철회",
    withdrawTitle = "개인정보 처리 동의를 철회할까요?",
    withdrawBody = "로그인은 유지되지만 다시 동의할 때까지 앱을 사용할 수 없습니다. 철회 전 처리에는 영향이 없으며 계정이 삭제되지는 않습니다.",
    withdrawFailed = "동의를 철회하지 못했습니다. 다시 시도해 주세요.",
    version = "버전 {version}",
    newVersion = "새 버전 {version}",
    previousVersion = "이전 동의: 버전 {version}",
)
