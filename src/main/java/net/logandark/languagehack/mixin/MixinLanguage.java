package net.logandark.languagehack.mixin;

import com.google.common.collect.ImmutableMap;
import net.logandark.languagehack.LanguageHacker;
import net.minecraft.util.Language;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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
		// Just kind of let vanilla's builder fall into the garbage collector.
		//
		// We don't need it; Fabric provides a `minecraft` mod that will tell us
		// to load its localizations anyway.

		return LanguageHacker.activate();
	}
}
