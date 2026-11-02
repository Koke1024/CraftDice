import { test } from 'node:test';
import assert from 'node:assert/strict';
import { Game } from '../game-core/build/dist/js/productionLibrary/craftdice-game-core.mjs';
const snapshot = g => JSON.parse(g.snapshot());
const command = (g, action, arg = '') => JSON.parse(g.command(action, String(arg)));

test('purchases and class selection round-trip; invalid saves are rejected', () => {
  const g = new Game(JSON.stringify({ version: 1, progress: {
    diceShards: 100, totalEarned: 100, upgrades: [], classId: 'warrior',
  } }));
  command(g, 'purchase', 'unlock_mage');
  command(g, 'class', 'mage');
  assert.equal(snapshot(g).balance, 50);
  assert.throws(() => command(g, 'purchase', 'unlock_mage'));
  assert.throws(() => command(g, 'class', 'guardian'));
  const reloaded = new Game(g.save());
  assert.equal(snapshot(reloaded).classId, 'mage');
  assert.equal(snapshot(reloaded).balance, 50);
  assert.throws(() => new Game('broken'));
  assert.throws(() => new Game('{"version":99}'));
});

test('seeded maps and crafting use shared domain rules', () => {
  const a = new Game(''), b = new Game('');
  command(a, 'craft', 'remove:5');
  assert.equal(snapshot(a).loadout[0].length, 5);
  command(a, 'craft', 'replace:0:CRIT:99');
  assert.equal(snapshot(a).loadout[0][0].value, 99);
  assert.throws(() => command(a, 'craft', 'replace:0:ATK:0'));
  command(a, 'start', 42); command(b, 'start', 42);
  assert.deepEqual(snapshot(a).run.rooms, snapshot(b).run.rooms);
  assert.throws(() => command(a, 'move', 999));
  assert.throws(() => command(a, 'craft', 'remove:0'));
  assert.throws(() => command(a, 'start', 1));
});

test('a complete run drives real physics and combat; rewards cannot be claimed twice', () => {
  const g = new Game('');
  command(g, 'start', 42);
  let s = snapshot(g), battles = 0;
  for (let turns = 0; turns < 100 && s.run.status === 'ONGOING'; turns++) {
    const room = s.run.rooms.find(r => r.id === s.run.current);
    if (room.type === 'START' || room.cleared) {
      const next = s.run.rooms.find(r => r.reachable);
      assert.ok(next); s = command(g, 'move', next.id); continue;
    }
    assert.throws(() => command(g, 'move', s.run.rooms.find(r => r.floor > room.floor)?.id ?? 0));
    if (['COMBAT', 'ELITE_COMBAT', 'BOSS'].includes(room.type)) {
      s = command(g, 'fight'); battles++;
      assert.throws(() => command(g, 'fight'));
      for (let round = 0; round < 200 && s.battle.status === 'ONGOING'; round++) {
        s = command(g, 'throw', `${300 + round * 7}:180`);
        assert.throws(() => command(g, 'throw', '400:100'));
        let ticks = 0;
        while (s.battle.rolling && ticks++ < 1200) s = JSON.parse(g.tick());
        assert.equal(s.battle.rolling, false, 'dice must settle');
      }
      assert.notEqual(s.battle.status, 'ONGOING');
      s = command(g, 'continue');
      assert.throws(() => command(g, 'continue'));
    } else if (room.type === 'EVENT') s = command(g, 'event', 1);
    else if (room.type === 'REWARD') { s = command(g, 'reward'); assert.throws(() => command(g, 'reward')); }
    else if (room.type === 'REST') { s = command(g, 'rest'); assert.throws(() => command(g, 'rest')); }
  }
  assert.ok(battles > 0);
  assert.notEqual(s.run.status, 'ONGOING');
  assert.equal(s.balance, s.run.reward);
  const balance = s.balance;
  command(g, 'home');
  assert.equal(snapshot(new Game(g.save())).balance, balance);
  assert.throws(() => command(g, 'home'));
});
