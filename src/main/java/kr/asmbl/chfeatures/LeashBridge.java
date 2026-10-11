package kr.asmbl.chfeatures;

import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.exceptions.CRE.CREIllegalArgumentException;
import com.laytonsmith.core.exceptions.CRE.CREPluginInternalException;

import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.util.Collection;

/**
 * 시각 전용 "가짜 끈" — ClientboundSetEntityLinkPacket을 직접 보내 클라이언트에만 끈을 그린다.
 *
 * 서버 측 리시 데이터(Leashable)를 전혀 건드리지 않으므로:
 * - 끌려오는 물리 없음 (vanilla tickLeash는 ServerLevel 전용 = 서버 상태가 있어야만 동작)
 * - 거리 초과로 끊기거나 리드 아이템이 드랍되지 않음
 * - Bukkit setLeashHolder의 홀더 제약도 우회 (홀더는 임의 엔티티 가능)
 *
 * 단, 클라이언트가 끈을 그리는 대상(leashed)은 하드코딩된 제약이 있다:
 * ClientPacketListener.handleEntityLinkPacket이 `instanceof Leashable`을 검사하며,
 * 1.21.8에서 Leashable을 구현하는 것은 Mob과 Boat뿐이다. (플레이어·아머스탠드·디스플레이 불가)
 *
 * 무어노테이션 클래스 — json-api가 로드하지 않는다. NMS 이름은 Mojang 매핑 런타임 기준(1.21.x 검증).
 */
final class LeashBridge {

	private static Constructor<?> packetConstructor;

	private LeashBridge() {
	}

	private static Constructor<?> packetConstructor() throws ReflectiveOperationException {
		if(packetConstructor == null) {
			Class<?> nmsEntity = Class.forName("net.minecraft.world.entity.Entity");
			packetConstructor = Class.forName("net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket")
					.getConstructor(nmsEntity, nmsEntity);
		}
		return packetConstructor;
	}

	/** 클라이언트가 실제로 끈을 렌더링할 수 있는 대상인지 검사한다. */
	static void checkRenderable(Entity entity, Target t) {
		if(!(entity instanceof Mob) && !(entity instanceof Boat)) {
			throw new CREIllegalArgumentException("Only mobs and boats can have a leash rendered on them"
					+ " (the client only draws leashes for entities it considers leashable), but "
					+ entity.getType() + " was given. The leash HOLDER may be any entity type."
					+ " Tip: use an invisible mob at the desired position as the leashed end.", t);
		}
	}

	/**
	 * @param holder null이면 해당 엔티티의 가짜 끈을 제거한다 (destId 0).
	 */
	static void send(Entity leashed, Entity holder, Collection<Player> receivers, Target t) {
		try {
			Object packet = packetConstructor().newInstance(
					NmsPackets.handle(leashed), holder == null ? null : NmsPackets.handle(holder));
			for(Player receiver : receivers) {
				NmsPackets.send(receiver, packet);
			}
		} catch (ReflectiveOperationException ex) {
			throw new CREPluginInternalException("Failed to send the fake leash packet."
					+ " The server version may not match the expected NMS mappings (1.21.x): " + ex, t);
		}
	}
}
