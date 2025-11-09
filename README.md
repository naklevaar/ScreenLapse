🔒 ScreenLapse: Secure Digital Boundaries for Families
A robust parental control application built to enforce screen time limits with technical precision.


💡 Project Overview: The Solution to Screen Time Woes
ScreenLapse is designed to provide parents with a simple, secure, and effective way to manage their children's app usage.
Unlike lighter control apps, ScreenLapse uses advanced Android features to ensure time limits are strictly enforced and cannot be easily bypassed.
Feature AreaParent Goal SolvedEnforcementNo cheating. Limits are robust and circumventing the lock is difficult.SecuritySecure Access.
Only the parent can unlock the device.DataUnderstand Habits. Provides clear insights into where time is spent.PrivacyTrust.
No personal accounts, sign-ups, or data leaving the device.


Core Technical Highlights (The Code That Matters)
This project demonstrates strong proficiency in core and advanced Android development principles.
1. Robust Time Enforcement via Accessibility Service
The app relies on the Accessibility Service to accurately monitor which application is currently in the foreground. This implementation is crucial for the app's core function and showcases the ability to work with powerful, system-level APIs.
2. Secure Biometric (Fingerprint) Authentication
To ensure the child cannot unlock the app, the parent's section is protected by BiometricPrompt. This provides a modern, secure, and frictionless way for the parent to gain access.
3. Usage Data & Persistence
Daily and weekly usage statistics are tracked using a local database solution to ensure all data is private and remains on the user's device.
Database: Room Persistence Library / SQLite
4. Non-Circumvention Lock Mechanism
When the timer expires, the app launches a full-screen black overlay that prevents re-entry into the locked application.
 It specifically handles system events like the Back key to ensure the lock cannot be broken simply by pressing "Back."


🚀 Getting Started (Build and Run)
This project is built using the industry-standard tools: Kotlin and Android Studio.
Prerequisites
Android Studio IDE (Latest Stable Version)
Android SDK API 30+ (Required for modern Biometric and Accessibility implementations)
A physical Android device with Fingerprint Authentication enabled for full testing.


Installation & Setup
Clone the repository:
Bash
git clone https://github.com/naklevaar/ScreenLapse.git 
Import into Android Studio:
Open Android Studio and select File > Open.
Navigate to and select the cloned ScreenLapse-Android-App directory.
Run on Device:
Connect your physical Android device.
Select the device in the target selector and click the Run button.
Important: The app will prompt you to grant Accessibility Service Permission on first run. This step is mandatory for the app's core functionality.


📱 App Demonstration
<img width="512" height="512" alt="ic_launcher-playstore" src="https://github.com/user-attachments/assets/b6d1fd24-1805-405c-a899-2b27ce918b52" />
[Screenshot_ScreenLapse](https://github.com/user-attachments/assets/643336b2-b8ee-4f27-9798-050a48e2b157)
[Screenshot_ScreenLapse](https://github.com/user-attachments/assets/032cfb5b-b2f3-4937-b00e-ec7a110f553e)


👨‍💻 My Story: Code, Passion, and a Potato PC
This entire project was developed during my 3rd year of BCA, built on a challenging hardware setup.
I faced a significant hurdle with the Google Play Store's new "closed testing" requirements.
Rather than abandoning the project, I chose to showcase my complete work and full source code here on GitHub.
This demonstrates my ability to not only write complex Android code but also to navigate real-world distribution challenges.
Developed by: [Affan Qureshi] - Third Year BCA Student
