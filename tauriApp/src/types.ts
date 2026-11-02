export interface Face { type: string; label: string; value: number; rarity: string }
export interface Room { id: number; floor: number; type: string; name: string; cleared: boolean; reachable: boolean }
export interface Snapshot {
  screen: 'home' | 'dungeon' | 'battle'; message: string; balance: number; totalEarned: number; classId: string;
  loadout: Face[][];
  classes: { id: string; name: string; description: string; unlocked: boolean }[];
  upgrades: { id: string; name: string; description: string; cost: number; owned: boolean }[];
  run?: { status: string; seed: string; cleared: number; reward: number; hp: number; maxHp: number; material: number;
    fragments: number; current: number; rooms: Room[]; choices: string[] };
  battle?: { round: number; status: string; rolling: boolean; width: number; height: number;
    units: { id: number; name: string; enemy: boolean; hp: number; maxHp: number; broken: number }[];
    dice: { id: number; x: number; y: number; radius: number; face: Face; enemy: boolean }[]; log: string[] };
}
