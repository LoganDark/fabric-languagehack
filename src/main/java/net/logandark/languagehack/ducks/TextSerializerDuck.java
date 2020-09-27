package net.logandark.languagehack.ducks;

import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.text.Style;

public interface TextSerializerDuck {
	void languagehack$addStyle(
		Style style,
		JsonObject json,
		JsonSerializationContext ctx
	);
}
