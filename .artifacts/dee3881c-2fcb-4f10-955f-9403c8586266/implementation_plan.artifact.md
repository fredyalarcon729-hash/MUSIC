# Funcionalidad y Estética: Botón de Encendido Neon

Este plan hace funcional el botón de encendido (NeonPowerButton) y mejora su integración estética en la interfaz principal.

## User Review Required

> [!IMPORTANT]
> **Acción de Apagado**: Al tocar el botón, se iniciará la secuencia de cierre: la música se detendrá, se mostrará la pantalla de "Cerrando Sesión" y la aplicación se cerrará tras 3 segundos.
> **Nueva Ubicación**: Desplazaremos el botón para que esté mejor alineado con el borde superior, dándole un aire de "panel de control" más integrado.

## Proposed Changes

### [UI Layer] Interactividad y Diseño

#### [MODIFY] [MainActivity.kt](file:///C:/Users/falarcon/StudioProjects/MUSIC/app/src/main/java/com/example/MainActivity.kt)
- **Refactorizar `NeonPowerButton`**: Añadir parámetro `onClick: () -> Unit`.
- **Ajuste Estético**:
    - Reducir ligeramente el tamaño para que sea más elegante.
    - Mejorar el sombreado neon para que no parezca una "mancha" sino un aura definida.
    - Cambiar la ubicación de `TopEnd` a una posición con márgenes más consistentes (ej. dentro de un `Box` con `statusBarsPadding` y un desplazamiento suave).
- **Lógica de Click**: Conectar el botón con `mainViewModel.shutdownApp()`.

#### [MODIFY] [FusionMainViewModel.kt](file:///C:/Users/falarcon/StudioProjects/MUSIC/app/src/main/java/com/example/ui/FusionMainViewModel.kt)
- Añadir `fun shutdownApp()` que invoque la lógica de apagado en el `PlayerManager`.

### [Core Layer] Lógica de Apagado

#### [MODIFY] [FusionPlayerManager.kt](file:///C:/Users/falarcon/StudioProjects/MUSIC/app/src/main/java/com/example/player/FusionPlayerManager.kt)
- Añadir `fun initiateManualShutdown()` para pausar la reproducción y activar el flag `isShuttingDown`.

## Verification Plan

### Manual Verification
1.  **Toque en el Botón**: Confirmar que al pulsar el icono de encendido, la música se pausa inmediatamente.
2.  **Secuencia Visual**: Verificar que aparece el overlay de "CERRANDO SESIÓN" con la animación de carga.
3.  **Cierre**: Comprobar que la actividad finaliza automáticamente después de la cuenta regresiva.
4.  **Estética**: Asegurar que el botón se ve "limpio" y bien posicionado en modo vertical y horizontal.
