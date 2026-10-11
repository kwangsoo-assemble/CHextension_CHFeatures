package kr.asmbl.chfeatures;

import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.exceptions.CRE.CREFormatException;
import com.laytonsmith.core.exceptions.CRE.CREPluginInternalException;

import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * 레시피북 열림 상태 강제 설정/조회.
 *
 * Bukkit/Paper API에는 없는 기능이라 NMS 리플렉션을 사용한다.
 * Paper 1.20.5+ 런타임은 Mojang 매핑이므로 아래 이름들은 실제 런타임 클래스/멤버명이다.
 * (1.21.8 server.txt 매핑으로 검증됨 — 서버 메이저 업데이트 시 재검증 필요)
 *
 * 무어노테이션 클래스 — json-api가 로드하지 않는다.
 */
final class RecipeBookBridge {

	private RecipeBookBridge() {
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Object recipeBookType(String typeName, Target t) throws ReflectiveOperationException {
		Class<?> typeClass = Class.forName("net.minecraft.world.inventory.RecipeBookType");
		try {
			return Enum.valueOf((Class<? extends Enum>) typeClass.asSubclass(Enum.class), typeName.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new CREFormatException("Invalid recipe book type: " + typeName
					+ ". Must be one of CRAFTING, FURNACE, BLAST_FURNACE, SMOKER.", t);
		}
	}

	private static Object recipeBook(Player player) throws ReflectiveOperationException {
		Object handle = player.getClass().getMethod("getHandle").invoke(player);
		return handle.getClass().getMethod("getRecipeBook").invoke(handle);
	}

	static void setOpen(Player player, String typeName, boolean open, Target t) {
		try {
			Class<?> typeClass = Class.forName("net.minecraft.world.inventory.RecipeBookType");
			Object type = recipeBookType(typeName, t);
			Object book = recipeBook(player);
			Method setOpen = book.getClass().getMethod("setOpen", typeClass, boolean.class);
			setOpen.invoke(book, type, open);

			// 서버 상태만 바꾸면 클라이언트는 재접속 전까지 모른다 → 설정 패킷으로 즉시 동기화
			Object settings = book.getClass().getMethod("getBookSettings").invoke(book);
			Class<?> settingsClass = Class.forName("net.minecraft.stats.RecipeBookSettings");
			Object packet = Class.forName("net.minecraft.network.protocol.game.ClientboundRecipeBookSettingsPacket")
					.getConstructor(settingsClass).newInstance(settings);
			Object handle = player.getClass().getMethod("getHandle").invoke(player);
			Object connection = handle.getClass().getField("connection").get(handle);
			connection.getClass().getMethod("send", Class.forName("net.minecraft.network.protocol.Packet"))
					.invoke(connection, packet);

			// 리스너의 열림 상태 캐시도 동기화 (player_recipe_book_toggle 중복 판정 유지)
			ChfListener.markOpenState(player.getUniqueId(), typeName.toUpperCase(), open);
		} catch (ReflectiveOperationException ex) {
			throw new CREPluginInternalException("Failed to set recipe book state."
					+ " The server version may not match the expected NMS mappings (1.21.x): " + ex, t);
		}
	}

	static boolean isOpen(Player player, String typeName, Target t) {
		try {
			Class<?> typeClass = Class.forName("net.minecraft.world.inventory.RecipeBookType");
			Object type = recipeBookType(typeName, t);
			Object book = recipeBook(player);
			Method isOpen = book.getClass().getMethod("isOpen", typeClass);
			return (boolean) isOpen.invoke(book, type);
		} catch (ReflectiveOperationException ex) {
			throw new CREPluginInternalException("Failed to read recipe book state."
					+ " The server version may not match the expected NMS mappings (1.21.x): " + ex, t);
		}
	}
}
