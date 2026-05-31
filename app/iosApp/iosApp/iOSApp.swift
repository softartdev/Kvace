import SwiftUI
import Shared

@main
struct iOSApp: App {
    private let onDevicePromptApi: OnDevicePromptApi?

    init() {
        onDevicePromptApi = Self.makeOnDevicePromptApi()
        AppleOnDevicePromptApiRegistry.shared.promptApi = onDevicePromptApi
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }

    private static func makeOnDevicePromptApi() -> OnDevicePromptApi? {
        #if canImport(FoundationModels)
        if #available(iOS 26.0, macCatalyst 26.0, *) {
            return PromptApiIos()
        }
        #endif
        return nil
    }
}
