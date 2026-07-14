# 🚐 Smart Van Telemetry Dashboard System

An integrated hardware-software ecosystem designed to monitor real-time vehicle entry points and internal cabin climate metrics. The project pairs an **Arduino Nano** capturing negative-switching vehicle hardware logic with a **Jetpack Compose Android Foreground Service application** optimized for Android Car Players (Head Units) and mobile devices.

---

## ✨ Features

* 📱 **Automated Background Wakeup:** The Android background service automatically monitors serial ports, instantly bringing the UI overlay to the screen foreground if a door is opened.
* 🔄 **Native Appliance Lifecycle (Boot Launch):** Features an integrated `BootReceiver` that hooks into `BOOT_COMPLETED` system intents, automatically launching the telemetry service as soon as the vehicle's Android Car Player powers on.
* ⏳ **5-Second Intelligent Dismissal:** When all entry points return to a closed state, a non-blocking coroutine gracefully counts down for 5 seconds before minimizing the application to conserve system memory and battery health.
* 📊 **Glassmorphic Cyber-Wireframe UI:** Renders structural isometric alert animations with real-time blinking aura sweeps (`BlendMode.Screen`) when a door state violation is active.
* 🌡️ **Smooth Numerical Rolling Gauge:** Leverages a localized `animateIntAsState` tween configuration to smoothly count temperature metrics up or down one-by-one, removing dry numerical interface snapping.
* 🎛️ **Dashboard Illumination Over-USB Controller:** Integrates an interactive UI slider sending PWM signals back down the data cable to a logic-level MOSFET, allowing the driver to dim or brighten physical 12V dashboard cluster lights from the application screen.
* ⚡ **Zero-Converter USB Powered:** Engineered to operate completely using the stable 5V output of a standard Android Head Unit USB port, eliminating the need for complex buck-boost voltage regulators.

---

## 🛠️ System Architecture

### 1. Hardware Pinout Configuration
* **Digital Pin 2 (`INPUT_PULLUP`):** Front Left (FL) Door Switch Line
* **Digital Pin 3 (`INPUT_PULLUP`):** Front Right (FR) Door Switch Line
* **Digital Pin 4 (`INPUT_PULLUP`):** Rear Left (RL) Door Switch Line
* **Digital Pin 5 (`INPUT_PULLUP`):** Back Hatch (BACK) Door Switch Line
* **Digital Pin 6 (`PWM Output`):** IRLZ44N MOSFET Gate (12V Dashboard Illumination Control)
* **Analog Pin A1:** LM35DZ Ambient Air Temperature Sensor

### 2. Stream Data Transmission Protocol
To ensure maximum text parsing resilience against data truncation or loop delays, the microcontroller uses an **Atomic Line-by-Line Streaming Framework** running over a 9600 Baud link payload profile. Data packets stream out as single strings ended by a standard newline literal (`\n`):

```text
DATA_STREAM:FL_OPEN
DATA_STREAM:FR_CLOSED
DATA_STREAM:TEMP:27
