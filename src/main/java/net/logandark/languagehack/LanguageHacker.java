package net.logandark.languagehack;

import com.google.common.collect.ImmutableMap;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.Language;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.Closer;

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
	private static final Logger LOGGER = LogManager.getLogger("LanguageHack");
	private static final AtomicBoolean ACTIVATED = new AtomicBoolean();

	private static String[] getChain() {
		return new String[]{"en_us"};
	}

	private static List<Path> findLangDirs(Path assets) throws IOException {
		List<Path> lang_dirs = new ArrayList<>();

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(assets)) {
			stream.forEach(path -> {
				Path lang_path = path.resolve("lang");

				if (Files.isDirectory(lang_path)) {
					lang_dirs.add(lang_path);
				}
			});
		}

		return lang_dirs;
	}

	private static Map<String, String> loadTranslations(ModContainer container, String[] chain) {
		String modid = container.getMetadata().getId();
		Map<String, String> translations = new HashMap<>();

		List<Path> lang_dirs;

		try {
			lang_dirs = findLangDirs(container.getPath("assets"));
			LOGGER.info("{}: Found {} lang directories to check", modid, lang_dirs.size());
		} catch (IOException e) {
			LOGGER.warn("{}: Error scanning lang directories", modid);
			return translations;
		}

		for (Path dir : lang_dirs) {
			InputStream inputStream = null;

			for (String lang : chain) {
				Path lang_path = dir.resolve(lang + ".json");

				try {
					inputStream = Files.newInputStream(lang_path);
					LOGGER.info("{}: Found {} translations ({})", modid, lang, container.getRootPath().relativize(lang_path));
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
				Closer.closeSilently(inputStream);
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
