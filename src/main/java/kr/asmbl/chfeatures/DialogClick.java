package kr.asmbl.chfeatures;

import com.laytonsmith.core.events.BindableEvent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * chf_dialog_click 이벤트의 값 — 순수 값(Bukkit 참조 0). DialogBridge 가 만들어 넘긴다.
 */
final class DialogClick implements BindableEvent {

	final String player;
	final String uuid;
	final String dialog;
	final String button;
	final boolean exit;
	final int seq;
	final boolean current;
	/** 입력 키 → 입력한 글(그 다이얼로그에 입력 칸이 없으면 빈 맵). */
	final Map<String, String> inputs = new LinkedHashMap<>();

	DialogClick(String player, String uuid, String dialog, String button, boolean exit, int seq, boolean current) {
		this.player = player;
		this.uuid = uuid;
		this.dialog = dialog;
		this.button = button;
		this.exit = exit;
		this.seq = seq;
		this.current = current;
	}

	@Override
	public Object _GetObject() {
		return this;
	}
}
