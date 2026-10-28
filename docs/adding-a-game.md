# 加一款新游戏

目标：只新增 `engine/src/main/java/boardgame/engine/games/<id>/` 和 `web/src/games/<id>/`，不修改 `engine/core`。

## 1. 数据模型

把规则需要的东西写成不可变 record，随机顺序（牌堆、袋）直接存进 state。

```java
public record FooState(int playerCount, int currentSeat, List<Card> deck, ...) {
    public FooState {
        deck = List.copyOf(deck);   // 构造时拷贝，保证外部改不动
    }
}
```

## 2. 动作

动作是离散且可序列化的，必须给出稳定的 `key()`，前端只回传 key。

```java
public sealed interface FooAction extends Action {
    record Play(Card card) implements FooAction {
        public String key() { return "play:" + card.code(); }
        public String label() { return "打出 " + card.code(); }
    }
}
```

## 3. 实现 GameDefinition

```java
public final class FooDefinition implements GameDefinition<FooState, FooAction> {
    public String id() { return "foo"; }
    public String displayName() { return "示例游戏"; }
    public int minPlayers() { return 2; }
    public int maxPlayers() { return 4; }

    public FooState newGame(NewGameConfig cfg) { /* 只用 cfg.seed() 洗牌 */ }
    public List<FooAction> legalActions(FooState state) { /* 必须覆盖所有合法选择 */ }
    public FooState apply(FooState state, FooAction action) { /* 纯函数，返回新状态 */ }
    public boolean isTerminal(FooState state) { return false; }
    public GameOutcome outcome(FooState state) { /* 分数与胜者 */ }
    public int currentSeat(FooState state) { return 0; }
    public String phase(FooState state) { return "PLAY"; }
    public Object view(FooState state, int seat) { /* 按座位裁剪隐藏信息 */ }
    public String describe(FooState state, FooAction action) { return action.label(); }
}
```

按需覆盖的钩子：

- `determinize(state, rng, seat)`：有隐藏信息时，采样一个与已知信息一致的世界。MCTS 会反复调用它。
- `heuristic(state, seat)`：搜索截断时的估值，越大越好。不实现也能跑，只是 AI 会弱一些。

## 4. 测试

- 规则边界：每个合法动作的准入条件、计分、终局。
- 随机对局：几百局随机自对战，断言必然终局、不抛异常。
- 动作 key 在任意局面下唯一。

照着 `engine/src/test/java/.../splendor/SplendorRulesTest.java` 抄结构即可。

## 5. 注册后端

`server/.../config/GameCatalog.java` 构造函数里加一行：

```java
register(new FooDefinition());
```

## 6. 注册前端

`web/src/games/<id>/Board.tsx` 接收 `{ view, mySeat, actions, disabled, onAction }`，然后在
`web/src/games/registry.ts` 注册：

```ts
export const GAME_BOARDS = {
  splendor: SplendorBoard,
  foo: FooBoard,      // 新增
};
```

## 7. 验收

```bash
mvn test
cd web && npm run typecheck && npm run test
```

能在大厅里开局、和 AI 打完一整局，且 `engine/core` 一行没改，就说明抽象成立。
