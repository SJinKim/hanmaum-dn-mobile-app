package com.hanmaum.dn.mobile.core.i18n

interface AppStrings {
    // Shared actions
    val retry: String
    val back: String
    val save: String
    val cancel: String
    val saving: String
    val errorOccurred: String
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
    // Profile screen
    val profileEdit: String
    val profileLogout: String
    val profileLanguage: String
    val profileAccountPreferences: String
    val selectLanguage: String
    // Info card labels
    val labelEmail: String
    val labelPhone: String
    val labelStreet: String
    val labelZipCode: String
    val labelCity: String
    val labelPrimaryGroup: String
    // Ministry
    val registerNow: String
    val registered: String
    val alreadyMember: String
    val bioLabel: String
    val bioPlaceholder: String
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
}

object EnStrings : AppStrings {
    override val retry = "Retry"
    override val back = "Back"
    override val save = "Save"
    override val cancel = "Cancel"
    override val saving = "Saving…"
    override val errorOccurred = "An error occurred"
    override val laterButton = "Later"
    override val allowPermission = "Allow"
    override val navHome = "Home"
    override val navNews = "News"
    override val navCalendar = "Calendar"
    override val navAlbum = "Album"
    override val navProfile = "Profile"
    override val navAttendance = "Attendance"
    override val profileEdit = "Edit Profile"
    override val profileLogout = "Logout"
    override val profileLanguage = "LANGUAGE"
    override val profileAccountPreferences = "Account Preferences"
    override val selectLanguage = "Select Language"
    override val labelEmail = "EMAIL ADDRESS"
    override val labelPhone = "PHONE NUMBER"
    override val labelStreet = "STREET"
    override val labelZipCode = "ZIP CODE"
    override val labelCity = "CITY"
    override val labelPrimaryGroup = "PRIMARY GROUP"
    override val registerNow = "Register Now"
    override val registered = "Applied ✓"
    override val alreadyMember = "Already a member ✓"
    override val bioLabel = "Bio (optional)"
    override val bioPlaceholder = "Introduce yourself to the leader"
    override val checkStatus = "Check Status"
    override val albumEmpty = "No photos yet"
    override val albumsEmpty = "No albums"
    override val comingSoon = "Coming soon"
    override val list = "List"
    override val months = listOf("", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December")
    override val yearSuffix = ""
}

object KoStrings : AppStrings {
    override val retry = "다시 시도"
    override val back = "뒤로"
    override val save = "저장"
    override val cancel = "취소"
    override val saving = "저장 중"
    override val errorOccurred = "오류가 발생했습니다"
    override val laterButton = "나중에"
    override val allowPermission = "권한 허용"
    override val navHome = "홈"
    override val navNews = "소식"
    override val navCalendar = "캘린더"
    override val navAlbum = "앨범"
    override val navProfile = "프로필"
    override val navAttendance = "출석 체크"
    override val profileEdit = "프로필 수정"
    override val profileLogout = "로그아웃"
    override val profileLanguage = "언어"
    override val profileAccountPreferences = "계정 설정"
    override val selectLanguage = "언어 선택"
    override val labelEmail = "이메일"
    override val labelPhone = "전화번호"
    override val labelStreet = "주소"
    override val labelZipCode = "우편번호"
    override val labelCity = "도시"
    override val labelPrimaryGroup = "소속 그룹"
    override val registerNow = "신청하기"
    override val registered = "신청되었습니다 ✓"
    override val alreadyMember = "멤버입니다 ✓"
    override val bioLabel = "자기소개 (선택)"
    override val bioPlaceholder = "리더에게 전달할 자기소개를 입력하세요"
    override val checkStatus = "승인 상태 확인"
    override val albumEmpty = "아직 사진이 없습니다"
    override val albumsEmpty = "앨범이 없습니다"
    override val comingSoon = "준비 중입니다"
    override val list = "목록"
    override val months = listOf("", "1월", "2월", "3월", "4월", "5월", "6월",
        "7월", "8월", "9월", "10월", "11월", "12월")
    override val yearSuffix = "년"
}

object DeStrings : AppStrings {
    override val retry = "Erneut versuchen"
    override val back = "Zurück"
    override val save = "Speichern"
    override val cancel = "Abbrechen"
    override val saving = "Wird gespeichert…"
    override val errorOccurred = "Ein Fehler ist aufgetreten"
    override val laterButton = "Später"
    override val allowPermission = "Erlauben"
    override val navHome = "Start"
    override val navNews = "Neuigkeiten"
    override val navCalendar = "Kalender"
    override val navAlbum = "Album"
    override val navProfile = "Profil"
    override val navAttendance = "Anwesenheit"
    override val profileEdit = "Profil bearbeiten"
    override val profileLogout = "Abmelden"
    override val profileLanguage = "SPRACHE"
    override val profileAccountPreferences = "Kontoeinstellungen"
    override val selectLanguage = "Sprache auswählen"
    override val labelEmail = "E-MAIL-ADRESSE"
    override val labelPhone = "TELEFONNUMMER"
    override val labelStreet = "STRASSE"
    override val labelZipCode = "POSTLEITZAHL"
    override val labelCity = "STADT"
    override val labelPrimaryGroup = "HAUPTGRUPPE"
    override val registerNow = "Jetzt anmelden"
    override val registered = "Angemeldet ✓"
    override val alreadyMember = "Bereits Mitglied ✓"
    override val bioLabel = "Biografie (optional)"
    override val bioPlaceholder = "Stellen Sie sich dem Leiter vor"
    override val checkStatus = "Status prüfen"
    override val albumEmpty = "Noch keine Fotos"
    override val albumsEmpty = "Keine Alben"
    override val comingSoon = "Demnächst verfügbar"
    override val list = "Liste"
    override val months = listOf("", "Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember")
    override val yearSuffix = ""
}
