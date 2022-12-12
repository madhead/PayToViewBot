package me.madhead.pay_to_view_bot.launcher.app.config

import me.madhead.pay_to_view_bot.launcher.app.LONG_POLLING
import me.madhead.pay_to_view_bot.launcher.app.Mode
import me.madhead.pay_to_view_bot.launcher.app.WEBHOOK

val Array<String>.mode: Mode
    get() = when (val mode = getOrNull(0)) {
        "webhook" -> WEBHOOK
        "long-polling" -> LONG_POLLING
        else -> throw IllegalArgumentException("Unknown mode: $mode")
    }
