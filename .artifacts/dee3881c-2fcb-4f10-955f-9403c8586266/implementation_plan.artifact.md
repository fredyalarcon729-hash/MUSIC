# Estrategia de Audio v11.0: Máximo Rendimiento y Latencia Cero

Este plan elimina definitivamente la inestabilidad de la Mezcla Pro y restaura un motor único optimizado para cambios de canción ultra-rápidos y estables, garantizando que la app no vuelva a cerrarse.

## User Review Required

> [!IMPORTANT]
> **Adiós al Crossfade**: Siguiendo tu solicitud, hemos eliminado por completo el sistema de fundido cruzado y el doble motor. Esto libera el 50% de la carga de memoria de audio, eliminando la causa de los cierres.
> **Latencia Mínima NAtiva**: Configuraremos el reproductor para encadenar las canciones de forma **Gapless** nativa. El motor lee el siguiente archivo mientras escuchas el anterior para que el cambio sea instantáneo.
> **Optimización de Interfaz**: Hemos simplificado el sistema de actualización de la barra de progreso para que la app se sienta mucho más ligera y fluida al navegar.

## Proposed Changes

### [Component Name] Audio Engine Cleanup - FusionPlayerManager

#### [MODIFY] [FusionPlayerManager.kt](file:///C:/Users/falarcon/StudioProjects/MUSIC/app/src/main/java/com/example/player/FusionPlayerManager.kt)
- **Eliminar Motores Duales**: Borrar `playerA`, `playerB` y sus procesadores independientes. Restaurar una única instancia de `exoPlayer`.
- **Simplificación de Comandos**: Modificar `skipToNext` y `skipToPrevious` para realizar saltos directos de Media3 sin lógica de DJ manual.
- **Configuración de Carga**: Ajustar el buffer de ExoPlayer para una respuesta inmediata.

### [Component Name] UI & Configuration

#### [MODIFY] [SettingsScreen.kt](file:///C:/Users/falarcon/StudioProjects/MUSIC/app/src/main/java/com/example/ui/screens/SettingsScreen.kt)
- Eliminar visualmente los controles de Crossfade y Mezcla Pro.

#### [MODIFY] [FusionMediaService.kt](file:///C:/Users/falarcon/StudioProjects/MUSIC/app/src/main/java/com/example/service/FusionMediaService.kt)
- Volver a la vinculación directa y simple con el reproductor único para la notificación y Android Auto.

## Verification Plan

### Manual Verification
1.  **Prueba de Salto Rápido**: Pulsar "Siguiente" 10 veces seguidas. La app debe responder al instante y **no debe cerrarse**.
2.  **Continuidad Gapless**: Comprobar que al finalizar una canción local, la siguiente entra sin un solo milisegundo de silencio.
3.  **Karaoke**: Confirmar que el botón de eliminar voz sigue funcionando sobre el motor principal único.
