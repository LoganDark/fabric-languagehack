package net.logandark.languagehack;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.logandark.languagehack.mixin.MixinTranslatableText;
import net.logandark.languagehack.ducks.TextSerializerDuck;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

/**
 * This subclass of {@link TranslatableText} translates itself on the server
 * side and sends the translated text to clients. This means that clients don't
 * need to have the localization installed in order to see the translated text.
 * <p>
 * This goes well with LanguageHack, since it normally wouldn't work without
 * localizations being available on the server. However, once localizations are
 * available, {@link SSTranslatableText} becomes useful for server-sided mods
 * that don't require the client to install anything, Bukkit plugin-style.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class SSTranslatableText extends TranslatableText {
	public SSTranslatableText(String key, Object... args) {
		super(key, args);
	}

	public SSTranslatableText(String key) {
		super(key);
	}

	public JsonObject serialize(
		Text.Serializer serializer,
		JsonSerializationContext ctx
	) {
		MixinTranslatableText mixin = (MixinTranslatableText) this;

		mixin.languagehack$updateTranslations();

		JsonObject jsonObject = new JsonObject();
		TextSerializerDuck betterSerializer = (TextSerializerDuck) serializer;

		jsonObject.addProperty("text", "");

		Style style = getStyle();

		if (!style.isEmpty()) {
			betterSerializer.languagehack$addStyle(style, jsonObject, ctx);
		}

		JsonArray extra = new JsonArray();

		for (StringVisitable translation : mixin.languagehack$getTranslations()) {
			if (translation instanceof Text) {
				extra.add(serializer.serialize((Text) translation, translation.getClass(), ctx));
			} else {
				extra.add(translation.getString());
			}
		}

		for (Text sibling : getSiblings()) {
			extra.add(serializer.serialize(sibling, sibling.getClass(), ctx));
		}

		if (extra.size() > 0) {
			jsonObject.add("extra", extra);
		}

		return jsonObject;
	}

	@Override
	public TranslatableText copy() {
		Object[] currentArgs = this.getArgs();
		Object[] newArgs = new Object[currentArgs.length];

		for (int i = 0; i < currentArgs.length; i++) {
			Object arg = currentArgs[i];

			if (arg instanceof Text) {
				newArgs[i] = ((Text) arg).shallowCopy();
			} else {
				newArgs[i] = arg;
			}
		}

		return new SSTranslatableText(getKey(), newArgs);
	}
}
