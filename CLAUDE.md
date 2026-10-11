# CHFeatures — 커스텀 CommandHelper(MethodScript) 확장

Purpur 1.21.8 서버용 CH 확장 (Java/Maven). Paper API·NMS·타 플러그인(MEG, FiguraFSB) 기능을
MethodScript 함수/이벤트로 노출한다. 패키지 `kr.asmbl.chfeatures`.
GitHub `kwangsoo-assemble/CHextension_CHFeatures`(공개 · MIT · 2026-10-12) — 이 작업본을 그대로 올린다
(하네스 `projects/mc_repositories/tools/repos.py publish chfeatures` 로만 · README 한글 원본은 하네스 `readme_ko/`).

## 빌드·배포

- `.\mvnw.cmd package` (Maven 미설치 — 래퍼 사용, Java release 21, 로컬 JDK Temurin 25로 실행 OK)
- 산출물 `target/chfeatures-<버전>.jar` → 서버 `plugins/CommandHelper/extensions/`에 복사
  (**구버전 jar 반드시 제거** — 중복 로드됨) → `/reloadaliases`
- CommandHelper full jar는 공개 레포에 없음 → letsbuild.net Jenkins에서 받아 `libs/`에 두고
  `install:install-file` (com.sk89q:commandhelper:3.3.5-SNAPSHOT:full). FiguraFSB jar도 동일 방식
  (org.figuramc.figura:figura-fsb-spigot, provided)

## 📋 업데이트 규칙 (기능 추가/변경/제거 시 필수)

세션/컨텍스트가 바뀌어도 이력이 이어지도록, 코드 변경이 있는 모든 업데이트에서 다음을 수행한다:

1. **버전 동기화**: `pom.xml`의 `<version>`과 `Extension.java`의 `SimpleVersion`을 함께 올린다.
   기능 추가 = minor(0.x.0), 수정·소규모 = patch(0.0.x)
2. **CHANGELOG.md 갱신**: 해당 버전 섹션을 추가하고 다음을 기록한다 —
   추가/변경/제거된 함수·이벤트의 **시그니처와 동작 요약**, 설계 결정과 그 **이유**,
   새로 발견한 제약(지뢰)은 이 파일의 "핵심 제약"에도 반영.
   다른 세션이 CHANGELOG만 읽고 문맥을 복원할 수 있는 수준으로 쓴다.
3. **아래 함수 인벤토리 갱신**: 새 함수/이벤트를 한 줄 요약으로 추가한다.
4. **json-api 검증**: 스크래치 폴더에 CH full jar + `MethodScript/extensions/<빌드 jar>` 배치 후
   `java "-Djava.awt.headless=true" -jar <CH jar> json-api` → exit 0 + 새 함수명이 JSON에 존재 확인.

## ⚠️ 핵심 제약 (어기면 빌드/도구가 깨짐)

1. **json-api 안전성**: MethodScriptVSC가 Bukkit 클래스패스 없이 `java -jar CH.jar json-api`를 실행하며
   docs()/getName()/static 초기화가 호출됨 → 이 경로에서 Bukkit/Paper 클래스 참조 시
   NoClassDefFoundError로 전체가 죽음. **Bukkit 참조는 exec() 안에만** (static 필드 NamespacedKey 등 금지,
   지연 생성). docs()에서 Bukkit enum 나열 금지 — 문자열 하드코딩
2. **CacheAnnotations 리플렉션**: @api 클래스의 메서드 시그니처(private 포함)에 등장하는 타입은
   exec 클래스패스에서 해석돼야 함 → paper-api는 **compile 스코프 유지**(provided 금지),
   MEG·FSB처럼 provided인 타입은 @api 클래스 시그니처에 사용 금지 (바디는 OK)
3. **격리 패턴**: 외부 플러그인 타입은 어노테이션 없는 브리지 클래스에만
   (MegBridge/FsbBridge/RecipeBookBridge/ChfListener) — @api/@MSExtension 클래스는 참조 0,
   isPluginEnabled 가드 후 위임
4. 이벤트: Bukkit Listener는 ChfListener에 격리, @api AbstractEvent(ChfEvents)는 Bukkit 참조 0,
   Extension.onStartup에서 try/catch(Throwable) 등록. json-api엔 확장 이벤트 원래 미포함(정상)

## 함수 인벤토리 (0.9.1 기준, 상세 이력은 CHANGELOG.md)

- 가짜 발광: `chf_send_fake_glowing(entity, glowing|null, [targets])` /
  `chf_clear_fake_glowing(entity, [targets])` — 보는 사람별 발광(GlowBridge). 공유 플래그 바이트의
  0x40 비트만 변경해 SetEntityData 전송(타 플래그 보존), null=초기화. 서버 메타데이터 갱신 시 덮어써짐
- 패킷 유틸: `NmsPackets`(handle/send) — 새 패킷 기반 기능은 이걸 재사용할 것

- 가짜 끈: `chf_send_fake_leash(entity, holder, [receivers])` / `chf_clear_fake_leash(entity, [receivers])` —
  SetEntityLink 패킷 직접 전송(LeashBridge). 물리·끊김·드랍 없음, 홀더는 임의 엔티티,
  **끈이 그려지는 대상은 Mob/Boat만**(클라 Leashable 하드코딩). 클라 상태라 트래킹 재진입 시 재전송 필요

- 기본: `chf_version()`, `get_heightmap_highest_block_at(...)`,
  `chf_get_entity_data`/`chf_set_entity_data`(PDC 영구 저장), `chf_heal(uuid,amount,[reason])`,
  `chf_play_hurt_animation(uuid,[yaw])`(데미지 없는 피격 연출 — 코어 play_entity_effect HURT 는 1.19.4+ 미동작)
  (SetHealth 패킷 즉시 전송으로 HUD 회복 하이라이트 보장)
- 쿨다운: `chf_pcooldown([player],key)`/`chf_set_pcooldown([player],key,ticks)` — 코어 pcooldown 확장판,
  key가 아이템 메테리얼이면 메테리얼로, 아니면 커스텀 쿨다운 그룹 키(`ns:key`)로 처리 (CooldownBridge)
- 레시피북/거래: `chf_set/get_recipe_book_open(...)`(NMS 리플렉션), `get_trading_inventory([player])`,
  `set_trading_inventory([player],array)` — input1/input2만 설정 가능(result는 서버 재계산),
  이벤트 `player_recipe_book_toggle`(player/type[INVENTORY·WORKBENCH·화로3종]/is_open, 필터 토글 미발동)
- MEG(R4.1.0, mvn.lumine.io): meg_add/remove/get_models, meg_modify_model, meg_play/modify/stop_animation,
  meg_get_animations, meg_list_blueprints — **서버 MEG 업데이트 시 pom 버전 갱신+재빌드 필수**
- FiguraController(0.9.0 ~ · FcBridge 격리 · provided `kr.asmbl:figuracontroller` 0.2.0): `fc_state_get/get_all/set/unset/clear`, `fc_states_set`,
  `fc_signal_send(subject,name,[data],[audience])`, `fc_preset_load`, `fc_is_ready` · 이벤트 `fc_state_changed`(**name 프리필터 필수** —
  없으면 BindException · 던지기 전에 unbind) · `fc_signal` · 값 변환은 직접(CH json_decode 는 1.0 → 1 이라 안 쓴다) · 하네스 `projects/figura_controller`
- FiguraFSB: `fsb_send_packet(receiver,[avatarOwnerUUID,]name,data)`, `fsb_register_packet(name)`,
  `fsb_is_connected(player)`, 이벤트 `fsb_server_packet`(packet_name/player/sender_uuid/avatar_owner/is_host/data)
- 트래킹: `chf_get_tracked_by(entityUUID)` — Paper `Entity#getTrackedBy()` 래핑, 트래킹 중인 플레이어
  이름 배열. **실측상 본인 미포함이라 확장에서 플레이어 본인을 수동 보장 포함**(0.4.2).
  FSB 상태 전송 receivers 최적화용
- 아이템 디스폰: `chf_get_item_despawn_ticks(entityUUID)` / `chf_set_item_despawn_ticks(entityUUID, ticks)` —
  떨어진 아이템이 사라지기까지 남은 틱 (ItemDespawnBridge, NMS 리플렉션 `ItemEntity.age`+`despawnRate`).
  ticks=null 이면 무한. **age 가 NBT 에 short 로 저장돼 최대 약 38700틱까지만 설정 가능**(그 이상은 null 사용)

## 관련 문서

- 서버 CH 스크립트/브리지 규약: mc_content_production 하네스 `knowledge/` (fsb_bridge_system.md 등)
- FiguraFSB 플러그인 API: `../../group_figura/figura_fsbplugin_internal/API.md` (2026-09-25 `mc_repositories` 로 옮김)
