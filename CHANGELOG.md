# Changelog

## v1.0.1

버그 수정 릴리스.

- **컴파일 오류 수정**: `ItemEditorMenu`의 가격 입력 시 인벤토리를 닫는 코드(`runTask(plugin, p::closeInventory)`)가
  Paper 1.21.4 API 에서 모호한 메서드 참조로 컴파일에 실패하던 문제를 람다(`() -> p.closeInventory()`)로 교체하여 수정.
  (`closeInventory()` 오버로드 때문에 `runTask(Plugin, Runnable)` / `runTask(Plugin, Consumer<BukkitTask>)` 중
  어느 쪽인지 컴파일러가 결정하지 못하던 문제)
- 기능 변화는 없습니다.

## v1.0.0

최초 릴리스.

- `/상점` — GUI 상점 (좌클릭 구매 / 우클릭 판매 / 쉬프트+클릭 대량 거래)
- `/상점관리 설정` — 드래그 앤 드롭 상점 편집 UI
- `/상점관리 페이지 <번호>` — 최대 페이지 설정
- `/상점관리 리로드` — config.yml + shop.yml 리로드
- Vault 이코노미 연동 (Paper 1.21.4)
