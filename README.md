# ServoMotor Android App

## Descripción

Aplicación móvil Android desarrollada para el control remoto de servomotores mediante conexión Bluetooth con módulos ESP32. La aplicación proporciona una interfaz intuitiva con control manual y posiciones predefinidas, ideal para proyectos de robótica, automatización y prototipado.

## Características

### Control dual
- **Control manual**: slider deslizante para control preciso (0-180°)
- **Posiciones rápidas**: botones para ángulos predefinidos (0°, 45°, 90°, 135°, 180°)
- **Interfaz visual**: tacómetro animado en tiempo real

### Interfaz de usuario
- **Indicadores visuales**: estado de conexión con indicadores de color
- **Tacómetro**: vista gráfica del ángulo actual con animaciones
- **Retroalimentación**: actualización bidireccional con el dispositivo ESP32

### Conectividad Bluetooth
- **Conexión automática**: búsqueda y conexión con ESP32 emparejado
- **Protocolo personalizado**: comandos específicos para control de servo
- **Manejo de errores**: gestión robusta de desconexiones y errores
- **Permisos dinámicos**: solicitud inteligente de permisos según la versión de Android

## Especificaciones técnicas

### Requisitos del sistema
- **Android**: 6.0 (API 23) o superior
- **Bluetooth**: 4.0 o superior
- **RAM**: 2GB mínimo recomendado
- **Almacenamiento**: 50MB libres

### Compatibilidad
- **Android**: 6.0 - 14.0
- **Arquitecturas**: ARM64, ARMv7, x86_64
- **Orientación**: Portrait (vertical)

### Protocolo de Comunicación
```
Comando de Envío: ANGLE:<valor>
Respuesta ESP32: POSITION:<ángulo_actual>
```

## Instalación

### Prerrequisitos
1. **Android Studio**: 4.0 o superior
2. **SDK Android**: API 23-34
3. **Gradle**: 7.0 o superior

### Configuración del Proyecto
```bash
# Clonar el repositorio
git clone https://github.com/05545/ServoMotorAPP.git

# Abrir en Android Studio
cd ServoMotorAPP
# Abrir con Android Studio

# Sincronizar dependencias
# Build > Sync Project with Gradle Files
```

### Dependencias principales
```gradle
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.cardview:cardview:1.0.0'
    implementation 'com.google.android.material:material:1.9.0'
}
```

## Arquitectura del proyecto

### Estructura de archivos
```
app/src/main/java/com/example/servomotor/
├── MainActivity.java          # Actividad principal
├── BluetoothManager.java      # Gestor de conexión Bluetooth
└── TachometerView.java        # Vista personalizada del tacómetro

app/src/main/res/
├── layout/activity_main.xml   # Diseño principal
├── drawable/                  # Recursos gráficos
└── values/                    # Colores, strings, estilos
```

### Componentes principales

#### MainActivity
- **Funcionalidad**: coordinación de la interfaz y lógica de control
- **Responsabilidades**:
  - Gestión de la UI y eventos de usuario
  - Coordinación con BluetoothManager
  - Actualización de elementos visuales
  - Manejo de permisos y ciclo de vida

#### BluetoothManager
- **Funcionalidad**: gestión completa de la conexión Bluetooth
- **Características**:
  - Manejo de permisos dinámicos (Android 6-12+)
  - Conexión automática con ESP32
  - Protocolo de comunicación bidireccional
  - Gestión de errores y reconexión

#### TachometerView
- **Funcionalidad**: vista personalizada del tacómetro
- **Características**:
  - Animaciones suaves entre ángulos
  - Graduaciones y etiquetas automáticas
  - Indicador tipo aguja con colores personalizados
  - Renderizado optimizado con Canvas

## Configuración y uso

### Configuración inicial
1. **Emparejar ESP32**: emparejar el dispositivo "ServoController_ESP32" desde configuración de Bluetooth
2. **Permisos**: la app solicitará permisos de Bluetooth automáticamente
3. **Conexión**: presionar "Conectar Bluetooth" en la aplicación

### Modos de control
1. **Slider manual**: deslizar para control continuo
2. **Botones rápidos**: tocar para posiciones específicas

### Protocolo de Comunicación
```java
// Envío de comando
bluetoothManager.sendAngle(90); // Envía "ANGLE:90\n"

// Recepción de datos
onDataReceived("POSITION:90"); // Actualiza UI con posición actual
```

## Personalización

### Modificar colores
```xml
<!-- res/values/colors.xml -->
<color name="primary_color">#6C0C91</color>
<color name="accent_color">#E74C3C</color>
<color name="background_color">#F8F9FA</color>
```

### Cambiar nombre del dispositivo ESP32
```java
// BluetoothManager.java
private static final String ESP32_NAME = "TuNombreESP32";
// Asegurate de que coincida con tu ESP32
```

### Personalizar tacómetro
```java
// TachometerView.java
private static final float START_ANGLE = 135;    // Ángulo inicial
private static final float SWEEP_ANGLE = 270;    // Rango de barrido
```

## Solución de problemas

### Problemas comunes

#### 1. No se encuentra el ESP32
- **Solución**: verificar que el ESP32 esté emparejado y el nombre coincida
- **Verificación**: Configuración > Bluetooth > Dispositivos emparejados

#### 2. Permisos de bluetooth
- **Android 6-11**: Requiere BLUETOOTH, BLUETOOTH_ADMIN, ACCESS_FINE_LOCATION
- **Android 12+**: Requiere BLUETOOTH_CONNECT, BLUETOOTH_SCAN
- **Solución**: permitir todos los permisos solicitados

#### 3. Conexión intermitente
- **Causa**: interferencia o distancia
- **Solución**: mantener dispositivos cerca (< 10 metros)

#### 4. La app no responde
- **Solución**: verificar que el ESP32 tenga el código correcto cargado, puedes descargarlo desde - [ESP-32-FIRMWARE-SERVO](https://github.com/05545/ESP32-ServoMotor-Bluetooth)
- **Verificación**: monitor serial del ESP32 debe mostrar mensajes conforme a código cargado en el módulo.

### Logs de depuración
```bash
# Filtrar logs de la aplicación
adb logcat | grep "BluetoothManager\|MainActivity\|TachometerView"
```

### Compilación
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

## Licencia

Este proyecto está bajo la licencia MIT. Consulte el archivo [LICENSE](LICENSE) para más detalles.

## Roadmap

- [ ] Soporte para múltiples servos
- [ ] Configuración de velocidad de movimiento
- [ ] Guardar posiciones favoritas
- [ ] Modo de secuencias automáticas
- [ ] Soporte para WiFi además de Bluetooth

## Autor

**Rodrigo Sosa Romero**
- GitHub: [@05545](https://github.com/05545)
- Proyecto: [ServoMotorAPP](https://github.com/05545/ServoMotorAPP)
