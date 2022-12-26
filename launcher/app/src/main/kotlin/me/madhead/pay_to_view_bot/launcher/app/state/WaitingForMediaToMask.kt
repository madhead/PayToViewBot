package me.madhead.pay_to_view_bot.launcher.app.state

import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.ChatIdentifier

data class WaitingForMediaToMask(
    override val context: ChatIdentifier,
) : State
