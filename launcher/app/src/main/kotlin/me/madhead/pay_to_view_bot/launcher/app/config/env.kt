package me.madhead.pay_to_view_bot.launcher.app.config

@Suppress("ClassName")
object env {
    val telegramToken: String by lazy { variable("TELEGRAM_TOKEN") }

    val port: Int by lazy { optional.variable("PORT")?.toInt() ?: 5000 }

    val webhookUrl: String by lazy { variable("WEBHOOK_URL") }

    private fun variable(name: String): String = System.getenv(name)
        ?: throw IllegalArgumentException("$name environment variable is not set")

    object optional {
        internal fun variable(name: String): String? = System.getenv(name)
    }
}
