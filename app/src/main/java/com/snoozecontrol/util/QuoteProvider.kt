package com.snoozecontrol.util

import java.util.Calendar

data class Quote(
    val text: String,
    val author: String
)

object QuoteProvider {
    private val quotes = listOf(
        // Calm & Mindful
        Quote("An early-morning walk is a blessing for the whole day.", "Henry David Thoreau"),
        Quote(
            "When you arise in the morning think of what a privilege it is to be alive, to think, to enjoy, to love.",
            "Marcus Aurelius"
        ),
        Quote(
            "Write it on your heart that every day is the best day in the year.",
            "Ralph Waldo Emerson"
        ),
        Quote(
            "Smile in the mirror. Do that every morning and you'll start to see a big difference in your life.",
            "Yoko Ono"
        ),
        Quote("Peace comes from within. Do not seek it without.", "Buddha"),
        Quote(
            "Breathe. Let go. And remind yourself that this very moment is the only one you know you have for sure.",
            "Oprah Winfrey"
        ),
        Quote("The sun is new each day.", "Heraclitus"),
        Quote("Be willing to be a beginner every single morning.", "Meister Eckhart"),

        // Energetic & Action-Oriented
        Quote("The secret of getting ahead is getting started.", "Mark Twain"),
        Quote("Either you run the day or the day runs you.", "Jim Rohn"),
        Quote("Don't count the days, make the days count.", "Muhammad Ali"),
        Quote("Start where you are. Use what you have. Do what you can.", "Arthur Ashe"),
        Quote("Action is the foundational key to all success.", "Pablo Picasso"),
        Quote("Your future is created by what you do today, not tomorrow.", "Robert Kiyosaki"),
        Quote("Small daily improvements over time lead to stunning results.", "Robin Sharma"),
        Quote("Do one thing every day that scares you.", "Eleanor Roosevelt"),

        // Focus & Determination
        Quote("The best way to predict the future is to create it.", "Peter Drucker"),
        Quote(
            "Success is not final, failure is not fatal: it is the courage to continue that counts.",
            "Winston Churchill"
        ),
        Quote("The only way to do great work is to love what you do.", "Steve Jobs"),
        Quote("Focus on being productive instead of busy.", "Tim Ferriss"),
        Quote("Today’s accomplishments were yesterday’s impossibilities.", "Robert H. Schuller"),
        Quote("Believe you can and you're halfway there.", "Theodore Roosevelt"),
        Quote("Turn your wounds into wisdom.", "Oprah Winfrey"),

        // Hope, Possibility & Optimism
        Quote(
            "Opportunities are like sunrises. If you wait too long, you miss them.",
            "William Arthur Ward"
        ),
        Quote("With the new day comes new strength and new thoughts.", "Eleanor Roosevelt"),
        Quote(
            "Keep your face always toward the sunshine—and shadows will fall behind you.",
            "Walt Whitman"
        ),
        Quote("Light tomorrow with today.", "Elizabeth Barrett Browning"),
        Quote("Every morning brings new potential, but only if you wake up and act.", "Unknown"),
        Quote(
            "There was never a night or a problem that could defeat sunrise or hope.",
            "Bernard Williams"
        ),
        Quote(
            "Happiness is not something ready made. It comes from your own actions.",
            "Dalai Lama"
        ),

        // Philosophical & Reflective
        Quote("The journey of a thousand miles begins with one step.", "Lao Tzu"),
        Quote("It is never too late to be what you might have been.", "George Eliot"),
        Quote(
            "What lies behind us and what lies before us are tiny matters compared to what lies within us.",
            "Ralph Waldo Emerson"
        ),
        Quote("Wherever you go, go with all your heart.", "Confucius"),
        Quote("Life is what happens when you're busy making other plans.", "John Lennon")
    )

    fun getTodayQuote(): Quote {
        val dayOfYear = Calendar.getInstance()[Calendar.DAY_OF_YEAR]
        val index = (dayOfYear % quotes.size).coerceAtLeast(0)
        return quotes[index]
    }

    fun getRandomQuote(): Quote {
        return quotes.random()
    }
}
