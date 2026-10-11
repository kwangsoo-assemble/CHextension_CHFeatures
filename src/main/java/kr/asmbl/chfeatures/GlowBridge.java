package kr.asmbl.chfeatures;

import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.exceptions.CRE.CREPluginInternalException;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

/**
 * 시각 전용 "가짜 발광" — 엔티티 메타데이터(공유 플래그 바이트)를 조작한
 * ClientboundSetEntityDataPacket을 특정 플레이어에게만 보낸다.
 *
 * 서버의 실제 glowing 상태는 건드리지 않으므로 set_entity_glowing()과 달리
 * 보는 사람별로 다른 발광 상태를 만들 수 있다.
 *
 * 공유 플래그는 비트필드(0x01 불붙음, 0x02 웅크림, 0x08 스프린트, 0x10 수영,
 * 0x20 투명, 0x40 발광, 0x80 엘리트라)이므로, 서버의 현재 값을 읽어 발광 비트만
 * 바꿔 보낸다 — 다른 플래그가 초기화되지 않도록.
 *
 * 무어노테이션 클래스 — json-api가 로드하지 않는다. NMS 이름은 Mojang 매핑 런타임 기준(1.21.x 검증).
 */
final class GlowBridge {

	private static final int GLOWING_BIT = 0x40;

	private static Object flagsAccessor;
	private static Class<?> accessorClass;
	private static Method dataValueCreate;
	private static Constructor<?> packetConstructor;

	private GlowBridge() {
	}

	/** Entity.DATA_SHARED_FLAGS_ID (EntityDataAccessor&lt;Byte&gt;) 를 얻는다. */
	private static Object flagsAccessor() throws ReflectiveOperationException {
		if(flagsAccessor == null) {
			accessorClass = Class.forName("net.minecraft.network.syncher.EntityDataAccessor");
			try {
				Field field = Class.forName("net.minecraft.world.entity.Entity")
						.getDeclaredField("DATA_SHARED_FLAGS_ID");
				field.setAccessible(true);
				flagsAccessor = field.get(null);
			} catch (ReflectiveOperationException | RuntimeException ex) {
				// 폴백: 공유 플래그는 항상 데이터 인덱스 0, BYTE 직렬화기
				Class<?> serializerClass = Class.forName("net.minecraft.network.syncher.EntityDataSerializer");
				Object byteSerializer = Class.forName("net.minecraft.network.syncher.EntityDataSerializers")
						.getField("BYTE").get(null);
				flagsAccessor = accessorClass.getConstructor(int.class, serializerClass)
						.newInstance(0, byteSerializer);
			}
			dataValueCreate = Class.forName("net.minecraft.network.syncher.SynchedEntityData$DataValue")
					.getMethod("create", accessorClass, Object.class);
			packetConstructor = Class.forName("net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket")
					.getConstructor(int.class, List.class);
		}
		return flagsAccessor;
	}

	/**
	 * @param glowing null이면 서버의 실제 상태를 그대로 재전송한다 (= 해당 수신자에 대해 초기화)
	 */
	static void send(Entity entity, Boolean glowing, Collection<Player> receivers, Target t) {
		try {
			Object accessor = flagsAccessor();
			Object handle = NmsPackets.handle(entity);
			Object entityData = handle.getClass().getMethod("getEntityData").invoke(handle);
			byte serverFlags = (Byte) entityData.getClass().getMethod("get", accessorClass)
					.invoke(entityData, accessor);

			byte flags = serverFlags;
			if(glowing != null) {
				flags = glowing
						? (byte) (serverFlags | GLOWING_BIT)
						: (byte) (serverFlags & ~GLOWING_BIT);
			}

			Object dataValue = dataValueCreate.invoke(null, accessor, flags);
			Object packet = packetConstructor.newInstance(entity.getEntityId(), List.of(dataValue));
			for(Player receiver : receivers) {
				NmsPackets.send(receiver, packet);
			}
		} catch (ReflectiveOperationException ex) {
			throw new CREPluginInternalException("Failed to send the fake glowing packet."
					+ " The server version may not match the expected NMS mappings (1.21.x): " + ex, t);
		}
	}
}
