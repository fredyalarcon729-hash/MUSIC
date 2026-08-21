# Optimización de Rendimiento y Fluidez UI (v13.0)

Este plan reduce el lag y los tirones al navegar por la aplicación mediante la optimización de las recomposiciones y el manejo inteligente de las listas de música.

## User Review Required

> [!IMPORTANT]
> **Aislamiento de Estado**: Hemos detectado que el "reloj" de la app (la posición de la canción) actualiza toda la interfaz cada 100ms. Vamos a "aislar" este movimiento para que las listas de canciones no se enteren de cada milisegundo que pasa, eliminando el lag.
> **Animaciones Ligeras**: El ecualizador animado de las listas se optimizará para que use el motor de gráficos directamente, sin forzar a toda la fila a redibujarse constantemente.
> **Estabilidad de Listas**: Aseguraremos que al cambiar de pestaña, Android no intente recalcular todas tus canciones de nuevo, haciendo que la navegación sea instantánea.

## Proposed Changes

### [Component Name] UI Optimization - Components

#### [MODIFY] [SongListItem.kt](file:///C:/Users/falarcon/StudioProjects/MUSIC/app/src/main/java/com/example/ui/components/SongListItem.kt)
- **Lazy State**: Cambiar los parámetros para que solo reciba lo estrictamente necesario.
- **Equalizer Performance**: Optimizar `EqualizerBars` usando `Modifier.graphicsLayer` para evitar recomposiciones del árbol de UI durante la animación.

#### [MODIFY] [MiniPlayer.kt](file:///C:/Users/falarcon/StudioProjects/MUSIC/app/src/main/java/com/example/ui/components/MiniPlayer.kt)
- **Progreso Eficiente**: Hacer que la barra de progreso del mini-reproductor lea el tiempo mediante una función lambda, evitando que todo el reproductor se redibuje 10 veces por segundo.

### [Component Name] Screens Refinement

#### [MODIFY] [HomeScreen.kt](file:///C:/Users/falarcon/StudioProjects/MUSIC/app/src/main/java/com/example/ui/screens/HomeScreen.kt), [LibraryScreen.kt](file:///C:/Users/falarcon/StudioProjects/MUSIC/app/src/main/java/com/example/ui/screens/LibraryScreen.kt)
- **Memoización de Lambdas**: Usar `remember` para todas las acciones de clic (`onPlay`, `onFavorite`), evitando que los elementos de la lista se crean nuevos innecesariamente.
- **Keys Estables**: Reforzar el uso de `key` en todos los `items()` para que Compose reutilice los componentes visibles.

### [Component Name] Architecture

#### [MODIFY] [FusionMainViewModel.kt](file:///C:/Users/falarcon/StudioProjects/MUSIC/app/src/main/java/com/example/ui/FusionMainViewModel.kt)
- **Particionamiento de StateFlow**: Crear flujos de estado específicos para los datos que cambian rápido (posición) y los que cambian lento (lista de canciones).

## Verification Plan

### Manual Verification
1.  **Scroll Infinito**: Deslizar rápidamente por una lista de 500 canciones y verificar que no hay saltos ni "congelamientos".
2.  **Cambio de Pestaña**: Navegar entre Inicio y Biblioteca repetidamente; la transición debe ser instantánea.
3.  **Monitor de GPU**: (Opcional) Observar que las barras de renderizado se mantienen por debajo de la línea de 16ms.
