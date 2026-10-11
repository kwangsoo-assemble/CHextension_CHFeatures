# CHFeatures 변경 이력

버전은 `pom.xml` + `Extension.java`(SimpleVersion) 동기. 규칙은 CLAUDE.md의 "업데이트 규칙" 참고.
(0.4.2 이전 이력은 소급 정리한 것 — 이후 버전부터는 업데이트 시점에 기록할 것)

## 0.9.1

- **제거** 대상 `'~global'` — 옛 CH 컨트롤러 문법의 흔적. 전역은 `'global'` 하나다(FiguraController 0.2.0 과 함께 — 하네스 `projects/figura_controller` T11 레거시 정리)
  ⚠ `'~global'` 을 넘기면 이제 플레이어 이름으로 읽혀 `PlayerOfflineException` — 라이브 CH 는 2026-10-10 실측 0곳(아카이브 `x_save_contents/` 만)
- **문서** `fc_state_changed` 의 `cause` 에서 `IMPORT` 를 뺐다(플러그인 0.2.0 이 옛 CH 저장 가져오기 `import-legacy` 를 지웠다) · `fc_state_unset` 설명의 «옛 `~none`» 을 뺐다
- 빌드 의존 `kr.asmbl:figuracontroller` 0.1.0 → **0.2.0**(provided — 플러그인 저장소에서 `mvnw install`)

## 0.9.0

- **추가** FiguraController(서버가 주인인 Figura 상태 · 신호 플러그인 — 옛 CH 「플레이어 피구라 컨트롤러」를 갈음) 연동 9함수 · 이벤트 2
  (FcBridge 격리 · `kr.asmbl:figuracontroller:0.1.0` provided — 플러그인 저장소에서 `mvnw install`):
  - `fc_state_get(subject, name)` — 없으면 null · **자기 값만**(전역 대체는 클라 아바타 쪽)
  - `fc_state_get_all(subject)` — 연관 배열(사본)
  - `fc_state_set(subject, name, value, [options])` → 바뀌었나 · 옵션 `persist`(기본 true) · `private`(false) · `expire`(틱, 0)
  - `fc_states_set(subject, array, [options])` → 바뀐 수
  - `fc_state_unset(subject, name)` → 있었나 (옛 `~none`) · `fc_state_clear(subject)` → 지운 수
  - `fc_signal_send(subject, name, [data], [audience])` — `'trackers'`(기본) · `'self'` · `'all'` · 또는 플레이어 배열 · 전역은 늘 전원(배열이면 그들만)
  - `fc_preset_load(preset, subject | array)` → 바뀐 수 (전역은 `/fc preset load <이름> global`)
  - `fc_is_ready(player)` — 그 클라가 HELLO 를 보냈나(새 코어)
  - 대상 = 플레이어 이름(접속 · 캐시된 오프라인) · UUID(플레이어 · 몹 · 32자도) · `'global'`/`'~global'`
  - 이벤트 `fc_state_changed`(name · subject · player · old · new · existed · exists · cause) — ★ **name 프리필터 필수**: 없으면 bind 가
    `BindException`. CH 는 등록(add) 뒤 `bind()` 를 부르므로 **던지기 전에 그 묶음을 푼다**(안 풀면 오류인데도 돈다 — `EventUtils.RegisterEvent` 바이트코드 실측)
  - 이벤트 `fc_signal`(name · player · uuid · data) — 아바타(호스트) → 서버 `server_data.send`
  - **값 변환을 직접 한다** — CH `json_decode` 는 `1.0` 을 정수 1 로 바꿔(실측) 서버가 준 실수를 CH 가 다시 쓰면 «바뀜» 이 헛나온다.
    CInt ↔ 정수 · CDouble ↔ 실수 · 연관 배열 ↔ 객체 · 빈 배열/연관 배열 구분 유지 · 클로저 등은 CastException · 깊이 32
  - 플러그인 예외 → CRE: 메인 스레드 아님 → PluginInternalException · 너무 큼 → LengthException · 그 밖 → FormatException
- 검증: json-api rc 0 · 새 함수 9 실림(확장 이벤트는 원래 json-api 에 없다 — SEVERE 4줄은 옛 이벤트와 같음) ·
  하네스 `projects/figura_controller/tools/smoke_server.py` 격리 서버(Purpur + FSB + FiguraController + CommandHelper + 이 jar)에서
  `tools/smoke_ch.ms` **23/23**(정수/실수 · 묶음 · 옵션 · 이벤트 · 이름 없는 bind 오류 · 몹 · 프리셋 · 거절) · ⚠ `fc_signal` 은 클라가 있어야(인게임)

## 0.8.0

- **추가** `chf_get_item_despawn_ticks(entityUUID)` / `chf_set_item_despawn_ticks(entityUUID, ticks)` —
  떨어진 아이템이 사라지기까지 남은 틱 조회·설정 (ItemDespawnBridge).
  `ticks` 에 null 을 주면 영원히 사라지지 않는다. 설정 함수는 **실제로 적용된 남은 틱**을 돌려준다.
  - **배경**: 바닐라는 `ItemEntity.age` 를 매 틱 올리다가 디스폰 시간에 닿으면 지운다.
    즉 남은 시간 = `디스폰시간 - age` 인데, Bukkit/Paper API 로는 둘 다 다룰 수 없다 —
    `Item#setTicksLived()` 는 CraftItem 이 age 도 같이 세팅해 주긴 하지만 **값이 1 이상이어야** 하고
    (`CraftEntity` 의 Preconditions), 무엇보다 **적용 중인 디스폰 시간을 알 방법이 없다.**
  - 그래서 NMS 리플렉션으로 `ItemEntity.age`(public int)와 `ItemEntity.despawnRate`(Paper 추가 private int)를
    직접 읽고 쓴다. despawnRate 는 Paper 가 **월드 설정(item-despawn-rate)과 아이템별 예외 설정
    (alt-item-despawn-rate)을 엔티티 생성 시점에 캐시해 둔 값**이라, 이것만 읽으면 두 설정이 모두 반영된다.
    못 읽으면 바닐라 기본값 6000 으로 폴백한다 (기능이 죽는 것보다는 낫다).
  - **short 클램프**: age 는 청크 저장 시 NBT `Age` 에 short 로 기록된다. 범위를 넘겨두면 다음 로드에서
    값이 잘려 엉뚱한 시간에 사라지므로 age 를 `[-32767, 32767]` 로 자른다 →
    기본 디스폰 6000틱 기준 최대 약 38700틱까지만 설정 가능. 더 오래 두려면 null(무한)을 쓸 것.
    하한을 -32768 이 아니라 -32767 로 잡은 이유는 -32768(`Short.MIN_VALUE`)이 vanilla·Paper 양쪽에서
    **"영원히 안 사라짐" 센티넬**이기 때문이다 (`Item#willAge()` 도 이 값과 비교한다).
    그대로 두면 "아주 오래 유지" 를 요청했는데 조용히 무한이 되어버린다.
  - 검증: Purpur 1.21.8 서버 jar 를 javap 으로 확인 — `ItemEntity.age`(public int),
    `ItemEntity.despawnRate`(private int), `CraftItem.setTicksLived` 오버라이드 존재.
    서버 메이저 업데이트 시 재검증 필요

## 0.7.0

- **추가** `chf_play_hurt_animation(entityUUID, [yaw])` — 데미지 없이 바닐라 피격 연출(빨간 깜빡임)만 재생.
  `yaw` 는 피해가 날아온 방향(도, 엔티티 정면 기준 0). 기본 0.
  - **배경**: 코어의 `play_entity_effect(uuid, 'HURT')` 가 **1.19.4 이후 아무 동작도 하지 않는다.**
    피격 연출이 엔티티 상태(EntityEffect, status 2)에서 전용 패킷(hurt animation)으로 옮겨졌기 때문.
    실측(`/chdebug eval play_entity_effect(puuid(), 'HURT')`)에서도 반응 없음 확인.
  - 구현은 Paper API `LivingEntity#playHurtAnimation(float)` 한 줄. 본인 화면과 관전자 화면 모두 반영.
  - 데미지가 아니므로 **EntityDamageEvent·넉백·무적시간(noDamageTicks)이 전부 발생하지 않는다.**
    `damage_entity(uuid, 0.0001)` 로 연출을 흉내내면 이 부작용들이 따라오므로 그 대안으로 쓴다.
  - 용도 예: 체력 공유 시스템에서 실제로 맞지 않은 파트너에게 피격 연출만 전달

## 0.6.0

- **추가** `chf_send_fake_glowing(entityUUID, glowing, [targets])` /
  `chf_clear_fake_glowing(entityUUID, [targets])` — 보는 사람별 시각 전용 발광 (GlowBridge).
  코어 `set_entity_glowing()`은 전역이지만 이건 대상 플레이어에게만 적용되고 서버 상태는 그대로.
  `glowing`에 null → 해당 대상만 서버 실제 상태로 초기화. clear는 전원(기본) 초기화용.
  - 구현: 엔티티 메타데이터 공유 플래그 바이트(0x40=발광)를 **서버 현재값에서 읽어 해당 비트만 변경**해
    `ClientboundSetEntityDataPacket` 전송 → 불붙음·웅크림·스프린트·수영·투명·엘리트라 플래그 보존.
    accessor는 `Entity.DATA_SHARED_FLAGS_ID` 리플렉션, 실패 시 인덱스 0 + BYTE 직렬화기로 폴백
  - 발광 테두리 색은 각 수신자가 보는 그 엔티티의 스코어보드 팀 색을 따름
  - 클라 상태 전용: 서버가 해당 엔티티 메타데이터를 갱신하면(웅크림·화염 등) 덮어써짐, 트래킹
    재진입 시에도 소실 → 필요 시 재전송
- **정리** 패킷 송신 리플렉션을 `NmsPackets`(handle/send)로 공용화, LeashBridge가 이를 사용
  (RecipeBookBridge는 기존 동작 유지 위해 미변경)

## 0.5.0

- **추가** `chf_send_fake_leash(entityUUID, holderUUID, [receivers])` /
  `chf_clear_fake_leash(entityUUID, [receivers])` — 시각 전용 가짜 끈 (LeashBridge).
  `ClientboundSetEntityLinkPacket`을 NMS 리플렉션으로 직접 전송(ProtocolLib 불필요 — 송신만 하면 되므로).
  서버 리시 상태를 만들지 않으므로 **물리·거리 끊김·리드 드랍 없음**
  (vanilla `Leashable.tickLeash(ServerLevel, Entity)`가 서버 전용이라 구조적으로 보장).
  홀더는 **임의 엔티티 가능**(플레이어·아머스탠드·디스플레이 등) — Bukkit setLeashHolder 제약 우회.
  - ⚠️ **불가피한 제약**: 끈이 그려지는 대상(leashed)은 클라 하드코딩상 Mob 또는 Boat만 가능.
    `ClientPacketListener.handleEntityLinkPacket`이 `instanceof Leashable`을 검사하며,
    1.21.8에서 Leashable 구현체는 Mob·AbstractBoat뿐(Player/ArmorStand/Display/ItemFrame 불가 — 바이트코드로 확인).
    → 다른 타입에는 IllegalArgumentException + 안내(투명 몹을 끈 끝으로 쓰는 우회법)
  - receivers: 플레이어/배열/생략(=엔티티를 추적 중인 전원, getTrackedBy). 양쪽 엔티티가 수신자의
    트래킹 범위 안에 있어야 보임
  - 클라 상태 전용이라 트래킹 재진입·리스폰·차원 이동 시 소실 → 필요 시 재전송 (chf_get_tracked_by 활용)

## 0.4.2

- **추가** `chf_pcooldown([player], key)` / `chf_set_pcooldown([player], key, ticks)` —
  코어 pcooldown/set_pcooldown 확장판. key가 아이템 메테리얼과 일치하면 메테리얼 기준,
  아니면 커스텀 쿨다운 그룹 키(`namespace:key`, 네임스페이스 생략 시 `minecraft:`)로 처리.
  커스텀 그룹은 아이템의 `minecraft:use_cooldown` 컴포넌트 `cooldown_group`과 매칭되어
  HUD 오버레이 포함 적용. Paper `get/setCooldown(Key, int)` 사용.
  - ⚠️ 지뢰 발견: `setCooldown(NamespacedKey,..)`는 javac가 Adventure `Key` 오버로드로 컴파일
    → CH full jar에 Adventure 미번들이라 @api 클래스에 두면 json-api 로드 실패
    → `CooldownBridge`로 격리 (CLAUDE.md 핵심 제약 2·3 참고)
- **추가** `chf_get_tracked_by(entityUUID)` — 해당 엔티티를 클라이언트에서 추적(트래킹) 중인
  플레이어 이름 배열. Paper `Entity#getTrackedBy()` 래핑, 플레이어 본인은 수동 보장 포함.
  FSB 상태 전송 receivers 최적화용. (타 세션에서 추가)

## 0.4.0

- **추가** FiguraFSB 연동 (FsbBridge/FsbFunctions, jar는 install-file로 로컬 설치):
  - `fsb_send_packet(receiver, packetName, data)` — 수신자 호스트 아바타로 서버 패킷 전송
  - `fsb_send_packet(receiver, avatarOwnerUUID, packetName, data)` — 비호스트 아바타 지정
    (응답까지 받으려면 FSB config `allowNonHostPackets`)
  - data는 byte_array 또는 문자열(UTF-8), 최대 32,746바이트. FSB 미접속자는 조용히 무시
  - `fsb_register_packet(packetName)` — 수신 등록. FSB는 이름당 리스너 1개(전 플러그인 공용,
    재등록 덮어씀) → `chf:` 등 접두어 필수
  - `fsb_is_connected(player)` — FSB 핸드셰이크 완료 여부
  - 이벤트 `fsb_server_packet` — 프리필터 packet_name(macro)/player.
    데이터: packet_name, player(오프라인 시 null), sender_uuid, avatar_owner, is_host, data(byte_array)
- 사용 팁: 아바타의 `writeByteArray`는 날것 바이트 → CH에서 `string_from_bytes()`로 읽기.
  `ba_get_string()`은 CH 전용 길이-프리픽스 포맷이라 호환 안 됨 (RangeException)

## 0.3.3

- **추가** `set_trading_inventory([player], array)` — 거래 GUI 재료 슬롯 설정.
  키 input1/input2만 허용(값은 아이템 배열 또는 null=비우기), result는 서버가 재계산하므로 설정 불가.
  거래 중이 아니면 NotFoundException

## 0.3.2

- **추가** `get_trading_inventory([player])` — 보고 있는 주민/떠돌이상인 거래 GUI 조회.
  반환: input1/input2/result(거래 성립 시 미리 채워진 결과 포함, 빈 슬롯 null)/recipe_index.
  거래 중 아니면 null. 아이템은 CH 표준 아이템 배열(ObjectGenerator.item)
- ⚠️ 지뢰 발견: @api 클래스 메서드 시그니처(private 포함)의 타입은 CacheAnnotations 리플렉션 시
  해석 필요 → paper-api를 provided에서 **compile 스코프로 전환** (CHPaper와 동일)

## 0.3.1

- **추가** `chf_set_recipe_book_open(player, type, is_open)` / `chf_get_recipe_book_open(player, type)` —
  레시피북 열림 상태 강제 설정/조회. type: CRAFTING/FURNACE/BLAST_FURNACE/SMOKER.
  Bukkit API에 없어 NMS 리플렉션(RecipeBookBridge, Mojang 매핑 런타임 이름, 1.21.8 server.txt로 검증).
  설정 후 `ClientboundRecipeBookSettingsPacket`으로 즉시 클라 동기화 (GUI가 열려 있는 중엔 다음 열 때 반영).
  toggle 이벤트 캐시도 동기화, 이벤트는 발동 안 함. 서버 메이저 업데이트 시 매핑 재검증 필요

## 0.3.0

- **추가** 이벤트 `player_recipe_book_toggle` — 레시피북 열림/닫힘 감지 (첫 CH 이벤트).
  데이터: player, type, is_open. type은 CRAFTING북 공유 문제를 열려 있는 GUI로 구분:
  INVENTORY(2x2)/WORKBENCH(3x3)/FURNACE/BLAST_FURNACE/SMOKER.
  필터(제작가능만 보기) 토글은 플레이어별 열림 상태 캐시(EnumSet)로 걸러 미발동.
  구조: ChfListener(Bukkit 리스너 격리) + ChfEvents(@api AbstractEvent) + onStartup try/catch 등록.
  json-api에 확장 이벤트는 원래 미포함(CHPaper도 동일) — VSC 자동완성에만 안 뜨는 것

## 0.2.4

- **복구/수정** `chf_heal` — HUD 하트 하이라이트 미동작의 근본 원인 규명 후 재추가:
  클라 하이라이트는 SetHealth 패킷이 "클라이언트가 아는 체력보다 높은 값"으로 도착할 때만 발동하는데,
  CraftBukkit이 플레이어 체력을 메타데이터(DATA_HEALTH)로도 보내며 명령/스케줄러 타이밍에서는
  메타데이터가 먼저 도착해 하이라이트가 안 뜸 (바닐라 리젠·포션은 doTick 내부라 SetHealth가 먼저).
  → 회복 반영 시 `Player#sendHealthUpdate()`로 SetHealth 패킷을 즉시 전송해 해결.
  1.21.8 클라 바이트코드(hurtTo/Gui.renderPlayerHealth) + Paper 소스로 검증

## 0.2.3

- **제거** `chf_heal` (HUD 미동작으로 판단 — 0.2.4에서 원인 규명 후 복구됨)

## 0.2.2

- **추가** `chf_heal(entityUUID, amount, [reason])` — EntityRegainHealthEvent를 태우는 회복.
  reason: REGEN/SATIATED/EATING/ENDER_CRYSTAL/MAGIC/MAGIC_REGEN/WITHER_SPAWN/WITHER/CUSTOM(기본).
  Paper `Damageable#heal(double, RegainReason)` 사용

## 0.2.1

- **변경** `meg_add_model` — 모델 적용 시 베이스 엔티티 기본 숨김(setBaseEntityVisible(false)).
  재표시는 `meg_modify_model(..., array(base_visible: true))`

## 0.2.0

- **추가** ModelEngine R4.1.0 연동 9종 (MegBridge 격리, mvn.lumine.io provided 의존):
  - `meg_add_model(uuid, modelId, [overrideHitbox])` / `meg_remove_model` / `meg_get_models`
  - `meg_modify_model(uuid, modelId, options)` — scale/hitbox_scale/tint/damage_tint/glow/glow_color/
    hitbox_visible/shadow_visible/can_hurt/main_hitbox/base_visible
  - `meg_play_animation(uuid, modelId, anim, [options])` — lerpin/lerpout/lerpin_time/lerpout_time/
    speed/force/loop_mode(ONCE·HOLD·LOOP)/override(NONE·OVERRIDE)/empty_zero/skip_last_frame/
    merge_lerp/ignore_death (R4.1.0 IAnimationProperty 전수 반영)
  - `meg_modify_animation(uuid, modelId, anim, options)` — 재생 중 속성 변경 (force 제외 동일 옵션)
  - `meg_stop_animation(uuid, modelId, anim, [force])` — anim null이면 전체 강제 정지
  - `meg_get_animations(uuid, modelId)` / `meg_list_blueprints()`
  - ⚠️ 서버 MEG 버전과 pom 버전 일치 필수 (마이너 간 시그니처 변경 잦음)

## 0.1.2

- **수정** json-api(MethodScriptVSC) 크래시 — `get_heightmap_highest_block_at.docs()`의
  `HeightMap.values()` 참조가 Bukkit 없는 cmdline에서 NoClassDefFoundError로 전체를 죽임.
  docs() 하드코딩 + NamespacedKey static 필드 지연 생성으로 수정.
  **규칙 확립: docs()/getName()/static에서 Bukkit 참조 금지** (CLAUDE.md 핵심 제약 1)

## 0.1.1

- **추가** `chf_get_entity_data(uuid, [key])` / `chf_set_entity_data(uuid, array | uuid, key, value)` —
  엔티티 커스텀 데이터 영구 저장. 바닐라 `{data:{}}`는 marker 전용이라 PersistentDataContainer 사용,
  `BukkitValues."chfeatures:data"`에 CArray를 JSON 문자열로 저장 (재시작·청크 언로드 유지, 플레이어 포함).
  바닐라에서 값 전체를 문자열로 읽기/수정은 가능하나 내부 구조 접근은 불가

## 0.1.0

- 개발 환경 구성: Maven Wrapper 내장, CH full jar 로컬 설치(letsbuild Jenkins),
  ExtensionAnnotationProcessor + CacheAnnotations 빌드 파이프라인 (CHPaper/CHNaughty pom 참고)
- **추가** `chf_version()` — 로드 확인용
- **추가** `get_heightmap_highest_block_at(x, z, heightmap, [world] | locArray, heightmap, [world])` —
  코어 get_highest_block_at의 HeightMap 지정판. MOTION_BLOCKING/MOTION_BLOCKING_NO_LEAVES/
  OCEAN_FLOOR/OCEAN_FLOOR_WG/WORLD_SURFACE/WORLD_SURFACE_WG
