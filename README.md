# Qtengo 📱

Aplicación Android desarrollada como Trabajo de Final de Grado del ciclo formativo de **Desarrollo de Aplicaciones Multiplataforma (DAM)**.

**Autor:** [TU NOMBRE COMPLETO]  
**Tutor:** [NOMBRE DEL TUTOR]  
**Centro:** [NOMBRE DEL CENTRO]  
**Curso:** 2025/2026

---

## 📋 Descripción

Qtengo es una aplicación de gestión integral diseñada para tres tipos de usuarios:

- 🏠 **Familiar** — Lista de la compra, control de gastos, inventario del hogar y tareas/recordatorios
- 🏢 **Pyme** — Gestión de productos/stock, finanzas, proveedores, empleados y agenda de tareas
- 🍽️ **Restauración** — Carta/menú del día, stock de cocina, reservas y proveedores

Cada usuario puede tener uno o varios perfiles asignados y cambiar entre ellos sin cerrar sesión.

---

## 🛠️ Tecnologías utilizadas

| Tecnología | Uso |
|---|---|
| **Kotlin** | Lenguaje principal |
| **Jetpack Compose** | Interfaz de usuario declarativa |
| **Firebase Auth** | Autenticación de usuarios |
| **Firebase Firestore** | Base de datos en la nube |
| **ViewModel + StateFlow** | Arquitectura MVVM |
| **EncryptedSharedPreferences** | Almacenamiento seguro local |
| **WorkManager** | Notificaciones programadas |
| **Material Design 3** | Componentes visuales |

---

## ⚙️ Requisitos previos

- Android Studio **Hedgehog** o superior
- JDK 17
- Android SDK mínimo: **API 26 (Android 8.0)**
- Cuenta de Firebase con proyecto configurado

---

## 🚀 Instrucciones de instalación

1. Clona el repositorio:
```bash
git clone https://github.com/Oscar-carpio/QtengoPropio.git
```

2. Abre el proyecto en **Android Studio**

3. Añade el archivo `google-services.json` en la carpeta `app/`:
   - Ve a tu proyecto en [Firebase Console](https://console.firebase.google.com)
   - Descarga el archivo `google-services.json`
   - Cópialo en `app/google-services.json`

4. Sincroniza el proyecto con Gradle:
   - **File** → **Sync Project with Gradle Files**

---

## ▶️ Instrucciones para ejecutar

### En emulador
1. Abre **Device Manager** en Android Studio
2. Crea un dispositivo virtual con **API 26** o superior
3. Pulsa **Run** (▶️) o `Shift + F10`

### En dispositivo físico
1. Activa el **modo desarrollador** en tu dispositivo
2. Activa la **depuración USB**
3. Conecta el dispositivo por USB
4. Pulsa **Run** (▶️) o `Shift + F10`



---

## 📁 Estructura del proyecto