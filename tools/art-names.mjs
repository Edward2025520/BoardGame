// 卡面文件名的唯一真源：抓图脚本、导入脚本、测试都从这里取规则。
// 标准名：
//   发展卡 card-{等级}-{颜色}-{分数}-{白-蓝-绿-红-黑成本}
//   贵族   noble-{白-蓝-绿-红-黑需求}
// 不用卡号命名，避免 3b/3B、4w4b/4w4B 在 Windows 上撞成同一个文件。

export const COLORS = ['white', 'blue', 'green', 'red', 'black'];
export const IMAGE_EXTENSIONS = ['.png', '.jpg', '.jpeg', '.webp'];

/** 允许 cost/requirement 传长度 ≥5 的数组，或 {white:2,...} 这样的对象。 */
export function normalizeCost(cost) {
  const vector = [0, 0, 0, 0, 0];
  if (Array.isArray(cost)) {
    for (let index = 0; index < 5; index++) {
      vector[index] = Number(cost[index] ?? 0);
    }
    return vector;
  }
  for (const [key, value] of Object.entries(cost ?? {})) {
    const index = COLORS.indexOf(key);
    if (index >= 0) {
      vector[index] = Number(value);
    }
  }
  return vector;
}

export function colorName(color) {
  return typeof color === 'number' ? (COLORS[color] ?? 'unknown') : color;
}

export function cardArtName(card) {
  const cost = normalizeCost(card.cost);
  return `card-${card.tier}-${colorName(card.color)}-${card.points}-${cost.join('-')}`;
}

export function nobleArtName(noble) {
  const requirement = normalizeCost(noble.requirement);
  return `noble-${requirement.join('-')}`;
}

export function isImageFile(fileName) {
  const lower = fileName.toLowerCase();
  return IMAGE_EXTENSIONS.some((extension) => lower.endsWith(extension));
}

/** jpeg 统一成 jpg，其余小写。 */
export function normalizeExtension(fileName) {
  const dot = fileName.lastIndexOf('.');
  if (dot < 0) {
    return '';
  }
  const extension = fileName.slice(dot).toLowerCase();
  return extension === '.jpeg' ? '.jpg' : extension;
}
