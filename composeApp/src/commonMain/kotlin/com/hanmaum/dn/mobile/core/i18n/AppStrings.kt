package com.hanmaum.dn.mobile.core.i18n

interface AppStrings {
    // Shared actions
    val retry: String
    val back: String
    val save: String
    val cancel: String
    val confirm: String
    val saving: String
    val errorOccurred: String
    val settingsGroupDisplay: String
    val settingsGroupSignIn: String
    val settingsGroupPrivacy: String
    val settingsGroupNotifications: String
    val settingsPushDesc: String
    val settingsThemeSubtitle: String
    val settingsLocationDenied: String
    val errorTitle: String
    val errorBody: String
    val errorRetry: String
    val errorGoHome: String
    // Register form validation
    val errorRequired: String
    val errorInvalidEmail: String
    val errorPasswordRequirements: String
    val errorDateIncomplete: String
    val errorDateInvalid: String
    val errorInvalidPostcode: String
    val errorInvalidCity: String
    val errorInvalidHouseNumber: String
    val registerFailed: String
    val registerSuccessLogin: String
    // Attendance history list (#110)
    val attendancePresent: String
    val attendanceAbsent: String
    val attendanceNoRecords: String
    // Attendance history calendar (#164)
    val attendanceHistoryTitle: String
    val attendanceViewAll: String
    val attendanceDayServices: String
    val attendanceCount: String
    val attendanceNoneThisDay: String
    val attendanceLoadFailed: String
    val attendanceRetry: String
    val attendanceCheckedInAt: String
    // Register form — chrome, field labels and the password checklist
    val registerTitle: String
    val registerHeadline: String
    val registerSubtitle: String
    val registerRequiredLegend: String
    val registerMissingRequired: String
    val registerSubmit: String
    val registerSubmitting: String
    val fieldLastName: String
    val fieldFirstName: String
    val fieldEmail: String
    val fieldPhone: String
    val fieldPassword: String
    val fieldBirthDate: String
    val fieldStreet: String
    val fieldHouseNumber: String
    val fieldZipCode: String
    val fieldCity: String
    val passwordRuleLength: String
    val passwordRuleCase: String
    val passwordRuleDigit: String
    val passwordRuleSpecial: String
    val passwordRuleNotEmail: String
    // Notification permission dialog (HomeScreen)
    val laterButton: String
    val allowPermission: String
    // Bottom navigation labels
    val navHome: String
    val navNews: String
    val navCalendar: String
    val navAlbum: String
    val navProfile: String
    // Attendance
    val navAttendance: String
    // Notifications
    val notifications: String
    val notificationsTitle: String
    val notificationsEmpty: String
    val notificationsReadAll: String
    val notificationsDeleteAll: String
    val notificationsDelete: String
    val moreOptions: String
    val notificationsToday: String
    val notificationsYesterday: String
    val notificationsEarlier: String
    val notificationsError: String
    val notificationTimeJustNow: String
    fun notificationTimeMinutesAgo(minutes: Int): String
    fun notificationTimeHoursAgo(hours: Int): String
    fun notificationTimeDaysAgo(days: Int): String
    val pushPrimingTitle: String
    val pushPrimingBody: String
    val pushPrimingEnable: String
    val settingsPushToggle: String
    val settingsPushPermissionHint: String
    // Profile screen
    val profileLogout: String
    val profileTimeTogether: String

    /**
     * The "함께한 시간" tile value. Takes years and months rather than a
     * pre-formatted string because the three languages break differently:
     * Korean concatenates units, German abbreviates them.
     */
    fun profileTimeTogetherValue(years: Int, months: Int): String
    val profileLanguage: String
    val selectLanguage: String
    val profileTheme: String
    val selectTheme: String
    val themeSystem: String
    val themeLight: String
    val themeDark: String
    val profileAppLock: String
    val appLockUnavailable: String
    val lockTitle: String
    val lockSubtitle: String
    val lockUnlockButton: String
    val lockUsePassword: String
    // Face ID sign-in
    val profileFaceIdLogin: String
    val faceIdLoginDesc: String
    val loginForgotPassword: String
    val loginUseFaceId: String
    val loginSignInWithFaceId: String
    // Keep me signed in
    val profileKeepSignedIn: String
    val keepSignedInDesc: String
    // Location sharing
    val profileLocationSharing: String
    val locationSharingDesc: String
    // Announcement sections
    val sectionThisMonth: String
    val sectionLastMonth: String
    val sectionEarlier: String
    // Announcement no-longer-available (stale notification deep link)
    val announcementUnavailableTitle: String
    val announcementUnavailableBody: String
    val announcementUnavailableAction: String
    // Info card labels
    val labelEmail: String
    val labelPhone: String
    val labelStreet: String
    val labelHouseNumber: String
    val labelZipCode: String
    val labelCity: String
    val profileImageUrl: String
    // Ministry
    val ministryListTitle: String
    val ministryListSubtitle: String
    val ministryListEmpty: String
    val newsFeatured: String
    val newsReadFull: String
    val newsDetails: String
    val newsEmpty: String
    val churchName: String
    val share: String
    /** Photo count on an album tile; {n} is replaced with the number. */
    val albumPhotoCount: String
    val attendanceServiceDayOnly: String
    /** Check-in window; {start} and {end} are replaced with times. */
    val attendanceWindow: String
    val attendanceCheckIn: String
    val attendanceNotInWindow: String
    val ministryAbout: String
    val ministryRequirements: String
    val ministrySchedule: String
    val ministryContact: String
    // Pending screen
    val checkStatus: String
    // Album
    val albumEmpty: String
    val albumsEmpty: String
    // Community
    val comingSoon: String
    // Floor plan list label
    val list: String
    // Calendar — 1-indexed (index 0 is unused empty string)
    val months: List<String>
    val yearSuffix: String
    // Pending screen
    val pendingTitle: String
    val rejectedTitle: String
    val rejectedBody: String
    val rejectedContactLabel: String
    val pendingBody: String
    // Calendar navigation & labels
    val calendarPrevMonth: String
    val calendarNextMonth: String
    val calendarEventsThisMonth: String
    val calendarNoEvents: String
    val calendarNoEventsThisDay: String
    val calendarAllDay: String
    val dayHeaders: List<String>
    // Attendance
    val attendanceNoService: String
    val attendanceCheckedIn: String
    val attendanceReady: String
    val attendanceLocation: String
    // Floor plan
    val floorPlanTitle: String
    // Event RSVP
    val rsvpSheetTitle: String
    val rsvpMultiHeader: String
    val rsvpAttend: String
    val rsvpAttendShort: String
    val rsvpLater: String
    val rsvpDone: String
    val rsvpAnnouncementCta: String
    // Settings & personal info screens
    val settingsTitle: String
    val personalInfoTitle: String
    val labelBirthDate: String
    val labelDivision: String
    val labelGroup: String
    val labelName: String
    val labelChurchRole: String
    val lockedFieldHint: String
    val profileSaved: String
    // Date picker (BirthdayField)
    val selectDate: String
}

object EnStrings : AppStrings {
    override val retry = "Retry"
    override val back = "Back"
    override val save = "Save"
    override val cancel = "Cancel"
    override val confirm = "Confirm"
    override val saving = "Saving…"
    override val errorOccurred = "An error occurred"
    override val settingsGroupDisplay = "Display"
    override val settingsGroupSignIn = "Sign-in"
    override val settingsGroupPrivacy = "Privacy"
    override val settingsGroupNotifications = "Notifications"
    override val settingsPushDesc = "Announcements, events and attendance reminders"
    override val settingsThemeSubtitle = "Choose how the app looks"
    override val settingsLocationDenied = "Allow location access in system settings"
    override val errorTitle = "Something went wrong"
    override val errorBody = "The page could not be loaded. Check your connection and try again."
    override val errorRetry = "Try again"
    override val errorGoHome = "Go to home"
    override val errorRequired = "This field is required"
    override val errorInvalidEmail = "Please enter a valid email address"
    override val errorPasswordRequirements = "Password doesn't meet the requirements"
    override val errorDateIncomplete = "Please enter the full date (YYYY.MM.DD)"
    override val errorDateInvalid = "Invalid date"
    override val errorInvalidPostcode = "Enter a valid postcode (5 digits)"
    override val errorInvalidCity = "Enter a valid city"
    override val errorInvalidHouseNumber = "Enter a valid house number (e.g. 5, 12a)"
    override val registerFailed = "Registration failed. Please try again."
    override val registerSuccessLogin = "Registration successful. Please log in."
    // Attendance history list (#110)
    override val attendancePresent = "Attended"
    override val attendanceAbsent = "Missed"
    override val attendanceNoRecords = "No attendance recorded yet"
    // Attendance history calendar (#164)
    override val attendanceHistoryTitle = "Attendance record"
    override val attendanceViewAll = "View all"
    override val attendanceDayServices = "Services on {month} {day}"
    override val attendanceCount = "{n}"
    override val attendanceNoneThisDay = "No attendance on this day"
    override val attendanceLoadFailed = "Could not load your attendance record"
    override val attendanceRetry = "Retry"
    override val attendanceCheckedInAt = "Attended at {time}"
    // Register form — chrome, field labels and the password checklist
    override val registerTitle = "Sign up"
    override val registerHeadline = "Let's begin together"
    override val registerSubtitle = "After you apply, a leader reviews your request before you can use the app."
    override val registerRequiredLegend = "* marks a required field"
    override val registerMissingRequired = "Please fill in every required field"
    override val registerSubmit = "Apply"
    override val registerSubmitting = "Submitting…"
    override val fieldLastName = "Last name"
    override val fieldFirstName = "First name"
    override val fieldEmail = "Email"
    override val fieldPhone = "Phone"
    override val fieldPassword = "Password"
    override val fieldBirthDate = "Date of birth"
    override val fieldStreet = "Street"
    override val fieldHouseNumber = "No."
    override val fieldZipCode = "Postcode"
    override val fieldCity = "City"
    override val passwordRuleLength = "At least 8 characters"
    override val passwordRuleCase = "Upper and lower case"
    override val passwordRuleDigit = "A digit"
    override val passwordRuleSpecial = "A special character"
    override val passwordRuleNotEmail = "Different from your email"
    override val laterButton = "Later"
    override val allowPermission = "Allow"
    override val navHome = "Home"
    override val navNews = "News"
    override val navCalendar = "Calendar"
    override val navAlbum = "Album"
    override val navProfile = "Profile"
    override val navAttendance = "Attendance"
    override val notifications = "Notifications"
    override val notificationsTitle = "Notifications"
    override val notificationsEmpty = "You'll see notifications here when they arrive"
    override val notificationsReadAll = "Mark all read"
    override val notificationsDeleteAll = "Delete all"
    override val notificationsDelete = "Delete"
    override val moreOptions = "More options"
    override val notificationsToday = "Today"
    override val notificationsYesterday = "Yesterday"
    override val notificationsEarlier = "Earlier"
    override val notificationsError = "Couldn't load notifications"
    override val notificationTimeJustNow = "just now"
    override fun notificationTimeMinutesAgo(minutes: Int) = "${minutes}m ago"
    override fun notificationTimeHoursAgo(hours: Int) = "${hours}h ago"
    override fun notificationTimeDaysAgo(days: Int) = "${days}d ago"
    override val pushPrimingTitle = "Don't miss new announcements"
    override val pushPrimingBody = "We'll notify you when something new is posted"
    override val pushPrimingEnable = "Turn on notifications"
    override val settingsPushToggle = "Push notifications"
    override val settingsPushPermissionHint = "Allow notifications in system settings"
    override val profileLogout = "Logout"
    override val profileTimeTogether = "Time together"
    override fun profileTimeTogetherValue(years: Int, months: Int) = when {
        years > 0 && months > 0 -> "${years}y ${months}m"
        years > 0 -> "${years}y"
        months > 0 -> "${months}m"
        else -> "New"
    }
    override val profileLanguage = "LANGUAGE"
    override val selectLanguage = "Select Language"
    override val profileTheme = "THEME"
    override val selectTheme = "Select Theme"
    override val themeSystem = "System"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val profileAppLock = "App Lock (Face ID / Touch ID)"
    override val appLockUnavailable = "No biometrics enrolled on this device"
    override val lockTitle = "Locked"
    override val lockSubtitle = "Unlock DN App to continue"
    override val lockUnlockButton = "Unlock"
    override val lockUsePassword = "Use password instead"
    override val profileFaceIdLogin = "Face ID Sign-In"
    override val faceIdLoginDesc = "Use Face ID / Touch ID to sign in automatically"
    override val loginForgotPassword = "Forgot?"
    override val loginUseFaceId = "Enable Face ID sign-in"
    override val loginSignInWithFaceId = "Sign in with Face ID"
    override val profileKeepSignedIn = "Keep me signed in"
    override val keepSignedInDesc = "Stay signed in between app launches"
    override val profileLocationSharing = "Location Sharing"
    override val locationSharingDesc = "Allow attendance check-in near the church"
    override val sectionThisMonth = "THIS MONTH"
    override val sectionLastMonth = "LAST MONTH"
    override val sectionEarlier = "EARLIER"
    override val announcementUnavailableTitle = "This announcement is no longer available"
    override val announcementUnavailableBody = "It may have expired or been removed."
    override val announcementUnavailableAction = "See all announcements"
    override val labelEmail = "EMAIL ADDRESS"
    override val labelPhone = "PHONE NUMBER"
    override val labelStreet = "STREET"
    override val labelHouseNumber = "HOUSE NO."
    override val labelZipCode = "ZIP CODE"
    override val labelCity = "CITY"
    override val profileImageUrl = "PROFILE IMAGE URL"
    override val ministryListTitle = "Ministries"
    override val ministryListSubtitle = "Discover the ministries serving our church"
    override val ministryListEmpty = "No ministries yet"
    override val newsFeatured = "FEATURED"
    override val newsReadFull = "READ FULL STORY →"
    override val newsDetails = "DETAILS →"
    override val newsEmpty = "No news yet"
    override val churchName = "한마음 교회"
    override val share = "Share"
    override val albumPhotoCount = "{n} photos"
    override val attendanceServiceDayOnly = "Check-in is only available on a service day"
    override val attendanceWindow = "Check-in window: {start} – {end}"
    override val attendanceCheckIn = "Check in"
    override val attendanceNotInWindow = "Outside check-in hours"
    override val ministryAbout = "About"
    override val ministryRequirements = "Requirements"
    override val ministrySchedule = "Schedule"
    override val ministryContact = "Contact"
    override val checkStatus = "Check Status"
    override val albumEmpty = "No photos yet"
    override val albumsEmpty = "No albums"
    override val comingSoon = "Coming soon"
    override val list = "List"
    override val months = listOf("", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December")
    override val yearSuffix = ""
    override val pendingTitle = "Pending Approval"
    override val rejectedTitle = "Registration Not Approved"
    override val rejectedBody = "You are not registered as part of the Hanmaum D+N congregation.\nPlease contact us at the address below."
    override val rejectedContactLabel = "Contact"
    override val pendingBody = "Your registration has been submitted. An admin will review your application soon."
    override val calendarPrevMonth = "Previous month"
    override val calendarNextMonth = "Next month"
    override val calendarEventsThisMonth = "Events this month"
    override val calendarNoEvents = "No events"
    override val calendarNoEventsThisDay = "No events on this day"
    override val calendarAllDay = "All day"
    override val dayHeaders = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    override val attendanceNoService = "No service today"
    override val attendanceCheckedIn = "Checked in!"
    override val attendanceReady = "Ready to check in"
    override val attendanceLocation = "Location"
    override val floorPlanTitle = "Church Map"
    override val rsvpSheetTitle = "Event RSVP"
    override val rsvpMultiHeader = "Events to attend"
    override val rsvpAttend = "Attend"
    override val rsvpAttendShort = "Attend"
    override val rsvpLater = "Later"
    override val rsvpDone = "Attending"
    override val rsvpAnnouncementCta = "RSVP to this event"
    override val settingsTitle = "Settings"
    override val personalInfoTitle = "Personal Info"
    override val labelBirthDate = "BIRTH DATE"
    override val labelDivision = "DIVISION"
    override val labelGroup = "GROUP"
    override val labelName = "NAME"
    override val labelChurchRole = "CHURCH ROLE"
    override val lockedFieldHint = "Managed by the church office"
    override val profileSaved = "Saved"
    override val selectDate = "Select date"
}

object KoStrings : AppStrings {
    override val retry = "다시 시도"
    override val back = "뒤로"
    override val save = "저장"
    override val cancel = "취소"
    override val confirm = "확인"
    override val saving = "저장 중"
    override val errorOccurred = "오류가 발생했습니다"
    override val settingsGroupDisplay = "표시"
    override val settingsGroupSignIn = "로그인"
    override val settingsGroupPrivacy = "개인정보"
    override val settingsGroupNotifications = "알림"
    override val settingsPushDesc = "공지, 행사, 출석 알림을 받습니다"
    override val settingsThemeSubtitle = "앱의 화면 모드를 선택하세요"
    override val settingsLocationDenied = "시스템 설정에서 위치 접근을 허용해 주세요"
    override val errorTitle = "문제가 발생했습니다"
    override val errorBody = "페이지를 불러오지 못했습니다. 연결을 확인한 뒤 다시 시도해 주세요."
    override val errorRetry = "다시 시도"
    override val errorGoHome = "홈으로"
    override val errorRequired = "필수 항목입니다"
    override val errorInvalidEmail = "올바른 이메일을 입력해주세요"
    override val errorPasswordRequirements = "비밀번호 조건을 충족하지 않습니다"
    override val errorDateIncomplete = "날짜를 완전히 입력해주세요 (YYYY.MM.DD)"
    override val errorDateInvalid = "유효하지 않은 날짜입니다"
    override val errorInvalidPostcode = "올바른 우편번호를 입력해주세요 (5자리)"
    override val errorInvalidCity = "올바른 도시명을 입력해주세요"
    override val errorInvalidHouseNumber = "올바른 번지수를 입력해주세요 (예: 5, 12a)"
    override val registerFailed = "회원가입에 실패했습니다. 다시 시도해주세요."
    override val registerSuccessLogin = "등록 성공했습니다. 로그인 해주세요."
    // Attendance history list (#110)
    override val attendancePresent = "출석"
    override val attendanceAbsent = "미출석"
    override val attendanceNoRecords = "아직 출석 기록이 없습니다"
    // Attendance history calendar (#164)
    override val attendanceHistoryTitle = "출석 확인"
    override val attendanceViewAll = "전체 보기"
    override val attendanceDayServices = "{month} {day}일 예배"
    override val attendanceCount = "{n}건"
    override val attendanceNoneThisDay = "이 날은 출석 기록이 없습니다"
    override val attendanceLoadFailed = "출석 기록을 불러오지 못했습니다"
    override val attendanceRetry = "다시 시도"
    override val attendanceCheckedInAt = "{time} 출석"
    // Register form — chrome, field labels and the password checklist
    override val registerTitle = "회원가입"
    override val registerHeadline = "함께 시작해요"
    override val registerSubtitle = "가입 신청 후 담당자의 승인을 거쳐 이용하실 수 있습니다."
    override val registerRequiredLegend = "* 표시는 필수 항목입니다"
    override val registerMissingRequired = "필수 항목을 모두 입력해 주세요"
    override val registerSubmit = "가입 신청하기"
    override val registerSubmitting = "신청 중…"
    override val fieldLastName = "성"
    override val fieldFirstName = "이름"
    override val fieldEmail = "이메일"
    override val fieldPhone = "전화번호"
    override val fieldPassword = "비밀번호"
    override val fieldBirthDate = "생년월일"
    override val fieldStreet = "도로명"
    override val fieldHouseNumber = "번지"
    override val fieldZipCode = "우편번호"
    override val fieldCity = "도시"
    override val passwordRuleLength = "8자 이상"
    override val passwordRuleCase = "대문자와 소문자 포함"
    override val passwordRuleDigit = "숫자 포함"
    override val passwordRuleSpecial = "특수문자 포함"
    override val passwordRuleNotEmail = "이메일 주소와 다름"
    override val laterButton = "나중에"
    override val allowPermission = "권한 허용"
    override val navHome = "홈"
    override val navNews = "소식"
    override val navCalendar = "캘린더"
    override val navAlbum = "앨범"
    override val navProfile = "프로필"
    override val navAttendance = "출석 체크"
    override val notifications = "알림"
    override val notificationsTitle = "알림"
    override val notificationsEmpty = "알림이 오면 여기에 표시됩니다"
    override val notificationsReadAll = "모두 읽음"
    override val notificationsDeleteAll = "모두 삭제"
    override val notificationsDelete = "삭제"
    override val moreOptions = "더 보기"
    override val notificationsToday = "오늘"
    override val notificationsYesterday = "어제"
    override val notificationsEarlier = "이전"
    override val notificationsError = "알림을 불러오지 못했습니다"
    override val notificationTimeJustNow = "방금 전"
    override fun notificationTimeMinutesAgo(minutes: Int) = "${minutes}분 전"
    override fun notificationTimeHoursAgo(hours: Int) = "${hours}시간 전"
    override fun notificationTimeDaysAgo(days: Int) = "${days}일 전"
    override val pushPrimingTitle = "새 소식을 놓치지 마세요"
    override val pushPrimingBody = "새로운 공지가 올라오면 알려드릴게요"
    override val pushPrimingEnable = "알림 켜기"
    override val settingsPushToggle = "푸시 알림"
    override val settingsPushPermissionHint = "기기 설정에서 알림을 허용해주세요"
    override val profileLogout = "로그아웃"
    override val profileTimeTogether = "함께한 시간"
    override fun profileTimeTogetherValue(years: Int, months: Int) = when {
        years > 0 && months > 0 -> "${years}년 ${months}개월"
        years > 0 -> "${years}년"
        months > 0 -> "${months}개월"
        else -> "새 가족"
    }
    override val profileLanguage = "언어"
    override val selectLanguage = "언어 선택"
    override val profileTheme = "테마"
    override val selectTheme = "테마 선택"
    override val themeSystem = "시스템"
    override val themeLight = "라이트"
    override val themeDark = "다크"
    override val profileAppLock = "앱 잠금 (Face ID / Touch ID)"
    override val appLockUnavailable = "기기에 등록된 생체 인증이 없습니다"
    override val lockTitle = "잠금"
    override val lockSubtitle = "계속하려면 DN 앱 잠금을 해제하세요"
    override val lockUnlockButton = "잠금 해제"
    override val lockUsePassword = "비밀번호로 로그인"
    override val profileFaceIdLogin = "Face ID 로그인"
    override val faceIdLoginDesc = "Face ID / Touch ID로 자동 로그인합니다"
    override val loginForgotPassword = "비밀번호 찾기"
    override val loginUseFaceId = "다음에 Face ID로 로그인"
    override val loginSignInWithFaceId = "Face ID로 로그인"
    override val profileKeepSignedIn = "로그인 상태 유지"
    override val keepSignedInDesc = "앱을 다시 열어도 로그인 상태를 유지합니다"
    override val profileLocationSharing = "위치 공유"
    override val locationSharingDesc = "교회 근처에서 출석 체크를 허용합니다"
    override val sectionThisMonth = "이번 달"
    override val sectionLastMonth = "지난 달"
    override val sectionEarlier = "이전"
    override val announcementUnavailableTitle = "이 소식은 더 이상 볼 수 없어요"
    override val announcementUnavailableBody = "게시 기간이 종료되었거나 삭제되었어요."
    override val announcementUnavailableAction = "소식 전체 보기"
    override val labelEmail = "이메일"
    override val labelPhone = "전화번호"
    override val labelStreet = "주소"
    override val labelHouseNumber = "번지"
    override val labelZipCode = "우편번호"
    override val labelCity = "도시"
    override val profileImageUrl = "프로필 사진 URL"
    override val ministryListTitle = "사역"
    override val ministryListSubtitle = "교회의 다양한 사역을 만나보세요"
    override val ministryListEmpty = "등록된 사역이 없습니다"
    override val newsFeatured = "주요 소식"
    override val newsReadFull = "전체 보기 →"
    override val newsDetails = "자세히 보기 →"
    override val newsEmpty = "소식이 없습니다"
    override val churchName = "한마음 교회"
    override val share = "공유"
    override val albumPhotoCount = "{n}장"
    override val attendanceServiceDayOnly = "출석 체크는 예배 당일에만 가능합니다"
    override val attendanceWindow = "출석 시간: {start} ~ {end}"
    override val attendanceCheckIn = "출석 체크"
    override val attendanceNotInWindow = "출석 시간이 아닙니다"
    override val ministryAbout = "소개"
    override val ministryRequirements = "지원 자격"
    override val ministrySchedule = "일정"
    override val ministryContact = "문의"
    override val checkStatus = "승인 상태 확인"
    override val albumEmpty = "아직 사진이 없습니다"
    override val albumsEmpty = "앨범이 없습니다"
    override val comingSoon = "준비 중입니다"
    override val list = "목록"
    override val months = listOf("", "1월", "2월", "3월", "4월", "5월", "6월",
        "7월", "8월", "9월", "10월", "11월", "12월")
    override val yearSuffix = "년"
    override val pendingTitle = "가입 대기 중"
    override val rejectedTitle = "가입이 승인되지 않았습니다"
    override val rejectedBody = "한마음 D+N 소속으로 등록되지 않았습니다.\n문의는 아래 주소로 부탁드립니다."
    override val rejectedContactLabel = "문의"
    override val pendingBody = "가입 신청이 완료되었습니다.\n관리자가 곧 신청을 검토할 예정입니다."
    override val calendarPrevMonth = "이전 달"
    override val calendarNextMonth = "다음 달"
    override val calendarEventsThisMonth = "이번 달 행사"
    override val calendarNoEvents = "이벤트 없음"
    override val calendarNoEventsThisDay = "이 날은 행사가 없습니다"
    override val calendarAllDay = "하루 종일"
    override val dayHeaders = listOf("일", "월", "화", "수", "목", "금", "토")
    override val attendanceNoService = "오늘은 예배가 없습니다"
    override val attendanceCheckedIn = "출석 완료!"
    override val attendanceReady = "출석 가능"
    override val attendanceLocation = "위치"
    override val floorPlanTitle = "교회 지도"
    override val rsvpSheetTitle = "행사 참석"
    override val rsvpMultiHeader = "참석할 행사"
    override val rsvpAttend = "참석하기"
    override val rsvpAttendShort = "참석"
    override val rsvpLater = "나중에"
    override val rsvpDone = "참석 완료"
    override val rsvpAnnouncementCta = "행사 참석하기"
    override val settingsTitle = "설정"
    override val personalInfoTitle = "개인 정보"
    override val labelBirthDate = "생년월일"
    override val labelDivision = "교구"
    override val labelGroup = "목장"
    override val labelName = "이름"
    override val labelChurchRole = "직분"
    override val lockedFieldHint = "교회에서 관리하는 정보입니다"
    override val profileSaved = "저장되었습니다"
    override val selectDate = "날짜 선택"
}

object DeStrings : AppStrings {
    override val retry = "Erneut versuchen"
    override val back = "Zurück"
    override val save = "Speichern"
    override val cancel = "Abbrechen"
    override val confirm = "Bestätigen"
    override val saving = "Wird gespeichert…"
    override val errorOccurred = "Ein Fehler ist aufgetreten"
    override val settingsGroupDisplay = "Darstellung"
    override val settingsGroupSignIn = "Anmeldung"
    override val settingsGroupPrivacy = "Datenschutz"
    override val settingsGroupNotifications = "Benachrichtigungen"
    override val settingsPushDesc = "Ankündigungen, Termine und Anwesenheits-Erinnerungen"
    override val settingsThemeSubtitle = "Wähle das Erscheinungsbild der App"
    override val settingsLocationDenied = "Standortzugriff in den Systemeinstellungen erlauben"
    override val errorTitle = "Etwas ist schiefgelaufen"
    override val errorBody = "Die Seite konnte nicht geladen werden. Prüfe die Verbindung und versuche es erneut."
    override val errorRetry = "Erneut versuchen"
    override val errorGoHome = "Zur Startseite"
    override val errorRequired = "Dieses Feld ist erforderlich"
    override val errorInvalidEmail = "Bitte geben Sie eine gültige E-Mail-Adresse ein"
    override val errorPasswordRequirements = "Passwort erfüllt die Anforderungen nicht"
    override val errorDateIncomplete = "Bitte geben Sie das vollständige Datum ein (YYYY.MM.DD)"
    override val errorDateInvalid = "Ungültiges Datum"
    override val errorInvalidPostcode = "Bitte gültige PLZ eingeben (5 Ziffern)"
    override val errorInvalidCity = "Bitte gültige Stadt eingeben"
    override val errorInvalidHouseNumber = "Bitte gültige Hausnummer angeben (z. B. 5, 12a)"
    override val registerFailed = "Registrierung fehlgeschlagen. Bitte versuchen Sie es erneut."
    override val registerSuccessLogin = "Registrierung erfolgreich. Bitte melden Sie sich an."
    // Attendance history list (#110)
    override val attendancePresent = "Anwesend"
    override val attendanceAbsent = "Gefehlt"
    override val attendanceNoRecords = "Noch keine Anwesenheit erfasst"
    // Attendance history calendar (#164)
    override val attendanceHistoryTitle = "Anwesenheit"
    override val attendanceViewAll = "Alle anzeigen"
    override val attendanceDayServices = "Gottesdienste am {day}. {month}"
    override val attendanceCount = "{n}"
    override val attendanceNoneThisDay = "An diesem Tag keine Anwesenheit"
    override val attendanceLoadFailed = "Anwesenheit konnte nicht geladen werden"
    override val attendanceRetry = "Erneut versuchen"
    override val attendanceCheckedInAt = "Anwesend um {time}"
    // Register form — chrome, field labels and the password checklist
    override val registerTitle = "Registrieren"
    override val registerHeadline = "Schön, dass du da bist"
    override val registerSubtitle = "Nach der Anmeldung prüft ein Verantwortlicher deine Anfrage, bevor du die App nutzen kannst."
    override val registerRequiredLegend = "* kennzeichnet ein Pflichtfeld"
    override val registerMissingRequired = "Bitte fülle alle Pflichtfelder aus"
    override val registerSubmit = "Anmeldung absenden"
    override val registerSubmitting = "Wird gesendet…"
    override val fieldLastName = "Nachname"
    override val fieldFirstName = "Vorname"
    override val fieldEmail = "E-Mail"
    override val fieldPhone = "Telefon"
    override val fieldPassword = "Passwort"
    override val fieldBirthDate = "Geburtsdatum"
    override val fieldStreet = "Straße"
    override val fieldHouseNumber = "Nr."
    override val fieldZipCode = "PLZ"
    override val fieldCity = "Ort"
    override val passwordRuleLength = "Mindestens 8 Zeichen"
    override val passwordRuleCase = "Groß- und Kleinbuchstaben"
    override val passwordRuleDigit = "Eine Ziffer"
    override val passwordRuleSpecial = "Ein Sonderzeichen"
    override val passwordRuleNotEmail = "Nicht die E-Mail-Adresse"
    override val laterButton = "Später"
    override val allowPermission = "Erlauben"
    override val navHome = "Start"
    override val navNews = "Neuigkeiten"
    override val navCalendar = "Kalender"
    override val navAlbum = "Album"
    override val navProfile = "Profil"
    override val navAttendance = "Anwesenheit"
    override val notifications = "Benachrichtigungen"
    override val notificationsTitle = "Mitteilungen"
    override val notificationsEmpty = "Neue Mitteilungen erscheinen hier"
    override val notificationsReadAll = "Alle gelesen"
    override val notificationsDeleteAll = "Alle löschen"
    override val notificationsDelete = "Löschen"
    override val moreOptions = "Weitere Optionen"
    override val notificationsToday = "Heute"
    override val notificationsYesterday = "Gestern"
    override val notificationsEarlier = "Früher"
    override val notificationsError = "Mitteilungen konnten nicht geladen werden"
    override val notificationTimeJustNow = "gerade eben"
    override fun notificationTimeMinutesAgo(minutes: Int) = "vor ${minutes} Min."
    override fun notificationTimeHoursAgo(hours: Int) = "vor ${hours} Std."
    override fun notificationTimeDaysAgo(days: Int) = "vor ${days} Tagen"
    override val pushPrimingTitle = "Verpassen Sie keine Neuigkeiten"
    override val pushPrimingBody = "Wir benachrichtigen Sie bei neuen Ankündigungen"
    override val pushPrimingEnable = "Mitteilungen aktivieren"
    override val settingsPushToggle = "Push-Mitteilungen"
    override val settingsPushPermissionHint = "Erlauben Sie Mitteilungen in den Systemeinstellungen"
    override val profileLogout = "Abmelden"
    override val profileTimeTogether = "Gemeinsame Zeit"
    override fun profileTimeTogetherValue(years: Int, months: Int) = when {
        years > 0 && months > 0 -> "${years} J. ${months} Mon."
        years > 0 -> "${years} J."
        months > 0 -> "${months} Mon."
        else -> "Neu"
    }
    override val profileLanguage = "SPRACHE"
    override val selectLanguage = "Sprache auswählen"
    override val profileTheme = "DARSTELLUNG"
    override val selectTheme = "Darstellung wählen"
    override val themeSystem = "System"
    override val themeLight = "Hell"
    override val themeDark = "Dunkel"
    override val profileAppLock = "App-Sperre (Face ID / Touch ID)"
    override val appLockUnavailable = "Keine Biometrie auf diesem Gerät eingerichtet"
    override val lockTitle = "Gesperrt"
    override val lockSubtitle = "Entsperre die DN App, um fortzufahren"
    override val lockUnlockButton = "Entsperren"
    override val lockUsePassword = "Stattdessen Passwort verwenden"
    override val profileFaceIdLogin = "Face-ID-Anmeldung"
    override val faceIdLoginDesc = "Mit Face ID / Touch ID automatisch anmelden"
    override val loginForgotPassword = "Vergessen?"
    override val loginUseFaceId = "Face-ID-Anmeldung aktivieren"
    override val loginSignInWithFaceId = "Mit Face ID anmelden"
    override val profileKeepSignedIn = "Angemeldet bleiben"
    override val keepSignedInDesc = "Zwischen App-Starts angemeldet bleiben"
    override val profileLocationSharing = "Standortfreigabe"
    override val locationSharingDesc = "Anwesenheits-Check-in in Kirchennähe erlauben"
    override val sectionThisMonth = "DIESEN MONAT"
    override val sectionLastMonth = "LETZTEN MONAT"
    override val sectionEarlier = "FRÜHER"
    override val announcementUnavailableTitle = "Diese Mitteilung ist nicht mehr verfügbar"
    override val announcementUnavailableBody = "Sie ist möglicherweise abgelaufen oder wurde entfernt."
    override val announcementUnavailableAction = "Alle Mitteilungen ansehen"
    override val labelEmail = "E-MAIL-ADRESSE"
    override val labelPhone = "TELEFONNUMMER"
    override val labelStreet = "STRASSE"
    override val labelHouseNumber = "HAUSNUMMER"
    override val labelZipCode = "POSTLEITZAHL"
    override val labelCity = "STADT"
    override val profileImageUrl = "PROFILBILD-URL"
    override val ministryListTitle = "Dienste"
    override val ministryListSubtitle = "Entdecken Sie die Dienste unserer Gemeinde"
    override val ministryListEmpty = "Noch keine Dienste"
    override val newsFeatured = "HERVORGEHOBEN"
    override val newsReadFull = "GANZE STORY →"
    override val newsDetails = "DETAILS →"
    override val newsEmpty = "Noch keine Neuigkeiten"
    override val churchName = "한마음 교회"
    override val share = "Teilen"
    override val albumPhotoCount = "{n} Fotos"
    override val attendanceServiceDayOnly = "Check-in ist nur an einem Gottesdiensttag möglich"
    override val attendanceWindow = "Check-in-Zeitraum: {start} – {end}"
    override val attendanceCheckIn = "Einchecken"
    override val attendanceNotInWindow = "Außerhalb der Check-in-Zeit"
    override val ministryAbout = "Über uns"
    override val ministryRequirements = "Voraussetzungen"
    override val ministrySchedule = "Zeitplan"
    override val ministryContact = "Kontakt"
    override val checkStatus = "Status prüfen"
    override val albumEmpty = "Noch keine Fotos"
    override val albumsEmpty = "Keine Alben"
    override val comingSoon = "Demnächst verfügbar"
    override val list = "Liste"
    override val months = listOf("", "Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember")
    override val yearSuffix = ""
    override val pendingTitle = "Genehmigung ausstehend"
    override val rejectedTitle = "Anmeldung nicht genehmigt"
    override val rejectedBody = "Sie sind nicht als Mitglied der Gemeinde Hanmaum D+N eingetragen.\nBei Fragen wenden Sie sich bitte an die untenstehende Adresse."
    override val rejectedContactLabel = "Kontakt"
    override val pendingBody = "Ihre Anmeldung wurde eingereicht. Ein Administrator wird Ihre Anfrage in Kürze prüfen."
    override val calendarPrevMonth = "Vorheriger Monat"
    override val calendarNextMonth = "Nächster Monat"
    override val calendarEventsThisMonth = "Veranstaltungen diesen Monat"
    override val calendarNoEvents = "Keine Veranstaltungen"
    override val calendarNoEventsThisDay = "Keine Veranstaltungen an diesem Tag"
    override val calendarAllDay = "Ganztägig"
    override val dayHeaders = listOf("So", "Mo", "Di", "Mi", "Do", "Fr", "Sa")
    override val attendanceNoService = "Heute kein Gottesdienst"
    override val attendanceCheckedIn = "Eingecheckt!"
    override val attendanceReady = "Bereit zum Einchecken"
    override val attendanceLocation = "Standort"
    override val floorPlanTitle = "Kirchenkarte"
    override val rsvpSheetTitle = "Veranstaltung"
    override val rsvpMultiHeader = "Teilnahme"
    override val rsvpAttend = "Teilnehmen"
    override val rsvpAttendShort = "Teilnehmen"
    override val rsvpLater = "Später"
    override val rsvpDone = "Zugesagt"
    override val rsvpAnnouncementCta = "An Veranstaltung teilnehmen"
    override val settingsTitle = "Einstellungen"
    override val personalInfoTitle = "Persönliche Daten"
    override val labelBirthDate = "GEBURTSDATUM"
    override val labelDivision = "BEZIRK"
    override val labelGroup = "GRUPPE"
    override val labelName = "NAME"
    override val labelChurchRole = "KIRCHENROLLE"
    override val lockedFieldHint = "Wird vom Gemeindebüro verwaltet"
    override val profileSaved = "Gespeichert"
    override val selectDate = "Datum wählen"
}
