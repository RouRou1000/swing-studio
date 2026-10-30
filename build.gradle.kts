plugins {
    id("net.fabricmc.fabric-loom") version "1.19.1"
}

version = "${property("mod.version")}+${property("minecraft_version")}"
group = "dev.swingstudio"
base.archivesName = "swing-studio"

repositories {
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version")}")
    compileOnly("maven.modrinth:modmenu:${property("modmenu_version")}")
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.encoding = "UTF-8"
}

tasks.processResources {
    val props = mapOf("version" to project.version.toString())
    inputs.properties(props)
    filesMatching("fabric.mod.json") { expand(props) }
}

tasks.jar {
    from("LICENSE") { rename { "LICENSE_swing-studio" } }
}

loom {
    runs {
        register("capture") {
            client()
            name = "Capture"
            vmArg("-Dswingstudio.capture=true")
            vmArg("-Xmx2G")
            programArgs("--quickPlaySingleplayer", "capture", "--width", "1280", "--height", "720")
        }
        register("captureShort") {
            client()
            name = "Capture Short"
            vmArg("-Dswingstudio.capture=short")
            vmArg("-Xmx3G")
            programArgs("--quickPlaySingleplayer", "capture", "--width", "1600", "--height", "900")
        }
    }
}

tasks.jar {
    exclude("dev/swingstudio/dev/**")
}
