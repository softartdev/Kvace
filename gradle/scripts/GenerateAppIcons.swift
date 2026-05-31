import CoreGraphics
import Foundation
import ImageIO

enum IconGenerationError: Error, CustomStringConvertible {
    case missingSource(URL)
    case unreadableSource(URL)
    case renderFailed(Int)
    case pngEncodingFailed(Int)
    case icnsScriptFailed(Int32)

    var description: String {
        switch self {
        case let .missingSource(url):
            return "Source icon does not exist: \(url.path)"
        case let .unreadableSource(url):
            return "Could not read source icon: \(url.path)"
        case let .renderFailed(size):
            return "Could not render \(size)x\(size) icon"
        case let .pngEncodingFailed(size):
            return "Could not encode \(size)x\(size) PNG"
        case let .icnsScriptFailed(status):
            return "convert_iconset_to_icns.sh failed with exit code \(status)"
        }
    }
}

extension URL {
    func appendingPath(_ path: String) -> URL {
        path
            .split(separator: "/")
            .reduce(self) { url, component in
                url.appendingPathComponent(String(component))
            }
    }
}

extension Data {
    mutating func appendUInt16LE(_ value: UInt16) {
        append(UInt8(value & 0xff))
        append(UInt8((value >> 8) & 0xff))
    }

    mutating func appendUInt32LE(_ value: UInt32) {
        append(UInt8(value & 0xff))
        append(UInt8((value >> 8) & 0xff))
        append(UInt8((value >> 16) & 0xff))
        append(UInt8((value >> 24) & 0xff))
    }
}

let fileManager = FileManager.default
let repositoryRoot = URL(fileURLWithPath: fileManager.currentDirectoryPath, isDirectory: true)
let sourceURL = repositoryRoot.appendingPath("app/androidApp/src/main/ic_launcher-playstore.png")
let appleIconSourceURL = repositoryRoot.appendingPath(
    "app/desktopApp/src/main/resources/icons/Kvace-iOS-Default-1024x1024@1x.png"
)
let convertIconsetToICNSScriptURL = repositoryRoot.appendingPath(
    "gradle/scripts/convert_iconset_to_icns.sh"
)

guard fileManager.fileExists(atPath: sourceURL.path) else {
    throw IconGenerationError.missingSource(sourceURL)
}
guard fileManager.fileExists(atPath: appleIconSourceURL.path) else {
    throw IconGenerationError.missingSource(appleIconSourceURL)
}

func loadImage(at url: URL) throws -> CGImage {
    guard
        let imageSource = CGImageSourceCreateWithURL(url as CFURL, nil),
        let image = CGImageSourceCreateImageAtIndex(imageSource, 0, nil)
    else {
        throw IconGenerationError.unreadableSource(url)
    }

    return image
}

let sourceImage = try loadImage(at: sourceURL)

func ensureParentDirectoryExists(for url: URL) throws {
    try fileManager.createDirectory(
        at: url.deletingLastPathComponent(),
        withIntermediateDirectories: true
    )
}

enum IconStyle {
    case fullSquare
    case appleRounded
}

let appleIconCornerRadiusFraction: CGFloat = 0.22

func pngData(size: Int, style: IconStyle) throws -> Data {
    let colorSpace = CGColorSpaceCreateDeviceRGB()
    let bitmapInfo = CGImageAlphaInfo.premultipliedLast.rawValue

    guard
        let context = CGContext(
            data: nil,
            width: size,
            height: size,
            bitsPerComponent: 8,
            bytesPerRow: 0,
            space: colorSpace,
            bitmapInfo: bitmapInfo
        )
    else {
        throw IconGenerationError.renderFailed(size)
    }

    context.interpolationQuality = .high

    switch style {
    case .fullSquare:
        context.draw(sourceImage, in: CGRect(x: 0, y: 0, width: size, height: size))
    case .appleRounded:
        context.clear(CGRect(x: 0, y: 0, width: size, height: size))
        let rect = CGRect(x: 0, y: 0, width: size, height: size)
        let cornerRadius = CGFloat(size) * appleIconCornerRadiusFraction
        context.addPath(CGPath(
            roundedRect: rect,
            cornerWidth: cornerRadius,
            cornerHeight: cornerRadius,
            transform: nil
        ))
        context.clip()
        context.draw(sourceImage, in: rect)
    }

    guard let outputImage = context.makeImage() else {
        throw IconGenerationError.renderFailed(size)
    }

    let data = NSMutableData()
    guard
        let destination = CGImageDestinationCreateWithData(data, "public.png" as CFString, 1, nil)
    else {
        throw IconGenerationError.pngEncodingFailed(size)
    }

    CGImageDestinationAddImage(destination, outputImage, nil)

    guard CGImageDestinationFinalize(destination) else {
        throw IconGenerationError.pngEncodingFailed(size)
    }

    return data as Data
}

func writePNG(size: Int, to url: URL, style: IconStyle = .fullSquare) throws {
    try ensureParentDirectoryExists(for: url)
    try pngData(size: size, style: style).write(to: url, options: .atomic)
    print("Generated \(url.path) (\(size)x\(size))")
}

func makeICOData(sizes: [Int], style: IconStyle = .fullSquare) throws -> Data {
    let pngEntries = try sizes.map { size in
        (size: size, data: try pngData(size: size, style: style))
    }

    var ico = Data()
    ico.appendUInt16LE(0)
    ico.appendUInt16LE(1)
    ico.appendUInt16LE(UInt16(pngEntries.count))

    var imageOffset = UInt32(6 + 16 * pngEntries.count)

    for entry in pngEntries {
        ico.append(UInt8(entry.size == 256 ? 0 : entry.size))
        ico.append(UInt8(entry.size == 256 ? 0 : entry.size))
        ico.append(0)
        ico.append(0)
        ico.appendUInt16LE(1)
        ico.appendUInt16LE(32)
        ico.appendUInt32LE(UInt32(entry.data.count))
        ico.appendUInt32LE(imageOffset)
        imageOffset += UInt32(entry.data.count)
    }

    for entry in pngEntries {
        ico.append(entry.data)
    }

    return ico
}

func writeICO(sizes: [Int], to url: URL, style: IconStyle = .fullSquare) throws {
    try ensureParentDirectoryExists(for: url)
    try makeICOData(sizes: sizes, style: style).write(to: url, options: .atomic)
    print("Generated \(url.path) (\(sizes.map(String.init).joined(separator: ", ")) ICO)")
}

func writeText(_ text: String, to url: URL) throws {
    try ensureParentDirectoryExists(for: url)
    try text.data(using: .utf8)!.write(to: url, options: .atomic)
    print("Generated \(url.path)")
}

func copyFile(from sourceURL: URL, to destinationURL: URL) throws {
    try ensureParentDirectoryExists(for: destinationURL)

    if fileManager.fileExists(atPath: destinationURL.path) {
        try fileManager.removeItem(at: destinationURL)
    }

    try fileManager.copyItem(at: sourceURL, to: destinationURL)
    print("Generated \(destinationURL.path)")
}

func writeICNS(from sourceURL: URL, to outputURL: URL) throws {
    let process = Process()
    process.executableURL = convertIconsetToICNSScriptURL
    process.arguments = [sourceURL.path, outputURL.path]

    try process.run()
    process.waitUntilExit()

    guard process.terminationStatus == 0 else {
        throw IconGenerationError.icnsScriptFailed(process.terminationStatus)
    }

    print("Generated \(outputURL.path)")
}

let desktopIconDirectory = repositoryRoot.appendingPath("app/desktopApp/src/main/resources/icons")
let desktopComposeResourceDirectory = repositoryRoot.appendingPath(
    "app/desktopApp/src/main/composeResources/drawable"
)
let webResourceDirectory = repositoryRoot.appendingPath("app/webApp/src/webMain/resources")
let iosAppIconDirectory = repositoryRoot.appendingPath(
    "app/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"
)

try copyFile(
    from: appleIconSourceURL,
    to: desktopComposeResourceDirectory.appendingPathComponent("kvace_window_icon.png")
)
try writeICO(
    sizes: [16, 32, 48, 64, 128, 256],
    to: desktopIconDirectory.appendingPathComponent("kvace.ico"),
    style: .appleRounded
)
try writeICNS(
    from: appleIconSourceURL,
    to: desktopIconDirectory.appendingPathComponent("kvace.icns")
)
try copyFile(
    from: appleIconSourceURL,
    to: iosAppIconDirectory.appendingPathComponent("app-icon-1024.png")
)

try writePNG(size: 16, to: webResourceDirectory.appendingPathComponent("favicon-16x16.png"))
try writePNG(size: 32, to: webResourceDirectory.appendingPathComponent("favicon-32x32.png"))
try writePNG(size: 180, to: webResourceDirectory.appendingPathComponent("apple-touch-icon.png"))
try writePNG(size: 192, to: webResourceDirectory.appendingPathComponent("web-app-manifest-192x192.png"))
try writePNG(size: 512, to: webResourceDirectory.appendingPathComponent("web-app-manifest-512x512.png"))
try writeICO(
    sizes: [16, 32, 48],
    to: webResourceDirectory.appendingPathComponent("favicon.ico")
)

let webManifest = """
{
  "name": "Kvace",
  "short_name": "Kvace",
  "icons": [
    {
      "src": "web-app-manifest-192x192.png",
      "sizes": "192x192",
      "type": "image/png",
      "purpose": "any"
    },
    {
      "src": "web-app-manifest-512x512.png",
      "sizes": "512x512",
      "type": "image/png",
      "purpose": "any"
    }
  ],
  "theme_color": "#312E81",
  "background_color": "#0B1020",
  "display": "standalone"
}
"""

try writeText(webManifest, to: webResourceDirectory.appendingPathComponent("site.webmanifest"))
