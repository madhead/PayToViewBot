package me.madhead.pay_to_view_bot.launcher.app

import dev.inmo.tgbotapi.bot.ktor.telegramBot
import me.madhead.pay_to_view_bot.launcher.app.config.env
import me.madhead.pay_to_view_bot.launcher.app.config.mode

suspend fun main(args: Array<String>) {
    args.mode.start(telegramBot(env.telegramToken))
}
