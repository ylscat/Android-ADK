#if !defined(_CMD_H_)
#define _CMD_H_

#define CMD_NONE 0
#define CMD_QUERY 0x70
#define CMD_OPEN 0x20
#define CMD_RF_SCAN 0x30
#define CMD_RF_STOP 0x40
#define CMD_RF_READ_CONF 0x50
#define CMD_RF_WRITE_CONF 0x60

#define RESP_MASK_FAIL 0x80

const uint8_t CMD_STATE[] = {0x04, 0xFF, 0x21, 0x19, 0x95};
const uint8_t CMD_SCAN_1[] = {0x09, 0xFF, 0x1, 0xA, 0, 0, 0x80, 20, 0xC0, 0xCC};
const uint8_t CMD_SCAN_2[] = {0x09, 0xFF, 0x1, 0xA, 0, 0, 0x81, 20, 0x18, 0xD5};
const uint8_t CMD_GPIO_OFF[] = {0x05, 0xFF, 0x46, 0x00, 0x33, 0xB9 };
const uint8_t CMD_GPIO_ON[] = {0x05, 0xFF, 0x46, 0x01, 0xBA, 0xA8 };
const uint8_t CMD_SWITCH[] = {0x05, 0xFF, 0x45, 0x14, 0xFE, 0xC5}; 

#endif
