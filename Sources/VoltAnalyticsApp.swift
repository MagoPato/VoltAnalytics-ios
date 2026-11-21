import SwiftUI
import WebKit

// 👉 DIRECCIÓN DE TU WEB
let WEB_URL = URL(string: "https://magopato.github.io/VoltAnalytics/")!
let BG = Color(red: 11 / 255, green: 15 / 255, blue: 16 / 255) // #0b0f10, mismo fondo que la web
let VOLT = Color(red: 212 / 255, green: 1, blue: 63 / 255)      // #d4ff3f

@main
struct VoltAnalyticsApp: App {
    var body: some Scene {
        WindowGroup { ContentView().preferredColorScheme(.dark) }
    }
}

final class WebModel: NSObject, ObservableObject, WKNavigationDelegate, WKUIDelegate {
    @Published var loading = true
    @Published var errorText: String?
    let webView: WKWebView

    override init() {
        let config = WKWebViewConfiguration()
        config.allowsInlineMediaPlayback = true
        config.mediaTypesRequiringUserActionForPlayback = []
        config.websiteDataStore = .default()
        config.preferences.javaScriptCanOpenWindowsAutomatically = true
        webView = WKWebView(frame: .zero, configuration: config)
        super.init()

        webView.navigationDelegate = self
        webView.uiDelegate = self
        webView.allowsBackForwardNavigationGestures = true
        webView.isOpaque = false
        webView.backgroundColor = UIColor(red: 11 / 255, green: 15 / 255, blue: 16 / 255, alpha: 1)
        webView.scrollView.backgroundColor = webView.backgroundColor
        webView.scrollView.contentInsetAdjustmentBehavior = .never // sin hueco abajo
        if #available(iOS 16.4, *) { webView.isInspectable = true }

        let refresh = UIRefreshControl()
        refresh.addTarget(self, action: #selector(pulledToRefresh(_:)), for: .valueChanged)
        webView.scrollView.refreshControl = refresh

        load()
    }

    func load() {
        errorText = nil
        loading = true
        webView.load(URLRequest(url: WEB_URL, cachePolicy: .reloadIgnoringLocalCacheData))
    }

    @objc private func pulledToRefresh(_ sender: UIRefreshControl) {
        webView.reload()
        sender.endRefreshing()
    }

    // MARK: navegación
    func webView(_ webView: WKWebView, didStartProvisionalNavigation navigation: WKNavigation!) {
        loading = true
    }

    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        loading = false
        errorText = nil
    }

    func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
        handle(error)
    }

    func webView(_ webView: WKWebView, didFailProvisionalNavigation navigation: WKNavigation!, withError error: Error) {
        handle(error)
    }

    private func handle(_ error: Error) {
        let ns = error as NSError
        // -999 = navegación cancelada porque la página redirigió (normal, no es un error)
        if ns.domain == NSURLErrorDomain && ns.code == NSURLErrorCancelled { return }
        loading = false
        errorText = "\(ns.localizedDescription)\n(\(ns.domain) \(ns.code))"
    }

    func webViewWebContentProcessDidTerminate(_ webView: WKWebView) {
        webView.reload()
    }

    // Enlaces target="_blank" / window.open → abrir en la misma vista
    func webView(_ webView: WKWebView, createWebViewWith configuration: WKWebViewConfiguration,
                 for navigationAction: WKNavigationAction, windowFeatures: WKWindowFeatures) -> WKWebView? {
        if let url = navigationAction.request.url { webView.load(URLRequest(url: url)) }
        return nil
    }

    // alert() de JavaScript
    func webView(_ webView: WKWebView, runJavaScriptAlertPanelWithMessage message: String,
                 initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping () -> Void) {
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default) { _ in completionHandler() })
        topController()?.present(alert, animated: true) ?? completionHandler()
    }

    // confirm() de JavaScript
    func webView(_ webView: WKWebView, runJavaScriptConfirmPanelWithMessage message: String,
                 initiatedByFrame frame: WKFrameInfo, completionHandler: @escaping (Bool) -> Void) {
        let alert = UIAlertController(title: nil, message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "Cancelar", style: .cancel) { _ in completionHandler(false) })
        alert.addAction(UIAlertAction(title: "OK", style: .default) { _ in completionHandler(true) })
        topController()?.present(alert, animated: true) ?? completionHandler(false)
    }

    private func topController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene
        var top = scene?.windows.first(where: { $0.isKeyWindow })?.rootViewController
        while let presented = top?.presentedViewController { top = presented }
        return top
    }
}

struct WebView: UIViewRepresentable {
    let model: WebModel
    func makeUIView(context: Context) -> WKWebView { model.webView }
    func updateUIView(_ uiView: WKWebView, context: Context) {}
}

struct ContentView: View {
    @StateObject private var model = WebModel()

    var body: some View {
        ZStack {
            BG.ignoresSafeArea()
            WebView(model: model)
                .ignoresSafeArea(.container, edges: .bottom) // la web llega hasta el borde inferior

            if let err = model.errorText {
                BG.ignoresSafeArea()
                VStack(spacing: 14) {
                    Text("No se pudo cargar").font(.headline).foregroundColor(.white)
                    Text(err).font(.footnote).foregroundColor(.gray).multilineTextAlignment(.center)
                    Button("Reintentar") { model.load() }
                        .padding(.horizontal, 22).padding(.vertical, 10)
                        .foregroundColor(VOLT)
                        .overlay(RoundedRectangle(cornerRadius: 4).stroke(VOLT))
                }
                .padding(24)
            } else if model.loading {
                VStack {
                    Spacer()
                    HStack(spacing: 8) {
                        ProgressView().tint(VOLT)
                        Text("Cargando VoltAnalytics…").font(.footnote).foregroundColor(.white)
                    }
                    .padding(.horizontal, 14).padding(.vertical, 8)
                    .background(Color.black.opacity(0.8))
                    .clipShape(Capsule())
                    .padding(.bottom, 24)
                }
                .allowsHitTesting(false)
            }
        }
    }
}
