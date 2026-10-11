package kr.asmbl.chfeatures;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.laytonsmith.commandhelper.CommandHelperPlugin;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CBoolean;
import com.laytonsmith.core.constructs.CDouble;
import com.laytonsmith.core.constructs.CInt;
import com.laytonsmith.core.constructs.CNull;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.events.BindableEvent;
import com.laytonsmith.core.events.Driver;
import com.laytonsmith.core.events.EventUtils;
import com.laytonsmith.core.exceptions.CRE.CRECastException;
import com.laytonsmith.core.exceptions.CRE.CREFormatException;
import com.laytonsmith.core.natives.interfaces.Mixed;

import kr.asmbl.figuracontroller.FiguraController;
import kr.asmbl.figuracontroller.api.Audience;
import kr.asmbl.figuracontroller.api.FiguraControllerAPI;
import kr.asmbl.figuracontroller.api.FiguraSignalReceiveEvent;
import kr.asmbl.figuracontroller.api.FiguraStateChangeEvent;
import kr.asmbl.figuracontroller.api.StateOptions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FiguraController 플러그인 API 를 실제로 만지는 유일한 클래스 (FsbBridge 와 같은 격리 — CLAUDE.md 핵심 제약 3).
 * 어노테이션 없음 · FcFunctions · ChfEvents 는 플러그인이 켜져 있는지 본 뒤 여기로 위임한다.
 * <p>
 * 값 변환은 CH {@code json_encode}/{@code json_decode} 를 거치지 않고 직접 한다 — CH 의 decode 는 {@code 1.0} 을 정수 1 로 바꿔
 * 서버가 준 실수를 CH 가 다시 쓰면 «값이 바뀌었다» 가 헛나온다(2026-10-10 실측). 직접 바꾸면 정수 ↔ INT · 실수 ↔ DOUBLE 이 그대로 간다.
 * 대상 {@code null} = 전역.
 */
final class FcBridge {

	private FcBridge() {
	}

	private static FiguraControllerAPI api() {
		return FiguraController.api();
	}

	// ── 값 변환

	static JsonElement toJson(Mixed m, Target t) {
		return toJson(m, t, 0);
	}

	private static JsonElement toJson(Mixed m, Target t, int depth) {
		if(m == null || m instanceof CNull) {
			return JsonNull.INSTANCE;
		}
		if(m instanceof CBoolean b) {
			return new JsonPrimitive(b.getBoolean());
		}
		if(m instanceof CInt i) {
			return new JsonPrimitive(i.getInt());
		}
		if(m instanceof CDouble d) {
			return new JsonPrimitive(d.getDouble());
		}
		if(m instanceof CString) {
			return new JsonPrimitive(m.val());
		}
		if(m instanceof CArray a) {
			if(depth >= 32) {
				throw new CREFormatException("Value is nested too deep (32).", t);
			}
			if(a.isAssociative()) {
				JsonObject o = new JsonObject();
				for(Mixed k : a.keySet()) {
					o.add(k.val(), toJson(a.get(k, t), t, depth + 1));
				}
				return o;
			}
			JsonArray arr = new JsonArray();
			for(Mixed v : a.asList()) {
				arr.add(toJson(v, t, depth + 1));
			}
			return arr;
		}
		throw new CRECastException("Cannot send a " + m.typeof().getSimpleName()
				+ " to Figura (null, boolean, int, double, string or array only).", t);
	}

	static Mixed toMixed(JsonElement e, Target t) {
		if(e == null || e.isJsonNull()) {
			return CNull.NULL;
		}
		if(e.isJsonPrimitive()) {
			JsonPrimitive p = e.getAsJsonPrimitive();
			if(p.isBoolean()) {
				return CBoolean.get(p.getAsBoolean());
			}
			if(p.isString()) {
				return new CString(p.getAsString(), t);
			}
			Number n = p.getAsNumber();
			if(n instanceof Double || n instanceof Float) {
				return new CDouble(n.doubleValue(), t);
			}
			if(n instanceof Long || n instanceof Integer || n instanceof Short || n instanceof Byte) {
				return new CInt(n.longValue(), t);
			}
			String s = n.toString();
			if(s.matches("-?[0-9]+")) {
				try {
					return new CInt(Long.parseLong(s), t);
				} catch(NumberFormatException ex) {
					return new CDouble(n.doubleValue(), t);
				}
			}
			return new CDouble(n.doubleValue(), t);
		}
		if(e.isJsonArray()) {
			CArray a = new CArray(t);
			for(JsonElement v : e.getAsJsonArray()) {
				a.push(toMixed(v, t), t);
			}
			return a;
		}
		CArray o = CArray.GetAssociativeArray(t);
		for(Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
			o.set(en.getKey(), toMixed(en.getValue(), t), t);
		}
		return o;
	}

	// ── 상태

	static Mixed get(UUID subject, String name, Target t) {
		JsonElement v = subject == null ? api().global().get(name).orElse(null) : api().get(subject, name).orElse(null);
		return v == null ? CNull.NULL : toMixed(v, t);
	}

	static CArray getAll(UUID subject, Target t) {
		Map<String, JsonElement> m = subject == null ? api().global().getAll() : api().getAll(subject);
		CArray out = CArray.GetAssociativeArray(t);
		for(Map.Entry<String, JsonElement> e : m.entrySet()) {
			out.set(e.getKey(), toMixed(e.getValue(), t), t);
		}
		return out;
	}

	static boolean set(UUID subject, String name, JsonElement value, FcOptions opt) {
		StateOptions o = new StateOptions(opt.persist(), opt.priv(), opt.expire());
		return subject == null ? api().global().set(name, value, o) : api().set(subject, name, value, o);
	}

	static boolean unset(UUID subject, String name) {
		return subject == null ? api().global().unset(name) : api().unset(subject, name);
	}

	static int clear(UUID subject) {
		return subject == null ? api().global().clear() : api().clear(subject);
	}

	/** @param audience null = 기본(대상 = TRACKERS · 전역 = 전원) · 이름 "self"/"trackers"/"all" · 또는 플레이어 UUID 목록 */
	static void signal(UUID subject, String name, JsonElement data, String audience, List<UUID> players) {
		Audience a;
		if(players != null) {
			a = Audience.players(players);
		} else if(audience == null) {
			a = subject == null ? Audience.ALL : Audience.TRACKERS;
		} else {
			a = Audience.parse(audience);
		}
		if(subject == null) {
			api().global().sendSignal(name, data, a);
		} else {
			api().sendSignal(subject, name, data, a);
		}
	}

	static int applyPreset(String preset, Collection<UUID> subjects) {
		return api().applyPreset(preset, subjects);
	}

	static boolean isReady(UUID player) {
		return api().isReady(player);
	}

	// ── 이벤트 (Bukkit → CH)

	private static Listener listener;

	/** Extension.onStartup — FiguraController 가 켜져 있을 때만 */
	static void register() {
		if(listener != null || !Bukkit.getPluginManager().isPluginEnabled("FiguraController")) {
			return;
		}
		listener = new FcListener();
		CommandHelperPlugin.self.registerEvents(listener);
	}

	static void unregister() {
		if(listener != null) {
			HandlerList.unregisterAll(listener);
			listener = null;
		}
	}

	public static final class FcListener implements Listener {
		@EventHandler(priority = EventPriority.MONITOR)
		public void onState(FiguraStateChangeEvent e) {
			UUID s = e.getSubject();
			Player p = s == null ? null : Bukkit.getPlayer(s);
			EventUtils.TriggerListener(Driver.EXTENSION, "fc_state_changed", new StateChanged(
					s, p == null ? null : p.getName(), e.getName(), e.getOldValue(), e.getNewValue(), e.getCause().name()));
		}

		@EventHandler(priority = EventPriority.MONITOR)
		public void onSignal(FiguraSignalReceiveEvent e) {
			EventUtils.TriggerListener(Driver.EXTENSION, "fc_signal", new SignalReceived(
					e.getPlayer().getUniqueId(), e.getPlayer().getName(), e.getName(), e.getData()));
		}
	}

	/** fc_state_changed — FiguraController 타입 없이 값만 */
	public static final class StateChanged implements BindableEvent {
		final UUID subject;
		final String player;
		final String name;
		final JsonElement oldValue;
		final JsonElement newValue;
		final String cause;

		StateChanged(UUID subject, String player, String name, JsonElement oldValue, JsonElement newValue, String cause) {
			this.subject = subject;
			this.player = player;
			this.name = name;
			this.oldValue = oldValue;
			this.newValue = newValue;
			this.cause = cause;
		}

		@Override
		public Object _GetObject() {
			return this;
		}
	}

	/** fc_signal — 아바타(호스트) → 서버 */
	public static final class SignalReceived implements BindableEvent {
		final UUID uuid;
		final String player;
		final String name;
		final JsonElement data;

		SignalReceived(UUID uuid, String player, String name, JsonElement data) {
			this.uuid = uuid;
			this.player = player;
			this.name = name;
			this.data = data;
		}

		@Override
		public Object _GetObject() {
			return this;
		}
	}
}
