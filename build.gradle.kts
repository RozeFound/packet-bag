import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
  `java-library`
  id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
  id("xyz.jpenilla.run-paper") version "3.0.0-beta.1"
  id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.3.0"
}

group = "io.github.rozefound"
version = "1.0.0-SNAPSHOT"
description = "Test plugin for paperweight-userdev and packet manipulation"

java {
  toolchain.languageVersion = JavaLanguageVersion.of(21)
}

// For 1.20.4 or below, or when you care about supporting Spigot on >=1.20.5:
/*
paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.REOBF_PRODUCTION

tasks.assemble {
  dependsOn(tasks.reobfJar)
}
 */

repositories {
  maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }
  maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
}

dependencies {

  paperweight.paperDevBundle("1.21.5-R0.1-SNAPSHOT")
  compileOnly("com.github.retrooper:packetevents-spigot:2.9.3")
}

tasks {
  compileJava {
    options.release = 21
  }
  javadoc {
    options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
  }

  // Only relevant for 1.20.4 or below, or when you care about supporting Spigot on >=1.20.5:
  /*
  reobfJar {
    // This is an example of how you might change the output location for reobfJar. It's recommended not to do this
    // for a variety of reasons, however it's asked frequently enough that an example of how to do it is included here.
    outputJar = layout.buildDirectory.file("libs/PaperweightTestPlugin-${project.version}.jar")
  }
   */
}

// Configure plugin.yml generation
// - name, version, and description are inherited from the Gradle project.
bukkitPluginYaml {
  main = "io.github.rozefound.packetbag.Main"
  load = BukkitPluginYaml.PluginLoadOrder.STARTUP
  authors.add("Author")
  apiVersion = "1.21.5"
  depend.add("packetevents")
}
