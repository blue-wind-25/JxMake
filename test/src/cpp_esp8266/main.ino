#include <ArduinoOTA.h>
#include <ESP8266WebServer.h>
#include <ESP8266WiFi.h>
#include <LittleFS.h>
#include <Ticker.h>

#include <Adafruit_NeoPixel.h> // It is not actually used; include this file only for compilation test
#include <Adafruit_SSD1306.h>  // It is not actually used; include this file only for compilation test


#ifndef USE_LITTLEFS
    #define SelectedFS       SPIFFS
    #define SelectedFSConfig SPIFFSConfig
#else
    #define SelectedFS       LittleFS
    #define SelectedFSConfig LittleFSConfig
#endif


static constexpr const char*      apSSID      = "NodeMCU Generic OTA";
static constexpr const char*      apPassword  = "*GenOTA@NodeMCU!";

static constexpr const char*      otaHostname = "nodemcu";
static constexpr const int        otaPort     = 8266;
static constexpr const char*      otaPassword = "ota$*";

static           ESP8266WebServer webServer(80);


#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wdeprecated-declarations"

void setup()
{
    // Initialize LED
    static Ticker toggle;

    pinMode(LED_BUILTIN, OUTPUT);
    digitalWrite(LED_BUILTIN, LOW);

    toggle.attach(0.3, []() {
          digitalWrite( LED_BUILTIN, !digitalRead(LED_BUILTIN) );
    } );

    // Initialize serial port
    Serial.begin(9600);
    Serial.println();
    Serial.println();

    // Initialize file system
    Serial.println("Mounting file system ...");

    SelectedFSConfig fsConfig;
    SelectedFS.setConfig(fsConfig);

    if( SelectedFS.begin() ) Serial.println("Done\n");

    // Initialize access point
    Serial.println("Configuring access point ...");

    WiFi.persistent      (false);
    WiFi.softAPdisconnect(true );
    WiFi.disconnect      (true );
    delay(500);

    uint8_t macAddress[] = { 0x1A, 0xFE, 0x34, 0xC2, 0x32, 0xA9 };
    wifi_set_macaddr(SOFTAP_IF, macAddress);

    WiFi.hostname    ( otaHostname                                                                           );
    WiFi.setSleepMode( WIFI_NONE_SLEEP                                                                       );
    WiFi.mode        ( WIFI_AP                                                                               );
    WiFi.softAPConfig( IPAddress(192, 168, 0, 100), IPAddress(192, 168, 0, 100), IPAddress(255, 255, 255, 0) );
    WiFi.softAP      ( apSSID, apPassword, 1, 4                                                              );

    delay(500);

    const IPAddress myIP = WiFi.softAPIP();
    Serial.print("The access point IP address is ");
    Serial.println(myIP);
    Serial.println();

    // Initialize OTA
    ArduinoOTA.onStart( []() {
        toggle.detach();
        Serial.println(
            ( ArduinoOTA.getCommand() == U_FLASH )
            ? "Start updating sketch"
            : "Start updating filesystem"
        );
    } );

    ArduinoOTA.onEnd( []() {
        Serial.println("\nEnd\n");
    } );

    ArduinoOTA.onProgress( [](unsigned int progress, unsigned int /*total*/) {
        static int cnt = 0;
        if(progress == 0 || cnt > 3) cnt = 0;
        if(!cnt++) Serial.print('.');
    } );

    ArduinoOTA.onError( [](ota_error_t error) {
        Serial.printf("Error[%u]: ", error);
             if(error == OTA_AUTH_ERROR   ) Serial.println("Auth Failed"   );
        else if(error == OTA_BEGIN_ERROR  ) Serial.println("Begin Failed"  );
        else if(error == OTA_CONNECT_ERROR) Serial.println("Connect Failed");
        else if(error == OTA_RECEIVE_ERROR) Serial.println("Receive Failed");
        else if(error == OTA_END_ERROR    ) Serial.println("End Failed"    );
    } );

    ArduinoOTA.setHostname(otaHostname);
    ArduinoOTA.setPort    (otaPort    );
    ArduinoOTA.setPassword(otaPassword);

    ArduinoOTA.begin();

    // Initialize web server
    Serial.println("Initializing web server ...");

    webServer.serveStatic("/", SelectedFS, "/test.html");
    webServer.begin();

    Serial.println("Done\n");
}

#pragma GCC diagnostic pop


void loop()
{
    ArduinoOTA.handle();
    webServer.handleClient();
}
