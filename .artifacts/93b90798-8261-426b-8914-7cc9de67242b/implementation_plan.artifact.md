# Bug Fix: Duplicate Daily Records and Incorrect Profit Calculation

This plan addresses the issue where approving a record edit results in duplicate entries for the same day, doubled revenue totals, and incorrect profit calculations in the dashboard.

## User Review Required

> [!IMPORTANT]
> I will be modifying the **Hybrid Reporting Logic**. The app currently uses a mix of "Legacy" daily summaries and "New System" granular transactions. The fix will prioritize granular data for new shifts while maintaining legacy support for older records.

## Proposed Changes

### Domain Layer (Reporting Logic)

#### [MODIFY] [GetBusinessInsightsUseCase.kt](file:///E:/PROJECTS/CMS/app/src/main/java/com/raymond/cms/domain/GetBusinessInsightsUseCase.kt)
- Fix the daily breakdown loop to ensure profit is calculated as `revenue - expenses` for new system records.
- Prevent new system transactions from being added to profit twice.

---

### UI & ViewModels

#### [MODIFY] [FinancialViewModel.kt](file:///E:/PROJECTS/CMS/app/src/main/java/com/raymond/cms/ui/FinancialViewModel.kt)
- Fix the `approveRequest` logic for `DAILY_SUMMARY`. It currently uses the timestamp as the document ID, but the repository uses the formatted date string.
- Ensure approvals correctly overwrite the intended document.

#### [MODIFY] [Screens.kt](file:///E:/PROJECTS/CMS/app/src/main/java/com/raymond/cms/ui/Screens.kt)
- Update `consolidatedDailyRecords` in `TransactionListScreen`.
- If a `Shift` exists for a day, ignore the auto-generated `DailyTransaction` summary for that same day to prevent double-counting revenue in the list.

---

### Models

#### [MODIFY] [DailyTransaction.kt](file:///E:/PROJECTS/CMS/model/DailyTransaction.kt)
- Adjust the `profit` getter to handle cases where opening balances might be zero during an edit, ensuring it doesn't default to the entire closing balance unless intended.

## Verification Plan

### Manual Verification
1. **Clock Out as Staff**: Verify that a shift is closed and a summary is generated.
2. **Check Records**: Verify the "Business Records" screen shows only one entry for that day with the correct total.
3. **Admin Edit**: As Staff, request an edit to a Daily Summary. As Admin, approve it.
4. **Verification**: Confirm the record is updated (not duplicated) and the profit/revenue remains accurate.
