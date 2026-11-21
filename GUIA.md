# Tu web como app de iPhone (desde Windows, gratis)

## 0. Personaliza
- `Sources/VoltAnalyticsApp.swift` → cambia `WEB_URL` por la dirección de tu web.
- `project.yml` → cambia `CFBundleDisplayName` (nombre bajo el ícono) y `PRODUCT_BUNDLE_IDENTIFIER`.

La app es nativa en Swift (WKWebView, el mismo motor que Safari); GitHub Actions genera el proyecto con XcodeGen y compila sin firma.

## 1. Sube el proyecto a GitHub
1. Crea un repositorio en github.com (público = minutos de Mac ilimitados; privado = cuota gratis limitada, los minutos de macOS cuentan x10).
2. Sube estos archivos (incluida la carpeta `.github`):
   ```
   git init
   git add .
   git commit -m "App web iOS"
   git branch -M main
   git remote add origin https://github.com/TU_USUARIO/TU_REPO.git
   git push -u origin main
   ```

## 2. Descarga el .ipa
1. En GitHub → pestaña **Actions** → "Compilar IPA iOS (sin firmar)". Se ejecuta solo con cada push (o pulsa **Run workflow**).
2. Tarda ~10–20 min. Al terminar, abre la ejecución y descarga el artefacto **ipa-sin-firmar** (viene en un .zip; dentro está `app-sin-firmar.ipa`).

## 3. Instala en el iPhone con Sideloadly
1. En Windows instala **iTunes** e **iCloud** versión web de apple.com (no la de Microsoft Store), y luego **Sideloadly** (sideloadly.io).
2. Conecta el iPhone por USB y pulsa "Confiar" en el teléfono.
3. Abre Sideloadly, arrastra `app-sin-firmar.ipa`, pon tu Apple ID y pulsa **Start**. Sideloadly firma la app con tu cuenta gratuita.
4. En el iPhone:
   - Ajustes → Privacidad y seguridad → **Modo de desarrollador** → activar (reinicia).
   - Ajustes → General → **VPN y gestión de dispositivos** → tu Apple ID → **Confiar**.

## Límites de la cuenta gratuita de Apple
- La app caduca a los **7 días**: vuelve a pasarla por Sideloadly (o usa AltStore/SideStore para refrescarla por Wi‑Fi).
- Máximo 3 apps instaladas así a la vez.

## ¿Por qué no Expo EAS Build?
EAS solo genera `.ipa` para dispositivos reales si le das una cuenta de desarrollador de Apple de pago; sin ella solo produce builds de **simulador**, que no se instalan en un iPhone. Por eso usamos GitHub Actions, que compila sin firmar y deja que Sideloadly firme con tu Apple ID gratuito.

## Cambios de la web
La app carga la web en vivo: si actualizas tu web, la app muestra los cambios sin recompilar. Solo recompila si cambias `WEB_URL`, el nombre o el ícono.
