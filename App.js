import React, { useRef, useState } from 'react';
import { SafeAreaView, StyleSheet, View, Text, TouchableOpacity, ActivityIndicator, StatusBar } from 'react-native';
import { WebView } from 'react-native-webview';

// 👉 CAMBIA AQUÍ LA DIRECCIÓN DE TU WEB
const WEB_URL = 'https://magopato.github.io/VoltAnalytics/';

export default function App() {
  const webRef = useRef(null);
  const [error, setError] = useState(false);

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="default" />
      {error ? (
        <View style={styles.center}>
          <Text style={styles.msg}>No se pudo cargar la página.{'\n'}Revisa tu conexión.</Text>
          <TouchableOpacity
            style={styles.btn}
            onPress={() => {
              setError(false);
              webRef.current?.reload();
            }}
          >
            <Text style={styles.btnText}>Reintentar</Text>
          </TouchableOpacity>
        </View>
      ) : (
        <WebView
          ref={webRef}
          source={{ uri: WEB_URL }}
          style={styles.container}
          startInLoadingState
          renderLoading={() => (
            <View style={styles.center}>
              <ActivityIndicator size="large" />
            </View>
          )}
          allowsBackForwardNavigationGestures // deslizar desde el borde = atrás
          pullToRefreshEnabled // tirar hacia abajo = recargar
          allowsInlineMediaPlayback
          javaScriptEnabled
          domStorageEnabled
          sharedCookiesEnabled
          onError={() => setError(true)}
        />
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  center: { ...StyleSheet.absoluteFillObject, alignItems: 'center', justifyContent: 'center', padding: 24 },
  msg: { fontSize: 16, textAlign: 'center', marginBottom: 16 },
  btn: { backgroundColor: '#007AFF', paddingHorizontal: 20, paddingVertical: 10, borderRadius: 8 },
  btnText: { color: '#fff', fontSize: 16, fontWeight: '600' },
});
