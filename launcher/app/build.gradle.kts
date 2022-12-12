plugins {
    alias(libs.plugins.kotlin)
    application
}

application {
    mainClass.set("me.madhead.pay_to_view_bot.launcher.app.MainKt")
}

dependencies {
    implementation(libs.tgbotapi.behaviour.fsm)
    implementation(libs.tgbotapi.api)
    implementation(libs.ktor.server.netty)
}
