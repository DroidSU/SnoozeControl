package com.snoozecontrol.util

import java.util.Calendar

data class Quote(
    val text: String,
    val author: String
)

object QuoteProvider {
    private val quotes = listOf(
        Quote("The secret of getting ahead is getting started.", "Mark Twain"),
        Quote(
            "Write it on your heart that every day is the best day in the year.",
            "Ralph Waldo Emerson"
        ),
        Quote("An early-morning walk is a blessing for the whole day.", "Henry David Thoreau"),
        Quote("Today’s accomplishments were yesterday’s impossibilities.", "Robert H. Schuller"),
        Quote("Your future is created by what you do today, not tomorrow.", "Robert Kiyosaki"),
        Quote("The sun is new each day.", "Heraclitus"),
        Quote(
            "Opportunities are like sunrises. If you wait too long, you miss them.",
            "William Arthur Ward"
        ),
        Quote("Believe you can and you're halfway there.", "Theodore Roosevelt"),
        Quote("Every morning brings new potential, but only if you wake up and act.", "Unknown"),
        Quote("Small daily improvements over time lead to stunning results.", "Robin Sharma")
    )

    fun getTodayQuote(): Quote {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val index = (dayOfYear % quotes.size).coerceAtLeast(0)
        return quotes[index]
    }
}
