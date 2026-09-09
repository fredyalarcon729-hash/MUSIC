# Plan de Optimización de Rendimiento y Fluidez (v25.0)

Este plan aborda los problemas de lentitud y sensación de lag reportados, optimizando la carga de música en la nube y reduciendo la carga de procesamiento en la interfaz de usuario.

## User Review Required

> [!IMPORTANT]
> **Carga Inteligente de Firebase**: Cambiaremos la forma en que se cargan las canciones de la nube. Ya no esperaremos a que toda la lista se resuelva; la app cargará solo la canción actual instantáneamente y pre-cargará la siguiente en segundo plano.
> **Optimización de Interfaz**: Refactorizaremos el reproductor para que las animaciones de neón y la barra de progreso no ralenticen el resto de la aplicación, usando técnicas de "recomposición selectiva".

## Proposed Changes

### [Core Layer] Carga de Medios Optimizada

#### [MODIFY] [FusionPlayerManager.kt](file:///C:/Users/falarcon/StudioProjects/MUSIC/app/src/main/java/com/example/player/FusionPlayerManager.kt)
- **Resolución Bajo Demanda**: En `playSong`, resolver solo la URL de la canción que se va a reproducir inmediatamente.
- **Pre-fetching**: Implementar lógica para resolver la URL de la *siguiente* canción en la cola mientras la actual se reproduce, eliminando la espera al saltar de pista.
- **Throttle de Estado**: Aumentar el intervalo de actualización de metadatos pesados y separar el flujo de la posición de reproducción (milisegundos) del estado general de la UI.

### [UI Layer] Estabilización de Recomposición

#### [MODIFY] [FullPlayerSheet.kt](file:///C:/Users/falarcon/StudioProjects/MUSIC/app/src/main/java/com/example/ui/player/FullPlayerSheet.kt)
- **Desacoplamiento de Estado**: En lugar de pasar todo el `PlayerUiState` a cada componente, pasaremos solo los valores necesarios (ej. `songTitle`, `isPlaying`).
- **Optimización de Ambient Aura**:
    - Reducir el radio de desenfoque (`blur`) o usar capas de dibujo más eficientes.
    - Usar `derivedStateOf` para cálculos de progreso para evitar recomposiciones innecesarias de la pantalla completa.
- **Neon Visualizer**: Asegurar que el visualizador use `Modifier.drawBehind` o `Canvas` de forma que no afecte al layout principal.

### [Data Layer] Eficiencia de Repositorio

#### [MODIFY] [MusicRepository.kt](file:///C:/Users/falarcon/StudioProjects/MUSIC/app/src/main/java/com/example/data/repository/MusicRepository.kt)
- **Caché de Favoritos**: Optimizar la unión de flujos (`combine`) para evitar procesar toda la lista de canciones cada vez que cambia un pequeño detalle.

## Verification Plan

### Manual Verification
1.  **Skip Test**: Pasar 10 canciones rápidamente y verificar que la transición es casi instantánea.
2.  **Firebase Stream**: Reproducir una canción de la nube y confirmar que comienza a sonar en menos de 2 segundos.
3.  **UI Fluidity**: Abrir el ecualizador y mover los sliders mientras suena la música para asegurar que no hay tirones visuales.
4.  **Batería/Calor**: Verificar que el dispositivo no se calienta excesivamente tras 10 minutos de uso con el Ambient Aura activo.
