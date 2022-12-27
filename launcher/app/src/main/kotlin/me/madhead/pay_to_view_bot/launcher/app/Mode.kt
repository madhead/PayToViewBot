package me.madhead.pay_to_view_bot.launcher.app

import com.github.pgreze.process.process
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.micro_utils.fsm.common.managers.DefaultStatesManager
import dev.inmo.micro_utils.fsm.common.managers.InMemoryDefaultStatesManagerRepo
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.files.downloadFile
import dev.inmo.tgbotapi.extensions.api.send.media.sendPhoto
import dev.inmo.tgbotapi.extensions.api.send.sendMessage
import dev.inmo.tgbotapi.extensions.api.webhook.deleteWebhook
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviourWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitMediaMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitText
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onCommand
import dev.inmo.tgbotapi.extensions.utils.asMessageUpdate
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
import java.net.URI
import kotlin.io.path.createTempFile
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
        val block = bot.behaviour()

        bot.setWebhookInfoAndStartListenWebhooks(
            listenPort = env.port,
            engineFactory = Netty,
            listenRoute = env.telegramToken,
            setWebhookRequest = SetWebhook(
                url = URI("${env.webhookBaseUrl}/${env.telegramToken}").normalize().toString(),
            ),
            block = block,
        )
    }
}

object LONG_POLLING : Mode {
    override suspend fun start(bot: TelegramBot) {
        bot.deleteWebhook()
        bot.startGettingOfUpdatesByLongPolling(updatesReceiver = bot.behaviour()).join()
    }
}

val statesManagerRepo = InMemoryDefaultStatesManagerRepo<State>()
val statesManager = DefaultStatesManager(
    repo = statesManagerRepo,
    onStartContextsConflictResolver = { currentState, newState ->
        println("Current state: $currentState (${System.identityHashCode(currentState)})")
        println("New state: $newState (${System.identityHashCode(currentState)})")

        true
    },
    onUpdateContextsConflictResolver = { oldState, newState, currentNewState ->
        println("Old state: $oldState (${System.identityHashCode(oldState)})")
        println("New state: $newState (${System.identityHashCode(newState)})")
        println("Current New state: $newState (${System.identityHashCode(currentNewState)})")

        true
    }
)

suspend fun TelegramBot.behaviour(): suspend (Update) -> Unit {
    return this.buildBehaviourWithFSM(statesManager = statesManager) {
        onCommand("blur") {
            startChain(WaitingForMediaToBlur(it.chat.id))
        }

        onCommand("pay") {
            startChain(WaitingForMediaToMask(it.chat.id))
        }

        strictlyOn<WaitingForMediaToBlur> { state ->
            val mediaToBlurMessage: CommonMessage<MediaContent> = waitMediaMessage(
                initRequest = SendTextMessage(state.context, "Send me a photo to blur", MarkdownV2),
                errorFactory = {
                    SendTextMessage(
                        chatId = state.context,
                        text = "Send me *a photo*\\!",
                        parseMode = MarkdownV2,
                        replyToMessageId = it.asMessageUpdate()?.data?.messageId
                    )
                },
            ).first()

            val mediaToBlurContent = mediaToBlurMessage.content
            val tempFile = createTempFile()

            bot.downloadFile(mediaToBlurContent, tempFile.toFile())

            WaitingForBlurSize(state.context, tempFile)
        }

        strictlyOn<WaitingForBlurSize> { state ->
            var blurSize: String = waitText(
                initRequest = SendTextMessage(state.context, "Send me the blur size", MarkdownV2),
                errorFactory = { SendTextMessage(state.context, "Should be an integer", MarkdownV2) },
            ).first().text

            if (blurSize.toIntOrNull() == null) {
                blurSize = "10"
            }

            val tempFile = createTempFile(suffix = ".jpg")

            // TODO: Blur
            // ffmpeg -i ihar.jpg -vf "boxblur=10" -c:a copy ihar-blurred.jpg
            // process("ffmpeg", "-y", "-i", state.media.toString(), "-vf", "boxblur=$blurSize", "-c:a", "copy", tempFile.toString())

            // TODO: Repond with blurred media
            bot.sendMessage(state.context, "Blurred", MarkdownV2)

            // bot.sendPhoto(
            //     state.context,
            //     MultipartFile(
            //         "blurred.jpg"
            //     ) {
            //         tempFile.inputStream().asInput()
            //     }
            // )

            null
        }
    }.run {
        start()
        asUpdateReceiver
    }
}
