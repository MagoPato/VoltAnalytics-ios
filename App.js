import React, { useRef, useState } from 'react';
import { SafeAreaView, StyleSheet, View, Text, TouchableOpacity, ActivityIndicator, StatusBar } from 'react-native';
import { WebView } from 'react-native-webview';

// 👉 DIRECCIÓN DE TU WEB
const WEB_URL = 'https://magopato.github.io/VoltAnalytics/';
const BG = '#0b0f10'; // mismo fondo que la web

export default function App() {
  const webRef = useRef(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [key, setKey] = useState(0); // cambiar la key recrea el WebView

  const reload = () => {
    setError(null);
    setLoading(true);
    setKey((k) => k + 1);
  };

  return (
    <SafeAreaView style={styles.root}>
      <StatusBar barStyle="light-content" backgroundColor={BG} />
      <View style={styles.root}>
        <WebView
          key={key}
          ref={webRef}
          source={{ uri: WEB_URL }}
          style={styles.web}
          containerStyle={styles.web}
          originWhitelist={['*']}
          onShouldStartLoadWithRequest={() => true} // permite la redirección al túnel
          javaScriptEnabled
          domStorageEnabled
          sharedCookiesEnabled
          thirdPartyCookiesEnabled
          allowsInlineMediaPlayback
          mediaPlaybackRequiresUserAction={false}
          allowsBackForwardNavigationGestures
          pullToRefreshEnabled
          setSupportMultipleWindows={false}
          onLoadStart={() => setLoading(true)}
          onLoadEnd={() => setLoading(false)}
          onError={(e) => setError(e.nativeEvent.description || 'Error de red')}
          onHttpError={(e) => setError('HTTP ' + e.nativeEvent.statusCode)}
          onContentProcessDidTerminate={() => webRef.current?.reload()}
        />

        {loading && !error && (
          <View style={styles.badge} pointerEvents="none">
            <ActivityIndicator color="#d4ff3f" />
            <Text style={styles.badgeText}>Cargando VoltAnalytics…</Text>
          </View>
        )}

        {error && (
          <View style={styles.overlay}>
            <Text style={styles.title}>No se pudo cargar</Text>
            <Text style={styles.msg}>{error}</Text>
            <TouchableOpacity style={styles.btn} onPress={reload}>
              <Text style={styles.btnText}>Reintentar</Text>
            </TouchableOpacity>
          </View>
        )}
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: BG },
  web: { flex: 1, backgroundColor: BG },
  badge: {
    position: 'absolute', bottom: 24, alignSelf: 'center', flexDirection: 'row', alignItems: 'center',
    backgroundColor: 'rgba(16,22,26,0.95)', paddingHorizontal: 14, paddingVertical: 8, borderRadius: 20,
  },
  badgeText: { color: '#eef4ee', marginLeft: 8, fontSize: 13 },
  overlay: { ...StyleSheet.absoluteFillObject, backgroundColor: BG, alignItems: 'center', justifyContent: 'center', padding: 24 },
  title: { color: '#eef4ee', fontSize: 18, fontWeight: '600', marginBottom: 8 },
  msg: { color: '#7f938d', fontSize: 14, textAlign: 'center', marginBottom: 20 },
  btn: { borderColor: '#d4ff3f', borderWidth: 1, paddingHorizontal: 20, paddingVertical: 10, borderRadius: 4 },
  btnText: { color: '#d4ff3f', fontSize: 15, fontWeight: '600' },
});
