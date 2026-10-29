package kr.asmbl.chfeatures;

import java.util.ArrayList;
import java.util.List;

/**
 * chf_show_dialog 의 다이얼로그 한 장 — CH 배열을 옮겨 담은 순수 값(Bukkit · Paper 참조 0 · json-api 안전).
 * Paper 객체로 만드는 일은 DialogBridge 가 한다.
 */
final class DialogSpec {

	/** 버튼 하나 — id 는 필수(동작 없는 바닐라 버튼은 누르면 서버에 알리지 않고 조용히 닫힌다). */
	static final class Button {
		final String id;
		final String label;
		final String tooltip;
		final int width;

		Button(String id, String label, String tooltip, int width) {
			this.id = id;
			this.label = label;
			this.tooltip = tooltip;
			this.width = width;
		}
	}

	/** 글 입력 칸 하나. */
	static final class TextInput {
		final String key;
		final String label;
		final boolean labelVisible;
		final String initial;
		final int maxLength;
		final int width;

		TextInput(String key, String label, boolean labelVisible, String initial, int maxLength, int width) {
			this.key = key;
			this.label = label;
			this.labelVisible = labelVisible;
			this.initial = initial;
			this.maxLength = maxLength;
			this.width = width;
		}
	}

	String id;
	String type = "multi";          // multi · notice · confirm
	String title = "";
	final List<String> body = new ArrayList<>();
	final List<TextInput> inputs = new ArrayList<>();
	final List<Button> buttons = new ArrayList<>();
	int columns = 2;
	Button exit = new Button("exit", "닫기", null, 150);
	boolean escape = true;
	String after = "close";         // close · none · wait
}
