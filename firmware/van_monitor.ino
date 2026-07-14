/*
 * Van Status Telemetry System Arduino Firmware - One-By-One Streaming
 * Monitored Pins:
 * - D2: Front Left (FL) 
 * - D3: Front Right (FR) 
 * - D4: Rear Left (RL) 
 * - D5: Back Hatch (BACK) 
 * - A1: Cabin Temperature Sensor (LM35/TMP36) 
 * * Logic: Negative-switching (LOW = OPEN, HIGH = CLOSED).
 * Telemetry Broadcast Interval: 1000ms [cite: 2]
 */

#define PIN_FL 2 // [cite: 3]
#define PIN_FR 3 // [cite: 3]
#define PIN_RL 4 // [cite: 3]
#define PIN_BACK 5 // [cite: 3]
#define TEMP_PIN A1 // [cite: 3]

const int NUM_DOORS = 4; // [cite: 3]
const int doorPins[NUM_DOORS] = {PIN_FL, PIN_FR, PIN_RL, PIN_BACK}; // [cite: 3]
const char* doorNames[NUM_DOORS] = {"FL", "FR", "RL", "BACK"}; // [cite: 3]

int lastStableState[NUM_DOORS]; // [cite: 4]
int lastReadState[NUM_DOORS]; // [cite: 4]
unsigned long lastDebounceTime[NUM_DOORS]; // [cite: 4]
const unsigned long DEBOUNCE_DELAY = 40; // 40ms debounce limit [cite: 4]

unsigned long lastTelemetryTime = 0; // [cite: 5]
const unsigned long TELEMETRY_INTERVAL = 1000; // Stream full state every 1 second [cite: 5]

void setup() {
  Serial.begin(9600); // [cite: 6]
  
  for (int i = 0; i < NUM_DOORS; i++) { // [cite: 6]
    pinMode(doorPins[i], INPUT_PULLUP); // [cite: 6]
    
    int state = digitalRead(doorPins[i]); // [cite: 6]
    lastStableState[i] = state; // [cite: 7]
    lastReadState[i] = state; // [cite: 7]
    lastDebounceTime[i] = 0; // [cite: 7]
  }
}

void loop() {
  unsigned long now = millis(); // [cite: 8]
  bool stateChanged = false; // [cite: 8]
  
  // 1. Process Non-Blocking Debounce for Pin State Changes
  for (int i = 0; i < NUM_DOORS; i++) { // [cite: 8]
    int reading = digitalRead(doorPins[i]); // [cite: 8]
    
    if (reading != lastReadState[i]) { // [cite: 9]
      lastDebounceTime[i] = now; // [cite: 9]
      lastReadState[i] = reading; // [cite: 9]
    } // [cite: 10]
    
    if ((now - lastDebounceTime[i]) >= DEBOUNCE_DELAY) { // [cite: 10]
      if (reading != lastStableState[i]) { // [cite: 10]
        lastStableState[i] = reading; // [cite: 10]
        stateChanged = true; // Trigger immediate broadcast on state change [cite: 11]
      }
    }
  }

  // 2. Broadcast Telemetry (On Interval OR Instant State Change)
  if (stateChanged || (now - lastTelemetryTime >= TELEMETRY_INTERVAL)) { // [cite: 11]
    broadcastTelemetry();
    lastTelemetryTime = now; // [cite: 12]
  }
}

void broadcastTelemetry() {
  // Read and calculate Temperature
  int tempRaw = analogRead(TEMP_PIN); // [cite: 12]
  float voltageTemp = (tempRaw * 5.0) / 1023.0; // [cite: 13]
  float tempC = (voltageTemp - 0.5) * 100.0; // [cite: 13]
  
  // Stream each door state on its own clean line
  for (int i = 0; i < NUM_DOORS; i++) { // [cite: 15]
    Serial.print("DATA_STREAM:"); // Prefix keeps it compatible with your service
    Serial.print(doorNames[i]); // [cite: 15]
    if (lastStableState[i] == LOW) { // [cite: 16]
      Serial.println("_OPEN"); // Ends the line immediately
    } else {
      Serial.println("_CLOSED"); // Ends the line immediately [cite: 16]
    }
  }
  
  // Stream the temperature on its own clean line
  Serial.print("DATA_STREAM:TEMP:");
  Serial.println((int)tempC);
}