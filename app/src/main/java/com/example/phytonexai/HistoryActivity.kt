
package com.example.phytonexai

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyContainer: LinearLayout
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        historyContainer = findViewById(R.id.historyContainer)
        tvEmpty = findViewById(R.id.tvEmpty)

        findViewById<Button>(R.id.btnClearHistory).setOnClickListener {
            ScanHistory.clearHistory(this)
            loadHistory()
        }

        loadHistory()
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        historyContainer.removeAllViews()

        val history = ScanHistory.getHistory(this)

        if (history.length() == 0) {
            tvEmpty.visibility = TextView.VISIBLE
            findViewById<Button>(R.id.btnClearHistory).visibility =
                Button.GONE
            return
        }

        tvEmpty.visibility = TextView.GONE
        findViewById<Button>(R.id.btnClearHistory).visibility =
            Button.VISIBLE

        for (i in 0 until history.length()) {
            val record = history.getJSONObject(i)

            val disease = record.optString("disease", "Unknown")
            val confidence = record.optDouble("confidence", 0.0)
            val date = record.optString("date", "")

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(20, 18, 20, 18)
                setBackgroundResource(R.drawable.card_background)

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = 16
                layoutParams = params
                elevation = 3f
            }

            val title = TextView(this).apply {
                text = disease
                textSize = 18f
                setTextColor(
                    getColor(R.color.green_dark)
                )
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            val confidenceText = TextView(this).apply {
                text = String.format(
                    java.util.Locale.US,
                    "Confidence: %.2f%%",
                    confidence
                )
                textSize = 15f
                setTextColor(getColor(R.color.text_primary))
                setPadding(0, 10, 0, 0)
            }

            val dateText = TextView(this).apply {
                text = date
                textSize = 13f
                setTextColor(getColor(R.color.text_secondary))
                setPadding(0, 6, 0, 0)
            }

            card.addView(title)
            card.addView(confidenceText)
            card.addView(dateText)

            historyContainer.addView(card)
        }
    }
}