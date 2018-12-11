# Android NFC read and write example

http://www.codexpedia.com/android/android-nfc-read-and-write-example/

## 2018-12-10 Notes
Fixed below crash:

1. App crashes. ndef is null after trying to .getTag and passing in myTAg.
2. round tag is: TAG: Tech [android.nfc.tech.NfcA, android.nfc.tech.MifareUltralight, android.nfc.tech.NdefFormatable]
3. Resined tag is: TAG: Tech [android.nfc.tech.NfcA, android.nfc.tech.MifareUltralight, android.nfc.tech.NdefFormatable]
4. WORKS -- Soldered tag is: TAG: Tech [android.nfc.tech.NfcA, android.nfc.tech.MifareUltralight, android.nfc.tech.Ndef]
