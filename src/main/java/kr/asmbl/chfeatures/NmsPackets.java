package kr.asmbl.chfeatures;

import org.bukkit.entity.Player;

/**
 * 클라이언트로 NMS 패킷을 직접 보내기 위한 공용 리플렉션 헬퍼.
 *
 * Paper 1.20.5+ 런타임은 Mojang 매핑이므로 아래 이름들은 실제 런타임 이름이다.
 * 무어노테이션 클래스 — json-api가 로드하지 않는다.
 */
final class NmsPackets {

	private static Class<?> packetInterface;

	private NmsPackets() {
	}

	/** CraftEntity/CraftPlayer 등에서 NMS 핸들을 얻는다. */
	static Object handle(Object craftObject) throws ReflectiveOperationException {
		return craftObject.getClass().getMethod("getHandle").invoke(craftObject);
	}

	static void send(Player receiver, Object packet) throws ReflectiveOperationException {
		if(packetInterface == null) {
			packetInterface = Class.forName("net.minecraft.network.protocol.Packet");
		}
		Object handle = handle(receiver);
		Object connection = handle.getClass().getField("connection").get(handle);
		connection.getClass().getMethod("send", packetInterface).invoke(connection, packet);
	}
}
