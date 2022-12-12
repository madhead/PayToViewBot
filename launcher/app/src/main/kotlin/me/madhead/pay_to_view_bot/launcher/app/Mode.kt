package me.madhead.pay_to_view_bot.launcher.app

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.setWebhookInfoAndStartListenWebhooks
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.startGettingOfUpdatesByLongPolling
import dev.inmo.tgbotapi.requests.webhook.SetWebhook
import io.ktor.server.netty.Netty
import me.madhead.pay_to_view_bot.launcher.app.config.env

sealed interface Mode {
    suspend fun start(bot: TelegramBot)
}

object WEBHOOK : Mode {
    override suspend fun start(bot: TelegramBot) {
        bot.setWebhookInfoAndStartListenWebhooks(
            listenPort = env.port,
            engineFactory = Netty,
            setWebhookRequest = SetWebhook(
                url = env.webhookUrl,
            ),
        ) {
            println(it)
        }
    }
}

object LONG_POLLING : Mode {
    override suspend fun start(bot: TelegramBot) {
        bot.startGettingOfUpdatesByLongPolling {
            println(it)
        }.join()
    }
}
