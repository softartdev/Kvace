import Shared

#if canImport(FoundationModels)
import FoundationModels

@available(iOS 26.0, macCatalyst 26.0, *)
class PromptApiIos: OnDevicePromptApi {
    func generateContent(prompt: String) async throws -> String? {
        let session = LanguageModelSession()
        let response = try await session.respond(to: prompt)
        return response.content
    }
}
#endif
