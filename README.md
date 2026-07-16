# Van Status Monitor 🚌💨

An advanced, real-time Android vehicle telemetry dashboard designed to interface with onboard microcontrollers over a direct physical USB-to-Serial connection. Built entirely with modern Android patterns, Kotlin, Jetpack Compose, and asynchronous Coroutines.

---

## 🚀 Key Features Built Today

### 📡 Process-Isolated Architecture & IPC Log Bridge
* **Cross-Process Bus:** Bypasses Android's process isolation boundaries. Telemetry gathered by the background thread (`UsbBackgroundService`) safely hops the memory divide via an internal intent bus (`"com.van.status.UPDATE_UI_LOGS"`) to feed the frontend interface instance.
* **Modern Security Alignment:** Fully compliant with Android 11+ up to Android 14+ (API 34). Implements strict conditional runtime flags using `Context.RECEIVER_NOT_EXPORTED` to protect internal UI logs from external system sniffing while keeping simulation lines open for ADB diagnostics.

### 🔐 Lock-Screen Overrides & Automation Engine
* **Intelligent Display Activation:** Uses a combination of hardware `PowerManager.WakeLock` and advanced windowing flags (`setShowWhenLocked`, `setTurnScreenOn`) to automatically wake up a black screen and float the app interface directly over the device lock screen when an alert triggers.
* **Bypass Security Restrictions:** Restricts navigation to the core **Settings Screen** and **Developer Mode** when the app is active over a locked keyguard, ensuring third parties cannot modify parameters without the owner's authorization.
* **Auto-Tardown & Re-Lock:** The moment the incoming data stream reports that all active entry points are safely closed, the app explicitly strips its wake flags and calls `finishAndRemoveTask()`, closing the layout and forcing the OS to immediately lock the display back down.

### 🎵 Stateful Chime Profiling & Settings Matrix
* **4-Track Telemetry Audio:** Integrates four custom warning chime tracks (`audi_chime`, `chime_two`, `chime_three`, and `alert_chime_4`) housed directly in the application's physical binary paths (`res/raw/`).
* **Interactive UI Preview Engine:** Choosing an audio profile within the settings panel updates the typography font colors and transforms row item vector properties from a dynamic Play arrow to a Stop icon instantly during manual previews.
* **Hot-Reload Sync Layer:** Committing channel filter toggles (`flEnabled`, `frEnabled`, etc.) instantly updates shared device configurations on the fly and broadcasts a reload signal to the background execution thread without forcing data mutation side-effects.

---

## 🛠️ Data Ingestion Matrix (Serial Token Spec)
The parsing subsystem processes raw telemetry strings using pipeline separation parameters. Data arriving via the USB UART driver or simulated ADB shell pipes must match this explicit string design signature:

```text
DATA_STREAM:FL_OPEN|FR_CLOSED|RL_CLOSED|BACK_CLOSED|TEMP:24
DATA_STREAM:TEMP:27
