# MCplugin_CHFeatures

**[한국어](#한국어)** 설명은 아래에 있습니다.

> A **custom CommandHelper (MethodScript) extension** for Purpur 1.21.8 (Paper-based) servers.
> It exposes Paper API · NMS · other plugins (ModelEngine · [FiguraFSB](https://github.com/kwangsoo-assemble/MCplugin_FiguraFSB_quadaFork) ·
> [FiguraController](https://github.com/kwangsoo-assemble/MCplugin_FiguraController)) as MethodScript functions and events.
> Package `kr.asmbl.chfeatures`.

> This extension was written with an AI coding assistant (Claude Code) and checked by the maintainer on a real server.
> The function and event list is in `CLAUDE.md` («함수 인벤토리»), and per-version changes are in `CHANGELOG.md` (both in Korean).

## Environment

- Java 21+ (locally: Temurin 25)
- Maven Wrapper included (`mvnw.cmd`) — no separate Maven install needed
- CommandHelper 3.3.5-SNAPSHOT (letsbuild.net Jenkins build)
- Paper API 1.21.8-R0.1-SNAPSHOT

## One-time setup: install the CommandHelper jar into the local repository

CommandHelper is not on a public Maven repository, so the full jar from Jenkins has to be installed into the
local Maven repository (put the downloaded jar in `libs/` — it is not committed to this repository).

```
.\mvnw.cmd install:install-file "-Dfile=libs/commandhelper-3.3.5-SNAPSHOT-full.jar" "-DgroupId=com.sk89q" "-DartifactId=commandhelper" "-Dversion=3.3.5-SNAPSHOT" "-Dclassifier=full" "-Dpackaging=jar"
```

When updating the CH snapshot, download it again from the link below and rerun the command:
https://letsbuild.net/jenkins/job/CommandHelper/lastSuccessfulBuild/artifact/target/commandhelper-3.3.5-SNAPSHOT-full.jar

The FiguraFSB jar needs the same local install (copy the FSB project's build output into `libs/`):

```
.\mvnw.cmd install:install-file "-Dfile=libs/figura-fsb-0.1.6-spigot.jar" "-DgroupId=org.figuramc.figura" "-DartifactId=figura-fsb-spigot" "-Dversion=0.1.6" "-Dpackaging=jar"
```

The FiguraController integration (0.9.0+) is installed locally from the plugin repository (`../FiguraController`):

```
cd ../FiguraController && .\mvnw.cmd install
```

ModelEngine resolves automatically from its public repository (mvn.lumine.io).
Keep the FSB/MEG versions in line with the versions installed on the server, and bump them in pom.xml when they change.

## Building

```
.\mvnw.cmd package
```

Output: `target/chfeatures-<version>.jar`
→ put it in the server's `plugins/CommandHelper/extensions/` and run `/reloadaliases` (or restart the server).

## Structure

- `src/main/java/kr/asmbl/chfeatures/Extension.java` — the `@MSExtension` lifecycle class
- `src/main/java/kr/asmbl/chfeatures/Functions.java` — `@api` functions (sample: `chf_version()`)
- At build time `maven-processor-plugin` validates the extension annotations at compile time, and
  `exec-maven-plugin` (CacheAnnotations) bundles the annotation cache used at runtime into the jar.
  Both plugins are required — without them the extension does not load or loads very slowly.

## References

- Official docs: https://methodscript.com/docs/3.3.5/Extension_Development.html
- Reference extensions: [CHPaper](https://github.com/PseudoKnight/CHPaper), [CHNaughty](https://github.com/PseudoKnight/CHNaughty)
- Build server: https://letsbuild.net/jenkins/

## License

**MIT** (`LICENSE`).

---

# 한국어

> Purpur 1.21.8(Paper 기반) 서버용 **커스텀 CommandHelper(MethodScript) 확장**이다.
> Paper API · NMS · 다른 플러그인(ModelEngine · [FiguraFSB](https://github.com/kwangsoo-assemble/MCplugin_FiguraFSB_quadaFork) ·
> [FiguraController](https://github.com/kwangsoo-assemble/MCplugin_FiguraController))의 기능을 MethodScript 함수 · 이벤트로 연다.
> 패키지 `kr.asmbl.chfeatures`.

> 이 확장은 AI 코딩 도구(Claude Code)로 작성했고 유지보수자가 실제 서버에서 확인했다.
> 함수 · 이벤트 목록은 `CLAUDE.md` 「함수 인벤토리」, 버전별 변경은 `CHANGELOG.md` 에 있다.

## 환경

- Java 21+ (로컬: Temurin 25)
- Maven Wrapper 내장 (`mvnw.cmd`) — Maven 별도 설치 불필요
- CommandHelper 3.3.5-SNAPSHOT (letsbuild.net Jenkins 빌드)
- Paper API 1.21.8-R0.1-SNAPSHOT

## 최초 1회: CommandHelper jar 로컬 레포 설치

CommandHelper는 공개 메이븐 레포에 없으므로, Jenkins에서 받은 full jar를
로컬 메이븐 레포에 설치해야 한다. (`libs/`에 다운로드된 jar 사용 — 저장소에는 올리지 않는다)

```
.\mvnw.cmd install:install-file "-Dfile=libs/commandhelper-3.3.5-SNAPSHOT-full.jar" "-DgroupId=com.sk89q" "-DartifactId=commandhelper" "-Dversion=3.3.5-SNAPSHOT" "-Dclassifier=full" "-Dpackaging=jar"
```

CH 스냅샷을 갱신할 때는 아래에서 다시 받아 위 명령을 재실행:
https://letsbuild.net/jenkins/job/CommandHelper/lastSuccessfulBuild/artifact/target/commandhelper-3.3.5-SNAPSHOT-full.jar

FiguraFSB 연동용 jar도 동일하게 로컬 설치가 필요하다 (FSB 프로젝트 빌드 산출물을 `libs/`에 복사):

```
.\mvnw.cmd install:install-file "-Dfile=libs/figura-fsb-0.1.6-spigot.jar" "-DgroupId=org.figuramc.figura" "-DartifactId=figura-fsb-spigot" "-Dversion=0.1.6" "-Dpackaging=jar"
```

FiguraController 연동(0.9.0~)은 플러그인 저장소(`../FiguraController`)에서 로컬 설치한다:

```
cd ../FiguraController && .\mvnw.cmd install
```

ModelEngine은 공개 레포(mvn.lumine.io)에서 자동 해석되므로 별도 설치 불필요.
FSB/MEG 버전은 서버 설치 버전과 일치시키고, 갱신 시 pom.xml의 버전도 같이 올린다.

## 빌드

```
.\mvnw.cmd package
```

산출물: `target/chfeatures-<버전>.jar`
→ 서버의 `plugins/CommandHelper/extensions/`에 넣고 `/reloadaliases` (또는 서버 재시작).

## 구조

- `src/main/java/kr/asmbl/chfeatures/Extension.java` — `@MSExtension` 라이프사이클 클래스
- `src/main/java/kr/asmbl/chfeatures/Functions.java` — `@api` 함수들 (샘플: `chf_version()`)
- 빌드 시 `maven-processor-plugin`이 확장 어노테이션을 컴파일 타임 검증하고,
  `exec-maven-plugin`(CacheAnnotations)이 런타임 로딩용 어노테이션 캐시를 jar에 포함시킨다.
  이 두 플러그인은 필수 — 빼면 확장이 로드되지 않거나 매우 느려진다.

## 참고

- 공식 문서: https://methodscript.com/docs/3.3.5/Extension_Development.html
- 참고 확장: [CHPaper](https://github.com/PseudoKnight/CHPaper), [CHNaughty](https://github.com/PseudoKnight/CHNaughty)
- 빌드 서버: https://letsbuild.net/jenkins/

## 라이선스

**MIT** (`LICENSE`).
