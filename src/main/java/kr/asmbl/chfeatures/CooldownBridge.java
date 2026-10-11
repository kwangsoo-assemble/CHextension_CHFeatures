package kr.asmbl.chfeatures;

import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.exceptions.CRE.CREFormatException;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

/**
 * 아이템 쿨다운(메테리얼/커스텀 그룹 키) 처리.
 *
 * 중요: 무어노테이션 클래스. setCooldown(NamespacedKey, ...)는 컴파일 시
 * net.kyori.adventure.key.Key 오버로드로 해석되는데, Adventure 클래스는 CH full jar에
 * 번들되지 않아 cmdline(json-api)에서 @api 클래스에 이 참조가 있으면 로드가 실패한다.
 * 그래서 Key 오버로드 호출은 반드시 이 클래스 안에만 둔다.
 */
final class CooldownBridge {

	private CooldownBridge() {
	}

	/** 아이템 메테리얼과 일치하면 메테리얼, 아니면 NamespacedKey(커스텀 쿨다운 그룹)로 해석. */
	private static Object resolveKey(String name, Target t) {
		Material mat = Material.matchMaterial(name);
		if(mat != null && mat.isItem()) {
			return mat;
		}
		NamespacedKey key = NamespacedKey.fromString(name.toLowerCase());
		if(key == null) {
			throw new CREFormatException("Invalid material or cooldown group key: " + name
					+ ". Keys must be in 'namespace:key' format (lowercase letters, numbers, ._-).", t);
		}
		return key;
	}

	static int getCooldown(Player player, String name, Target t) {
		Object key = resolveKey(name, t);
		if(key instanceof Material mat) {
			return player.getCooldown(mat);
		}
		return player.getCooldown((NamespacedKey) key);
	}

	static void setCooldown(Player player, String name, int cooldown, Target t) {
		Object key = resolveKey(name, t);
		if(key instanceof Material mat) {
			player.setCooldown(mat, cooldown);
		} else {
			player.setCooldown((NamespacedKey) key, cooldown);
		}
	}
}
