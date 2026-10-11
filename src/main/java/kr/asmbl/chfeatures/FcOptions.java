package kr.asmbl.chfeatures;

/** fc_state_set 옵션 — FiguraController 타입 없이(@api 클래스 몸에서 쓴다). StateOptions 로는 FcBridge 가 바꾼다 */
record FcOptions(boolean persist, boolean priv, int expire) {
}
