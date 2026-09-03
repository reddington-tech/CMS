package com.raymond.cms.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import com.raymond.cms.model.Transaction
import com.raymond.cms.model.Expense
import com.raymond.cms.model.Investment
import com.raymond.cms.util.DateTimeUtils
import java.io.File
import java.io.FileOutputStream
import java.util.Date

object ReportGenerator {
    fun generateDetailedPDF(
        context: Context, 
        title: String,
        transactions: List<Transaction>, 
        expenses: List<Expense>,
        investments: List<Investment>
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(600, 1000, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        var y = 40f
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText(title, 50f, y, paint)
        
        y += 30f
        paint.textSize = 10f
        paint.isFakeBoldText = false
        val summary = "Revenue: KSh ${transactions.sumOf { it.totalAmount }} | Expenses: KSh ${expenses.sumOf { it.amount }} | Investments: KSh ${investments.sumOf { it.amount }}"
        canvas.drawText(summary, 50f, y, paint)

        y += 40f
        paint.isFakeBoldText = true
        canvas.drawText("Transactions Log", 50f, y, paint)
        y += 20f
        paint.isFakeBoldText = false
        paint.textSize = 8f
        canvas.drawText("Time | Service | Amount | Staff", 50f, y, paint)
        y += 5f
        canvas.drawLine(50f, y, 550f, y, paint)

        transactions.take(30).forEach { tx ->
            y += 20f
            val time = DateTimeUtils.getFormat("hh:mm a").format(Date(tx.timestamp))
            val summaryText = if (tx.items.isNotEmpty()) tx.items.first().name else "Multi-item"
            canvas.drawText("$time | $summaryText | KSh ${tx.totalAmount} | ${tx.staffName}", 50f, y, paint)
            if (y > 950) return@forEach // Basic overflow protection
        }

        if (y < 700) {
            y += 40f
            paint.isFakeBoldText = true
            paint.textSize = 10f
            canvas.drawText("Expenses Log", 50f, y, paint)
            y += 20f
            paint.isFakeBoldText = false
            paint.textSize = 8f
            canvas.drawText("Time | Category | Amount | Staff", 50f, y, paint)
            y += 5f
            canvas.drawLine(50f, y, 550f, y, paint)

            expenses.take(20).forEach { ex ->
                y += 20f
                val time = DateTimeUtils.getFormat("hh:mm a").format(Date(ex.timestamp))
                canvas.drawText("$time | ${ex.category} | KSh ${ex.amount} | ${ex.staffName}", 50f, y, paint)
                if (y > 980) return@forEach
            }
        }

        if (y < 700) {
            y += 40f
            paint.isFakeBoldText = true
            paint.textSize = 10f
            canvas.drawText("Investments Log", 50f, y, paint)
            y += 20f
            paint.isFakeBoldText = false
            paint.textSize = 8f
            canvas.drawText("Time | Type | Amount | Staff", 50f, y, paint)
            y += 5f
            canvas.drawLine(50f, y, 550f, y, paint)

            investments.take(15).forEach { inv ->
                y += 20f
                val time = DateTimeUtils.getFormat("hh:mm a").format(Date(inv.timestamp))
                canvas.drawText("$time | ${inv.type} | KSh ${inv.amount} | ${inv.staffName}", 50f, y, paint)
                if (y > 980) return@forEach
            }
        }

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "CMS_Detailed_Report.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(context, "PDF Exported: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        pdfDocument.close()
    }
}
