package kr.asmbl.chfeatures;

import com.laytonsmith.commandhelper.CommandHelperPlugin;
import com.laytonsmith.core.events.BindableEvent;
import com.laytonsmith.core.events.Driver;
import com.laytonsmith.core.events.EventUtils;

import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRecipeBookSettingsChangeEvent;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * CHFeatures의 Bukkit 이벤트 리스너.
 *
 * 중요: 어노테이션 없는 클래스 — ExtensionManager/json-api가 로드하지 않으므로
 * Bukkit 참조를 자유롭게 둘 수 있다. 등록은 Extension.onStartup에서 try/catch로 감싼다.
 */
public class ChfListener implements Listener {

	private static ChfListener listener;

	/** 플레이어별로 열려 있는 레시피북 종류 캐시. 필터 토글로 인한 중복 발동을 걸러낸다. */
	private final Map<UUID, EnumSet<PlayerRecipeBookSettingsChangeEvent.RecipeBookType>> openBooks = new HashMap<>();

	public static void register() {
		if(listener == null) {
			listener = new ChfListener();
		}
		CommandHelperPlugin.self.registerEvents(listener);
	}

	public static void unregister() {
		if(listener != null) {
			PlayerRecipeBookSettingsChangeEvent.getHandlerList().unregister(listener);
			PlayerQuitEvent.getHandlerList().unregister(listener);
			PlayerStopSpectatingEntityEvent.getHandlerList().unregister(listener);
			listener.openBooks.clear();
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onRecipeBookSettingsChange(PlayerRecipeBookSettingsChangeEvent event) {
		Player player = event.getPlayer();
		EnumSet<PlayerRecipeBookSettingsChangeEvent.RecipeBookType> open = this.openBooks.computeIfAbsent(
				player.getUniqueId(),
				k -> EnumSet.noneOf(PlayerRecipeBookSettingsChangeEvent.RecipeBookType.class));

		boolean wasOpen = open.contains(event.getRecipeBookType());
		if(event.isOpen() == wasOpen) {
			// 열림 상태 변화 없음 = 필터 버튼 토글 → 이벤트 미발동
			return;
		}
		if(event.isOpen()) {
			open.add(event.getRecipeBookType());
		} else {
			open.remove(event.getRecipeBookType());
		}

		EventUtils.TriggerListener(Driver.EXTENSION, "player_recipe_book_toggle",
				new RecipeBookToggleEvent(event, resolveGuiType(event)));
	}

	/**
	 * 관전 대상에서 빠져나옴(시프트 · 대상 사라짐 · 게임 모드 변경 · /spectate 해제 등) — Paper 이벤트, 취소 가능.
	 * 코어 player_teleport 로는 안 잡힌다(바닐라 setCamera(self) 는 텔포가 아니다).
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onStopSpectating(PlayerStopSpectatingEntityEvent event) {
		EventUtils.TriggerListener(Driver.EXTENSION, "player_stop_spectating", new StopSpectatingEvent(event));
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		this.openBooks.remove(event.getPlayer().getUniqueId());
	}

	/** chf_set_recipe_book_open 등 외부에서 열림 상태를 바꿨을 때 캐시를 동기화한다. */
	public static void markOpenState(UUID uuid, String typeName, boolean open) {
		if(listener == null) {
			return;
		}
		PlayerRecipeBookSettingsChangeEvent.RecipeBookType type;
		try {
			type = PlayerRecipeBookSettingsChangeEvent.RecipeBookType.valueOf(typeName);
		} catch (IllegalArgumentException e) {
			return;
		}
		EnumSet<PlayerRecipeBookSettingsChangeEvent.RecipeBookType> open2 = listener.openBooks.computeIfAbsent(
				uuid, k -> EnumSet.noneOf(PlayerRecipeBookSettingsChangeEvent.RecipeBookType.class));
		if(open) {
			open2.add(type);
		} else {
			open2.remove(type);
		}
	}

	/**
	 * CRAFTING 레시피북은 플레이어 인벤토리(2x2)와 조합대(3x3)가 공유하므로,
	 * 현재 열려 있는 GUI로 어느 쪽 버튼이었는지 구분한다.
	 */
	private static String resolveGuiType(PlayerRecipeBookSettingsChangeEvent event) {
		switch(event.getRecipeBookType()) {
			case CRAFTING -> {
				if(event.getPlayer().getOpenInventory().getType() == InventoryType.WORKBENCH) {
					return "WORKBENCH";
				}
				return "INVENTORY";
			}
			case FURNACE -> {
				return "FURNACE";
			}
			case BLAST_FURNACE -> {
				return "BLAST_FURNACE";
			}
			case SMOKER -> {
				return "SMOKER";
			}
		}
		return event.getRecipeBookType().name();
	}

	/** player_stop_spectating 이벤트의 BindableEvent 래퍼 — _GetObject 가 Bukkit Cancellable 이라 CH cancel() 이 그대로 먹는다. */
	public static class StopSpectatingEvent implements BindableEvent {

		private final PlayerStopSpectatingEntityEvent event;

		StopSpectatingEvent(PlayerStopSpectatingEntityEvent event) {
			this.event = event;
		}

		public String getPlayerName() {
			return this.event.getPlayer().getName();
		}

		/** 보던 대상 — 사라졌으면 null */
		public String getTargetUuid() {
			Entity target = this.event.getSpectatorTarget();
			return target == null ? null : target.getUniqueId().toString();
		}

		public String getTargetType() {
			Entity target = this.event.getSpectatorTarget();
			return target == null ? null : target.getType().name();
		}

		/** 대상이 플레이어면 그 이름, 아니면 null */
		public String getTargetName() {
			Entity target = this.event.getSpectatorTarget();
			return target instanceof Player p ? p.getName() : null;
		}

		@Override
		public Object _GetObject() {
			return this.event;
		}
	}

	/** player_recipe_book_toggle 이벤트의 BindableEvent 래퍼. */
	public static class RecipeBookToggleEvent implements BindableEvent {

		private final PlayerRecipeBookSettingsChangeEvent event;
		private final String guiType;

		RecipeBookToggleEvent(PlayerRecipeBookSettingsChangeEvent event, String guiType) {
			this.event = event;
			this.guiType = guiType;
		}

		public String getPlayerName() {
			return this.event.getPlayer().getName();
		}

		public boolean isOpen() {
			return this.event.isOpen();
		}

		public String getGuiType() {
			return this.guiType;
		}

		@Override
		public Object _GetObject() {
			return this.event;
		}
	}
}
