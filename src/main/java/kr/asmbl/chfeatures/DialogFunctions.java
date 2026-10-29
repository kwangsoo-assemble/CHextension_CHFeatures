package kr.asmbl.chfeatures;

import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.ArgumentValidation;
import com.laytonsmith.core.MSVersion;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CNull;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.CVoid;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.exceptions.CRE.CRECastException;
import com.laytonsmith.core.exceptions.CRE.CREFormatException;
import com.laytonsmith.core.exceptions.CRE.CREIllegalArgumentException;
import com.laytonsmith.core.exceptions.CRE.CRELengthException;
import com.laytonsmith.core.exceptions.CRE.CREPlayerOfflineException;
import com.laytonsmith.core.exceptions.CRE.CREThrowable;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.AbstractFunction;
import com.laytonsmith.core.natives.interfaces.Mixed;

/**
 * 바닐라 다이얼로그 함수 — 본체는 DialogBridge(Paper 실험 API 격리).
 * 이 클래스는 CH 배열을 DialogSpec 으로 옮기기만 한다(Bukkit 타입은 exec 안에서만).
 */
public class DialogFunctions {

	public static String docs() {
		return "Vanilla dialog (1.21.6+) functions added by CHFeatures.";
	}

	private static final String SPEC_DOCS = " The dialog array: {id: string (required, comes back in chf_dialog_click),"
			+ " type: 'multi' (default, several buttons) | 'notice' (one button) | 'confirm' (two buttons),"
			+ " title: string, body: array of strings,"
			+ " inputs: array of {key (required), label, label_visible (default true), initial, max_length (default 32),"
			+ " width (default 200)} (text inputs only),"
			+ " buttons: array of {id (required), label, tooltip, width (default 150)},"
			+ " columns: int (default 2), exit: {id (default 'exit'), label (default '닫기'), width} (the exit button —"
			+ " pressing Escape sends it too), escape: boolean (default true), after: 'close' (default) | 'none' | 'wait'}."
			+ " Every button is a custom click handled by chf_dialog_click; a button without an id is an error because"
			+ " a vanilla button with no action closes the dialog without telling the server. Text uses § color codes.";

	@api
	public static class chf_show_dialog extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPlayerOfflineException.class, CRECastException.class, CREFormatException.class,
					CREIllegalArgumentException.class, CRELengthException.class};
		}

		@Override
		public boolean isRestricted() {
			return true;
		}

		@Override
		public Boolean runAsync() {
			return false;
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			org.bukkit.entity.Player player = (org.bukkit.entity.Player) Static.GetPlayer(args[0], t).getHandle();
			DialogSpec spec = parse(ArgumentValidation.getArray(args[1], t), t);
			DialogBridge.show(player, spec);
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "chf_show_dialog";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2};
		}

		@Override
		public String docs() {
			return "void {player, dialog} Shows a vanilla dialog to the player, replacing any dialog already open."
					+ SPEC_DOCS;
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	@api
	public static class chf_close_dialog extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPlayerOfflineException.class, CRELengthException.class};
		}

		@Override
		public boolean isRestricted() {
			return true;
		}

		@Override
		public Boolean runAsync() {
			return false;
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			org.bukkit.entity.Player player = (org.bukkit.entity.Player) Static.GetPlayer(args[0], t).getHandle();
			DialogBridge.close(player);
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "chf_close_dialog";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1};
		}

		@Override
		public String docs() {
			return "void {player} Closes the player's open dialog.";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	@api
	public static class chf_get_dialog extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPlayerOfflineException.class, CRELengthException.class};
		}

		@Override
		public boolean isRestricted() {
			return true;
		}

		@Override
		public Boolean runAsync() {
			return false;
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			org.bukkit.entity.Player player = (org.bukkit.entity.Player) Static.GetPlayer(args[0], t).getHandle();
			String[] open = DialogBridge.get(player);
			if(open == null) {
				return CNull.NULL;
			}
			CArray ret = CArray.GetAssociativeArray(t);
			ret.set("id", new CString(open[0], t), t);
			ret.set("state", new CString(open[1], t), t);
			return ret;
		}

		@Override
		public String getName() {
			return "chf_get_dialog";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1};
		}

		@Override
		public String docs() {
			return "array {player} Returns the dialog this extension believes is open for the player as"
					+ " {id, state} (state 'open', or 'waiting' after a button of an after:'wait' dialog), or null"
					+ " when closed. Closing is known from the exit button and Escape (both send the exit action),"
					+ " buttons of after:'close' dialogs, chf_close_dialog() and quitting. Dialogs shown by other"
					+ " plugins or commands are not tracked.";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	// ── CH 배열 → DialogSpec ──
	private static DialogSpec parse(CArray a, Target t) {
		DialogSpec spec = new DialogSpec();
		spec.id = requireString(a, "id", t);
		spec.type = optString(a, "type", "multi", t);
		if(!spec.type.equals("multi") && !spec.type.equals("notice") && !spec.type.equals("confirm")) {
			throw new CREIllegalArgumentException("type must be multi, notice or confirm: " + spec.type, t);
		}
		spec.title = optString(a, "title", "", t);
		if(a.containsKey("body") && !(a.get("body", t) instanceof CNull)) {
			CArray body = ArgumentValidation.getArray(a.get("body", t), t);
			for(int i = 0; i < body.size(); i++) {
				spec.body.add(body.get(i, t).val());
			}
		}
		if(a.containsKey("inputs") && !(a.get("inputs", t) instanceof CNull)) {
			CArray inputs = ArgumentValidation.getArray(a.get("inputs", t), t);
			for(int i = 0; i < inputs.size(); i++) {
				CArray in = ArgumentValidation.getArray(inputs.get(i, t), t);
				spec.inputs.add(new DialogSpec.TextInput(
						requireString(in, "key", t),
						optString(in, "label", "", t),
						optBool(in, "label_visible", true, t),
						optString(in, "initial", "", t),
						optInt(in, "max_length", 32, 1, Integer.MAX_VALUE, t),
						optInt(in, "width", 200, 1, 1024, t)));
			}
		}
		if(a.containsKey("buttons") && !(a.get("buttons", t) instanceof CNull)) {
			CArray buttons = ArgumentValidation.getArray(a.get("buttons", t), t);
			for(int i = 0; i < buttons.size(); i++) {
				spec.buttons.add(parseButton(ArgumentValidation.getArray(buttons.get(i, t), t), null, t));
			}
		}
		if(spec.type.equals("confirm") && spec.buttons.isEmpty()) {
			throw new CREIllegalArgumentException("confirm needs at least one button", t);
		}
		spec.columns = optInt(a, "columns", 2, 1, Integer.MAX_VALUE, t);
		if(a.containsKey("exit") && !(a.get("exit", t) instanceof CNull)) {
			spec.exit = parseButton(ArgumentValidation.getArray(a.get("exit", t), t), "exit", t);
		}
		spec.escape = optBool(a, "escape", true, t);
		spec.after = optString(a, "after", "close", t);
		if(!spec.after.equals("close") && !spec.after.equals("none") && !spec.after.equals("wait")) {
			throw new CREIllegalArgumentException("after must be close, none or wait: " + spec.after, t);
		}
		return spec;
	}

	private static DialogSpec.Button parseButton(CArray b, String defaultId, Target t) {
		String id = defaultId == null ? requireString(b, "id", t) : optString(b, "id", defaultId, t);
		String label = optString(b, "label", defaultId == null ? id : "닫기", t);
		String tooltip = b.containsKey("tooltip") && !(b.get("tooltip", t) instanceof CNull)
				? b.get("tooltip", t).val() : null;
		return new DialogSpec.Button(id, label, tooltip, optInt(b, "width", 150, 1, 1024, t));
	}

	private static String requireString(CArray a, String key, Target t) {
		if(!a.containsKey(key) || a.get(key, t) instanceof CNull) {
			throw new CREFormatException("dialog key '" + key + "' is required", t);
		}
		return a.get(key, t).val();
	}

	private static String optString(CArray a, String key, String def, Target t) {
		return a.containsKey(key) && !(a.get(key, t) instanceof CNull) ? a.get(key, t).val() : def;
	}

	private static boolean optBool(CArray a, String key, boolean def, Target t) {
		return a.containsKey(key) && !(a.get(key, t) instanceof CNull)
				? ArgumentValidation.getBooleanObject(a.get(key, t), t) : def;
	}

	private static int optInt(CArray a, String key, int def, int min, int max, Target t) {
		if(!a.containsKey(key) || a.get(key, t) instanceof CNull) {
			return def;
		}
		int v = ArgumentValidation.getInt32(a.get(key, t), t);
		if(v < min || v > max) {
			throw new CREIllegalArgumentException(key + " must be " + min + " ~ " + max + ": " + v, t);
		}
		return v;
	}
}
