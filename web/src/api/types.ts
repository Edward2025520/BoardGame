export type Difficulty =
  | 'BEGINNER'
  | 'CASUAL'
  | 'ADVANCED'
  | 'MEDIUM'
  | 'HARD'
  | 'MASTER'
  | 'HELL';

export interface GameSummary {
  id: string;
  name: string;
  description: string;
  minPlayers: number;
  maxPlayers: number;
}

export interface ActionOption {
  key: string;
  label: string;
}

export interface SeatInfo {
  seat: number;
  kind: 'HUMAN' | 'AI';
  current: boolean;
}

export interface LogItem {
  version: number;
  seat: number;
  text: string;
  at: number;
}

export interface Outcome {
  points: number[];
  winners: number[];
}

export interface SessionDto {
  sessionId: string;
  gameId: string;
  gameName: string;
  seatCount: number;
  version: number;
  currentSeat: number;
  phase: string;
  finished: boolean;
  difficulty: Difficulty;
  difficultyLabel: string;
  humanSeats: number[];
  seats: SeatInfo[];
  aiThinking: boolean;
  canUndo: boolean;
  reviewable: boolean;
  boards: Record<string, unknown>;
  legalActions: ActionOption[];
  log: LogItem[];
  outcome: Outcome | null;
}

export interface HintResult {
  actionKey: string;
  label: string;
}

export interface ReviewItem {
  index: number;
  label: string;
}

export interface ReviewList {
  total: number;
  items: ReviewItem[];
}

export interface ReviewMove {
  index: number;
  chosenLabel: string;
  bestLabel: string;
  delta: number;
}

export const GEMS = ['white', 'blue', 'green', 'red', 'black', 'gold'] as const;
export type Gem = (typeof GEMS)[number];

export const GEM_LABEL: Record<Gem, string> = {
  white: '白',
  blue: '蓝',
  green: '绿',
  red: '红',
  black: '黑',
  gold: '金',
};

export const GEM_COLOR: Record<Gem, string> = {
  white: '#efe9dc',
  blue: '#4a86c8',
  green: '#3f9a5f',
  red: '#c8584f',
  black: '#33333d',
  gold: '#d3ac46',
};

export interface SplendorCard {
  code: string;
  tier: number;
  points: number;
  bonus: string;
  cost: Record<string, number>;
}

export interface SplendorSeat {
  seat: number;
  points: number;
  cards: number;
  tokens: number[];
  bonuses: number[];
  reservedCount: number;
  reserved: SplendorCard[];
  current: boolean;
}

export interface SplendorRow {
  tier: number;
  cards: SplendorCard[];
  deckSize: number;
}

export interface SplendorNoble {
  code: string;
  requirement: Record<string, number>;
  qualified: boolean;
}

export interface SplendorView {
  playerCount: number;
  currentSeat: number;
  phase: string;
  finalRound: boolean;
  bank: number[];
  goldBank: number;
  reservedLimit: number;
  seats: SplendorSeat[];
  rows: SplendorRow[];
  nobles: SplendorNoble[];
}

export const AZUL_COLORS = ['blue', 'yellow', 'red', 'black', 'white'] as const;
export type AzulColor = (typeof AZUL_COLORS)[number];

export const AZUL_LABEL: Record<AzulColor, string> = {
  blue: '蓝',
  yellow: '黄',
  red: '红',
  black: '黑',
  white: '白',
};

export interface AzulLine {
  color: string | null;
  count: number;
  capacity: number;
}

export interface AzulWallCell {
  color: string;
  filled: boolean;
}

export interface AzulSeat {
  seat: number;
  score: number;
  lines: AzulLine[];
  wall: AzulWallCell[][];
  floor: string[];
  hasMarker: boolean;
  current: boolean;
  first: boolean;
}

export interface AzulFactory {
  index: number;
  tiles: string[];
}

export interface AzulCenter {
  tiles: string[];
  hasMarker: boolean;
}

export interface AzulView {
  playerCount: number;
  currentSeat: number;
  round: number;
  phase: string;
  bagCount: number;
  factories: AzulFactory[];
  center: AzulCenter;
  seats: AzulSeat[];
}

