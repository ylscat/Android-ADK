#include<adk.h>

#define LED1 18
#define LED2 19

#define BUF_SIZE 64

USB Usb;

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

bool adk_ready = false;
uint32_t timer;
uint8_t send_buf[BUF_SIZE];
uint8_t send_buf_len = 0;


void setup() {
  Serial.begin(115200);
  Serial3.begin(57600);

  pinMode(LED_BUILTIN, OUTPUT);
  digitalWrite(LED_BUILTIN, LOW);

  pinMode(LED1, OUTPUT);
  digitalWrite(LED1, LOW);

  pinMode(LED2, OUTPUT);
  digitalWrite(LED2, LOW);

  if (Usb.Init() == -1) {
    Serial.println("Usb Init fail");
    digitalWrite(LED_BUILTIN, HIGH);
    while (1); // halt
  }
  uint32_t a = 100;
  uint32_t b = 0xFFFFFF00;
  Serial.println(a - b, DEC);
}

inline void check_adk_ready() {
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
}

void loop() {
  uint16_t len;
  uint8_t buf[BUF_SIZE];
  uint8_t rcode;

  Usb.Task();
  check_adk_ready();
  if (!adk_ready)
    return;

  len = Serial3.available();
  if (len) {
    if (len + send_buf_len > BUF_SIZE) {
      len = BUF_SIZE - send_buf_len;
    }

    uint8_t *p = send_buf + send_buf_len;
    for (int i = 0; i < len; i++)
      *p++ = Serial3.read();
    send_buf_len += len;
    if(send_buf_len == 1)
      timer = millis();
  }

  if (send_buf_len > 0) {
    if (millis() - timer < 3) {
      Serial.println("Skip");
      goto AFTER_SEND;
    }
    uint8_t retry = 0;
SEND:
    rcode = adk.SndData(send_buf_len, send_buf);
    if (rcode) {
      if (rcode == hrNAK) {
        retry++;
        goto SEND;
      }
      else {
        Serial.print("Send Err "); Serial.println(rcode);
      }
    }
    else {
      timer = millis();
      Serial.print(timer, DEC);
      Serial.print(" ");
      Serial.println(send_buf_len, DEC);
      delay(send_buf_len/10);
      send_buf_len = 0;
      if(retry) {
        Serial.print("Retry:");
        Serial.println(retry, DEC);
      }

    }
  }
AFTER_SEND:

  len = BUF_SIZE;
  rcode = adk.RcvData(&len, buf);
  if (rcode) {
    if (rcode != hrNAK) {
      Serial.print("Recv Err ");
      Serial.println(rcode, HEX);
    }
  }
  else {
    if (len > 0) {
      Serial3.write(buf, len);
    }
  }
}
