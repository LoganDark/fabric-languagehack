package net.logandark.languagehack;

import com.mojang.bridge.game.Language;
import net.logandark.languagehack.mixin.MixinTranslatableText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

import java.util.List;

/**
 * This subclass of {@link TranslatableText} translates itself on the server
 * side and sends the translated text to clients. This means that clients don't
 * need to have the localization installed in order to see the translated text.
 * <p>
 * This goes well with LanguageHack, since it normally wouldn't work without
 * localizations being available on the server. However, once localizations are
 * available, {@link SSTranslatableText} becomes useful for server-sided mods
 * that don't require the client to install anything, Bukkit plugin-style.
 * <p>
 * Unlike normal {@link TranslatableText}s, {@link SSTranslatableText}s are only
 * accurate upon creation. When they are saved to file or sent over the network,
 * they become a {@link LiteralText}.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class SSTranslatableText extends TranslatableText {
	private MixinTranslatableText mixin = (MixinTranslatableText) this;

	public SSTranslatableText(String key, Object... args) {
		super(key, args);
		updateTranslations();
	}

	public SSTranslatableText(String key) {
		super(key);
		updateTranslations();
	}

	private void updateTranslations() {
		mixin.languagehack$updateTranslations();
	}

	/**
	 * @return The list of translations stored within this {@link
	 * SSTranslatableText}. It basically consists of the translation obtained
	 * from the {@link Language}, but with format specifiers replaced with
	 * either blanks or the arguments passed to the {@link SSTranslatableText}'s
	 * constructor.
	 */
	public List<StringVisitable> getTranslations() {
		return mixin.languagehack$getTranslations();
	}

	/**
	 * Converts this {@link SSTranslatableText} into a {@link LiteralText}.
	 * <p>
	 * Does not convert nested {@link SSTranslatableText}s.
	 *
	 * @return A {@link LiteralText} that looks identical to this {@link
	 * SSTranslatableText}. All contained components are copied via {@link
	 * Text#shallowCopy()}.
	 */
	public LiteralText toLiteralText() {
		LiteralText literalText = new LiteralText("");
		literalText.setStyle(getStyle());

		for (StringVisitable translation : getTranslations()) {
			Text text = translation instanceof Text
				? ((Text) translation).shallowCopy()
				: new LiteralText(translation.getString());

			literalText.append(text);
		}

		for (Text sibling : siblings) {
			literalText.append(sibling.shallowCopy());
		}

		return literalText;
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
