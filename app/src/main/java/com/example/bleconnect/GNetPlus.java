package com.example.bleconnect;

public class GNetPlus {
    public  int gNetPlusCRC16(byte[] bBuffer, int iOffset, int iLength) {
        int iPreset = 0xFFFF;
        int iPolynom = 0xA001;
        int iCRC = iPreset;
        int i, j;
        for (i = iOffset; i < (iOffset + iLength); i++) {
            iCRC ^= (((int) bBuffer[i]) & 0xFF);
            for (j = 0; j < 8; j++) {
                if ((iCRC & 1) == 1)
                    iCRC = (iCRC >> 1) ^ iPolynom;
                else
                    iCRC = (iCRC >> 1);

            }

        }
        return iCRC=iCRC & 0xFFFF;
    }
}
