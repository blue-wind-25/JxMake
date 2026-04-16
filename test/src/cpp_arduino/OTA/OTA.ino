#include <ArduinoOTA.h>


void setup()
{
    // Initialize serial
    Serial.begin(115200);

    // Set the LED pin to output
    pinMode(LED_BUILTIN, OUTPUT);

    // Do not safe WiFi setting to flash
    WiFi.persistent(false);

    // Turn off all WiFi modes
    WiFi.softAPdisconnect(true);
    WiFi.disconnect(true);

    // Configure it as a WiFi access point
    const char*   hostname      = "nodemcu";
    const char*   ssid          = "NodeMCU Generic OTA";
    const char*   wifipswd      = "*GenOTA@NodeMCU!";
    const char*   otapswd       = "ota$*";
    const uint8_t macAddress[6] = { 0x1A, 0xFE, 0x34, 0xC2, 0x32, 0xA9 };
    const uint8_t localIP   [4] = { 192, 168,   0, 100 };
    const uint8_t gateway   [4] = { 192, 168,   0, 100 };
    const uint8_t subnetMask[4] = { 255, 255, 255,   0 };

    WiFi.hostname    ( hostname                                                      );
    WiFi.setSleepMode( WIFI_NONE_SLEEP                                               );
    WiFi.mode        ( WIFI_AP                                                       );
    WiFi.softAPConfig( IPAddress(localIP), IPAddress(gateway), IPAddress(subnetMask) );
    WiFi.softAP      ( ssid, wifipswd, 1, false, 4                                   );

    // Configure and enable OTA programming
    ArduinoOTA.onStart   ( [](                          ) { Serial.println("Start"); } );
    ArduinoOTA.onProgress( [](unsigned int, unsigned int) {                          } );
    ArduinoOTA.onEnd     ( [](                          ) { Serial.println("End"  ); } );
    ArduinoOTA.onError   ( [](ota_error_t               ) { Serial.println("Error"); } );

    ArduinoOTA.setHostname(hostname);
    ArduinoOTA.setPort    (8266    );
    ArduinoOTA.setPassword(otapswd );
    ArduinoOTA.begin      (        );
}


void loop()
{
    // Handle OTA
    ArduinoOTA.handle();

    // Blink the LED
    static bool          ledState       = false;
    static unsigned long previousMillis = 0;
    const  unsigned long currentMillis  = millis();

    if(currentMillis - previousMillis > 500) {
        ledState       = !ledState;
        previousMillis = currentMillis;
        digitalWrite(LED_BUILTIN, ledState ? HIGH : LOW);
    }
}
