# CMS Executive Suite 💼

**CMS Executive Suite** is a professional-grade Android application designed for Cyber Cafe and small business management. Built with **Jetpack Compose** and **Firebase**, it provides business owners (Admins) with high-level intelligence while giving staff a streamlined interface for daily operations.

![License](https://img.shields.io/badge/License-MIT-blue.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple.svg)
![Compose](https://img.shields.io/badge/Compose-M3-green.svg)
![Firebase](https://img.shields.io/badge/Firebase-Firestore-orange.svg)

---

## 🌟 Key Features

### 🛡️ Secure Access & Roles
- **Biometric Security**: Integrated app-lock with 60-second grace period.
- **Role-Based Access Control (RBAC)**:
    - **Admin (Owner)**: Full visibility into profit trends, staff performance, capital investments, and audit logs.
    - **Staff**: Dedicated workflow for shift management and transaction recording. Sensitive data is hidden.

### 📊 Executive Intelligence
- **Real-time Dashboard**: Live profit pulse and monthly growth charts.
- **Executive Audit Logic**: Financial performance is calculated based on **Cash Reconciliation** (Opening vs. Closing balances) to ensure zero-leakage accounting.
- **Reporting Suite**: Detailed reports for Daily, Weekly, Monthly, and Annual periods.
- **Export Capabilities**: Generate professional PDF reports or CSV exports for external accounting.

### ⚙️ Operational Excellence
- **Shift Management**: Full Clock-In/Clock-Out workflow with balance verification.
- **Automated Clock-Out**: System automatically closes stale shifts at 11:59 PM to maintain daily ledger integrity.
- **Smart Inventory**: Low-stock alerts and integrated stock management.
- **Service & Product Ledger**: Manage a wide range of cyber services (Printing, Gaming, etc.) and physical products.
- **Edit Approvals**: Staff edit requests require Admin authorization, ensuring an immutable audit trail.

---

## 🛠 Tech Stack

- **UI**: Jetpack Compose (Material 3)
- **Architecture**: MVVM with UseCase Layer
- **Database**: Firebase Firestore (Offline Persistence enabled)
- **Auth**: Firebase Authentication (Email/Password)
- **Background Tasks**: Android WorkManager (Auto Clock-Out & Reminders)
- **Navigation**: Jetpack Navigation 3
- **Dependency Management**: Gradle Version Catalog (libs.versions.toml)

---

## 🚀 Setup & Configuration

### 1. Firebase Setup
1. Create a project on the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with package name `com.raymond.cms`.
3. Download the `google-services.json` and place it in the `app/` folder.
4. Enable **Authentication** (Email/Password) and **Cloud Firestore**.

### 2. Firestore Security Rules
Use the following rules to secure your data:
```javascript
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### 3. Build & Run
1. Clone the repository: `git clone https://github.com/yourusername/cms-executive.git`
2. Open in **Android Studio Ladybug (or newer)**.
3. Sync Gradle and build the project.
4. On first launch, register an account. Note: The first user can be manually assigned the `ADMIN` role in Firestore.

---

## 📋 Business Logic (The "Executive" Way)

### 💰 Profit Calculation
Unlike traditional apps that just sum sales, CMS uses **Reconciliation Audit**:
- `Net Profit = (Closing Cash + Mpesa + Till) - (Opening Cash + Mpesa + Till)`
- This captures non-sale expenses, meals, and miscellaneous items automatically.

### 🛡️ The Drawer Limit
To maintain safety, any closing cash balance exceeding **KSh 50,000** is automatically flagged and recorded as a "Cash Inflow/Investment" to represent its removal from the daily drawer.

---

## 🤝 Contributing
Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

## 📄 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
© 2026 CMS Executive Suite - Built for modern entrepreneurs.
