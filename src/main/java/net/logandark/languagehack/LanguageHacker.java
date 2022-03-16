package net.logandark.languagehack;

import com.google.common.collect.ImmutableMap;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.Language;
import net.minecraft.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class LanguageHacker {
	private static final Logger LOGGER = LoggerFactory.getLogger(LanguageHacker.class);
	private static final AtomicBoolean ACTIVATED = new AtomicBoolean();

	private static String[] getChain() {
		return new String[]{"en_us"};
	}

	private static List<Pair<Path, Path>> findLangDirs(ModContainer container) throws IOException {
		List<Pair<Path, Path>> langDirs = new ArrayList<>();

		for (Path root : container.getRootPaths()) {
			Path assetsDir = root.resolve("assets");

			if (Files.isDirectory(assetsDir)) {
				try (DirectoryStream<Path> stream = Files.newDirectoryStream(assetsDir)) {
					stream.forEach(path -> {
						Path langDir = path.resolve("lang");

						if (Files.isDirectory(langDir)) {
							langDirs.add(new Pair<>(assetsDir, langDir));
						}
					});
				}
			}
		}

		return langDirs;
	}

	private static Map<String, String> loadTranslations(ModContainer container, String[] chain) {
		String modid = container.getMetadata().getId();
		Map<String, String> translations = new HashMap<>();

		List<Pair<Path, Path>> langDirs;

		try {
			langDirs = findLangDirs(container);
			LOGGER.info("{}: Found {} lang directories to check", modid, langDirs.size());
		} catch (IOException e) {
			LOGGER.warn("{}: Error scanning lang directories: {}", modid, e);
			return translations;
		}

		for (Pair<Path, Path> dir : langDirs) {
			Path rootDir = dir.getLeft();
			Path langDir = dir.getRight();

			InputStream inputStream = null;

			for (String lang : chain) {
				Path langFile = langDir.resolve(lang + ".json");

				try {
					inputStream = Files.newInputStream(langFile);
					LOGGER.info("{}: Found {} translations ({})", modid, lang, rootDir.relativize(langFile));
					break;
				} catch (IOException e) {
					if (!(e instanceof NoSuchFileException)) {
						LOGGER.error("{}: Error loading {} translations: {}", modid, lang, e);
					}
				}
			}

			if (inputStream == null) {
				LOGGER.error("{}: Couldn't load any translations, skipping", modid);
				continue;
			}

			try {
				Language.load(inputStream, translations::put);
			} finally {
				try {
					inputStream.close();
				} catch (IOException e) {
					LOGGER.error(e.toString());
				}
			}
		}

		if (translations.size() > 0) {
			LOGGER.info("{}: Successfully loaded {} translations", modid, translations.size());
		} else {
			LOGGER.warn("{}: Did not load any translations", modid);
		}

		return translations;
	}

	public static ImmutableMap<String, String> activate() {
		if (!ACTIVATED.compareAndSet(false, true)) {
			throw new IllegalStateException("Can't activate LanguageHack twice!");
		}

		// The chain of languages to try. They will be tried in order and if a
		// language file cannot be loaded, the next one will be tried, until
		// there are no more to try.
		String[] chain = getChain();

		LOGGER.info("Activating language hack! Language chain: {}", Arrays.deepToString(chain));

		// Don't use an ImmutableMap.Builder for this, ImmutableMap builders
		// throw an exception if there are any duplicate keys
		Map<String, String> hacked = new HashMap<>();

		for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
			hacked.putAll(loadTranslations(mod, chain));
		}

		return ImmutableMap.copyOf(hacked);
	}
}
