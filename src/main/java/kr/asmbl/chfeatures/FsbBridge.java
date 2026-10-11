package kr.asmbl.chfeatures;

import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.events.BindableEvent;
import com.laytonsmith.core.events.Driver;
import com.laytonsmith.core.events.EventUtils;
import com.laytonsmith.core.exceptions.CRE.CRELengthException;

import org.bukkit.entity.Player;
import org.figuramc.figura.server.api.FiguraFSBApi;
import org.figuramc.figura.server.packets.CustomFSBPacket;

import java.util.UUID;

/**
 * FiguraFSB 플러그인 API를 실제로 만지는 유일한 클래스.
 *
 * 중요: 어노테이션 없는 클래스 — json-api/FSB 미설치 환경에서 로드되지 않는다.
 * FSB 타입 참조는 전부 여기에만 두고, FsbFunctions는 플러그인 존재 확인 후 위임한다.
 * (provided 의존성이므로 @api 클래스 시그니처에 FSB 타입을 쓰면 CacheAnnotations가 깨진다)
 */
final class FsbBridge {

	private FsbBridge() {
	}

	static boolean isConnected(UUID player) {
		return FiguraFSBApi.isConnected(player);
	}

	/**
	 * @param avatarOwner null이면 receiver 본인의 호스트 아바타로 전송
	 */
	static void sendPacket(Player receiver, UUID avatarOwner, String packetName, byte[] data, Target t) {
		if(data.length > CustomFSBPacket.MAX_SERVER_PING_SIZE) {
			throw new CRELengthException("Packet data is too large: " + data.length
					+ " bytes (max " + CustomFSBPacket.MAX_SERVER_PING_SIZE + ").", t);
		}
		if(avatarOwner == null) {
			FiguraFSBApi.sendServerPacket(receiver, packetName, data);
		} else {
			FiguraFSBApi.sendServerPacket(receiver, avatarOwner, packetName, data);
		}
	}

	/**
	 * 해당 이름의 FSB 패킷 리스너를 등록하고, 수신 시 CH 이벤트 fsb_server_packet을 발동시킨다.
	 * FSB는 이름당 리스너 1개(전 플러그인 공용)이므로 재등록은 덮어쓴다.
	 */
	static void registerListener(String packetName) {
		FiguraFSBApi.onServerPacket(packetName, (name, senderUuid, sender, avatarOwner, data) ->
				EventUtils.TriggerListener(Driver.EXTENSION, "fsb_server_packet", new FsbPacketEvent(
						name, senderUuid, sender == null ? null : sender.getName(), avatarOwner, data)));
	}

	/** fsb_server_packet 이벤트의 BindableEvent 래퍼. FSB 타입 없이 순수 값만 담는다. */
	public static class FsbPacketEvent implements BindableEvent {

		private final String packetName;
		private final UUID senderUuid;
		private final String senderName;
		private final UUID avatarOwner;
		private final byte[] data;

		FsbPacketEvent(String packetName, UUID senderUuid, String senderName, UUID avatarOwner, byte[] data) {
			this.packetName = packetName;
			this.senderUuid = senderUuid;
			this.senderName = senderName;
			this.avatarOwner = avatarOwner;
			this.data = data;
		}

		public String getPacketName() {
			return this.packetName;
		}

		public UUID getSenderUuid() {
			return this.senderUuid;
		}

		public String getSenderName() {
			return this.senderName;
		}

		public UUID getAvatarOwner() {
			return this.avatarOwner;
		}

		public byte[] getData() {
			return this.data;
		}

		@Override
		public Object _GetObject() {
			return this;
		}
	}
}
