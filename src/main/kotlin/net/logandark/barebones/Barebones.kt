package net.logandark.barebones

import net.fabricmc.api.ModInitializer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Suppress("unused")
object Barebones : ModInitializer {
	val logger: Logger = LogManager.getLogger()

	override fun onInitialize() {
		logger.info("Hello Fabric world!")
	}
}
