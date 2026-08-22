# Walkthrough - Botón de Encendido Neon: Funcionalidad y Estética

Se ha transformado el indicador de encendido en un botón funcional y altamente estético, integrando una secuencia de apagado profesional.

## Mejoras Implementadas

### 1. Interactividad Completa
- **Botón de Apagado**: El `NeonPowerButton` ahora es un componente interactivo (`Surface` con `onClick`).
- **Lógica de Cierre**: Al pulsar el botón:
    1. Se pausa la música instantáneamente.
    2. Se activa el estado `isShuttingDown`.
    3. Se muestra un overlay inmersivo de "CERRANDO SESIÓN".
    4. La aplicación se cierra automáticamente tras 3 segundos.

### 2. Refinamiento Estético Premium
- **Diseño Compacto**: Se redujo el tamaño a **38dp**, dándole un aire más minimalista y sofisticado.
- **Aura de Neón Mejorada**: Se ajustó el gradiente radial y la sombra para que el resplandor sea más etéreo y nítido, evitando manchas visuales.
- **Ubicación Optimizada**: Se ajustó el margen superior (10dp) para una alineación perfecta con los iconos del sistema en la barra de estado.
- **Estados Visuales**: El botón se desactiva visualmente (gris y sin animación) una vez iniciado el proceso de apagado.

## Detalles Técnicos
- **Workflows**: `NeonPowerButton (UI)` -> `MainViewModel.shutdownApp()` -> `PlayerManager.initiateManualShutdown()`.
- **Accesibilidad**: Se añadió `contentDescription` para mejorar la experiencia con lectores de pantalla.

## Cómo Probarlo
1.  Busca el icono de encendido en la esquina superior derecha de la pantalla principal.
2.  Toca el botón.
3.  Observa cómo la música se detiene y aparece la pantalla de despedida neon.
4.  La app se cerrará por sí sola tras unos segundos.

> [!TIP]
> El color del botón siempre coincide con la carátula de la canción actual, creando una armonía visual constante en toda la interfaz.
