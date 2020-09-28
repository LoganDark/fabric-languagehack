package net.logandark.languagehack.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import net.logandark.languagehack.SSTranslatableText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;

@Mixin(Text.Serializer.class)
public abstract class MixinTextSerializer {
	@Shadow
	public abstract JsonElement serialize(Text text, Type type, JsonSerializationContext ctx);

	@Inject(
		at = @At("HEAD"),
		method = "serialize",
		cancellable = true
	)
	private void onSerialize(
		Text text,
		Type type,
		JsonSerializationContext ctx,
		CallbackInfoReturnable<JsonElement> cir
	) {
		if (text instanceof SSTranslatableText) {
			cir.setReturnValue(serialize(
				((SSTranslatableText) text).toLiteralText(),
				LiteralText.class,
				ctx
			));
		}
	}
}
