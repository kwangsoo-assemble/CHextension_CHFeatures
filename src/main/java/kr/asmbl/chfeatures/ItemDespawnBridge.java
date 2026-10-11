package kr.asmbl.chfeatures;

import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.exceptions.CRE.CREIllegalArgumentException;
import com.laytonsmith.core.exceptions.CRE.CREPluginInternalException;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;

import java.lang.reflect.Field;

/**
 * 떨어진 아이템이 사라지기까지 남은 시간 조회·설정.
 *
 * 바닐라는 {@code ItemEntity.age} 를 매 틱 올리다가 월드에 설정된 디스폰 시간에 도달하면 지운다.
 * 즉 "남은 시간" 은 {@code 디스폰시간 - age} 다. Bukkit/Paper API 에는 남은 시간을 직접
 * 다루는 수단이 없어서(={@code Item#setTicksLived} 는 값이 1 이상이어야 하고, 실제 적용되는
 * 디스폰 시간을 알 수 없다) NMS 리플렉션으로 두 값을 직접 읽고 쓴다.
 *
 * Paper 1.20.5+ 런타임은 Mojang 매핑이라 아래 이름들은 실제 런타임 멤버명이다.
 * ({@code ItemEntity.age} = public int, {@code ItemEntity.despawnRate} = Paper 추가 private int.
 *  Purpur 1.21.8 서버 jar 로 확인 — 서버 메이저 업데이트 시 재검증 필요)
 *
 * 무어노테이션 클래스 — json-api 가 로드하지 않는다.
 */
final class ItemDespawnBridge {

	/** 바닐라 기본 아이템 수명 (ItemEntity.LIFETIME). despawnRate 를 못 읽었을 때의 대비값 */
	private static final int DEFAULT_LIFETIME = 6000;

	/**
	 * age 가 이 값이면 "영원히 사라지지 않음" 이다 (ItemEntity.INFINITE_LIFETIME = Short.MIN_VALUE).
	 * Paper 의 {@code Item#willAge()} 도 이 값과 비교해서 판정한다.
	 */
	private static final int INFINITE_AGE = Short.MIN_VALUE;

	/**
	 * age 는 청크 저장 시 short 로 기록된다(NBT "Age"). 범위를 넘겨두면 다음 로드에서 값이 잘려
	 * 엉뚱한 시간에 사라지므로, 애초에 short 범위로 자른다.
	 * INFINITE_AGE 자체는 "영원" 의미라 하한을 한 칸 올린다.
	 */
	private static final int MIN_AGE = INFINITE_AGE + 1;
	private static final int MAX_AGE = Short.MAX_VALUE;

	private ItemDespawnBridge() {
	}

	private static Item asItem(Entity entity, Target t) {
		if(!(entity instanceof Item item)) {
			throw new CREIllegalArgumentException("The given entity is not a dropped item.", t);
		}
		return item;
	}

	private static Object handle(Item item) throws ReflectiveOperationException {
		return item.getClass().getMethod("getHandle").invoke(item);
	}

	/** 상속 계층을 따라 올라가며 필드를 찾는다 (서버 구현이 ItemEntity 를 상속했을 경우 대비) */
	private static Field field(Object nms, String name) throws NoSuchFieldException {
		Class<?> c = nms.getClass();
		while(c != null) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f;
			} catch (NoSuchFieldException ignored) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}

	/**
	 * 이 아이템에 실제로 적용되는 디스폰 시간(틱).
	 *
	 * Paper 는 월드 설정(item-despawn-rate)과 아이템 종류별 예외 설정(alt-item-despawn-rate)을
	 * 엔티티 생성 시점에 despawnRate 에 캐시해 둔다. 그래서 이 값만 읽으면 두 설정이 모두 반영된다.
	 * 못 읽으면 바닐라 기본값으로 떨어진다 (기능이 죽는 것보다는 낫다).
	 */
	private static int despawnRate(Object nms) {
		try {
			int rate = field(nms, "despawnRate").getInt(nms);
			return rate > 0 ? rate : DEFAULT_LIFETIME;
		} catch (ReflectiveOperationException | RuntimeException ex) {
			return DEFAULT_LIFETIME;
		}
	}

	/**
	 * 사라지기까지 남은 틱. 영원히 사라지지 않게 설정돼 있으면 null.
	 * 이미 수명을 넘긴 상태(다음 틱에 사라짐)면 0.
	 */
	static Integer getRemaining(Entity entity, Target t) {
		Item item = asItem(entity, t);
		try {
			Object nms = handle(item);
			int age = field(nms, "age").getInt(nms);
			if(age == INFINITE_AGE) {
				return null;
			}
			int remaining = despawnRate(nms) - age;
			return remaining > 0 ? remaining : 0;
		} catch (ReflectiveOperationException ex) {
			throw new CREPluginInternalException("Failed to read the item despawn time."
					+ " The server version may not match the expected NMS mappings (1.21.x): " + ex, t);
		}
	}

	/**
	 * 사라지기까지 남은 틱 설정. ticks 가 null 이면 영원히 사라지지 않는다.
	 * 반환값은 실제로 적용된 남은 틱 (short 범위로 잘렸을 수 있다). 무한이면 null.
	 */
	static Integer setRemaining(Entity entity, Integer ticks, Target t) {
		Item item = asItem(entity, t);
		try {
			Object nms = handle(item);
			Field ageField = field(nms, "age");

			if(ticks == null) {
				ageField.setInt(nms, INFINITE_AGE);
				return null;
			}
			if(ticks < 0) {
				throw new CREIllegalArgumentException("The despawn time must be 0 or greater.", t);
			}

			int rate = despawnRate(nms);
			// long 으로 계산한다 — ticks 가 Integer.MAX_VALUE 근처면 int 뺄셈이 뒤집힌다
			long age = (long) rate - ticks;
			if(age < MIN_AGE) {
				age = MIN_AGE;
			} else if(age > MAX_AGE) {
				age = MAX_AGE;
			}
			ageField.setInt(nms, (int) age);
			return rate - (int) age;
		} catch (ReflectiveOperationException ex) {
			throw new CREPluginInternalException("Failed to set the item despawn time."
					+ " The server version may not match the expected NMS mappings (1.21.x): " + ex, t);
		}
	}
}
