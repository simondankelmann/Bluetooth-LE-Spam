# Bluetooth LE Spam

This Project is about using the built in Bluetooth Low Energy (BLE) functionality of Android phones to generate Phantom Bluetooth Device Advertisements like known for example from the Flipper Zero. There are other apps that offer similar functionality, the goal of this App is to make it more convenient and easy to use.

> **_NOTE:_**  This Project is in a very early State.  Contribution by anyone is Welcome.

## Functionality
### Google Fast Pair
This App can spoof BLE Advertisers that pretend to make use of the Google Fast Pair Service resulting in a flood of unwanted popups on the receiving Device. 

More about the Google Fast Pair Service can be found [here](https://developers.google.com/nearby/fast-pair/landing-page)

## Range
Since the official Bluetooth Low Energy provided by Googles Android SDK lets you set the TX Powerlevel and also include it in the Advertisers Payload but not modify the actually transmitted Bytevalue in the Payload, the Range of the Fast Pair functionality is rather low. The receiving Devices calculates the Transmitters Proximity using the actual received Signal Strength and the transmitted Byte in the Payload which contains the Tx Powerlevel the Transmitter used. Since this can be modified by Devices like the Flipper Zero, their range is much higher.

## Credit
- [mh from mobile-hacker.com](https://www.mobile-hacker.com/author/boni11/) for the [Article / Guideline](https://www.mobile-hacker.com/2023/09/07/spoof-ios-devices-with-bluetooth-pairing-messages-using-android/) about using the nRF Connect App to Spoof iOS Devices

- [Willy-JL](https://github.com/Willy-JL) , [ECTO-1A](https://github.com/ECTO-1A) , [Spooks4567](https://github.com/Spooks4576) for their contribution in the BLE Spam App on the Flipper Zero

- [FuriousMAC](https://github.com/furiousMAC) and [Hexway](https://github.com/hexway) for their prior researches

- And anyone else who was involved in prior researches and publications about this topic. 


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


