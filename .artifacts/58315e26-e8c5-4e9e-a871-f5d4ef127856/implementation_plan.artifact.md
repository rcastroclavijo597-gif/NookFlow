# Implementación de Pantalla de Inicio de Sesión (NookFlow)

Este plan detalla los pasos para recrear la interfaz de usuario mostrada en la imagen en `activity_main.xml`, incluyendo la creación de recursos auxiliares (colores, cadenas y formas).

## Cambios Propuestos

### Recursos de Valores

#### [MODIFY] [colors.xml](file:///C:/Users/Usuario/AndroidStudioProjects/MyApplication3/app/src/main/res/values/colors.xml)
Añadir colores para los fondos de los campos de texto, bordes de botones y el estilo del logo.

#### [MODIFY] [strings.xml](file:///C:/Users/Usuario/AndroidStudioProjects/MyApplication3/app/src/main/res/values/strings.xml)
Añadir todas las cadenas de texto visibles: "Iniciar Sesion", "Correo electrónico", "Contraseña", "O continua con", "Continuar con Google/Apple/Facebook".

### Recursos Gráficos (Drawables)

#### [NEW] `bg_input_field.xml`
Forma rectangular con esquinas redondeadas y fondo gris claro para los campos de entrada.

#### [NEW] `bg_social_button.xml`
Forma rectangular con esquinas redondeadas, fondo blanco y borde gris para los botones de redes sociales.

### Interfaz de Usuario

#### [MODIFY] [activity_main.xml](file:///C:/Users/Usuario/AndroidStudioProjects/MyApplication3/app/src/main/res/layout/activity_main.xml)
Implementar el diseño completo usando `ConstraintLayout`. Se incluirán:
- Logo (Placeholder si no está disponible)
- Nombre de la app "NookFlow"
- Título "Iniciar Sesion"
- Campos de texto para Correo y Contraseña
- Separador con texto "O continua con"
- Botones de Google, Apple y Facebook

## Plan de Verificación

### Verificación Manual
- Previsualizar el diseño en el editor de Android Studio para asegurar que coincida con la imagen proporcionada.
- Ejecutar la aplicación en un emulador o dispositivo real.
