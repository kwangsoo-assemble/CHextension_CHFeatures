package kr.asmbl.chfeatures;

import com.laytonsmith.commandhelper.CommandHelperPlugin;
import com.laytonsmith.core.events.Driver;
import com.laytonsmith.core.events.EventUtils;

import io.papermc.paper.connection.PlayerCommonConnection;
import io.papermc.paper.connection.PlayerGameConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.api.BinaryTagHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 바닐라 다이얼로그(1.21.6+) — Paper 다이얼로그 API(@ApiStatus.Experimental)는 이 클래스에만 둔다.
 *
 * 버튼 · 나가기 버튼은 전부 사용자 지정 클릭 "chf:dialog" 이고 payload 는 compound {c:"<seq>:<번호>"} 다
 * (번호 -1 = 나가기). 클라는 ESC 로 닫아도 나가기 동작을 보낸다(1.21.8 DialogScreen.onClose → onCancel) —
 * 그래서 서버가 닫힘을 안다. 동작 없는 버튼은 패킷 없이 닫히므로 만들지 않는다.
 *
 * 무어노테이션 클래스 — json-api 가 로드하지 않는다. 등록은 Extension.onStartup 의 try/catch 안.
 */
final class DialogBridge implements Listener {

	private static final Key CLICK_KEY = Key.key("chf", "dialog");
	/** payload {c:"<seq>:<번호>"} — Paper 는 사용자 지정 클릭 payload 로 compound 태그만 받는다(글자 태그면 IllegalArgumentException). */
	private static final Pattern PAYLOAD = Pattern.compile("(\\d+):(-?\\d+)");
	private static final int KEEP = 8;   // 늦은 클릭을 풀기 위해 플레이어마다 남기는 최근 장 수

	private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

	private static DialogBridge listener;

	/** 플레이어 → 최근에 띄운 장(seq → spec) · 지금 장 seq · 상태. */
	private static final class Session {
		final LinkedHashMap<Integer, DialogSpec> recent = new LinkedHashMap<>();
		int seq;
		int currentSeq = -1;
		String state;   // open · waiting · null(닫힘)
	}

	private static final Map<UUID, Session> sessions = new HashMap<>();

	private DialogBridge() {
	}

	static void register() {
		if(listener == null) {
			listener = new DialogBridge();
		}
		CommandHelperPlugin.self.registerEvents(listener);
	}

	static void unregister() {
		if(listener != null) {
			PlayerCustomClickEvent.getHandlerList().unregister(listener);
			PlayerQuitEvent.getHandlerList().unregister(listener);
		}
		sessions.clear();
	}

	// ── 띄우기 · 닫기 · 상태 ──
	static void show(Player player, DialogSpec spec) {
		Session s = sessions.computeIfAbsent(player.getUniqueId(), k -> new Session());
		int seq = ++s.seq;
		s.recent.put(seq, spec);
		while(s.recent.size() > KEEP) {
			Integer oldest = s.recent.keySet().iterator().next();
			s.recent.remove(oldest);
		}
		s.currentSeq = seq;
		s.state = "open";
		player.showDialog(build(spec, seq));
	}

	static void close(Player player) {
		Session s = sessions.get(player.getUniqueId());
		if(s != null) {
			s.currentSeq = -1;
			s.state = null;
		}
		player.closeDialog();
	}

	/** {id, state} 또는 null — 이 플러그인이 띄운 것만 안다. */
	static String[] get(Player player) {
		Session s = sessions.get(player.getUniqueId());
		if(s == null || s.state == null) {
			return null;
		}
		DialogSpec spec = s.recent.get(s.currentSeq);
		return spec == null ? null : new String[]{spec.id, s.state};
	}

	private static Dialog build(DialogSpec spec, int seq) {
		List<DialogBody> body = new ArrayList<>();
		for(String line : spec.body) {
			body.add(DialogBody.plainMessage(text(line)));
		}
		List<DialogInput> inputs = new ArrayList<>();
		for(DialogSpec.TextInput in : spec.inputs) {
			inputs.add(DialogInput.text(in.key, text(in.label))
					.labelVisible(in.labelVisible)
					.initial(in.initial)
					.maxLength(in.maxLength)
					.width(in.width)
					.build());
		}
		DialogBase base = DialogBase.builder(text(spec.title))
				.canCloseWithEscape(spec.escape)
				.pause(false)
				.afterAction(afterAction(spec.after))
				.body(body)
				.inputs(inputs)
				.build();

		List<ActionButton> buttons = new ArrayList<>();
		for(int i = 0; i < spec.buttons.size(); i++) {
			buttons.add(button(spec.buttons.get(i), seq, i));
		}
		ActionButton exit = button(spec.exit, seq, -1);
		DialogType type = switch(spec.type) {
			case "notice" -> DialogType.notice(buttons.isEmpty() ? exit : buttons.get(0));
			case "confirm" -> DialogType.confirmation(buttons.get(0), buttons.size() > 1 ? buttons.get(1) : exit);
			default -> DialogType.multiAction(buttons).columns(spec.columns).exitAction(exit).build();
		};
		return Dialog.create(factory -> factory.empty().base(base).type(type));
	}

	private static ActionButton button(DialogSpec.Button b, int seq, int index) {
		ActionButton.Builder builder = ActionButton.builder(text(b.label))
				.width(b.width)
				.action(DialogAction.customClick(CLICK_KEY, BinaryTagHolder.binaryTagHolder("{c:\"" + seq + ":" + index + "\"}")));
		if(b.tooltip != null) {
			builder.tooltip(text(b.tooltip));
		}
		return builder.build();
	}

	private static DialogBase.DialogAfterAction afterAction(String after) {
		return switch(after) {
			case "none" -> DialogBase.DialogAfterAction.NONE;
			case "wait" -> DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE;
			default -> DialogBase.DialogAfterAction.CLOSE;
		};
	}

	private static Component text(String s) {
		return LEGACY.deserialize(s == null ? "" : s);
	}

	// ── 클릭 받기 ──
	@EventHandler
	public void onCustomClick(PlayerCustomClickEvent event) {
		if(!CLICK_KEY.equals(event.getIdentifier())) {
			return;   // 채팅 글의 사용자 지정 클릭 등 — 우리 것만
		}
		PlayerCommonConnection conn = event.getCommonConnection();
		if(!(conn instanceof PlayerGameConnection game)) {
			return;
		}
		Player player = game.getPlayer();
		BinaryTagHolder tag = event.getTag();
		if(tag == null) {
			return;
		}
		Matcher m = PAYLOAD.matcher(tag.string());
		if(!m.find()) {
			return;
		}
		int seq;
		int index;
		try {
			seq = Integer.parseInt(m.group(1));
			index = Integer.parseInt(m.group(2));
		} catch (NumberFormatException ex) {
			return;
		}
		DialogResponseView view = event.getDialogResponseView();
		Runnable fire = () -> dispatch(player, seq, index, view);
		if(Bukkit.isPrimaryThread()) {
			fire.run();
		} else {
			Bukkit.getScheduler().runTask(CommandHelperPlugin.self, fire);
		}
	}

	private static void dispatch(Player player, int seq, int index, DialogResponseView view) {
		if(!player.isOnline()) {
			return;
		}
		Session s = sessions.get(player.getUniqueId());
		DialogSpec spec = s == null ? null : s.recent.get(seq);
		if(spec == null) {
			return;   // 너무 오래된 장 · 리로드 전 장
		}
		boolean exit = index < 0;
		DialogSpec.Button b = exit ? spec.exit : (index < spec.buttons.size() ? spec.buttons.get(index) : null);
		if(b == null) {
			return;
		}
		boolean current = s.currentSeq == seq;
		if(current) {
			if(exit || "close".equals(spec.after)) {
				s.state = null;
			} else if("wait".equals(spec.after)) {
				s.state = "waiting";
			}
		}
		DialogClick click = new DialogClick(player.getName(), player.getUniqueId().toString(),
				spec.id, b.id, exit, seq, current);
		if(view != null) {
			for(DialogSpec.TextInput in : spec.inputs) {
				String v = view.getText(in.key);
				if(v != null) {
					click.inputs.put(in.key, v);
				}
			}
		}
		EventUtils.TriggerListener(Driver.EXTENSION, "chf_dialog_click", click);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		sessions.remove(event.getPlayer().getUniqueId());
	}
}
