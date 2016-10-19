#include <avr/pgmspace.h>
#include "crc.h"
#include "cmd.h"
#include "adk_io.h"

#define OUT1 4
#define OUT2 6
#define IN1 2
#define IN2 3

#define MSG_SIZE 64
#define MSG_SYM_A '#'
#define MSG_SYM_B '\n'
uint8_t msg[MSG_SIZE];

#define BUF_SIZE 128
uint8_t buf[BUF_SIZE];
uint16_t buf_start = 0, buf_length = 0;

#define STATE_BIT_MASK_RF 1
#define STATE_BIT_MASK_OUT 2
#define STATE_BIT_MASK_IN1 4
#define STATE_BIT_MASK_IN2 8
uint8_t status = 0;
uint8_t rf_cmd = CMD_NONE;

#define RF_TIMEOUT 100
#define RF_TIMEOUT_ENTIRE 1000
uint32_t rf_time;

#define OPEN_PULSE_DURATION 500 //500ms
uint32_t open_at = 0;

#define MAX_RESPONSE_RETRY 3

void read_signals(void);
void read_rf(void);
void write_rf(uint8_t *cmd);
int response_cmd(uint8_t cmd, uint8_t *data, uint8_t len);

uint8_t gpio = 0;

void setup() {
  Serial.begin(115200);
  Serial3.begin(57600);

  pinMode(LED_BUILTIN, OUTPUT);
  digitalWrite(LED_BUILTIN, LOW);

  pinMode(OUT1, OUTPUT);
  digitalWrite(OUT1, LOW);

  pinMode(OUT2, OUTPUT);
  digitalWrite(OUT2, LOW);

  pinMode(IN1, INPUT_PULLUP);
  pinMode(IN2, INPUT_PULLUP);

  msg[0] = MSG_SYM_A;

  adk_setup();
}

void loop() {
  adk_routine();
  
  uint8_t cmd = receive();

  switch (cmd & 0xF0) {
    case CMD_NONE:
      break;

    case CMD_QUERY:
      read_signals();
      response_cmd(cmd, &status, 1);
      break;

    case CMD_OPEN:
      if (open_at == 0) {
        digitalWrite(OUT1, HIGH);
        digitalWrite(OUT2, HIGH);
        status |= STATE_BIT_MASK_OUT;
        open_at = millis();
      }
      break;

    case CMD_RF_SCAN:
      if ((status & STATE_BIT_MASK_RF) == 0) {
        status |= STATE_BIT_MASK_RF;
        rf_cmd = CMD_RF_SCAN;
        write_rf(CMD_SCAN_1);
      }
      else {
        uint8_t d[] = {status, rf_cmd};
        response_cmd(CMD_RF_SCAN | RESP_MASK_FAIL, d, 2);
      }
      break;

    case CMD_RF_STOP:
      if ((status & STATE_BIT_MASK_RF) != 0) {
        status &= ~STATE_BIT_MASK_RF;
        rf_cmd = CMD_NONE;
      }
      response_cmd(CMD_RF_STOP, &status, 1);
      break;

    case CMD_RF_READ_CONF:
      if ((status & STATE_BIT_MASK_RF) == 0) {
        status |= STATE_BIT_MASK_RF;
        rf_cmd = CMD_RF_READ_CONF;
        write_rf(CMD_STATE);
      }
      else {
        uint8_t d[] = {status, rf_cmd};
        response_cmd(CMD_RF_READ_CONF | RESP_MASK_FAIL, d, 2);
      }
      break;

    case CMD_RF_WRITE_CONF:
      if ((status & STATE_BIT_MASK_RF) == 0) {
        status |= STATE_BIT_MASK_RF;
        rf_cmd = CMD_RF_WRITE_CONF;
        write_rf(CMD_GPIO_OFF);
      }
      else {
        uint8_t d[] = {status, rf_cmd};
        response_cmd(CMD_RF_WRITE_CONF | RESP_MASK_FAIL, d, 2);
      }
      break;
  }

  if (open_at != 0 && millis() - open_at > 500) {
    digitalWrite(OUT1, LOW);
    digitalWrite(OUT2, LOW);
    status &= ~STATE_BIT_MASK_OUT;
    open_at = 0;
    response_cmd(CMD_OPEN, &status, 1);
  }

  read_rf();
}

void read_signals(void) {
  if (digitalRead(IN1) == HIGH)
    status &= ~STATE_BIT_MASK_IN1;
  else
    status |= STATE_BIT_MASK_IN1;

  if (digitalRead(IN2) == HIGH)
    status &= ~STATE_BIT_MASK_IN2;
  else
    status |= STATE_BIT_MASK_IN2;
}

int read_response(void) {
  if(buf_length == 0)
    return 0;
  uint8_t len = buf[buf_start] + 1;
  if (len < 6) //length error
    return -1;

  if (len > buf_length) //pending
    return 0;

  uint8_t index = buf_start + 1;
  if (buf[index] != 0) //address error
    return -2;

  //check illegal length
  uint16_t crc = getCrc16(PRESET_VALUE, buf, buf_start, len);
  if (crc != 0) //crc error
    return -3;
  else {
    return len;
  }
}

void rf_response(uint16_t st) {
  if (rf_cmd == CMD_NONE)
    return;
  uint8_t len = buf[st];
  uint8_t re_cmd = buf[st + 2];
  uint8_t state = buf[st + 3];

  switch(rf_cmd & 0xF0) {
    case CMD_RF_READ_CONF:
      if(re_cmd == 0x21) {
        if(state == 0)
          response_cmd(rf_cmd, buf + 10, 1);
        else {
          uint8_t d[] = {0x20, state};
          response_cmd(rf_cmd|RESP_MASK_FAIL, d, 2);
        }
        status &= ~STATE_BIT_MASK_RF;
        rf_cmd = CMD_NONE;
      }
      break;
    case CMD_RF_SCAN:
      if(re_cmd == 1) {
        uint8_t ant, num;
        switch(state) {
          case 1:
            ant = buf[st + 4];
            num = buf[st + 5];
            //response_cmd(rf_cmd, &num, 1);
            if(ant == 1) {
              write_rf(CMD_SCAN_2);
            }
            else {
              write_rf(CMD_SCAN_1);
            }
          break;
          case 3:
            response_cmd(rf_cmd, buf + 7, buf[6]);
            rf_time = millis();         
          break;
        }
      }
      break;
    case CMD_RF_WRITE_CONF:
      if(re_cmd == 0x46) {
        if(state == 0)
          response_cmd(rf_cmd, &state, 1);
        else {
          uint8_t d[] = {0x20, state};
          response_cmd(rf_cmd|RESP_MASK_FAIL, d, 2);
        }
        status &= ~STATE_BIT_MASK_RF;
        rf_cmd = CMD_NONE;
      }
      break;
    default:
      Serial.print("U ");
      Serial.println(re_cmd, HEX);
      break;
  }
}

void read_rf(void) {
  uint8_t n = Serial3.available();
  if (n == 0) {
    if (rf_cmd == CMD_RF_SCAN)
      return;
    if (rf_cmd != CMD_NONE) {
      //check read timeout
      uint32_t d = millis() - rf_time;
      if (buf_length == 0 && d > RF_TIMEOUT) {
        uint8_t flag_timeout = 0x10;
        response_cmd(rf_cmd | RESP_MASK_FAIL, &flag_timeout, 1);
        status &= ~STATE_BIT_MASK_RF;
        rf_cmd = CMD_NONE;
      }
      else if (d > RF_TIMEOUT_ENTIRE) {
        uint8_t flag_timeout = 0x20;
        response_cmd(rf_cmd | RESP_MASK_FAIL, &flag_timeout, 1);
        status &= ~STATE_BIT_MASK_RF;
        rf_cmd = CMD_NONE;
      }
    }
    return;
  }

  uint16_t end = buf_start + buf_length;

  //avoid overflow, should not happen
  if (end + n > BUF_SIZE) {
    n = BUF_SIZE - end;
  }

  for (int i = 0; i < n; i++) {
    buf[end++] = Serial3.read();
    buf_length++;
  }

  for (int r = read_response(); r != 0; r = read_response()) {
    if (r < 0) {
      buf_length--;
      buf_start++;
    }
    else {
      rf_response(buf_start);
      buf_start += r;
      buf_length -= r;
    }
  }

  if(buf_length > 0 && buf_start > 0) {
    memcpy(buf, buf + buf_start, buf_length);
    buf_start = 0;
  }
}

void write_rf(const uint8_t *cmd) {
  /*
  uint8_t len = pgm_read_byte_near(cmd);
  Serial3.write(len);
  for (int i = 1; i <= len; i++) {
    Serial3.write(pgm_read_byte_near(cmd + i));
  }*/
  uint8_t len = *cmd;
  Serial3.write(len);
  for (int i = 1; i <= len; i++) {
    Serial3.write(*(cmd + i));
  }
  rf_time = millis();
}

int response_cmd(uint8_t cmd, uint8_t *data, uint8_t len) {
  msg[1] = len + 1;
  msg[2] = cmd;
  memcpy(msg + 3, data, len);
  msg[len + 3] = MSG_SYM_B;
  uint16_t retry;
  for (retry = 0; retry < MAX_RESPONSE_RETRY; retry++) {
    if (response(msg, len + 4) == 0) {
      return 0;
    }
  }

  return retry;
}
/*
  int push_response(uint8_t cmd, uint8_t *data, uint8_t data_len)
  {
  if(size == MSG_DEPTH)
    return -1;
  uint8_t end = start + size;
  if(end >= MSG_DEPTH)
    end = end%MSG_DEPTH;
  uint8_t *p = msg[end];
  p[0] = data_len + 1;
  p[1] = cmd;
  memcpy(p + 2, data, data_len);
  size += 1;
  return 0;
  }

  void response_all(void) {
  uint8_t retry;
  for(int i = size; i > 0; i--) {
    retry = 0;
    uint8_t *p = msg[start];
    while(retry < MAX_RESPONSE_RETRY) {
      if(response(p + 1, *p))
        retry++;
      else
        break;
    }
    if(retry < MAX_RESPONSE_RETRY) {
      start++;
      size--;
    } else
      break;
  }
  }
*/
