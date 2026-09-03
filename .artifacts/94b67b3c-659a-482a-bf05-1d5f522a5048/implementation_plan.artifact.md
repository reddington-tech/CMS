# Cyber Management System (CMS) Amendment Implementation Plan

This plan outlines the major upgrades to transform the system into a professional enterprise suite with strict staff workflows, audit logging, and advanced financial reconciliation.

## 1. Staff Workflow & Session Management
- **Clock-In Workflow**: Login -> `ClockInPromptScreen` (Clock In or Skip).
- **Session State**: Track whether a staff member has an `ACTIVE` shift. Skip mode is read-only.
- **Shift Model**: Create `Shift` model to track opening/closing balances, timestamps, and status.
- **Reconciliation**: Calculate profit as `Revenue - Expenses`. Track cash flow separately (Opening vs Closing vs Expected).

## 2. Opening & Closing Processes
- **Opening Confirmation**: Display previous closing balances. Prompt user for actual Opening Cash, M-Pesa, and Till. Flag differences for Admin.
- **Closing Confirmation**: Prompt user for actual Closing Cash, M-Pesa, and Till. Display summary: Expected vs Actual.
- **Investment/Transfer**: Replace the 50k auto-rule with a manual "Investment / Cash Transfer" feature that tracks capital removal separately from operating expenses.

## 3. Financial Recording & Integrity
- **Granular Transactions**: Record quantity, unit price, total, and payment method for every service.
- **Staff Metadata**: Automatically attach `staffId`, `staffName`, and `shiftId` to every transaction and expense.
- **Edit Approval System**: Staff can only *request* edits to saved records. Original records remain unchanged until an Admin approves the `ApprovalRequest`.
- **Audit Trail**: Record every significant action in an `audit_logs` collection.

## 4. Admin Suite & Reports
- **Admin Approval Screen**: A dashboard to review, approve, or reject edit requests.
- **Staff Performance**: Analytics showing revenue, expenses, and hours worked per staff member.
- **Enhanced Reports**: Filter by Today, Week, Month, Year, or Custom Range. Breakdown expenses by category.
- **Services Management**: Admin manages services and prices. Staff price changes require approval.

## 5. Proposed Changes (Technical)

### Data Layer
- **[NEW] [Shift.kt](file:///C:/Users/Admin/AndroidStudioProjects/CMS/app/src/main/java/com/raymond/cms/model/Shift.kt)**
- **[NEW] [Service.kt](file:///C:/Users/Admin/AndroidStudioProjects/CMS/app/src/main/java/com/raymond/cms/model/Service.kt)**
- **[NEW] [ApprovalRequest.kt](file:///C:/Users/Admin/AndroidStudioProjects/CMS/app/src/main/java/com/raymond/cms/model/ApprovalRequest.kt)**
- **[NEW] [AuditLog.kt](file:///C:/Users/Admin/AndroidStudioProjects/CMS/app/src/main/java/com/raymond/cms/model/AuditLog.kt)**
- **[MODIFY] [DailyTransaction.kt](file:///C:/Users/Admin/AndroidStudioProjects/CMS/app/src/main/java/com/raymond/cms/model/DailyTransaction.kt)**: Split into individual `Transaction` and `Expense` models for better tracking.

### ViewModels
- **[MODIFY] [AuthViewModel.kt](file:///C:/Users/Admin/AndroidStudioProjects/CMS/app/src/main/java/com/raymond/cms/ui/AuthViewModel.kt)**: Manage `Shift` state and staff credentials.
- **[MODIFY] [TransactionViewModel.kt](file:///C:/Users/Admin/AndroidStudioProjects/CMS/app/src/main/java/com/raymond/cms/ui/TransactionViewModel.kt)**: Handle granular recording, approvals, and reconciliation.

### UI Overhaul
- **[NEW] Screens**: `ClockInPromptScreen`, `OpeningBalanceConfirmScreen`, `AdminApprovalsScreen`, `StaffPerformanceScreen`.
- **[MODIFY] [AddTransactionScreen](file:///C:/Users/Admin/AndroidStudioProjects/CMS/app/src/main/java/com/raymond/cms/ui/Screens.kt)**: Update for quantity/price entry.
- **[MODIFY] [ReportsScreen](file:///C:/Users/Admin/AndroidStudioProjects/CMS/app/src/main/java/com/raymond/cms/ui/Screens.kt)**: Comprehensive financial breakdown.

## Verification Plan

### Manual Tests
1. **Clock-In**: Verify staff cannot record transactions without clocking in.
2. **Reconciliation**: Set opening balances, record transactions, and verify the "Expected Balance" during clock-out.
3. **Approval Flow**: Request a price change as staff. Approve it as Admin. Verify the price updates and an audit log is created.
4. **Reports**: Verify monthly profit matches `Revenue - Expenses` and rounding is correct.
5. **Role Security**: Attempt to access Admin reports as a staff member.
