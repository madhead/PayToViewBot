enableFeaturePreview(org.gradle.api.internal.FeaturePreviews.Feature.TYPESAFE_PROJECT_ACCESSORS.name)

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://git.inmo.dev/api/packages/InsanusMokrassar/maven")
    }
}

rootProject.name = "PayToViewBot"

include(":launcher:app")
