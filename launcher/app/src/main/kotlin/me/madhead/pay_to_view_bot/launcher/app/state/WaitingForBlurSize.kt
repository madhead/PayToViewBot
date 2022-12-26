package me.madhead.pay_to_view_bot.launcher.app.state

import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.ChatIdentifier
import java.nio.file.Path

data class WaitingForBlurSize(
    override val context: ChatIdentifier,
    val media: Path,
) : State
