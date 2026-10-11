package kr.asmbl.chfeatures;

import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.abstraction.MCPlayer;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.ArgumentValidation;
import com.laytonsmith.core.MSVersion;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CBoolean;
import com.laytonsmith.core.constructs.CInt;
import com.laytonsmith.core.constructs.CNull;
import com.laytonsmith.core.constructs.CVoid;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.exceptions.CRE.CRECastException;
import com.laytonsmith.core.exceptions.CRE.CREFormatException;
import com.laytonsmith.core.exceptions.CRE.CRELengthException;
import com.laytonsmith.core.exceptions.CRE.CREPlayerOfflineException;
import com.laytonsmith.core.exceptions.CRE.CREPluginInternalException;
import com.laytonsmith.core.exceptions.CRE.CREThrowable;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.AbstractFunction;
import com.laytonsmith.core.natives.interfaces.Mixed;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * FiguraController(서버가 주인인 Figura 상태 · 신호 플러그인) 연동 함수 — 옛 CH 「플레이어 피구라 컨트롤러」를 갈음한다.
 *
 * 중요: 이 파일의 @api 클래스에는 FiguraController 타입을 직접 참조하지 않는다(시그니처 포함 — provided 의존성).
 * 플러그인 API 호출은 전부 FcBridge 에 위임한다 (FsbFunctions 와 같은 격리 · CLAUDE.md 핵심 제약 2 · 3).
 * 대상 = 플레이어 이름 · UUID(플레이어 · 몹) · 'global'. 값 = null · boolean · int · double · string · array(묶음).
 */
public class FcFunctions {

	public static String docs() {
		return "Functions for the FiguraController plugin: server-owned Figura states (players, mobs, global) and signals.";
	}

	private static void checkFc(Target t) {
		if(!Bukkit.getPluginManager().isPluginEnabled("FiguraController")) {
			throw new CREPluginInternalException("FiguraController is not installed or enabled on this server.", t);
		}
	}

	/** null = 전역 */
	static UUID subject(Mixed m, Target t) {
		String s = m.val();
		if(s.equalsIgnoreCase("global")) {
			return null;
		}
		if(s.length() == 36 || s.length() == 32) {
			try {
				return Static.GetUUID(s, t);
			} catch(ConfigRuntimeException ex) {
				// 이름이다
			}
		}
		Player p = Bukkit.getPlayerExact(s);
		if(p != null) {
			return p.getUniqueId();
		}
		OfflinePlayer op = Bukkit.getOfflinePlayerIfCached(s);
		if(op != null) {
			return op.getUniqueId();
		}
		throw new CREPlayerOfflineException("Unknown FiguraController subject: " + s
				+ " (player name, UUID or 'global').", t);
	}

	/** 옵션 배열 → (persist, private, expire) */
	static FcOptions options(Mixed m, Target t) {
		boolean persist = true;
		boolean priv = false;
		int expire = 0;
		if(m != null && !(m instanceof CNull)) {
			if(!(m instanceof CArray a) || !a.isAssociative()) {
				throw new CRECastException("Options must be an associative array (persist, private, expire).", t);
			}
			for(Mixed k : a.keySet()) {
				String key = k.val();
				Mixed v = a.get(k, t);
				switch(key) {
					case "persist" -> persist = ArgumentValidation.getBooleanObject(v, t);
					case "private" -> priv = ArgumentValidation.getBooleanObject(v, t);
					case "expire" -> expire = ArgumentValidation.getInt32(v, t);
					default -> throw new CREFormatException("Unknown option: " + key + " (persist, private, expire).", t);
				}
			}
			if(expire < 0) {
				throw new CREFormatException("expire must be 0 or more ticks.", t);
			}
		}
		return new FcOptions(persist, priv, expire);
	}

	/** 플러그인이 던진 것 → CH 예외 */
	static RuntimeException wrap(RuntimeException e, Target t) {
		if(e instanceof ConfigRuntimeException) {
			return e;
		}
		String msg = e.getMessage() == null ? e.toString() : e.getMessage();
		if(e instanceof IllegalStateException) {
			return new CREPluginInternalException(msg, t);
		}
		if(msg.contains("too large") || msg.contains("너무 크다")) {
			return new CRELengthException(msg, t);
		}
		return new CREFormatException(msg, t);
	}

	private abstract static class FcFunction extends AbstractFunction {

		@Override
		public boolean isRestricted() {
			return true;
		}

		@Override
		public Boolean runAsync() {
			return false;
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPluginInternalException.class, CREPlayerOfflineException.class,
					CRECastException.class, CREFormatException.class, CRELengthException.class};
		}

		@Override
		public final Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			checkFc(t);
			try {
				return run(t, args);
			} catch(RuntimeException e) {
				throw wrap(e, t);
			}
		}

		abstract Mixed run(Target t, Mixed... args);
	}

	@api
	public static class fc_state_get extends FcFunction {
		@Override
		Mixed run(Target t, Mixed... args) {
			return FcBridge.get(subject(args[0], t), args[1].val(), t);
		}

		@Override
		public String getName() {
			return "fc_state_get";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2};
		}

		@Override
		public String docs() {
			return "mixed {subject, name} Returns a FiguraController state of the subject (player name, UUID of a player"
					+ " or mob, or 'global'), or null when it does not exist. Only the subject's own value — no fallback to"
					+ " the global value (avatars do fall back on the client).";
		}
	}

	@api
	public static class fc_state_get_all extends FcFunction {
		@Override
		Mixed run(Target t, Mixed... args) {
			return FcBridge.getAll(subject(args[0], t), t);
		}

		@Override
		public String getName() {
			return "fc_state_get_all";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1};
		}

		@Override
		public String docs() {
			return "array {subject} Returns all FiguraController states of the subject (player name, UUID or 'global')"
					+ " as an associative array (a copy).";
		}
	}

	@api
	public static class fc_state_set extends FcFunction {
		@Override
		Mixed run(Target t, Mixed... args) {
			FcOptions o = options(args.length > 3 ? args[3] : null, t);
			UUID s = subject(args[0], t);
			if(s == null && o.priv()) {
				throw new CREFormatException("A global state cannot be private.", t);
			}
			return CBoolean.get(FcBridge.set(s, args[1].val(), FcBridge.toJson(args[2], t), o));
		}

		@Override
		public String getName() {
			return "fc_state_set";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{3, 4};
		}

		@Override
		public String docs() {
			return "boolean {subject, name, value, [options]} Sets a FiguraController state. Returns false when the value"
					+ " and visibility did not change (nothing is sent then). Clients receive it at the end of the tick"
					+ " (several sets in one tick send only the last value). Options (associative array): persist (boolean,"
					+ " default true — saved by the plugin), private (boolean, default false — players: only that player's"
					+ " client; mobs: server only), expire (int ticks, default 0 — removed automatically after that).";
		}
	}

	@api
	public static class fc_states_set extends FcFunction {
		@Override
		Mixed run(Target t, Mixed... args) {
			if(!(args[1] instanceof CArray a) || !a.isAssociative()) {
				throw new CRECastException("Expecting an associative array of name: value.", t);
			}
			FcOptions o = options(args.length > 2 ? args[2] : null, t);
			UUID s = subject(args[0], t);
			if(s == null && o.priv()) {
				throw new CREFormatException("A global state cannot be private.", t);
			}
			int n = 0;
			for(Mixed k : a.keySet()) {
				if(FcBridge.set(s, k.val(), FcBridge.toJson(a.get(k, t), t), o)) {
					n++;
				}
			}
			return new CInt(n, t);
		}

		@Override
		public String getName() {
			return "fc_states_set";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2, 3};
		}

		@Override
		public String docs() {
			return "int {subject, states, [options]} Sets several FiguraController states at once from an associative"
					+ " array of name: value (same options as fc_state_set, applied to all). Returns how many changed.";
		}
	}

	@api
	public static class fc_state_unset extends FcFunction {
		@Override
		Mixed run(Target t, Mixed... args) {
			return CBoolean.get(FcBridge.unset(subject(args[0], t), args[1].val()));
		}

		@Override
		public String getName() {
			return "fc_state_unset";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2};
		}

		@Override
		public String docs() {
			return "boolean {subject, name} Removes a FiguraController state. Returns whether it existed.";
		}
	}

	@api
	public static class fc_state_clear extends FcFunction {
		@Override
		Mixed run(Target t, Mixed... args) {
			return new CInt(FcBridge.clear(subject(args[0], t)), t);
		}

		@Override
		public String getName() {
			return "fc_state_clear";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1};
		}

		@Override
		public String docs() {
			return "int {subject} Removes all FiguraController states of the subject. Returns how many were removed.";
		}
	}

	@api
	public static class fc_signal_send extends FcFunction {
		@Override
		Mixed run(Target t, Mixed... args) {
			UUID s = subject(args[0], t);
			Mixed data = args.length > 2 ? args[2] : CNull.NULL;
			String audience = null;
			List<UUID> players = null;
			if(args.length > 3 && !(args[3] instanceof CNull)) {
				if(args[3] instanceof CArray arr) {
					players = new ArrayList<>();
					for(Mixed m : arr.asList()) {
						UUID u = subject(m, t);
						if(u != null) {
							players.add(u);
						}
					}
				} else {
					audience = args[3].val().toLowerCase(Locale.ROOT);
					if(!audience.equals("self") && !audience.equals("trackers") && !audience.equals("all")) {
						throw new CREFormatException("Audience must be 'self', 'trackers', 'all' or an array of players.", t);
					}
				}
			}
			FcBridge.signal(s, args[1].val(), FcBridge.toJson(data, t), audience, players);
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "fc_signal_send";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2, 3, 4};
		}

		@Override
		public String docs() {
			return "void {subject, name, [data], [audience]} Sends a one-shot signal to the subject's avatar (not stored)."
					+ " Audience: 'trackers' (default — clients that see the subject, plus the player itself), 'self',"
					+ " 'all' (every ready client — use this for global staging so that spectating staff see it too), or an"
					+ " array of players. Global signals ('global' subject) always go to everyone unless a player array is"
					+ " given. Signals are sent at the end of the tick, after state changes of the same tick.";
		}
	}

	@api
	public static class fc_preset_load extends FcFunction {
		@Override
		Mixed run(Target t, Mixed... args) {
			List<UUID> subjects = new ArrayList<>();
			if(args[1] instanceof CArray arr) {
				for(Mixed m : arr.asList()) {
					subjects.add(subject(m, t));
				}
			} else {
				subjects.add(subject(args[1], t));
			}
			if(subjects.contains(null)) {
				throw new CREFormatException("Use '/fc preset load <name> global' for the global scope — fc_preset_load takes"
						+ " players and mobs.", t);
			}
			return new CInt(FcBridge.applyPreset(args[0].val(), subjects), t);
		}

		@Override
		public String getName() {
			return "fc_preset_load";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2};
		}

		@Override
		public String docs() {
			return "int {preset, subject | preset, array} Applies a FiguraController preset (plugins/FiguraController/"
					+ "presets/<preset>.json) to one or more players / mobs. Returns how many states changed.";
		}
	}

	@api
	public static class fc_is_ready extends FcFunction {
		@Override
		Mixed run(Target t, Mixed... args) {
			MCPlayer p = Static.GetPlayer(args[0], t);
			return CBoolean.get(FcBridge.isReady(((Player) p.getHandle()).getUniqueId()));
		}

		@Override
		public String getName() {
			return "fc_is_ready";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1};
		}

		@Override
		public String docs() {
			return "boolean {player} Whether the player's client announced itself to FiguraController (a Figura core that"
					+ " receives server_data). False for old cores and players without Figura.";
		}
	}
}
