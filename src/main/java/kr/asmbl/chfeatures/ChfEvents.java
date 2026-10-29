package kr.asmbl.chfeatures;

import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.MSVersion;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CBoolean;
import com.laytonsmith.core.constructs.CByteArray;
import com.laytonsmith.core.constructs.CNull;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.events.AbstractEvent;
import com.laytonsmith.core.events.BindableEvent;
import com.laytonsmith.core.events.Driver;
import com.laytonsmith.core.events.Prefilters;
import com.laytonsmith.core.events.Prefilters.PrefilterType;
import com.laytonsmith.core.exceptions.EventException;
import com.laytonsmith.core.exceptions.PrefilterNonMatchException;
import com.laytonsmith.core.natives.interfaces.Mixed;

import java.util.HashMap;
import java.util.Map;

/**
 * CHFeatures 이벤트.
 *
 * 중요: @api 클래스에서는 Bukkit/Paper 타입을 직접 참조하지 않는다 (json-api 안전).
 * Bukkit 이벤트 수신·가공은 ChfListener가 담당한다.
 */
public class ChfEvents {

	public static String docs() {
		return "Events added by CHFeatures.";
	}

	@api
	public static class player_recipe_book_toggle extends AbstractEvent {

		@Override
		public String getName() {
			return "player_recipe_book_toggle";
		}

		@Override
		public String docs() {
			return "{player: <string match> The player toggling the recipe book."
					+ " | type: <macro> The GUI the recipe book was toggled in."
					+ " | is_open: <boolean match> Whether the recipe book was opened.} "
					+ "This event is called when a player opens or closes a recipe book."
					+ " Filter (craftable-only) toggles do not fire this event."
					+ " The CRAFTING recipe book is shared between the player inventory and the crafting table,"
					+ " so type distinguishes the GUI it was toggled in:"
					+ " INVENTORY (2x2 player inventory), WORKBENCH (3x3 crafting table),"
					+ " FURNACE, BLAST_FURNACE, SMOKER. "
					+ "{player: The player. | type: The GUI type the recipe book was toggled in."
					+ " | is_open: True if the recipe book was opened, false if closed.} "
					+ "{} "
					+ "{}";
		}

		@Override
		public Driver driver() {
			return Driver.EXTENSION;
		}

		@Override
		public BindableEvent convert(CArray manualObject, Target t) {
			return null;
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}

		@Override
		public boolean matches(Map<String, Mixed> prefilter, BindableEvent e) throws PrefilterNonMatchException {
			if(!(e instanceof ChfListener.RecipeBookToggleEvent event)) {
				return false;
			}
			Prefilters.match(prefilter, "player", event.getPlayerName(), PrefilterType.STRING_MATCH);
			Prefilters.match(prefilter, "type", event.getGuiType(), PrefilterType.MACRO);
			Prefilters.match(prefilter, "is_open", event.isOpen() ? "true" : "false", PrefilterType.STRING_MATCH);
			return true;
		}

		@Override
		public Map<String, Mixed> evaluate(BindableEvent e) throws EventException {
			ChfListener.RecipeBookToggleEvent event = (ChfListener.RecipeBookToggleEvent) e;
			Map<String, Mixed> map = new HashMap<>();
			Target t = Target.UNKNOWN;
			map.put("player", new CString(event.getPlayerName(), t));
			map.put("type", new CString(event.getGuiType(), t));
			map.put("is_open", CBoolean.get(event.isOpen()));
			return map;
		}

		@Override
		public boolean modifyEvent(String key, Mixed value, BindableEvent e) {
			return false;
		}
	}

	/**
	 * 관전 대상에서 빠져나옴 — Paper PlayerStopSpectatingEntityEvent(취소 가능).
	 * 시프트로 나오기를 막는 데 쓴다 · 코어 player_teleport 로는 안 잡힌다.
	 */
	@api
	public static class player_stop_spectating extends AbstractEvent {

		@Override
		public String getName() {
			return "player_stop_spectating";
		}

		@Override
		public String docs() {
			return "{player: <string match> The spectator."
					+ " | target_type: <macro> The entity type the player was spectating.} "
					+ "This event is called when a spectator stops spectating an entity (sneaking out, the target"
					+ " disappearing, a game mode change, or /spectate with no target). Cancelling keeps the player"
					+ " attached to the target. The core player_teleport event does not fire for this. "
					+ "{player: The spectator's name."
					+ " | target: UUID of the entity that was spectated, or null if it is gone."
					+ " | target_type: Its entity type, or null."
					+ " | target_name: Its player name when the target is a player, otherwise null.} "
					+ "{} "
					+ "{}";
		}

		@Override
		public Driver driver() {
			return Driver.EXTENSION;
		}

		@Override
		public BindableEvent convert(CArray manualObject, Target t) {
			return null;
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}

		@Override
		public boolean matches(Map<String, Mixed> prefilter, BindableEvent e) throws PrefilterNonMatchException {
			if(!(e instanceof ChfListener.StopSpectatingEvent event)) {
				return false;
			}
			Prefilters.match(prefilter, "player", event.getPlayerName(), PrefilterType.STRING_MATCH);
			if(prefilter.containsKey("target_type")) {
				if(event.getTargetType() == null) {
					return false;
				}
				Prefilters.match(prefilter, "target_type", event.getTargetType(), PrefilterType.MACRO);
			}
			return true;
		}

		@Override
		public Map<String, Mixed> evaluate(BindableEvent e) throws EventException {
			ChfListener.StopSpectatingEvent event = (ChfListener.StopSpectatingEvent) e;
			Map<String, Mixed> map = new HashMap<>();
			Target t = Target.UNKNOWN;
			map.put("player", new CString(event.getPlayerName(), t));
			map.put("target", event.getTargetUuid() == null ? CNull.NULL : new CString(event.getTargetUuid(), t));
			map.put("target_type", event.getTargetType() == null ? CNull.NULL : new CString(event.getTargetType(), t));
			map.put("target_name", event.getTargetName() == null ? CNull.NULL : new CString(event.getTargetName(), t));
			return map;
		}

		@Override
		public boolean modifyEvent(String key, Mixed value, BindableEvent e) {
			return false;
		}
	}

	@api
	public static class fsb_server_packet extends AbstractEvent {

		@Override
		public String getName() {
			return "fsb_server_packet";
		}

		@Override
		public String docs() {
			return "{packet_name: <macro> The registered packet name."
					+ " | player: <string match> The sending player.} "
					+ "This event is called when a Figura avatar script sends a named server packet through the"
					+ " FiguraFSB plugin via server_packets:sendPacket(). Only packet names registered with"
					+ " fsb_register_packet() fire this event. "
					+ "{packet_name: The name the packet was sent under."
					+ " | player: The sending player's name, or null if they went offline."
					+ " | sender_uuid: UUID of the sending player (connection owner)."
					+ " | avatar_owner: UUID of the owner of the avatar whose script sent the packet."
					+ " | is_host: True unless the packet came from another player's avatar loaded on the"
					+ " sender's client (only possible with allowNonHostPackets enabled in the FSB config)."
					+ " | data: The raw payload as a byte_array.} "
					+ "{} "
					+ "{}";
		}

		@Override
		public Driver driver() {
			return Driver.EXTENSION;
		}

		@Override
		public BindableEvent convert(CArray manualObject, Target t) {
			return null;
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}

		@Override
		public boolean matches(Map<String, Mixed> prefilter, BindableEvent e) throws PrefilterNonMatchException {
			if(!(e instanceof FsbBridge.FsbPacketEvent event)) {
				return false;
			}
			Prefilters.match(prefilter, "packet_name", event.getPacketName(), PrefilterType.MACRO);
			if(event.getSenderName() != null) {
				Prefilters.match(prefilter, "player", event.getSenderName(), PrefilterType.STRING_MATCH);
			}
			return true;
		}

		@Override
		public Map<String, Mixed> evaluate(BindableEvent e) throws EventException {
			FsbBridge.FsbPacketEvent event = (FsbBridge.FsbPacketEvent) e;
			Map<String, Mixed> map = new HashMap<>();
			Target t = Target.UNKNOWN;
			map.put("packet_name", new CString(event.getPacketName(), t));
			map.put("player", event.getSenderName() == null
					? CNull.NULL : new CString(event.getSenderName(), t));
			map.put("sender_uuid", new CString(event.getSenderUuid().toString(), t));
			map.put("avatar_owner", new CString(event.getAvatarOwner().toString(), t));
			map.put("is_host", CBoolean.get(event.getSenderUuid().equals(event.getAvatarOwner())));
			map.put("data", CByteArray.wrap(event.getData(), t));
			return map;
		}

		@Override
		public boolean modifyEvent(String key, Mixed value, BindableEvent e) {
			return false;
		}
	}

	/**
	 * FiguraController 상태가 바뀌었다. ★ name 프리필터가 **필수**다 — 없으면 bind 가 오류다(모든 변화를 CH 로 올리면 무겁다 · 설계 §6).
	 * CH 는 등록(add) 뒤에 bind() 를 부르므로, 던지기 전에 그 묶음을 먼저 푼다(안 풀면 오류인데도 돈다 — EventUtils.RegisterEvent 바이트코드 실측).
	 */
	@api
	public static class fc_state_changed extends AbstractEvent {

		@Override
		public String getName() {
			return "fc_state_changed";
		}

		@Override
		public String docs() {
			return "{name: <macro> The state name — REQUIRED (binding without it is an error)."
					+ " | subject: <string match> 'global', or the subject UUID."
					+ " | player: <string match> The player name when the subject is an online player."
					+ " | cause: <string match> API, COMMAND, PRESET, EXPIRE or CLEAR.} "
					+ "Fires after a FiguraController state changed (not when it was set to the same value). "
					+ "{name: The state name."
					+ " | subject: 'global' or the subject's UUID."
					+ " | player: The player's name when the subject is an online player, otherwise null."
					+ " | old: The previous value (null when it did not exist)."
					+ " | new: The new value (null when it was removed)."
					+ " | existed: Whether the state existed before."
					+ " | exists: Whether the state exists now (false = removed)."
					+ " | cause: API, COMMAND, PRESET, EXPIRE or CLEAR.} "
					+ "{} "
					+ "{}";
		}

		@Override
		public void bind(com.laytonsmith.core.events.BoundEvent event) {
			Map<String, Mixed> p = event.getPrefilter();
			if(p == null || !p.containsKey("name")) {
				com.laytonsmith.core.events.EventUtils.UnregisterEvent(event.getId());
				throw new com.laytonsmith.core.exceptions.CRE.CREBindException("fc_state_changed needs a name prefilter"
						+ " (array(name: 'skin') or a macro) — binding every state change would make CommandHelper run on"
						+ " every change.", event.getTarget());
			}
			super.bind(event);
		}

		@Override
		public Driver driver() {
			return Driver.EXTENSION;
		}

		@Override
		public BindableEvent convert(CArray manualObject, Target t) {
			return null;
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}

		@Override
		public boolean matches(Map<String, Mixed> prefilter, BindableEvent e) throws PrefilterNonMatchException {
			if(!(e instanceof FcBridge.StateChanged event)) {
				return false;
			}
			Prefilters.match(prefilter, "name", event.name, PrefilterType.MACRO);
			Prefilters.match(prefilter, "subject", event.subject == null ? "global" : event.subject.toString(), PrefilterType.STRING_MATCH);
			Prefilters.match(prefilter, "cause", event.cause, PrefilterType.STRING_MATCH);
			if(prefilter.containsKey("player")) {
				if(event.player == null) {
					return false;
				}
				Prefilters.match(prefilter, "player", event.player, PrefilterType.STRING_MATCH);
			}
			return true;
		}

		@Override
		public Map<String, Mixed> evaluate(BindableEvent e) throws EventException {
			FcBridge.StateChanged event = (FcBridge.StateChanged) e;
			Map<String, Mixed> map = new HashMap<>();
			Target t = Target.UNKNOWN;
			map.put("name", new CString(event.name, t));
			map.put("subject", new CString(event.subject == null ? "global" : event.subject.toString(), t));
			map.put("player", event.player == null ? CNull.NULL : new CString(event.player, t));
			map.put("old", FcBridge.toMixed(event.oldValue, t));
			map.put("new", FcBridge.toMixed(event.newValue, t));
			map.put("existed", CBoolean.get(event.oldValue != null));
			map.put("exists", CBoolean.get(event.newValue != null));
			map.put("cause", new CString(event.cause, t));
			return map;
		}

		@Override
		public boolean modifyEvent(String key, Mixed value, BindableEvent e) {
			return false;
		}
	}

	/** 아바타(호스트 플레이어) → 서버 신호 — Lua `server_data.send(name, data)` */
	@api
	public static class fc_signal extends AbstractEvent {

		@Override
		public String getName() {
			return "fc_signal";
		}

		@Override
		public String docs() {
			return "{name: <macro> The signal name."
					+ " | player: <string match> The sending player.} "
					+ "Fires when a player's own (host) Figura avatar sends a signal through FiguraController"
					+ " (server_data.send(name, data) in Lua). Signals from other players' or mob avatars are dropped"
					+ " by the plugin. "
					+ "{name: The signal name."
					+ " | player: The sending player's name."
					+ " | uuid: The sending player's UUID."
					+ " | data: The data (null, boolean, int, double, string or array).} "
					+ "{} "
					+ "{}";
		}

		@Override
		public Driver driver() {
			return Driver.EXTENSION;
		}

		@Override
		public BindableEvent convert(CArray manualObject, Target t) {
			return null;
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}

		@Override
		public boolean matches(Map<String, Mixed> prefilter, BindableEvent e) throws PrefilterNonMatchException {
			if(!(e instanceof FcBridge.SignalReceived event)) {
				return false;
			}
			Prefilters.match(prefilter, "name", event.name, PrefilterType.MACRO);
			Prefilters.match(prefilter, "player", event.player, PrefilterType.STRING_MATCH);
			return true;
		}

		@Override
		public Map<String, Mixed> evaluate(BindableEvent e) throws EventException {
			FcBridge.SignalReceived event = (FcBridge.SignalReceived) e;
			Map<String, Mixed> map = new HashMap<>();
			Target t = Target.UNKNOWN;
			map.put("name", new CString(event.name, t));
			map.put("player", new CString(event.player, t));
			map.put("uuid", new CString(event.uuid.toString(), t));
			map.put("data", FcBridge.toMixed(event.data, t));
			return map;
		}

		@Override
		public boolean modifyEvent(String key, Mixed value, BindableEvent e) {
			return false;
		}
	}

	/**
	 * chf_show_dialog 로 띄운 다이얼로그의 버튼 · 나가기 버튼 · ESC — DialogBridge 가 PlayerCustomClickEvent 를 받아 넘긴다.
	 */
	@api
	public static class chf_dialog_click extends AbstractEvent {

		@Override
		public String getName() {
			return "chf_dialog_click";
		}

		@Override
		public String docs() {
			return "{player: <string match> The clicking player."
					+ " | dialog: <macro> The dialog id given to chf_show_dialog()."
					+ " | button: <macro> The button id.} "
					+ "Fires when a player presses a button of a dialog shown with chf_show_dialog(), its exit button,"
					+ " or Escape (Escape sends the exit action). Clicks of a dialog that was already replaced still fire,"
					+ " with current false. "
					+ "{player: The player's name. | uuid: The player's UUID. | dialog: The dialog id."
					+ " | button: The button id (the exit button's id for exit/Escape). | exit: True for the exit button or Escape."
					+ " | current: False if another dialog was shown to the player after this one."
					+ " | inputs: Associative array of text input key to the entered text.} "
					+ "{} "
					+ "{}";
		}

		@Override
		public Driver driver() {
			return Driver.EXTENSION;
		}

		@Override
		public BindableEvent convert(CArray manualObject, Target t) {
			return null;
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}

		@Override
		public boolean matches(Map<String, Mixed> prefilter, BindableEvent e) throws PrefilterNonMatchException {
			if(!(e instanceof DialogClick event)) {
				return false;
			}
			Prefilters.match(prefilter, "player", event.player, PrefilterType.STRING_MATCH);
			Prefilters.match(prefilter, "dialog", event.dialog, PrefilterType.MACRO);
			Prefilters.match(prefilter, "button", event.button, PrefilterType.MACRO);
			return true;
		}

		@Override
		public Map<String, Mixed> evaluate(BindableEvent e) throws EventException {
			DialogClick event = (DialogClick) e;
			Map<String, Mixed> map = new HashMap<>();
			Target t = Target.UNKNOWN;
			map.put("player", new CString(event.player, t));
			map.put("uuid", new CString(event.uuid, t));
			map.put("dialog", new CString(event.dialog, t));
			map.put("button", new CString(event.button, t));
			map.put("exit", CBoolean.get(event.exit));
			map.put("current", CBoolean.get(event.current));
			CArray inputs = CArray.GetAssociativeArray(t);
			for(Map.Entry<String, String> in : event.inputs.entrySet()) {
				inputs.set(in.getKey(), new CString(in.getValue(), t), t);
			}
			map.put("inputs", inputs);
			return map;
		}

		@Override
		public boolean modifyEvent(String key, Mixed value, BindableEvent e) {
			return false;
		}
	}
}
