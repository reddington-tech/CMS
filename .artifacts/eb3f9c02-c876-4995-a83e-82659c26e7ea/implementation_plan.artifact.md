# Bug Fix: Stale Active Sessions and Missing Profit/Closing Totals

This plan addresses the issue where shifts from previous days remain in an `ACTIVE` state, causing them to be displayed as "LIVE SESSIONS" in reports and preventing the display of closing balances and net profit totals. This occurs because the repository filters active shifts by the current date, effectively "hiding" stale shifts from both the user and the automated clock-out worker.

## User Review Required

> [!IMPORTANT]
> This change will make any forgotten active shifts from past days visible to the user upon login, prompting them to clock out or allowing the system to auto-close them.

## Proposed Changes

### [Shift Management & Repository]

#### [MODIFY] [ShiftRepository.kt](file:///E:/PROJECTS/CMS/app/src/main/java/com/raymond/cms/repository/ShiftRepository.kt)
- Remove the `date` filter from `getActiveShift`, `getActiveShiftSync`, and `getActiveShifts`. This allows the system to identify any shift that hasn't been closed, regardless of when it started.

#### [MODIFY] [ShiftManagementUseCase.kt](file:///E:/PROJECTS/CMS/app/src/main/java/com/raymond/cms/domain/ShiftManagementUseCase.kt)
- The existing stale shift detection in `startShift` will now function correctly as `getActiveShiftSync` will return open shifts from previous days.

#### [MODIFY] [ShiftWorker.kt](file:///E:/PROJECTS/CMS/app/src/main/java/com/raymond/cms/domain/ShiftWorker.kt)
- The `performAutoClockOut` logic will now correctly identify and close all stale active shifts, not just those started on the current day.

### [Attendance Tracking]

#### [MODIFY] [AttendanceRepository.kt](file:///E:/PROJECTS/CMS/app/src/main/java/com/raymond/cms/repository/AttendanceRepository.kt)
- Update `clockOut` to find the most recent record without a clock-out time instead of assuming the record belongs to the current calendar day. This prevents clock-out failures that occur after midnight.

## Verification Plan

### Manual Verification
1. **Simulate Stale Shift**: Manually set a shift in Firestore to `ACTIVE` with a date from yesterday.
2. **Launch App**: Verify the app detects the stale shift and displays the clock-out prompt.
3. **Report Audit**: Check the report for the previous day; verify it no longer shows "LIVE SESSION" once closed and displays the correct "Final Reconciliation" and "Shift Net Change" (Profit).
4. **Auto-Clock Out Test**: Run the `ShiftWorker` (via ADB or code trigger) and verify it closes the stale shift.
