package net.logandark.languagehack.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.logandark.languagehack.SSTranslatableText;
import net.logandark.languagehack.ducks.TextSerializerDuck;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;

@Mixin(Text.Serializer.class)
public abstract class MixinTextSerializer implements TextSerializerDuck {
	@Invoker("addStyle")
	public abstract void languagehack$addStyle(
		Style style,
		JsonObject json,
		JsonSerializationContext ctx
	);

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
			cir.setReturnValue(((SSTranslatableText) text).serialize(
				(Text.Serializer) (Object) this,
				ctx
			));
		}
	}
}
