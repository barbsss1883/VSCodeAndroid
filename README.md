# VSCode Android - Editor de Código

Un editor de código completo para Android inspirado en Visual Studio Code.

## Características

- **Monaco Editor** - El mismo editor que usa VS Code, con:
  - Resaltado de sintaxis para 20+ lenguajes
  - Autocompletado inteligente
  - Buscar y reemplazar
  - Plegado de código
  - Números de línea y minimapa
- **Explorador de archivos** lateral con árbol de directorios
- **Pestañas múltiples** para editar varios archivos a la vez
- **Indicador de cambios** sin guardar
- **Temas**: Oscuro (VS Dark), Claro, Alto contraste
- **Undo/Redo** ilimitado
- **Guardar con Ctrl+S**
- Soporte para: JS, TS, Python, Kotlin, Java, HTML, CSS, JSON, XML, Markdown, Shell, C/C++, Go, Rust, y más

## Requisitos

- Android Studio Hedgehog (2023.1.1) o superior
- Android SDK 26+ (Android 8.0+)
- Kotlin 1.9+
- **Conexión a internet** (Monaco Editor se carga desde CDN)

## Compilar e Instalar

### 1. Abrir en Android Studio
```
File → Open → Seleccionar carpeta VSCodeAndroid
```

### 2. Sincronizar Gradle
Android Studio lo hará automáticamente. Si no:
```
File → Sync Project with Gradle Files
```

### 3. Instalar en dispositivo/emulador
```
Run → Run 'app'  (Shift+F10)
```

## Arquitectura

```
VSCodeAndroid/
├── app/src/main/
│   ├── assets/monaco/
│   │   └── index.html          ← Monaco Editor + JavaScript bridge
│   ├── java/.../
│   │   ├── MainActivity.kt     ← Activity principal + UI
│   │   ├── EditorViewModel.kt  ← Estado y lógica (LiveData)
│   │   ├── EditorBridge.kt     ← Puente JS ↔ Android
│   │   ├── adapters/           ← RecyclerView adapters
│   │   ├── models/             ← Datos (FileItem, EditorTab, AppSettings)
│   │   └── utils/              ← FileUtils, PreferencesManager
│   └── res/
│       ├── layout/             ← Layouts XML
│       ├── drawable/           ← Íconos vectoriales
│       └── values/             ← Colores, strings, temas
```

## Uso

1. Abre el explorador (ícono ≡ arriba a la izquierda)
2. Toca el ícono de carpeta para abrir un directorio
3. Navega y toca cualquier archivo para editarlo
4. Usa los botones de la barra:
   - 💾 **Guardar** - Guarda el archivo actual
   - 🔍 **Buscar** - Abre buscar/reemplazar
   - ↩ **Deshacer** / ↪ **Rehacer**
   - ⚙️ **Ajustes** - Cambiar tema

## Permisos necesarios

- `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` - Acceso a archivos
- `INTERNET` - Cargar Monaco Editor desde CDN
- `MANAGE_EXTERNAL_STORAGE` - Acceso completo al almacenamiento (Android 11+)

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Kotlin |
| Editor | Monaco Editor 0.44 (WebView) |
| UI | Material Design 3 |
| Arquitectura | MVVM + LiveData |
| Navegación | Navigation Drawer |

## Personalización futura

Para funcionar **sin internet**, descarga Monaco Editor localmente:
```bash
npm install monaco-editor
cp -r node_modules/monaco-editor/min/vs app/src/main/assets/monaco/
```
Luego cambia en `index.html`:
```js
var require = { paths: { 'vs': 'vs' } };
```
```html
<script src="vs/loader.js"></script>
```
# VSCodeAndroid
# VSCodeAndroid
