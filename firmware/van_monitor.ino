/*
 * Van Status Telemetry System Arduino Firmware - Direct 1-Wire Skip ROM Protocol
 * 
 * Monitored Pins:
 * - D2: Front Left Door (FL)
 * - D3: Front Right Door (FR)
 * - D4: Rear Left Door (RL)
 * - D5: Back Hatch (BACK)
 * - D7: DS18B20 Digital Temperature Sensor (1-Wire Data Pin)
 * 
 * Hardware Requirements:
 * - Red Wire  -> 5V Pin on Arduino
 * - Black Wire -> GND Pin
 * - Yellow Wire -> Pin D7
 * - Resistors -> 3x 1k0 in series (3k0 total) between 5V and Pin D7
 * 
 * Logic: Negative-switching for doors (LOW = OPEN, HIGH = CLOSED).
 * Telemetry Broadcast Interval: 1000ms
 */

#include <OneWire.h>

#define PIN_FL 2
#define PIN_FR 3
#define PIN_RL 4
#define PIN_RR 5
#define PIN_BACK 6
#define ONE_WIRE_BUS 7 // Pin D7 connected to DS18B20 DATA wire

// Door pin setup
const int NUM_DOORS = 5;
const int doorPins[NUM_DOORS] = {PIN_FL, PIN_FR, PIN_RL,PIN_RR ,PIN_BACK};
const char* doorNames[NUM_DOORS] = {"FL", "FR", "RL","RR", "BACK"};

// State tracking arrays for non-blocking debouncing
int lastStableState[NUM_DOORS];
int lastReadState[NUM_DOORS];
unsigned long lastDebounceTime[NUM_DOORS];
const unsigned long DEBOUNCE_DELAY = 40; // 40ms non-blocking debounce threshold

// Telemetry timing
unsigned long lastTelemetryTime = 0;
const unsigned long TELEMETRY_INTERVAL = 1000; // Stream state every 1 second

// Initialize raw OneWire instance on Pin D7
OneWire ds(ONE_WIRE_BUS);

void setup() {
  Serial.begin(9600);

  // Enable internal pull-up resistor on Pin D7 as additional safety
  pinMode(ONE_WIRE_BUS, INPUT_PULLUP);

  // Initialize door pins with internal pull-up resistors
  for (int i = 0; i < NUM_DOORS; i++) {
    pinMode(doorPins[i], INPUT_PULLUP);
    
    int state = digitalRead(doorPins[i]);
    lastStableState[i] = state;
    lastReadState[i] = state;
    lastDebounceTime[i] = 0;
  }
}

void loop() {
  unsigned long now = millis();
  bool stateChanged = false;

  // 1. Non-Blocking Debounce for Door Sensors
  for (int i = 0; i < NUM_DOORS; i++) {
    int reading = digitalRead(doorPins[i]);

    // Reset debounce timer on edge transition
    if (reading != lastReadState[i]) {
      lastDebounceTime[i] = now;
      lastReadState[i] = reading;
    }

    // Confirm state change after stable duration
    if ((now - lastDebounceTime[i]) >= DEBOUNCE_DELAY) {
      if (reading != lastStableState[i]) {
        lastStableState[i] = reading;
        stateChanged = true; // Trigger immediate telemetry broadcast
      }
    }
  }

  // 2. Broadcast Telemetry (On instant state change OR 1000ms periodic interval)
  if (stateChanged || (now - lastTelemetryTime >= TELEMETRY_INTERVAL)) {
    broadcastTelemetry();
    lastTelemetryTime = now;
  }
}

int readDS18B20SkipROM() {
  byte data[9];

  // Step 1: Issue presence reset on 1-Wire bus
  if (!ds.reset()) {
    return -127; // Sensor not responding (Presence pulse missing)
  }

  // Step 2: Send Skip ROM command (0xCC) - Targets single attached sensor without address scanning
  ds.write(0xCC);
  
  // Step 3: Send Convert Temperature command (0x44)
  ds.write(0x44, 1);

  // Allow time for 12-bit ADC conversion (750ms)
  delay(750);

  // Step 4: Issue second reset to read conversion result
  if (!ds.reset()) {
    return -127; // Lost sensor during conversion
  }

  // Step 5: Send Skip ROM (0xCC) followed by Read Scratchpad (0xBE)
  ds.write(0xCC);
  ds.write(0xBE);

  // Read 9 bytes of scratchpad RAM
  for (int i = 0; i < 9; i++) {
    data[i] = ds.read();
  }

  // Step 6: Validate CRC checksum on data packet
  if (OneWire::crc8(data, 8) != data[8]) {
    return -127; // Transmission checksum error
  }

  // Step 7: Calculate temperature in Celsius from raw bytes
  int16_t raw = (data[1] << 8) | data[0];
  byte cfg = (data[4] & 0x60);
  if (cfg == 0x00) raw = raw & ~7;       // 9-bit resolution
  else if (cfg == 0x20) raw = raw & ~3;  // 10-bit resolution
  else if (cfg == 0x40) raw = raw & ~1;  // 11-bit resolution

  float celsius = (float)raw / 16.0;
  return (int)celsius;
}

void broadcastTelemetry() {
  // Read temperature directly using 1-Wire Skip ROM protocol
  int tempC = readDS18B20SkipROM();

  // Stream each door status on its own clean line
  for (int i = 0; i < NUM_DOORS; i++) {
    Serial.print("DATA_STREAM:");
    Serial.print(doorNames[i]);
    if (lastStableState[i] == LOW) {
      Serial.println("_OPEN");
    } else {
      Serial.println("_CLOSED");
    }
  }

  // Stream digital DS18B20 temperature on its own clean line
  Serial.print("DATA_STREAM:TEMP:");
  Serial.println(tempC);
}