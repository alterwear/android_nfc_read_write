# Android NFC read and write example

http://www.codexpedia.com/android/android-nfc-read-and-write-example/

## 2019-2-27 Notes
- Android reference links [MifareUltralight](https://developer.android.com/reference/android/nfc/tech/MifareUltralight)  [TagTechnology](https://developer.android.com/reference/android/nfc/tech/TagTechnology.html) 
- [Helpful NFC/Android tutorial](https://code.tutsplus.com/tutorials/reading-nfc-tags-with-android--mobile-17278)
- Links for learning a lil more about NFC tags
  - [nfc.today: all about nfc tags](https://nfc.today/advice/nfc-tags)
  - NDEF (NFC data exchange format) encoding. This universal way of storing NFC information means that almost any NFC enabled device will be able to read and understand the data and what type of data it is.
  - [nfc.today article: how to encode nfc tags](https://nfc.today/learn/how-to-encode-nfc-tags)
  - [adafruit's basic overview of MIFARE](https://learn.adafruit.com/adafruit-pn532-rfid-nfc/mifare)
  - [adafruit's basic overview of NDEF](https://learn.adafruit.com/adafruit-pn532-rfid-nfc/ndef)
  - "NFC tags... can be configured as NDEF tags, and data written to them by one NFC device (NDEF Records) can be understood and accessed by any other NDEF compatible device."
    - idk why it's necessary to add a case to catch the tags as MiFareUltraLight tags made things work, if all things follow the universal NDEF format already? 

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
