#if !defined(_CRC_H_)
#define _CRC_H_

#define PRESET_VALUE 0xFFFF
#define POLYNOMIAL 0x8408

uint16_t getCrc16(uint16_t crc, byte *data, int start, int len) {
  for (int i = 0; i < len; i++)
  {
    crc = crc ^ (data[start + i] & 0xFF);
    for (int j = 0; j < 8; j++)
    {
      if ((crc & 1) != 0)
      {
        crc = (crc >> 1) ^ POLYNOMIAL;
      }
      else
      {
        crc = (crc >> 1);
      }
    }
  }
  return crc;
}

#endif

