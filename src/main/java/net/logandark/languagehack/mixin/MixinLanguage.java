package net.logandark.languagehack.mixin;

import com.google.common.collect.ImmutableMap;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.Closer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Mixin(Language.class)
public abstract class MixinLanguage {
	@Redirect(
		method = "create",
		at = @At(
			value = "INVOKE",
			target = "Lcom/google/common/collect/ImmutableMap$Builder;build()Lcom/google/common/collect/ImmutableMap;",
			remap = false
		)
	)
	private static ImmutableMap<String, String> languagehack$activate(ImmutableMap.Builder<String, String> builder) {
		String lang = "en_us";
		String fallback = "en_us";

		Logger logger = LogManager.getLogger("LanguageHack");

		logger.info("Activating language hack!");

		// Just kind of let vanilla's builder fall into the garbage collector.
		//
		// We don't need it; Fabric provides a `minecraft` mod that will tell us
		// to load its localizations anyway.

		// Don't use an ImmutableMap.Builder for this, ImmutableMap builders
		// throw an exception if there are any duplicate keys
		Map<String, String> hacked = new HashMap<>();

		FabricLoader loader = FabricLoader.getInstance();
		Collection<ModContainer> allMods = loader.getAllMods();

		for (ModContainer mod : allMods) {
			String modid = mod.getMetadata().getId();

			Path path = mod.getPath(String.format("assets/%s/lang/%s.json", modid, lang));
			InputStream inputStream;

			try {
				inputStream = Files.newInputStream(path);
			} catch (IOException e) {
				if (!(e instanceof NoSuchFileException)) {
					logger.error("Error loading {} localizations for {}: {}", lang, modid, e);
					continue;
				}

				//noinspection ConstantConditions
				if (lang.equals(fallback)) {
					logger.warn("{} has no {} localizations, moving on", modid, lang);
					continue;
				}

				Path en_us = mod.getPath(String.format("assets/%s/lang/%s.json", modid, fallback));
				logger.warn("{} has no {} localizations, trying {}...", modid, lang, fallback);

				try {
					inputStream = Files.newInputStream(en_us);
				} catch (IOException e2) {
					if (e2 instanceof NoSuchFileException) {
						logger.warn("{} has no {} or {} localizations, moving on", modid, lang, fallback);
						continue;
					}

					e2.addSuppressed(e);

					logger.error("Error loading {} localizations for {}: {}", fallback, modid, e2);
					continue;
				}
			}

			try {
				Language.load(inputStream, hacked::put);
			} finally {
				Closer.closeSilently(inputStream);
			}

			logger.info("Successfully loaded {} localizations for {}", lang, modid);
		}

		return ImmutableMap.copyOf(hacked);
	}
}
