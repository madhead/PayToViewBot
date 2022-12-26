package me.madhead.pay_to_view_bot.launcher.app

import com.github.pgreze.process.process
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.webhook.deleteWebhook
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitMediaMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.setWebhookInfoAndStartListenWebhooks
import dev.inmo.tgbotapi.extensions.utils.updates.retrieving.startGettingOfUpdatesByLongPolling
import dev.inmo.tgbotapi.requests.abstracts.MultipartFile
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.requests.webhook.SetWebhook
import dev.inmo.tgbotapi.types.message.MarkdownV2
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.content.MediaContent
import dev.inmo.tgbotapi.types.update.abstracts.Update
import io.ktor.server.netty.Netty
import io.ktor.utils.io.streams.asInput
import kotlin.io.path.inputStream
import kotlinx.coroutines.flow.first
import me.madhead.pay_to_view_bot.launcher.app.config.env
import me.madhead.pay_to_view_bot.launcher.app.state.WaitingForBlurSize
import me.madhead.pay_to_view_bot.launcher.app.state.WaitingForMediaToBlur
import me.madhead.pay_to_view_bot.launcher.app.state.WaitingForMediaToMask

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
            block = bot.behaviour(),
        )
    }
}

object LONG_POLLING : Mode {
    override suspend fun start(bot: TelegramBot) {
        bot.deleteWebhook()
        bot.startGettingOfUpdatesByLongPolling(updatesReceiver = bot.behaviour()).join()
    }
}

suspend fun TelegramBot.behaviour(): suspend (Update) -> Unit {
    return this.buildBehaviourWithFSM {
        onCommand("blur") {
            startChain(WaitingForMediaToBlur(it.chat.id))
        }

        onCommand("pay") {
            startChain(WaitingForMediaToMask(it.chat.id))
        }

        strictlyOn<WaitingForMediaToBlur> { state ->
            // TODO: Filter out unsupported media types.
            val mediaToBlurMessage: CommonMessage<MediaContent> = waitMediaMessage(
                initRequest = SendTextMessage(state.context, "Send me the media to blur", MarkdownV2),
                errorFactory = { SendTextMessage(state.context, "Send me the *media* to blur", MarkdownV2) },
            ).first()

            // TODO: Save the media in a temporary file
            val mediaToBlurContent = mediaToBlurMessage.content
            val tempFile = kotlin.io.path.createTempFile()

            bot.downloadFile(mediaToBlurContent, tempFile.toFile())

            // TODO: Switch to the next state
            println("Downloaded media to $tempFile")

            WaitingForBlurSize(state.context, tempFile)
        }

        strictlyOn<WaitingForBlurSize> { state ->
            // TODO: Proper error handling
            var blurSize: String = waitText(
                initRequest = SendTextMessage(state.context, "Send me the blur size", MarkdownV2),
                errorFactory = { SendTextMessage(state.context, "Should be an integer", MarkdownV2) },
            ).first().text

            blurSize = "10"

            val tempFile = kotlin.io.path.createTempFile(suffix = ".jpg")

            // TODO: Blur
            // ffmpeg -i ihar.jpg -vf "boxblur=10" -c:a copy ihar-blurred.jpg
            process("ffmpeg", "-y", "-i", state.media.toString(), "-vf", "boxblur=$blurSize", "-c:a", "copy", tempFile.toString())

            // TODO: Repond with blurred media
            bot.sendMessage(state.context, "Blurred", MarkdownV2)

            bot.sendPhoto(
                state.context,
                MultipartFile(
                    "blurred.jpg"
                ) {
                    tempFile.inputStream().asInput()
                }
            )

            null
        }
    }.run {
        start()
        asUpdateReceiver
    }
}
