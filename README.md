# Android NFC read and write example

http://www.codexpedia.com/android/android-nfc-read-and-write-example/

## 2019-2-27 Notes
- Android reference links [MifareUltralight](https://developer.android.com/reference/android/nfc/tech/MifareUltralight) [TagTechnology](https://developer.android.com/reference/android/nfc/tech/TagTechnology.html) 

## 2019-01-09 Notes
- Unable to read from the two formatable NFC tags.
- prints out garbage (notes in the latest commit).
- also still not sure why the two tags are different... may need to read about different classes of mifare tags [link](https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/nfc/tech/MifareUltralight.java#39)

## 2018-12-10 Notes
Fixed below crash:
- For some reason two of the tags (the one w/ nothing attached, but pins soldered closed and the one in resin) are formatable. Adding a case to catch them as MiFareUltralight tags made things work, not really sure why that was necessary though.


1. App crashes. ndef is null after trying to .getTag and passing in myTAg.
2. round tag is: TAG: Tech [android.nfc.tech.NfcA, android.nfc.tech.MifareUltralight, android.nfc.tech.NdefFormatable]
3. Resined tag is: TAG: Tech [android.nfc.tech.NfcA, android.nfc.tech.MifareUltralight, android.nfc.tech.NdefFormatable]
4. WORKS -- Soldered tag is: TAG: Tech [android.nfc.tech.NfcA, android.nfc.tech.MifareUltralight, android.nfc.tech.Ndef]
