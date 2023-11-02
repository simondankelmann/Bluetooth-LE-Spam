# Bluetooth LE Spam

This project focuses on utilizing the built-in Bluetooth Low Energy (BLE) functionality of Android smartphones to create Phantom Bluetooth Device Advertisements, similar to what is known, for instance, in the case of the Flipper Zero. While there are other apps available that provide similar functionality, the objective of this app is to enhance convenience and user-friendliness in the process.

> **_NOTE:_**  This project is in its early stages of development. Contributions from anyone are welcome.

## Functionality
### Google Fast Pair
This app is capable of spoofing BLE advertisers that mimic the usage of the Google Fast Pair Service, leading to an influx of unwanted pop-up notifications on the receiving device.

For additional information about the Google Fast Pair Service, you can find it [here](https://developers.google.com/nearby/fast-pair/landing-page)

### Microsoft Swift Pair
This app can spoof BLE advertisers that mimic devices supporting the Microsoft Swift Pairing Service. If Swift Pair notifications are enabled on a nearby Windows 10 (or later) device, it will receive a flood of notifications regarding nearby devices.

For additional information about the Microsoft Swift Pair Service, you can find it [here](https://learn.microsoft.com/en-us/windows-hardware/design/component-guidelines/bluetooth-swift-pair)

## Range
Since the official Bluetooth Low Energy API provided by Google's Android SDK allows you to set the TX Power level and include it in the advertiser's payload, but doesn't permit direct modification of the byte values actually transmitted in the payload, the range of the Fast Pair functionality is somewhat limited. The receiving devices calculate the transmitter's proximity based on the actual received signal strength and the transmitted byte in the payload, which contains the TX Power level the transmitter used. However, devices like the Flipper Zero have the capability to modify this byte, significantly extending their range.

## Installation
You can clone the repository and open it in Android Studio to install the app, or simply use the installable APK files from the [Release Section](https://github.com/simondankelmann/Bluetooth-LE-Spam/releases)

## Credit
- [mh from mobile-hacker.com](https://www.mobile-hacker.com/author/boni11/) for the [Article / Guideline](https://www.mobile-hacker.com/2023/09/07/spoof-ios-devices-with-bluetooth-pairing-messages-using-android/) about using the nRF Connect App to Spoof iOS Devices

- [Willy-JL](https://github.com/Willy-JL) , [ECTO-1A](https://github.com/ECTO-1A) , [Spooks4567](https://github.com/Spooks4576) for their contribution in the BLE Spam App on the Flipper Zero

- [FuriousMAC](https://github.com/furiousMAC) and [Hexway](https://github.com/hexway) for their prior researches

- And special thanks to anyone else who has been involved in prior research and publications related to this topic.

## Screenshots
![](./Assets/Screenshots/ScreenshotFastPairing.jpeg)
![](./Assets/Screenshots/ScreenshotSwiftPair.jpeg)

## Disclaimer
Disclaimer for Bluetooth Low Energy Protocol Investigation Repository

This repository contains code for the investigation and experimentation of the Bluetooth Low Energy (BLE) protocol. Please be aware of the following disclaimers before using or contributing to this repository:

1. Purpose: The code and information provided in this repository are intended for educational and research purposes. It is not intended for any malicious or harmful activities.

2. Legal Compliance: Users are responsible for ensuring that their use of the code and information in this repository complies with all applicable laws and regulations, including those governing wireless communication and intellectual property rights.

3. No Warranty: The code and information provided in this repository are provided "as is" without any warranties, expressed or implied. The authors and contributors are not responsible for any consequences resulting from the use of this code.

4. Risks: Experimenting with BLE protocols can have potential security and privacy implications. Users should exercise caution and use this code responsibly, respecting the privacy and security of devices and systems.

5. Contribution Guidelines: If you contribute to this repository, ensure that your contributions are in compliance with the project's goals and the repository's license. By contributing, you agree to license your contributions under the same license as this repository.

6. Support: This repository is not maintained for production use. The authors and contributors may not provide support or updates regularly.

By using and contributing to this repository, you agree to these disclaimers and guidelines. If you do not agree, please refrain from using or contributing to this repository.

For any questions or concerns, please contact the repository maintainers.


