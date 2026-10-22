package com.hanmaum.dn.mobile.core.i18n

/** Language-neutral UI state. Resolve at render time so an existing error also changes language. */
enum class UiMessage(internal val english: String, internal val korean: String) {
    BIOMETRICS_CHANGED("Biometric settings changed. Please set up biometric sign-in again.", "생체 인증이 변경되어 다시 설정해야 합니다."),
    BIOMETRIC_FAILED("Biometric authentication failed. Please sign in with your account.", "생체 인증에 실패했습니다. 계정으로 로그인해 주세요."),
    AUTHENTICATING("Authenticating. Please wait…", "인증하는 중입니다. 잠시만 기다려주세요."),
    SIGN_IN_AGAIN("Please sign in again.", "다시 로그인해주세요."),
    CHECKING_PROFILE("Checking your account…", "사용자 정보를 확인하고 있어요."),
    CHECK_CONNECTION("Check your connection and try again.", "연결 상태를 확인하고 다시 시도해 주세요."),
    LOADING_PROFILE("Checking your account. Please wait…", "사용자 정보를 확인 중입니다. 잠시만 기다려주세요."),
    AUTHENTICATED("Signed in successfully", "인증 완료!"),
    PROFILE_SIGN_IN_FAILED("Couldn't load your account. Please sign in again.", "사용자 정보를 가져오지 못했습니다. 로그인을 다시 시도해주세요."),
    SIGN_IN_FAILED("Sign-in failed.", "로그인 실패하였습니다."),
    SIGN_IN_BEFORE_SETUP("Please sign in again before setting this up.", "다시 로그인한 뒤 설정해주세요."),
    PROFILE_CHECK_FAILED("Couldn't verify your account. Please try again.", "사용자 정보를 확인하지 못했습니다. 다시 시도해주세요."),
    BIOMETRICS_NOT_ENROLLED("No biometrics are enrolled on this device.", "기기에 등록된 생체 인증이 없습니다."),
    BIOMETRICS_DENIED("Allow Face ID for this app in your device settings.", "iPhone 설정에서 이 앱의 Face ID 사용을 허용해주세요."),
    BIOMETRICS_UNAVAILABLE("Biometric authentication is unavailable right now. Please try again later.", "지금은 생체 인증을 사용할 수 없습니다. 잠시 후 다시 시도해주세요."),
    BIOMETRIC_SETUP_FAILED("Couldn't set up biometric sign-in. Please try again.", "설정에 실패했습니다. 다시 시도해주세요."),
    CHECKING_STATUS("Checking your account status…", "계정 상태 확인중입니다."),
    STILL_PENDING("Your account is still awaiting approval. Please wait.", "아직 준비중입니다. 기다려주세요."),
    STATUS_CHECK_FAILED("Couldn't verify your account. Please sign in again.", "인증을 실패하였습니다. 로그인 다시 시도해주세요."),
    CHECK_IN_CLOSED("Check-in is not open right now.", "출석 시간이 아닙니다"),
    CHECK_IN_FAILED("Couldn't check you in. Please try again.", "출석 처리에 실패했습니다"),
    SAVE_PROFILE_FAILED("Couldn't save your profile. Please try again.", "저장 실패"),
    RSVP_WINDOW_CLOSED("The response window has closed.", "응답 기간이 지났습니다"),
    RSVP_SAVE_FAILED("Couldn't save your response. Please try again.", "응답을 저장하지 못했습니다"),
    RSVP_LOAD_FAILED("Couldn't load events. Please try again.", "행사 목록을 불러오지 못했습니다"),
}

fun AppStrings.message(message: UiMessage): String =
    if (languageTag == "ko") message.korean else message.english
