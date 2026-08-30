import Darwin
import Foundation
import FoundationModels

private let bridgeProtocolVersion = 1

private struct BridgeRequest: Decodable {
    let version: Int
    let operation: String
    let prompt: String?
}

private struct BridgeResponse: Encodable {
    let version: Int
    let kind: String
    let status: String?
    let reason: String?
    let content: String?
    let errorCode: String?
    let message: String?

    static func status(_ status: String, reason: String? = nil) -> BridgeResponse {
        BridgeResponse(
            version: bridgeProtocolVersion,
            kind: "status",
            status: status,
            reason: reason,
            content: nil,
            errorCode: nil,
            message: nil
        )
    }

    static func generation(_ content: String) -> BridgeResponse {
        BridgeResponse(
            version: bridgeProtocolVersion,
            kind: "generation",
            status: nil,
            reason: nil,
            content: content,
            errorCode: nil,
            message: nil
        )
    }

    static func error(code: String, message: String, reason: String? = nil) -> BridgeResponse {
        BridgeResponse(
            version: bridgeProtocolVersion,
            kind: "error",
            status: nil,
            reason: reason,
            content: nil,
            errorCode: code,
            message: message
        )
    }
}

private enum ModelAvailability {
    case available
    case unavailable(reason: String, message: String)
}

@main
private struct FoundationModelsBridge {
    static func main() async {
        let response: BridgeResponse
        do {
            let requestData = FileHandle.standardInput.readDataToEndOfFile()
            let request = try JSONDecoder().decode(BridgeRequest.self, from: requestData)
            response = await handle(request)
        } catch {
            writeDiagnostic("Invalid bridge request: \(error)")
            response = .error(
                code: "invalidRequest",
                message: "The Foundation Models bridge received an invalid request."
            )
        }

        do {
            let data = try JSONEncoder().encode(response)
            FileHandle.standardOutput.write(data)
            FileHandle.standardOutput.write(Data([0x0A]))
        } catch {
            writeDiagnostic("Failed to encode bridge response: \(error)")
            exit(EXIT_FAILURE)
        }
    }

    private static func handle(_ request: BridgeRequest) async -> BridgeResponse {
        guard request.version == bridgeProtocolVersion else {
            return .error(
                code: "invalidRequest",
                message: "Unsupported Foundation Models bridge protocol version."
            )
        }

        switch request.operation {
        case "status":
            return statusResponse(for: modelAvailability())
        case "generate":
            guard let prompt = request.prompt?.trimmingCharacters(in: .whitespacesAndNewlines), !prompt.isEmpty else {
                return .error(code: "invalidRequest", message: "The on-device prompt is empty.")
            }
            switch modelAvailability() {
            case .available:
                do {
                    let session = LanguageModelSession()
                    let response = try await session.respond(to: prompt)
                    return .generation(response.content)
                } catch {
                    writeDiagnostic("Foundation Models generation failed: \(error)")
                    return .error(
                        code: "generationFailed",
                        message: "Apple Foundation Models generation failed."
                    )
                }
            case .unavailable(let reason, let message):
                return .error(code: "unavailable", message: message, reason: reason)
            }
        default:
            return .error(code: "invalidRequest", message: "Unknown Foundation Models bridge operation.")
        }
    }

    private static func modelAvailability() -> ModelAvailability {
        #if !arch(arm64)
        return .unavailable(
            reason: "unsupportedHardware",
            message: "Apple Foundation Models requires a Mac with Apple silicon."
        )
        #elseif !os(macOS)
        return .unavailable(
            reason: "unsupportedOs",
            message: "Apple Foundation Models is only supported on macOS 26 or newer."
        )
        #else
        guard #available(macOS 26.0, *) else {
            return .unavailable(
                reason: "unsupportedOs",
                message: "Apple Foundation Models requires macOS 26 or newer."
            )
        }
        switch SystemLanguageModel.default.availability {
        case .available:
            return .available
        case .unavailable(let reason):
            switch reason {
            case .appleIntelligenceNotEnabled:
                return .unavailable(
                    reason: "appleIntelligenceNotEnabled",
                    message: "Turn on Apple Intelligence in System Settings to use Apple Foundation Models."
                )
            case .modelNotReady:
                return .unavailable(
                    reason: "modelNotReady",
                    message: "Apple Foundation Models is not ready yet. Try again after the model download completes."
                )
            case .deviceNotEligible:
                return .unavailable(
                    reason: "deviceNotEligible",
                    message: "Apple Foundation Models is unavailable on this Mac."
                )
            @unknown default:
                return .unavailable(
                    reason: "unknown",
                    message: "Apple Foundation Models is currently unavailable."
                )
            }
        }
        #endif
    }

    private static func statusResponse(for availability: ModelAvailability) -> BridgeResponse {
        switch availability {
        case .available:
            return .status("available")
        case .unavailable(let reason, _):
            return .status("unavailable", reason: reason)
        }
    }

    private static func writeDiagnostic(_ message: String) {
        guard let data = "\(message)\n".data(using: .utf8) else { return }
        FileHandle.standardError.write(data)
    }
}
