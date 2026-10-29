package kr.asmbl.chfeatures;

import com.laytonsmith.PureUtilities.SimpleVersion;
import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.extensions.AbstractExtension;
import com.laytonsmith.core.extensions.MSExtension;

@MSExtension("CHFeatures")
public class Extension extends AbstractExtension {

	private static final Version version = new SimpleVersion(0, 11, 0);

	@Override
	public Version getVersion() {
		return version;
	}

	@Override
	public void onStartup() {
		try {
			ChfListener.register();
		} catch (Throwable ex) {
			// Bukkit이 없는 환경(cmdline/json-api 등)에서는 리스너 등록을 건너뛴다.
		}
		try {
			FcBridge.register(); // FiguraController 가 켜져 있을 때만 (fc_state_changed · fc_signal)
		} catch (Throwable ex) {
			// 플러그인이 없거나 Bukkit 이 없는 환경
		}
		try {
			DialogBridge.register(); // 바닐라 다이얼로그 클릭 (chf_dialog_click)
		} catch (Throwable ex) {
			// Bukkit 이 없는 환경 · 다이얼로그 API 가 없는 서버
		}
		Static.getLogger().info("CHFeatures " + getVersion() + " loaded.");
	}

	@Override
	public void onShutdown() {
		try {
			ChfListener.unregister();
		} catch (Throwable ex) {
			// 등록된 적 없으면 무시
		}
		try {
			FcBridge.unregister();
		} catch (Throwable ex) {
			// 등록된 적 없으면 무시
		}
		try {
			DialogBridge.unregister();
		} catch (Throwable ex) {
			// 등록된 적 없으면 무시
		}
		Static.getLogger().info("CHFeatures " + getVersion() + " unloaded.");
	}
}
