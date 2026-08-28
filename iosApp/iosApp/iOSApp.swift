import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        KoinHelperKt.doInitKoinIos()
    }
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
