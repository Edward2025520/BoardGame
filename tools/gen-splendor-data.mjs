// 从公开文档里的数据块生成 SplendorData.java，避免手抄 90 张卡出错。
// 用法：node tools/gen-splendor-data.mjs
import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname } from 'node:path';

const SOURCE = 'https://roeey777.github.io/Splendor-AI/_modules/splendor/splendor/splendor_utils.html';
const TARGET = 'engine/src/main/java/boardgame/engine/games/splendor/rules/SplendorData.java';

const html = await (await fetch(SOURCE)).text();
const text = html
  .replace(/<[^>]+>/g, '')
  .replace(/&quot;/g, '"')
  .replace(/&#39;/g, "'")
  .replace(/&lt;/g, '<')
  .replace(/&gt;/g, '>')
  .replace(/&amp;/g, '&');

const cardsAt = text.indexOf('CARDS');
const noblesAt = text.indexOf('NOBLES');
const coloursAt = text.indexOf('COLOURS');
if (cardsAt < 0 || noblesAt < 0 || coloursAt < 0) {
  throw new Error('未找到 CARDS / NOBLES / COLOURS 区块，页面结构可能变了');
}
const cardBlock = text.slice(cardsAt, noblesAt);
const nobleBlock = text.slice(noblesAt, coloursAt);

const cards = [];
for (const m of cardBlock.matchAll(
  /"([^"]+)":\s*\("(\w+)",\s*\{([^}]*)\},\s*(\d+),\s*(\d+)\)/g,
)) {
  const [, code, colour, costRaw, deck, points] = m;
  const cost = {};
  for (const c of costRaw.matchAll(/"(\w+)":\s*(\d+)/g)) cost[c[1]] = Number(c[2]);
  cards.push({ code, colour, cost, tier: Number(deck), points: Number(points) });
}

const nobles = [];
for (const m of nobleBlock.matchAll(/\("([^"]+)",\s*\{([^}]*)\}\)/g)) {
  const [, code, reqRaw] = m;
  const requirement = {};
  for (const c of reqRaw.matchAll(/"(\w+)":\s*(\d+)/g)) requirement[c[1]] = Number(c[2]);
  nobles.push({ code, requirement });
}

const order = { white: 0, blue: 1, green: 2, red: 3, black: 4 };
cards.sort((a, b) => a.tier - b.tier || order[a.colour] - order[b.colour] || a.code.localeCompare(b.code));
nobles.sort((a, b) => a.code.localeCompare(b.code));

const counts = [0, 0, 0];
for (const c of cards) counts[c.tier - 1]++;
if (cards.length !== 90 || counts.join(',') !== '40,30,20') {
  throw new Error(`卡牌数量不对: ${cards.length} / ${counts}`);
}
if (nobles.length !== 10) {
  throw new Error(`贵族数量不对: ${nobles.length}`);
}

const costLine = (cost) =>
  Object.entries(cost).map(([g, n]) => `${g}=${n}`).join(',');

const cardLines = cards
  .map((c) => `${c.code}|${c.colour}|${c.tier}|${c.points}|${costLine(c.cost)}`)
  .join('\n');
const nobleLines = nobles.map((n) => `${n.code}|${costLine(n.requirement)}`).join('\n');

const java = `package boardgame.engine.games.splendor.rules;

import boardgame.engine.games.splendor.model.Card;
import boardgame.engine.games.splendor.model.Gem;
import boardgame.engine.games.splendor.model.Noble;
import java.util.ArrayList;
import java.util.List;

/**
 * 璀璨宝石的 90 张发展卡与 10 张贵族。
 *
 * <p>数据由 tools/gen-splendor-data.mjs 生成，格式：
 * {@code 卡号|颜色|等级|分数|成本}。卡号在原版中唯一，直接用作动作与美术资源的 key。
 */
public final class SplendorData {

    private static final String RAW_CARDS = """
${cardLines}
            """;

    private static final String RAW_NOBLES = """
${nobleLines}
            """;

    public static final List<Card> ALL_CARDS;
    public static final List<Noble> ALL_NOBLES;

    static {
        List<Card> cards = new ArrayList<>(90);
        for (String line : RAW_CARDS.strip().split("\\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.strip().split("\\\\|");
            cards.add(new Card(parts[0], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
                    Gem.fromName(parts[1]), parseCost(parts[4])));
        }
        ALL_CARDS = List.copyOf(cards);

        List<Noble> nobles = new ArrayList<>(10);
        for (String line : RAW_NOBLES.strip().split("\\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] parts = line.strip().split("\\\\|");
            nobles.add(new Noble(parts[0], parseCost(parts[1])));
        }
        ALL_NOBLES = List.copyOf(nobles);
    }

    private SplendorData() {
    }

    private static int[] parseCost(String raw) {
        int[] cost = new int[Gem.values().length];
        if (raw.isBlank()) {
            return cost;
        }
        for (String item : raw.split(",")) {
            String[] kv = item.split("=");
            cost[Gem.fromName(kv[0]).ordinal()] = Integer.parseInt(kv[1]);
        }
        return cost;
    }
}
`;

mkdirSync(dirname(TARGET), { recursive: true });
writeFileSync(TARGET, java, 'utf8');
console.log(`写入 ${TARGET}: ${cards.length} 张卡, ${nobles.length} 张贵族`);
