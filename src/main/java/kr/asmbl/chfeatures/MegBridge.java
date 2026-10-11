package kr.asmbl.chfeatures;

import com.laytonsmith.abstraction.MCEntity;
import com.laytonsmith.core.ArgumentValidation;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CBoolean;
import com.laytonsmith.core.constructs.CDouble;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.exceptions.CRE.CREFormatException;
import com.laytonsmith.core.exceptions.CRE.CREIllegalArgumentException;
import com.laytonsmith.core.exceptions.CRE.CRENotFoundException;
import com.laytonsmith.core.natives.interfaces.Mixed;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.animation.BlueprintAnimation;
import com.ticxo.modelengine.api.animation.handler.AnimationHandler;
import com.ticxo.modelengine.api.animation.property.IAnimationProperty;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;

import org.bukkit.Color;
import org.bukkit.entity.Entity;

import java.util.Map;
import java.util.UUID;

/**
 * ModelEngine API를 실제로 만지는 유일한 클래스.
 *
 * 중요: 이 클래스는 @api 어노테이션이 없어 ExtensionManager/json-api가 로드하지 않는다.
 * MEG 미설치 환경(cmdline json-api 포함)에서도 확장이 죽지 않도록,
 * MEG 타입 참조는 전부 여기에만 두고 MegFunctions에서는 플러그인 존재 확인 후 호출한다.
 */
final class MegBridge {

	private MegBridge() {
	}

	// ---- 공통 해석 ----

	private static Entity bukkitEntity(Mixed uuidArg, Target t) {
		MCEntity e = Static.getEntity(uuidArg, t);
		return (Entity) e.getHandle();
	}

	private static ModeledEntity modeledEntity(Mixed uuidArg, Target t) {
		Entity entity = bukkitEntity(uuidArg, t);
		ModeledEntity me = ModelEngineAPI.getModeledEntity(entity.getUniqueId());
		if(me == null) {
			throw new CRENotFoundException("Entity " + entity.getUniqueId() + " has no models applied.", t);
		}
		return me;
	}

	private static ActiveModel activeModel(Mixed uuidArg, String modelId, Target t) {
		ModeledEntity me = modeledEntity(uuidArg, t);
		return me.getModel(modelId).orElseThrow(() ->
				new CRENotFoundException("Model '" + modelId + "' is not applied to this entity.", t));
	}

	private static Color parseColor(Mixed value, Target t) {
		if(value.isInstanceOf(CArray.TYPE)) {
			CArray arr = (CArray) value;
			if(arr.size() < 3) {
				throw new CREFormatException("Color array must be array(r, g, b).", t);
			}
			return Color.fromRGB(
					ArgumentValidation.getInt32(arr.get(0, t), t),
					ArgumentValidation.getInt32(arr.get(1, t), t),
					ArgumentValidation.getInt32(arr.get(2, t), t));
		}
		String s = value.val().replace("#", "");
		try {
			return Color.fromRGB(Integer.parseInt(s, 16));
		} catch (NumberFormatException e) {
			throw new CREFormatException("Invalid color: " + value.val()
					+ ". Use array(r, g, b) or '#RRGGBB'.", t);
		}
	}

	// ---- 모델 적용/제거/조회 ----

	static void addModel(Mixed uuidArg, String modelId, boolean overrideHitbox, Target t) {
		if(ModelEngineAPI.getBlueprint(modelId) == null) {
			throw new CREIllegalArgumentException("Unknown model id: " + modelId, t);
		}
		Entity entity = bukkitEntity(uuidArg, t);
		ModeledEntity me = ModelEngineAPI.getModeledEntity(entity.getUniqueId());
		if(me == null) {
			me = ModelEngineAPI.createModeledEntity(entity);
		}
		ActiveModel am = ModelEngineAPI.createActiveModel(modelId);
		me.addModel(am, overrideHitbox).ifPresent(ActiveModel::destroy);
		// 모델을 씌우면 베이스 엔티티는 기본적으로 숨긴다. 필요 시 meg_modify_model의 base_visible로 재표시.
		me.setBaseEntityVisible(false);
	}

	static void removeModel(Mixed uuidArg, String modelId, Target t) {
		Entity entity = bukkitEntity(uuidArg, t);
		ModeledEntity me = modeledEntity(uuidArg, t);
		ActiveModel removed = me.removeModel(modelId).orElseThrow(() ->
				new CRENotFoundException("Model '" + modelId + "' is not applied to this entity.", t));
		removed.destroy();
		if(me.getModels().isEmpty()) {
			ModelEngineAPI.removeModeledEntity(entity.getUniqueId());
		}
	}

	static CArray getModels(Mixed uuidArg, Target t) {
		Entity entity = bukkitEntity(uuidArg, t);
		CArray result = new CArray(t);
		ModeledEntity me = ModelEngineAPI.getModeledEntity(entity.getUniqueId());
		if(me != null) {
			for(String id : me.getModels().keySet()) {
				result.push(new CString(id, t), t);
			}
		}
		return result;
	}

	// ---- 모델 속성 수정 ----

	static void modifyModel(Mixed uuidArg, String modelId, CArray options, Target t) {
		ModeledEntity me = modeledEntity(uuidArg, t);
		ActiveModel am = activeModel(uuidArg, modelId, t);
		for(String key : options.stringKeySet()) {
			Mixed value = options.get(key, t);
			switch(key) {
				case "scale" -> am.setScale(ArgumentValidation.getDouble(value, t));
				case "hitbox_scale" -> am.setHitboxScale(ArgumentValidation.getDouble(value, t));
				case "tint" -> am.setDefaultTint(parseColor(value, t));
				case "damage_tint" -> am.setDamageTint(parseColor(value, t));
				case "glow" -> am.setGlowing(ArgumentValidation.getBooleanObject(value, t));
				case "glow_color" -> am.setGlowColor(parseColor(value, t).asRGB());
				case "hitbox_visible" -> am.setHitboxVisible(ArgumentValidation.getBooleanObject(value, t));
				case "shadow_visible" -> am.setShadowVisible(ArgumentValidation.getBooleanObject(value, t));
				case "can_hurt" -> am.setCanHurt(ArgumentValidation.getBooleanObject(value, t));
				case "main_hitbox" -> am.setMainHitbox(ArgumentValidation.getBooleanObject(value, t));
				case "base_visible" -> me.setBaseEntityVisible(ArgumentValidation.getBooleanObject(value, t));
				default -> throw new CREIllegalArgumentException("Unknown model option: " + key
						+ ". Valid options: scale, hitbox_scale, tint, damage_tint, glow, glow_color,"
						+ " hitbox_visible, shadow_visible, can_hurt, main_hitbox, base_visible", t);
			}
		}
	}

	// ---- 애니메이션 ----

	static void playAnimation(Mixed uuidArg, String modelId, String animation, CArray options, Target t) {
		AnimationHandler handler = activeModel(uuidArg, modelId, t).getAnimationHandler();
		double lerpIn = 0;
		double lerpOut = 0;
		double speed = 1.0;
		boolean force = false;
		if(options != null) {
			if(options.containsKey("lerpin")) {
				lerpIn = ArgumentValidation.getDouble(options.get("lerpin", t), t);
			}
			if(options.containsKey("lerpout")) {
				lerpOut = ArgumentValidation.getDouble(options.get("lerpout", t), t);
			}
			if(options.containsKey("speed")) {
				speed = ArgumentValidation.getDouble(options.get("speed", t), t);
			}
			if(options.containsKey("force")) {
				force = ArgumentValidation.getBooleanObject(options.get("force", t), t);
			}
		}
		IAnimationProperty property = handler.playAnimation(animation, lerpIn, lerpOut, speed, force);
		if(property == null) {
			throw new CRENotFoundException("Could not play animation '" + animation
					+ "' (unknown animation, or blocked by the current state).", t);
		}
		if(options != null) {
			applyAnimationOptions(property, options, t, true);
		}
	}

	static void modifyAnimation(Mixed uuidArg, String modelId, String animation, CArray options, Target t) {
		AnimationHandler handler = activeModel(uuidArg, modelId, t).getAnimationHandler();
		IAnimationProperty property = handler.getAnimation(animation);
		if(property == null) {
			throw new CRENotFoundException("Animation '" + animation + "' is not currently playing.", t);
		}
		applyAnimationOptions(property, options, t, false);
	}

	/**
	 * @param playContext true면 playAnimation에서 이미 소비한 키(lerpin/lerpout/speed/force)를 건너뛴다.
	 */
	private static void applyAnimationOptions(IAnimationProperty property, CArray options, Target t, boolean playContext) {
		for(String key : options.stringKeySet()) {
			Mixed value = options.get(key, t);
			switch(key) {
				case "lerpin" -> {
					if(!playContext) {
						property.setLerpIn(ArgumentValidation.getDouble(value, t));
					}
				}
				case "lerpout" -> {
					if(!playContext) {
						property.setLerpOut(ArgumentValidation.getDouble(value, t));
					}
				}
				case "speed" -> {
					if(!playContext) {
						property.setSpeed(ArgumentValidation.getDouble(value, t));
					}
				}
				case "force" -> {
					if(!playContext) {
						throw new CREIllegalArgumentException(
								"'force' is a play-time option and cannot be modified afterwards.", t);
					}
				}
				case "lerpin_time" -> property.setLerpInTime(ArgumentValidation.getDouble(value, t));
				case "lerpout_time" -> property.setLerpOutTime(ArgumentValidation.getDouble(value, t));
				case "loop_mode" -> {
					BlueprintAnimation.LoopMode mode = BlueprintAnimation.LoopMode.getOrNull(value.val().toUpperCase());
					if(mode == null) {
						throw new CREIllegalArgumentException("Invalid loop_mode: " + value.val()
								+ ". Must be one of ONCE, HOLD, LOOP.", t);
					}
					property.setForceLoopMode(mode);
				}
				case "override" -> {
					BlueprintAnimation.OverrideMode mode = BlueprintAnimation.OverrideMode.getOrNull(value.val().toUpperCase());
					if(mode == null) {
						throw new CREIllegalArgumentException("Invalid override: " + value.val()
								+ ". Must be one of NONE, OVERRIDE.", t);
					}
					property.setForceOverride(mode);
				}
				case "empty_zero" -> property.setEmptyZero(ArgumentValidation.getBooleanObject(value, t));
				case "skip_last_frame" -> property.setSkipLastFrame(ArgumentValidation.getBooleanObject(value, t));
				case "merge_lerp" -> property.setMergeLerp(ArgumentValidation.getBooleanObject(value, t));
				case "ignore_death" -> property.setIgnoreDeath(ArgumentValidation.getBooleanObject(value, t));
				default -> throw new CREIllegalArgumentException("Unknown animation option: " + key
						+ ". Valid options: lerpin, lerpout, lerpin_time, lerpout_time, speed, force,"
						+ " loop_mode, override, empty_zero, skip_last_frame, merge_lerp, ignore_death", t);
			}
		}
	}

	static void stopAnimation(Mixed uuidArg, String modelId, String animation, boolean force, Target t) {
		AnimationHandler handler = activeModel(uuidArg, modelId, t).getAnimationHandler();
		if(animation == null) {
			handler.forceStopAllAnimations();
		} else if(force) {
			handler.forceStopAnimation(animation);
		} else {
			handler.stopAnimation(animation);
		}
	}

	static CArray getAnimations(Mixed uuidArg, String modelId, Target t) {
		AnimationHandler handler = activeModel(uuidArg, modelId, t).getAnimationHandler();
		CArray result = CArray.GetAssociativeArray(t);
		for(Map.Entry<String, IAnimationProperty> entry : handler.getAnimations().entrySet()) {
			IAnimationProperty p = entry.getValue();
			CArray info = CArray.GetAssociativeArray(t);
			info.set("speed", new CDouble(p.getSpeed(), t), t);
			info.set("lerpin", new CDouble(p.getLerpIn(), t), t);
			info.set("lerpout", new CDouble(p.getLerpOut(), t), t);
			info.set("time", new CDouble(p.getTime(), t), t);
			info.set("phase", new CString(p.getPhase().name(), t), t);
			info.set("loop_mode", new CString(p.getLoopMode().name(), t), t);
			info.set("finished", CBoolean.get(p.isFinished()), t);
			result.set(entry.getKey(), info, t);
		}
		return result;
	}

	// ---- 블루프린트 ----

	static CArray listBlueprints(Target t) {
		CArray result = new CArray(t);
		for(String key : ModelEngineAPI.getAPI().getModelRegistry().getKeys()) {
			result.push(new CString(key, t), t);
		}
		return result;
	}

	static UUID entityUuid(Mixed uuidArg, Target t) {
		return bukkitEntity(uuidArg, t).getUniqueId();
	}
}
