package kr.asmbl.chfeatures;

import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.abstraction.MCEntity;
import com.laytonsmith.abstraction.MCLocation;
import com.laytonsmith.abstraction.MCPlayer;
import com.laytonsmith.abstraction.MCWorld;
import com.laytonsmith.abstraction.bukkit.BukkitMCItemStack;
import com.laytonsmith.abstraction.bukkit.BukkitMCLocation;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.ArgumentValidation;
import com.laytonsmith.core.MSVersion;
import com.laytonsmith.core.ObjectGenerator;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CBoolean;
import com.laytonsmith.core.constructs.CDouble;
import com.laytonsmith.core.constructs.CInt;
import com.laytonsmith.core.constructs.CNull;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.CVoid;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.CommandHelperEnvironment;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.exceptions.CRE.CREBadEntityException;
import com.laytonsmith.core.exceptions.CRE.CRECastException;
import com.laytonsmith.core.exceptions.CRE.CREFormatException;
import com.laytonsmith.core.exceptions.CRE.CREIllegalArgumentException;
import com.laytonsmith.core.exceptions.CRE.CREInvalidWorldException;
import com.laytonsmith.core.exceptions.CRE.CRELengthException;
import com.laytonsmith.core.exceptions.CRE.CRENotFoundException;
import com.laytonsmith.core.exceptions.CRE.CREPlayerOfflineException;
import com.laytonsmith.core.exceptions.CRE.CREPluginInternalException;
import com.laytonsmith.core.exceptions.CRE.CRERangeException;
import com.laytonsmith.core.exceptions.CRE.CREThrowable;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.exceptions.MarshalException;
import com.laytonsmith.core.functions.AbstractFunction;
import com.laytonsmith.core.natives.interfaces.Mixed;

import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Functions {

	public static String docs() {
		return "CHFeatures functions using the Paper API.";
	}

	@api
	public static class chf_version extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{};
		}

		@Override
		public boolean isRestricted() {
			return false;
		}

		@Override
		public Boolean runAsync() {
			return false;
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			return new CString("CHFeatures " + new Extension().getVersion() + " on " + Bukkit.getVersion(), t);
		}

		@Override
		public String getName() {
			return "chf_version";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{0};
		}

		@Override
		public String docs() {
			return "string {} Returns the CHFeatures extension version and server version. (환경 구성 검증용 샘플 함수)";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	@api(environments = {CommandHelperEnvironment.class})
	public static class get_heightmap_highest_block_at extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREFormatException.class, CRECastException.class,
					CRELengthException.class, CREInvalidWorldException.class};
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
		public Mixed exec(Target t, Environment env, Mixed... args) throws ConfigRuntimeException {
			int x;
			int z;
			MCWorld w = null;
			String world = null;
			String heightmapName;

			MCPlayer player = env.getEnv(CommandHelperEnvironment.class).GetPlayer();
			if(player != null) {
				w = player.getWorld();
			}

			if(args[0].isInstanceOf(CArray.TYPE)) {
				// locationArray, heightmap, [world]
				MCLocation loc = ObjectGenerator.GetGenerator().location(args[0], w, t);
				x = loc.getBlockX();
				z = loc.getBlockZ();
				w = loc.getWorld();
				heightmapName = args[1].val();
				if(args.length == 3) {
					world = args[2].val();
				}
			} else {
				// x, z, heightmap, [world]
				if(args.length < 3) {
					throw new CREFormatException("Expecting a heightmap type as the third argument.", t);
				}
				x = (int) java.lang.Math.floor(ArgumentValidation.getDouble(args[0], t));
				z = (int) java.lang.Math.floor(ArgumentValidation.getDouble(args[1], t));
				heightmapName = args[2].val();
				if(args.length == 4) {
					world = args[3].val();
				}
			}
			if(world != null) {
				w = Static.getServer().getWorld(world);
			}
			if(w == null) {
				throw new CREInvalidWorldException("The specified world " + world + " doesn't exist", t);
			}

			HeightMap heightmap;
			try {
				heightmap = HeightMap.valueOf(heightmapName.toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new CREFormatException("Invalid heightmap type: " + heightmapName
						+ ". Must be one of " + Arrays.toString(HeightMap.values()), t);
			}

			World bukkitWorld = (World) w.getHandle();
			Block highestBlock = bukkitWorld.getHighestBlockAt(x, z, heightmap);
			return ObjectGenerator.GetGenerator().location(new BukkitMCLocation(highestBlock.getLocation()), false);
		}

		@Override
		public String getName() {
			return "get_heightmap_highest_block_at";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2, 3, 4};
		}

		@Override
		public String docs() {
			return "array {x, z, heightmap, [world] | locationArray, heightmap, [world]} Gets a location array for the"
					+ " highest block at a specific x and z column, as determined by the given heightmap type."
					+ " If a location array is specified, the y coordinate is ignored."
					// docs()는 cmdline(json-api) 모드에서도 호출되므로 Bukkit 클래스 참조 금지 — 목록 하드코딩
					+ " Heightmap can be one of MOTION_BLOCKING, MOTION_BLOCKING_NO_LEAVES, OCEAN_FLOOR,"
					+ " OCEAN_FLOOR_WG, WORLD_SURFACE, WORLD_SURFACE_WG."
					+ " MOTION_BLOCKING: highest block that blocks motion or contains fluid."
					+ " MOTION_BLOCKING_NO_LEAVES: same but ignoring leaves."
					+ " OCEAN_FLOOR: highest non-air solid block."
					+ " WORLD_SURFACE: highest non-air block."
					+ " (WG variants are world-generation snapshots and are generally not useful at runtime.)";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	@api
	public static class chf_play_hurt_animation extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREBadEntityException.class, CRECastException.class,
					CRELengthException.class};
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
			LivingEntity entity = (LivingEntity) Static.getLivingEntity(args[0], t).getHandle();
			float yaw = 0f;
			if(args.length == 2) {
				yaw = (float) ArgumentValidation.getDouble(args[1], t);
			}
			// 1.19.4 부터 피격 연출은 엔티티 상태(EntityEffect.HURT)가 아니라 전용 패킷으로 바뀌었다.
			// 그래서 play_entity_effect(uuid, 'HURT') 는 최신 버전에서 아무 일도 하지 않는다.
			// playHurtAnimation 은 그 전용 패킷을 보내며 본인 화면과 관전자 화면 모두에 반영된다.
			entity.playHurtAnimation(yaw);
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "chf_play_hurt_animation";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1, 2};
		}

		@Override
		public String docs() {
			return "void {entityUUID, [yaw]} Plays the vanilla hurt animation (red flash) on a living entity"
					+ " without dealing any damage. The optional yaw is the direction the damage came from,"
					+ " in degrees relative to the entity's facing (0 = front, the default)."
					+ " Note that play_entity_effect(uuid, 'HURT') no longer works since 1.19.4, because the"
					+ " hurt animation was moved from an entity status to a dedicated packet. Since no damage"
					+ " is dealt, this fires no damage event and causes no knockback or invulnerability ticks.";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	@api
	public static class chf_heal extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREBadEntityException.class, CRECastException.class,
					CRELengthException.class, CREFormatException.class};
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
			LivingEntity entity = (LivingEntity) Static.getLivingEntity(args[0], t).getHandle();
			double amount = ArgumentValidation.getDouble(args[1], t);
			EntityRegainHealthEvent.RegainReason reason = EntityRegainHealthEvent.RegainReason.CUSTOM;
			if(args.length == 3) {
				try {
					reason = EntityRegainHealthEvent.RegainReason.valueOf(args[2].val().toUpperCase());
				} catch (IllegalArgumentException e) {
					throw new CREFormatException("Invalid regain reason: " + args[2].val()
							+ ". Must be one of " + Arrays.toString(EntityRegainHealthEvent.RegainReason.values()), t);
				}
			}
			double before = entity.getHealth();
			entity.heal(amount, reason);
			// 명령/스케줄러 타이밍에서는 엔티티 메타데이터(DATA_HEALTH) 패킷이 SetHealth 패킷보다
			// 먼저 도착해 클라이언트 HUD의 회복 하이라이트가 뜨지 않는다. 회복이 실제로 반영됐으면
			// SetHealth 패킷을 즉시 전송해 메타데이터보다 먼저 도착시킨다. (틱 내 회복 연출 경로와 동일해짐)
			if(entity instanceof Player p && entity.getHealth() > before) {
				p.sendHealthUpdate();
			}
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "chf_heal";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2, 3};
		}

		@Override
		public String docs() {
			return "void {entityUUID, amount, [reason]} Heals a living entity by the given amount through the"
					+ " normal heal pipeline, firing EntityRegainHealthEvent with the given regain reason"
					+ " (default CUSTOM). Health is capped at max health, and the heal can be cancelled by"
					+ " plugins listening to the event. For players, a health update packet is pushed"
					+ " immediately so the client shows the heart regain highlight on the HUD."
					// docs()에서 Bukkit enum 참조 금지 — 목록 하드코딩
					+ " Reason can be one of REGEN, SATIATED, EATING, ENDER_CRYSTAL, MAGIC, MAGIC_REGEN,"
					+ " WITHER_SPAWN, WITHER, CUSTOM.";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	@api
	public static class chf_set_recipe_book_open extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPlayerOfflineException.class, CREFormatException.class,
					CRECastException.class, CREPluginInternalException.class};
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
			Player player = (Player) Static.GetPlayer(args[0], t).getHandle();
			String type = args[1].val();
			boolean open = ArgumentValidation.getBooleanObject(args[2], t);
			RecipeBookBridge.setOpen(player, type, open, t);
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "chf_set_recipe_book_open";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{3};
		}

		@Override
		public String docs() {
			return "void {player, type, is_open} Force-sets the open state of a player's recipe book and syncs"
					+ " it to the client. Type can be one of CRAFTING, FURNACE, BLAST_FURNACE, SMOKER."
					+ " The CRAFTING book is shared between the player inventory and the crafting table."
					+ " If the target GUI is currently open on the client, the change applies the next time"
					+ " it is opened. Does not fire player_recipe_book_toggle.";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	@api
	public static class chf_get_recipe_book_open extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPlayerOfflineException.class, CREFormatException.class,
					CREPluginInternalException.class};
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
			Player player = (Player) Static.GetPlayer(args[0], t).getHandle();
			return CBoolean.get(RecipeBookBridge.isOpen(player, args[1].val(), t));
		}

		@Override
		public String getName() {
			return "chf_get_recipe_book_open";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2};
		}

		@Override
		public String docs() {
			return "boolean {player, type} Returns whether a player's recipe book is currently open (server-side"
					+ " stored state). Type can be one of CRAFTING, FURNACE, BLAST_FURNACE, SMOKER.";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	@api(environments = {CommandHelperEnvironment.class})
	public static class get_trading_inventory extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPlayerOfflineException.class};
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
			Player player;
			if(args.length == 1) {
				player = (Player) Static.GetPlayer(args[0], t).getHandle();
			} else {
				player = (Player) Static.getPlayer(environment, t).getHandle();
			}
			Inventory top = player.getOpenInventory().getTopInventory();
			if(!(top instanceof MerchantInventory merchantInv)) {
				return CNull.NULL;
			}
			CArray ret = CArray.GetAssociativeArray(t);
			ret.set("input1", itemOrNull(merchantInv.getItem(0), t), t);
			ret.set("input2", itemOrNull(merchantInv.getItem(1), t), t);
			ret.set("result", itemOrNull(merchantInv.getItem(2), t), t);
			ret.set("recipe_index", new CInt(merchantInv.getSelectedRecipeIndex(), t), t);
			return ret;
		}

		private static Mixed itemOrNull(ItemStack item, Target t) {
			if(item == null || item.isEmpty()) {
				return CNull.NULL;
			}
			return ObjectGenerator.GetGenerator().item(new BukkitMCItemStack(item), t);
		}

		@Override
		public String getName() {
			return "get_trading_inventory";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{0, 1};
		}

		@Override
		public String docs() {
			return "array {[player]} Returns the items in the merchant (villager/wandering trader) trading GUI"
					+ " the player is currently viewing, or null if the player is not trading."
					+ " The returned associative array contains: input1 and input2 (the two ingredient slots),"
					+ " result (the result slot, which is pre-filled by the server when the placed ingredients"
					+ " match the selected trade), and recipe_index (the currently selected trade index, 0-based)."
					+ " Empty slots are null. Items use the standard item array format.";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	@api(environments = {CommandHelperEnvironment.class})
	public static class set_trading_inventory extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPlayerOfflineException.class, CRECastException.class,
					CREFormatException.class, CREIllegalArgumentException.class, CRENotFoundException.class};
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
			Player player;
			Mixed itemsArg;
			if(args.length == 2) {
				player = (Player) Static.GetPlayer(args[0], t).getHandle();
				itemsArg = args[1];
			} else {
				player = (Player) Static.getPlayer(environment, t).getHandle();
				itemsArg = args[0];
			}
			if(!(itemsArg.isInstanceOf(CArray.TYPE))) {
				throw new CRECastException("Expecting an array of slot items.", t);
			}
			CArray items = (CArray) itemsArg;

			Inventory top = player.getOpenInventory().getTopInventory();
			if(!(top instanceof MerchantInventory merchantInv)) {
				throw new CRENotFoundException("The player is not trading with a merchant.", t);
			}

			for(String key : items.stringKeySet()) {
				int slot;
				switch(key) {
					case "input1" -> slot = 0;
					case "input2" -> slot = 1;
					case "result" -> throw new CREIllegalArgumentException(
							"The result slot cannot be set. Only input1 and input2 are settable.", t);
					default -> throw new CREIllegalArgumentException("Unknown slot key: " + key
							+ ". Valid keys: input1, input2", t);
				}
				Mixed value = items.get(key, t);
				if(value instanceof CNull) {
					merchantInv.setItem(slot, null);
				} else {
					ItemStack item = (ItemStack) ObjectGenerator.GetGenerator().item(value, t).getHandle();
					merchantInv.setItem(slot, item);
				}
			}
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "set_trading_inventory";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1, 2};
		}

		@Override
		public String docs() {
			return "void {[player], array} Sets items in the ingredient slots of the merchant trading GUI the"
					+ " player is currently viewing. The array may contain the keys input1 and/or input2, each"
					+ " an item array (or null to clear the slot). The result slot cannot be set — it is"
					+ " recalculated by the server from the ingredients and the selected trade."
					+ " Throws NotFoundException if the player is not trading.";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	// ---- 가짜 끈 (시각 전용, 물리 없음) ----
	// 실제 패킷 전송은 LeashBridge 참고

	/** receivers 인자를 플레이어 목록으로 해석한다. null/미지정이면 해당 엔티티를 추적 중인 플레이어 전원. */
	private static List<Player> resolveReceivers(Mixed arg, Entity entity, Target t) {
		if(arg == null || arg instanceof CNull) {
			return new ArrayList<>(entity.getTrackedBy());
		}
		List<Player> receivers = new ArrayList<>();
		if(arg.isInstanceOf(CArray.TYPE)) {
			for(Mixed value : ((CArray) arg).asList()) {
				receivers.add((Player) Static.GetPlayer(value, t).getHandle());
			}
		} else {
			receivers.add((Player) Static.GetPlayer(arg, t).getHandle());
		}
		return receivers;
	}

	@api
	public static class chf_send_fake_leash extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREBadEntityException.class, CRELengthException.class,
					CREIllegalArgumentException.class, CREPlayerOfflineException.class,
					CREPluginInternalException.class};
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
			Entity leashed = (Entity) Static.getEntity(args[0], t).getHandle();
			Entity holder = (Entity) Static.getEntity(args[1], t).getHandle();
			LeashBridge.checkRenderable(leashed, t);
			List<Player> receivers = resolveReceivers(args.length == 3 ? args[2] : null, leashed, t);
			LeashBridge.send(leashed, holder, receivers, t);
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "chf_send_fake_leash";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2, 3};
		}

		@Override
		public String docs() {
			return "void {entityUUID, holderUUID, [receivers]} Makes a leash appear between two entities on"
					+ " clients only, without any server-side leash state: there is no pulling physics, no"
					+ " distance breaking, no lead item drop, and the holder may be ANY entity type"
					+ " (players, armor stands, display entities, other mobs...)."
					+ " The leashed entity, however, must be a mob or a boat — clients only draw leashes for"
					+ " entities they consider leashable, which cannot be changed from the server."
					+ " Receivers may be a player, an array of players, or null/omitted for every player"
					+ " currently tracking the entity. Both entities must be within a receiver's tracking range"
					+ " for them to see the leash."
					+ " This is client state only: it is lost when the entity leaves and re-enters tracking"
					+ " range, or on respawn/dimension change, so re-send it as needed."
					+ " Use chf_clear_fake_leash() to remove it.";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	@api
	public static class chf_clear_fake_leash extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREBadEntityException.class, CRELengthException.class,
					CREPlayerOfflineException.class, CREPluginInternalException.class};
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
			Entity leashed = (Entity) Static.getEntity(args[0], t).getHandle();
			List<Player> receivers = resolveReceivers(args.length == 2 ? args[1] : null, leashed, t);
			LeashBridge.send(leashed, null, receivers, t);
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "chf_clear_fake_leash";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1, 2};
		}

		@Override
		public String docs() {
			return "void {entityUUID, [receivers]} Removes a fake leash previously sent with"
					+ " chf_send_fake_leash() from the given entity. Receivers may be a player, an array of"
					+ " players, or null/omitted for every player currently tracking the entity."
					+ " Note that this also clears the client-side view of a real (server-side) leash until"
					+ " the entity is re-sent to the client.";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	// ---- 가짜 발광 (시각 전용, 보는 사람별) ----
	// 실제 패킷 전송은 GlowBridge 참고

	@api
	public static class chf_send_fake_glowing extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREBadEntityException.class, CRELengthException.class,
					CRECastException.class, CREPlayerOfflineException.class,
					CREPluginInternalException.class};
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
			Entity entity = (Entity) Static.getEntity(args[0], t).getHandle();
			Boolean glowing = args[1] instanceof CNull
					? null : ArgumentValidation.getBooleanObject(args[1], t);
			List<Player> receivers = resolveReceivers(args.length == 3 ? args[2] : null, entity, t);
			GlowBridge.send(entity, glowing, receivers, t);
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "chf_send_fake_glowing";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2, 3};
		}

		@Override
		public String docs() {
			return "void {entityUUID, glowing, [targets]} Forces the glowing outline of an entity on or off for"
					+ " specific players only, without changing the entity's real state (unlike"
					+ " set_entity_glowing(), which affects everyone). Passing null as glowing resets the given"
					+ " targets back to the entity's actual server-side state."
					+ " Targets may be a player, an array of players, or null/omitted for every player currently"
					+ " tracking the entity."
					+ " The outline color follows the entity's scoreboard team as seen by each viewer."
					+ " Other entity flags (on fire, sneaking, sprinting, swimming, invisible, gliding) are"
					+ " preserved, as the current server-side flags are read and only the glowing bit is changed."
					+ " This is client state only: the server overwrites it whenever it next updates that"
					+ " entity's metadata (e.g. the entity starts sneaking or burning), and it is lost when the"
					+ " entity leaves and re-enters tracking range, so re-send it as needed."
					+ " Use chf_clear_fake_glowing() to reset it.";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	@api
	public static class chf_clear_fake_glowing extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREBadEntityException.class, CRELengthException.class,
					CREPlayerOfflineException.class, CREPluginInternalException.class};
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
			Entity entity = (Entity) Static.getEntity(args[0], t).getHandle();
			List<Player> receivers = resolveReceivers(args.length == 2 ? args[1] : null, entity, t);
			GlowBridge.send(entity, null, receivers, t);
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "chf_clear_fake_glowing";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1, 2};
		}

		@Override
		public String docs() {
			return "void {entityUUID, [targets]} Clears a fake glowing state previously sent with"
					+ " chf_send_fake_glowing(), resending the entity's real server-side state."
					+ " Targets may be a player, an array of players, or null/omitted for every player currently"
					+ " tracking the entity (the usual way to fully reset an entity for everyone).";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	// ---- 아이템 쿨다운 (메테리얼 + 커스텀 쿨다운 그룹) ----
	// 실제 로직은 CooldownBridge 참고 (Adventure Key 참조를 @api 클래스 밖으로 격리)

	@api(environments = {CommandHelperEnvironment.class})
	public static class chf_pcooldown extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CRELengthException.class, CREFormatException.class,
					CREPlayerOfflineException.class};
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
		public Mixed exec(Target t, Environment env, Mixed... args) throws ConfigRuntimeException {
			MCPlayer mcp;
			String name;
			if(args.length == 2) {
				mcp = Static.GetPlayer(args[0], t);
				name = args[1].val();
			} else {
				mcp = env.getEnv(CommandHelperEnvironment.class).GetPlayer();
				Static.AssertPlayerNonNull(mcp, t);
				name = args[0].val();
			}
			Player player = (Player) mcp.getHandle();
			return new CInt(CooldownBridge.getCooldown(player, name, t), t);
		}

		@Override
		public String getName() {
			return "chf_pcooldown";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1, 2};
		}

		@Override
		public String docs() {
			return "int {[player], key} Gets the time left on the player's item cooldown, in server ticks."
					+ " Like pcooldown(), but the key may be either an item material name or a custom cooldown"
					+ " group key ('namespace:key'). Strings that don't match an item material are treated as"
					+ " custom group keys (without a namespace, 'minecraft:' is assumed). Custom groups match"
					+ " items whose minecraft:use_cooldown component declares the same cooldown_group.";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	@api(environments = {CommandHelperEnvironment.class})
	public static class chf_set_pcooldown extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CRELengthException.class, CREFormatException.class,
					CREPlayerOfflineException.class, CRERangeException.class, CRECastException.class};
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
		public Mixed exec(Target t, Environment env, Mixed... args) throws ConfigRuntimeException {
			MCPlayer mcp;
			String name;
			int cooldown;
			if(args.length == 3) {
				mcp = Static.GetPlayer(args[0], t);
				name = args[1].val();
				cooldown = ArgumentValidation.getInt32(args[2], t);
			} else {
				mcp = env.getEnv(CommandHelperEnvironment.class).GetPlayer();
				Static.AssertPlayerNonNull(mcp, t);
				name = args[0].val();
				cooldown = ArgumentValidation.getInt32(args[1], t);
			}
			if(cooldown < 0) {
				throw new CRERangeException("Cooldowns cannot be negative.", t);
			}
			Player player = (Player) mcp.getHandle();
			CooldownBridge.setCooldown(player, name, cooldown, t);
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "chf_set_pcooldown";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2, 3};
		}

		@Override
		public String docs() {
			return "void {[player], key, cooldown} Sets the player's item cooldown, in server ticks."
					+ " Like set_pcooldown(), but the key may be either an item material name or a custom cooldown"
					+ " group key ('namespace:key'). Strings that don't match an item material are treated as"
					+ " custom group keys (without a namespace, 'minecraft:' is assumed). Custom groups apply the"
					+ " cooldown (including the HUD overlay) to items whose minecraft:use_cooldown component"
					+ " declares the same cooldown_group.";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	// ---- 엔티티 커스텀 데이터 (PersistentDataContainer 기반) ----
	// 엔티티 NBT의 BukkitValues."chfeatures:data" 에 JSON 문자열로 영구 저장된다.

	// static 필드로 두면 cmdline(json-api) 모드에서 클래스 초기화 시 Bukkit 클래스 로드로 죽는다 — 지연 생성
	private static NamespacedKey dataKey() {
		return new NamespacedKey("chfeatures", "data");
	}

	private static Entity getBukkitEntity(Mixed uuid, Target t) {
		MCEntity e = Static.getEntity(uuid, t);
		return (Entity) e.getHandle();
	}

	private static CArray readEntityData(Entity entity, Target t) {
		PersistentDataContainer pdc = entity.getPersistentDataContainer();
		String json = pdc.get(dataKey(), PersistentDataType.STRING);
		if(json == null) {
			return CArray.GetAssociativeArray(t);
		}
		try {
			Construct decoded = Construct.json_decode(json, t);
			if(decoded instanceof CArray) {
				return (CArray) decoded;
			}
			return CArray.GetAssociativeArray(t);
		} catch (MarshalException e) {
			throw new CREFormatException("Could not decode stored entity data: " + e.getMessage(), t);
		}
	}

	private static void writeEntityData(Entity entity, CArray data, Target t) {
		PersistentDataContainer pdc = entity.getPersistentDataContainer();
		if(data == null) {
			pdc.remove(dataKey());
			return;
		}
		try {
			pdc.set(dataKey(), PersistentDataType.STRING, Construct.json_encode(data, t));
		} catch (MarshalException e) {
			throw new CREFormatException("Could not encode entity data: " + e.getMessage()
					+ " (only arrays, strings, ints, doubles, booleans and null are storable)", t);
		}
	}

	@api
	public static class chf_get_entity_data extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREBadEntityException.class, CRECastException.class,
					CRELengthException.class, CREFormatException.class};
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
			Entity entity = getBukkitEntity(args[0], t);
			CArray data = readEntityData(entity, t);
			if(args.length == 1) {
				return data;
			}
			String key = args[1].val();
			if(!data.containsKey(key)) {
				return CNull.NULL;
			}
			return data.get(key, t);
		}

		@Override
		public String getName() {
			return "chf_get_entity_data";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1, 2};
		}

		@Override
		public String docs() {
			return "mixed {entityUUID, [key]} Gets custom data stored on an entity via its PersistentDataContainer."
					+ " Without a key, returns the whole associative array (empty array if nothing is stored)."
					+ " With a key, returns the value at that key, or null if it doesn't exist."
					+ " Data persists through restarts and chunk unloads, and works on all entities including players."
					+ " Stored in entity NBT under BukkitValues as \"chfeatures:data\".";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	@api
	public static class chf_set_entity_data extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREBadEntityException.class, CRECastException.class,
					CRELengthException.class, CREFormatException.class};
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
			Entity entity = getBukkitEntity(args[0], t);
			if(args.length == 2) {
				// 전체 교체: array 또는 null(전체 삭제)
				if(args[1] instanceof CNull) {
					writeEntityData(entity, null, t);
				} else if(args[1].isInstanceOf(CArray.TYPE)) {
					writeEntityData(entity, (CArray) args[1], t);
				} else {
					throw new CRECastException("Expecting an array (or null to clear) as the second argument.", t);
				}
			} else {
				// 단일 키 설정: 값이 null이면 해당 키 제거
				String key = args[1].val();
				CArray data = readEntityData(entity, t);
				if(args[2] instanceof CNull) {
					if(data.containsKey(key)) {
						data.remove(new CString(key, t));
					}
				} else {
					data.set(new CString(key, t), args[2], t);
				}
				writeEntityData(entity, data, t);
			}
			return CNull.NULL;
		}

		@Override
		public String getName() {
			return "chf_set_entity_data";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2, 3};
		}

		@Override
		public String docs() {
			return "void {entityUUID, array | entityUUID, key, value} Stores custom data on an entity via its"
					+ " PersistentDataContainer. With an array, replaces all stored data (null clears everything)."
					+ " With a key and value, sets just that key (a null value removes the key)."
					+ " Values may be arrays, strings, ints, doubles, booleans or null."
					+ " Data persists through restarts and chunk unloads, and works on all entities including players.";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	@api
	public static class chf_get_tracked_by extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREBadEntityException.class, CRECastException.class,
					CRELengthException.class, CREFormatException.class};
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
			Entity entity = (Entity) Static.getEntity(args[0], t).getHandle();
			java.util.Set<Player> trackers = entity.getTrackedBy();
			CArray ret = new CArray(t);
			// 플레이어 엔티티는 본인을 항상 포함시킨다.
			// (Paper 1.21.8 실측: getTrackedBy()가 본인을 포함하지 않음 — 자기 클라이언트도
			//  자기 자신을 렌더링하므로 per-entity 데이터 전송 용도에서는 본인이 반드시 필요)
			if(entity instanceof Player self && !trackers.contains(self)) {
				ret.push(new CString(self.getName(), t), t);
			}
			for(Player tracker : trackers) {
				ret.push(new CString(tracker.getName(), t), t);
			}
			return ret;
		}

		@Override
		public String getName() {
			return "chf_get_tracked_by";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1};
		}

		@Override
		public String docs() {
			return "array {entityUUID} Returns an array of names of the players whose clients are currently"
					+ " tracking the given entity, meaning they receive its movement/state packets"
					+ " (roughly: the entity is within their view/tracking range)."
					+ " If the entity is a player, the returned array is guaranteed to include that player"
					+ " themselves (their own client always renders them)."
					+ " Useful for sending per-entity data only to clients that can actually see the entity.";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	// 떨어진 아이템의 디스폰까지 남은 시간.
	// 실제 로직은 ItemDespawnBridge 참고 (NMS 리플렉션을 @api 클래스 밖으로 격리)

	@api
	public static class chf_get_item_despawn_ticks extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREBadEntityException.class, CRECastException.class,
					CREIllegalArgumentException.class, CREFormatException.class,
					CREPluginInternalException.class};
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
			Entity entity = (Entity) Static.getEntity(args[0], t).getHandle();
			Integer remaining = ItemDespawnBridge.getRemaining(entity, t);
			if(remaining == null) {
				return CNull.NULL;
			}
			return new CInt(remaining, t);
		}

		@Override
		public String getName() {
			return "chf_get_item_despawn_ticks";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1};
		}

		@Override
		public String docs() {
			return "int {entityUUID} Returns how many ticks remain before the given dropped item entity"
					+ " despawns, or null if it is set to never despawn. Returns 0 if it has already"
					+ " outlived its despawn time and will be removed on the next tick."
					+ " The despawn time this is measured against is the one actually applied to that item,"
					+ " so the world's item-despawn-rate and any per-item overrides are accounted for."
					+ " Throws IllegalArgumentException if the entity is not a dropped item.";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}

	@api
	public static class chf_set_item_despawn_ticks extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREBadEntityException.class, CRECastException.class,
					CREIllegalArgumentException.class, CREFormatException.class,
					CREPluginInternalException.class};
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
			Entity entity = (Entity) Static.getEntity(args[0], t).getHandle();
			Integer ticks = null;
			if(!(args[1] instanceof CNull)) {
				ticks = ArgumentValidation.getInt32(args[1], t);
			}
			Integer applied = ItemDespawnBridge.setRemaining(entity, ticks, t);
			if(applied == null) {
				return CNull.NULL;
			}
			return new CInt(applied, t);
		}

		@Override
		public String getName() {
			return "chf_set_item_despawn_ticks";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2};
		}

		@Override
		public String docs() {
			return "int {entityUUID, ticks} Sets how many ticks remain before the given dropped item entity"
					+ " despawns, and returns the value that was actually applied. Passing null for ticks"
					+ " makes the item never despawn, and returns null."
					+ " Note that the item's age is stored as a short when its chunk is saved, so the"
					+ " remaining time is clamped to keep the underlying age within short range;"
					+ " with the default despawn rate of 6000 ticks that allows up to roughly 38700 ticks."
					+ " Use null instead if you need an item to stay indefinitely."
					+ " Throws IllegalArgumentException if the entity is not a dropped item"
					+ " or if ticks is negative.";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}
	@api
	public static class chf_collision_top extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CRECastException.class, CREInvalidWorldException.class, CRERangeException.class};
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
		public Mixed exec(Target t, Environment env, Mixed... args) throws ConfigRuntimeException {
			World w = Bukkit.getWorld(args[0].val());
			if(w == null) {
				throw new CREInvalidWorldException("The specified world " + args[0].val() + " doesn't exist", t);
			}
			double x1 = ArgumentValidation.getDouble(args[1], t);
			double y1 = ArgumentValidation.getDouble(args[2], t);
			double z1 = ArgumentValidation.getDouble(args[3], t);
			double x2 = ArgumentValidation.getDouble(args[4], t);
			double y2 = ArgumentValidation.getDouble(args[5], t);
			double z2 = ArgumentValidation.getDouble(args[6], t);
			org.bukkit.util.BoundingBox box = new org.bukkit.util.BoundingBox(x1, y1, z1, x2, y2, z2);
			int bx1 = (int) java.lang.Math.floor(box.getMinX());
			int by1 = (int) java.lang.Math.floor(box.getMinY());
			int bz1 = (int) java.lang.Math.floor(box.getMinZ());
			int bx2 = (int) java.lang.Math.floor(box.getMaxX());
			int by2 = (int) java.lang.Math.floor(box.getMaxY());
			int bz2 = (int) java.lang.Math.floor(box.getMaxZ());
			// 한 번에 너무 큰 상자는 서버를 멈춘다 — 판넬 · 엔티티 크기용
			if((long) (bx2 - bx1 + 1) * (by2 - by1 + 1) * (bz2 - bz1 + 1) > 4096) {
				throw new CRERangeException("The box is too large (more than 4096 blocks).", t);
			}
			double top = Double.NEGATIVE_INFINITY;
			for(int x = bx1; x <= bx2; x++) {
				for(int z = bz1; z <= bz2; z++) {
					// ⚠ 안 읽힌 청크를 읽지 않는다 — 막힌 것으로 보고 상자 꼭대기를 돌려준다
					if(!w.isChunkLoaded(x >> 4, z >> 4)) {
						top = java.lang.Math.max(top, box.getMaxY());
						continue;
					}
					for(int y = by1; y <= by2; y++) {
						Block b = w.getBlockAt(x, y, z);
						for(org.bukkit.util.BoundingBox part : b.getCollisionShape().getBoundingBoxes()) {
							part.shift(x, y, z);
							if(part.overlaps(box)) {
								top = java.lang.Math.max(top, part.getMaxY());
							}
						}
					}
				}
			}
			if(top == Double.NEGATIVE_INFINITY) {
				return CNull.NULL;
			}
			return new CDouble(top, t);
		}

		@Override
		public String getName() {
			return "chf_collision_top";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{7};
		}

		@Override
		public String docs() {
			return "double {world, x1, y1, z1, x2, y2, z2} Returns the highest top Y among the block collision shapes"
					+ " that overlap the given axis-aligned box, or null if the box touches no collision shape."
					+ " Uses each block's real collision shape (slabs, stairs, doors, open trapdoors, fences, carpets...),"
					+ " not a full cube. Faces that only touch do not count as overlapping."
					+ " Blocks in unloaded chunks count as colliding up to the box top (they are not loaded)."
					+ " The box may cover at most 4096 blocks.";
		}

		@Override
		public Version since() {
			return MSVersion.V3_3_5;
		}
	}
}
