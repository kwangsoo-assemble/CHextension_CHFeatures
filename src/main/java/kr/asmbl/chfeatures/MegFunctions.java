package kr.asmbl.chfeatures;

import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.ArgumentValidation;
import com.laytonsmith.core.MSVersion;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CNull;
import com.laytonsmith.core.constructs.CVoid;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.exceptions.CRE.CREBadEntityException;
import com.laytonsmith.core.exceptions.CRE.CRECastException;
import com.laytonsmith.core.exceptions.CRE.CREFormatException;
import com.laytonsmith.core.exceptions.CRE.CREIllegalArgumentException;
import com.laytonsmith.core.exceptions.CRE.CRELengthException;
import com.laytonsmith.core.exceptions.CRE.CRENotFoundException;
import com.laytonsmith.core.exceptions.CRE.CREPluginInternalException;
import com.laytonsmith.core.exceptions.CRE.CREThrowable;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.AbstractFunction;
import com.laytonsmith.core.natives.interfaces.Mixed;

import org.bukkit.Bukkit;

/**
 * ModelEngine(R4.1.0) 연동 함수.
 *
 * 중요: 이 파일의 @api 클래스에는 MEG 타입을 절대 직접 참조하지 않는다.
 * MEG API 호출은 전부 MegBridge에 위임한다. (json-api/미설치 서버 안전)
 */
public class MegFunctions {

	public static String docs() {
		return "Functions for interacting with the ModelEngine plugin (R4). Requires ModelEngine to be installed.";
	}

	/** exec() 첫 줄에서 호출. MEG 미설치 시 명확한 예외를 던진다. Bukkit 참조라 docs 생성과 무관. */
	private static void checkMeg(Target t) {
		if(!Bukkit.getPluginManager().isPluginEnabled("ModelEngine")) {
			throw new CREPluginInternalException("ModelEngine is not installed or enabled on this server.", t);
		}
	}

	private abstract static class MegFunction extends AbstractFunction {

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
	}

	@api
	public static class meg_add_model extends MegFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPluginInternalException.class, CREBadEntityException.class,
					CRELengthException.class, CREIllegalArgumentException.class};
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			checkMeg(t);
			boolean overrideHitbox = true;
			if(args.length == 3) {
				overrideHitbox = ArgumentValidation.getBooleanObject(args[2], t);
			}
			MegBridge.addModel(args[0], args[1].val(), overrideHitbox, t);
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "meg_add_model";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2, 3};
		}

		@Override
		public String docs() {
			return "void {entityUUID, modelId, [overrideHitbox]} Applies a ModelEngine model to an entity."
					+ " Creates the modeled entity if this is its first model. If the same model is already"
					+ " applied, it is replaced. overrideHitbox (default true) replaces the base entity's"
					+ " hitbox with the model's hitbox. The base entity is hidden by default; use"
					+ " meg_modify_model with base_visible: true to show it again."
					+ " Throws IllegalArgumentException for unknown model ids.";
		}
	}

	@api
	public static class meg_remove_model extends MegFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPluginInternalException.class, CREBadEntityException.class,
					CRELengthException.class, CRENotFoundException.class};
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			checkMeg(t);
			MegBridge.removeModel(args[0], args[1].val(), t);
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "meg_remove_model";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2};
		}

		@Override
		public String docs() {
			return "void {entityUUID, modelId} Removes a model from an entity and destroys it."
					+ " If it was the entity's last model, the modeled entity is unregistered as well."
					+ " Throws NotFoundException if the entity doesn't have that model.";
		}
	}

	@api
	public static class meg_get_models extends MegFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPluginInternalException.class, CREBadEntityException.class,
					CRELengthException.class};
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			checkMeg(t);
			return MegBridge.getModels(args[0], t);
		}

		@Override
		public String getName() {
			return "meg_get_models";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1};
		}

		@Override
		public String docs() {
			return "array {entityUUID} Returns an array of model ids currently applied to the entity."
					+ " Returns an empty array if the entity has no models.";
		}
	}

	@api
	public static class meg_modify_model extends MegFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPluginInternalException.class, CREBadEntityException.class,
					CRELengthException.class, CRENotFoundException.class, CREIllegalArgumentException.class,
					CRECastException.class, CREFormatException.class};
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			checkMeg(t);
			if(!(args[2].isInstanceOf(CArray.TYPE))) {
				throw new CRECastException("Expecting an options array as the third argument.", t);
			}
			MegBridge.modifyModel(args[0], args[1].val(), (CArray) args[2], t);
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "meg_modify_model";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{3};
		}

		@Override
		public String docs() {
			return "void {entityUUID, modelId, optionsArray} Modifies properties of a model applied to an entity."
					+ " Options: scale (double), hitbox_scale (double), tint (array(r,g,b) or '#RRGGBB'),"
					+ " damage_tint (color), glow (boolean), glow_color (color), hitbox_visible (boolean),"
					+ " shadow_visible (boolean), can_hurt (boolean), main_hitbox (boolean),"
					+ " base_visible (boolean). Unknown option keys throw IllegalArgumentException.";
		}
	}

	@api
	public static class meg_play_animation extends MegFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPluginInternalException.class, CREBadEntityException.class,
					CRELengthException.class, CRENotFoundException.class, CREIllegalArgumentException.class,
					CRECastException.class};
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			checkMeg(t);
			CArray options = null;
			if(args.length == 4 && !(args[3] instanceof CNull)) {
				if(!(args[3].isInstanceOf(CArray.TYPE))) {
					throw new CRECastException("Expecting an options array as the fourth argument.", t);
				}
				options = (CArray) args[3];
			}
			MegBridge.playAnimation(args[0], args[1].val(), args[2].val(), options, t);
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "meg_play_animation";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{3, 4};
		}

		@Override
		public String docs() {
			return "void {entityUUID, modelId, animation, [optionsArray]} Plays an animation on a model."
					+ " Options: lerpin (double, seconds), lerpout (double), speed (double, default 1.0),"
					+ " force (boolean, replace a playing instance of the same animation),"
					+ " lerpin_time (double), lerpout_time (double), loop_mode (ONCE|HOLD|LOOP),"
					+ " override (NONE|OVERRIDE), empty_zero (boolean), skip_last_frame (boolean),"
					+ " merge_lerp (boolean), ignore_death (boolean)."
					+ " Throws NotFoundException if the animation doesn't exist in the model or was blocked.";
		}
	}

	@api
	public static class meg_modify_animation extends MegFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPluginInternalException.class, CREBadEntityException.class,
					CRELengthException.class, CRENotFoundException.class, CREIllegalArgumentException.class,
					CRECastException.class};
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			checkMeg(t);
			if(!(args[3].isInstanceOf(CArray.TYPE))) {
				throw new CRECastException("Expecting an options array as the fourth argument.", t);
			}
			MegBridge.modifyAnimation(args[0], args[1].val(), args[2].val(), (CArray) args[3], t);
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "meg_modify_animation";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{4};
		}

		@Override
		public String docs() {
			return "void {entityUUID, modelId, animation, optionsArray} Modifies properties of a currently"
					+ " playing animation. Accepts the same options as meg_play_animation except 'force'."
					+ " Throws NotFoundException if the animation is not currently playing.";
		}
	}

	@api
	public static class meg_stop_animation extends MegFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPluginInternalException.class, CREBadEntityException.class,
					CRELengthException.class, CRENotFoundException.class};
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			checkMeg(t);
			String animation = (args[2] instanceof CNull) ? null : args[2].val();
			boolean force = false;
			if(args.length == 4) {
				force = ArgumentValidation.getBooleanObject(args[3], t);
			}
			MegBridge.stopAnimation(args[0], args[1].val(), animation, force, t);
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "meg_stop_animation";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{3, 4};
		}

		@Override
		public String docs() {
			return "void {entityUUID, modelId, animation, [force]} Stops an animation on a model."
					+ " With force false (default) the animation lerps out naturally; with force true it stops"
					+ " immediately. Passing null as the animation force-stops all animations on the model.";
		}
	}

	@api
	public static class meg_get_animations extends MegFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPluginInternalException.class, CREBadEntityException.class,
					CRELengthException.class, CRENotFoundException.class};
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			checkMeg(t);
			return MegBridge.getAnimations(args[0], args[1].val(), t);
		}

		@Override
		public String getName() {
			return "meg_get_animations";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2};
		}

		@Override
		public String docs() {
			return "array {entityUUID, modelId} Returns an associative array of currently playing animations"
					+ " on a model. Each value is an array with keys: speed, lerpin, lerpout, time, phase,"
					+ " loop_mode, finished.";
		}
	}

	@api
	public static class meg_list_blueprints extends MegFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREPluginInternalException.class};
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			checkMeg(t);
			return MegBridge.listBlueprints(t);
		}

		@Override
		public String getName() {
			return "meg_list_blueprints";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{0};
		}

		@Override
		public String docs() {
			return "array {} Returns an array of all registered ModelEngine model ids (blueprints).";
		}
	}
}
