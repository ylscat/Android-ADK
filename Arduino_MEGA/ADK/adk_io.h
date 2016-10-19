#if !defined(_ADK_IO_H_)
#define _ADK_IO_H_

#include "cmd.h"
#include<adk.h>

//#define MOCK 1

USB Usb;
#define LED1 18
#define LED2 19

#define ADK_BUF_SIZE 64

bool adk_ready = false;

/*ADK adk(&Usb, "TKJElectronics", // Manufacturer Name
              "ArduinoBlinkLED", // Model Name
              "Example sketch for the USB Host Shield", // Description (user-visible string)
              "1.0", // Version
              "http://www.tkjelectronics.dk/uploads/ArduinoBlinkLED.apk", // URL (web page to visit if no installed apps support the accessory)
              "123456789"); // Serial Number (optional)*/
ADK adk(&Usb, "Fangstar", // Manufacturer Name
        "KeyStore ADK", // Model Name
        "IO Controller", // Description (user-visible string)
        "1.0", // Version
        "https://www.fangstar.net", // URL (web page to visit if no installed apps support the accessory)
        "123456789"); // Serial Number (optional)

void adk_setup(void) {
  pinMode(LED1, OUTPUT);
  digitalWrite(LED1, LOW);

  pinMode(LED2, OUTPUT);
  digitalWrite(LED2, LOW);

#if !defined(MOCK)
  if (Usb.Init() == -1) {
    Serial.println("Usb Init fail");
    digitalWrite(LED_BUILTIN, HIGH);
    while (1); // halt
  }
#endif
}

void adk_routine(void) {
#if !defined(MOCK)
  Usb.Task();
   if (!adk.isReady()) {
    if (adk_ready) {
      adk_ready = false;
      digitalWrite(LED1, LOW);
      digitalWrite(LED2, LOW);
    }
  }
  else {
    if (!adk_ready) {
      adk_ready = true;
      digitalWrite(LED1, HIGH);
      digitalWrite(LED2, HIGH);
    }
  }
#endif
}

int response(uint8_t *data, uint16_t len)
{
#if !defined(MOCK)
  if(!adk_ready)
    return 0;
  uint8_t rcode = adk.SndData(len, data);
  if(rcode) {
    Serial.print("Send Err ");
    Serial.println(rcode, HEX);
    digitalWrite(LED_BUILTIN, HIGH);
  }
  else {
    digitalWrite(LED_BUILTIN, LOW);
  }
  return rcode;
#else
  for(int i = 0; i < len; i++)
    Serial.write(data[i]);
  return 0;
#endif
}

uint8_t receive()
{
#if !defined(MOCK)
  if(!adk_ready)
    return CMD_NONE;
  static uint8_t adk_buf[ADK_BUF_SIZE];
  uint16_t len = ADK_BUF_SIZE;
  uint8_t rcode = adk.RcvData(&len, adk_buf);
  if (rcode) {
    if (rcode != hrNAK) {
      Serial.print("Recv Err ");
      Serial.println(rcode, HEX);
      digitalWrite(LED_BUILTIN, HIGH);
    }
    return CMD_NONE;
  }
  else {
    digitalWrite(LED_BUILTIN, LOW);
    if (len > 0) {
      return adk_buf[len - 1];
    }
  }
#else
  uint8_t n = Serial.available();
  if(n > 0) {
    uint8_t cmd;
    while(n-- > 0)
      cmd = Serial.read();
    return cmd;
  }
  else {
    return CMD_NONE;
  }
#endif
}

#endif
