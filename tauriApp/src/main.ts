import { Game } from '../game-core/build/dist/js/productionLibrary';
import type { Face, Snapshot } from './types';
import * as storage from './storage';
import './style.css';

const app = document.querySelector<HTMLDivElement>('#app')!;
let game: Game;
let state: Snapshot;
let busy = false;
let tab = 'play';
let errorMessage = '';
let lastSaved = '';
let saveFailed = false;
let frame = 0;
const escape = (s: string) => s.replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]!));
const face = (f: Face) => `${escape(f.label)}${f.value ? ` ${f.value}` : ''}`;
const button = (label: string, action: string, arg = '', disabled = false) =>
  `<button data-action="${action}" data-arg="${escape(arg)}" ${disabled ? 'disabled' : ''}>${escape(label)}</button>`;
const labels: Record<string, string> = { START: '入口', COMBAT: '戦闘', ELITE_COMBAT: '精鋭戦', REWARD: '報酬', EVENT: 'イベント', REST: '休憩', BOSS: 'ボス' };

function render() {
  cancelAnimationFrame(frame);
  app.innerHTML = `<header><strong>CraftDice</strong><span>ダイスの欠片 <b>${state.balance}</b></span></header>
    <main><div id="notice" role="status" aria-live="polite">${escape(errorMessage || state.message)}</div>
    ${saveFailed ? `<section role="alert"><p>保存に失敗しました。再試行が完了するまで操作を停止しています。</p>${button('保存を再試行', 'retry-save')}</section>` : ''}
    ${state.screen === 'home' ? home() : state.screen === 'dungeon' ? dungeon() : battle()}</main>`;
  app.querySelectorAll<HTMLButtonElement>('button[data-action]').forEach(b => {
    if ((busy || saveFailed) && b.dataset.action !== 'retry-save') b.disabled = true;
    b.addEventListener('click', () => void dispatch(b.dataset.action!, b.dataset.arg!));
  });
  const canvas = app.querySelector<HTMLCanvasElement>('canvas');
  if (canvas) {
    let start: { x: number; y: number } | undefined;
    canvas.addEventListener('pointerdown', e => {
      if (busy || saveFailed || state.battle!.rolling || state.battle!.status !== 'ONGOING') return;
      start = { x: e.clientX, y: e.clientY }; canvas.setPointerCapture(e.pointerId);
    });
    canvas.addEventListener('pointerup', e => {
      if (!start) return;
      const scale = state.battle!.width / canvas.getBoundingClientRect().width;
      const dx = (e.clientX - start.x) * scale, dy = (e.clientY - start.y) * scale;
      start = undefined;
      if (Math.hypot(dx, dy) >= 20) void dispatch('throw', `${Math.max(-900, Math.min(900, dx * 3))}:${Math.max(-900, Math.min(900, dy * 3))}`);
    });
    canvas.addEventListener('pointercancel', () => { start = undefined; });
    draw();
    if (state.battle!.rolling) animate();
  }
}

function home() {
  return `<nav aria-label="メニュー">${['play', 'craft', 'workshop'].map((t, i) =>
    `<button data-action="tab" data-arg="${t}" aria-pressed="${tab === t}">${['探索', 'ダイス編集', '工房'][i]}</button>`).join('')}</nav>
    ${tab === 'play' ? `<section><h1>探索の準備</h1><div class="grid">${state.classes.map(c => `<article>
      <h2>${escape(c.name)}</h2><p>${escape(c.description)}</p>${button(c.id === state.classId ? '選択中' : c.unlocked ? '選択' : '未解放', 'class', c.id, !c.unlocked || c.id === state.classId)}</article>`).join('')}</div>
      <div class="faces">${state.loadout.flat().map(f => `<span class="face ${f.type}">${face(f)}</span>`).join('')}</div>
      <label>シード <input id="seed" type="number" step="1" value="${crypto.getRandomValues(new Uint32Array(1))[0]}"></label>
      ${button('探索開始', 'start')}<p class="muted">探索の途中経過とダイス編集は終了時にリセットされます。</p></section>`
    : tab === 'workshop' ? `<section><h1>永続強化</h1><p>累計獲得 ${state.totalEarned}</p><div class="grid">${state.upgrades.map(u =>
      `<article><h2>${escape(u.name)}</h2><p>${escape(u.description)}</p><p>費用 ${u.cost} 欠片</p>${button(u.owned ? '購入済み' : state.balance < u.cost ? '欠片不足' : '購入', 'purchase', u.id, u.owned || state.balance < u.cost)}</article>`).join('')}</div></section>`
    : `<section><h1>ダイス編集</h1><p class="muted">次の探索に適用・費用なし</p><div class="grid">${state.loadout[0].map((f, i) =>
      `<article><h2>面 ${i + 1}：${face(f)}</h2><p>出現確率 ${(100 / state.loadout[0].length).toFixed(1)}%</p>${button('置換', 'replace', String(i))}${button('削除', 'craft', `remove:${i}`, state.loadout[0].length <= 1)}</article>`).join('')}</div>
      <div class="controls"><label>種類 <select id="skill"><option value="ATK">攻撃</option><option value="DEF">防御</option><option value="HEAL">回復</option><option value="CRIT">必殺</option><option value="MISS">ミス</option></select></label>
      <label>数値 <input id="value" type="number" min="1" max="99" value="4"></label>${button('面を追加', 'add', '', state.loadout[0].length >= 6)}</div></section>`}`;
}

function dungeon() {
  const r = state.run!;
  if (r.status !== 'ONGOING') return `<section><h1>${r.status === 'VICTORY' ? '踏破成功' : '探索終了'}</h1><p>攻略部屋 ${r.cleared}・獲得 ${r.reward} 欠片</p>${button('工房へ', 'home')}</section>`;
  const room = r.rooms.find(n => n.id === r.current)!;
  return `<section><h1>ダンジョン</h1><p>HP ${r.hp} / ${r.maxHp}　素材 ${r.material}　面 ${r.fragments}</p>
    <p class="muted">シード ${r.seed}・攻略 ${r.cleared} 部屋</p>
    <div class="map">${[...new Set(r.rooms.map(n => n.floor))].map(floor => `<div class="floor"><span>${floor + 1} 層</span>${r.rooms.filter(n => n.floor === floor).map(n =>
      `<button data-action="move" data-arg="${n.id}" ${!n.reachable ? 'disabled' : ''} ${n.id === r.current ? 'aria-current="step"' : ''}>${escape(labels[n.type] ?? n.type)}${n.cleared ? ' ✓' : ''}${n.id === r.current ? '（現在地）' : ''}</button>`).join('')}</div>`).join('')}</div>
    <article><h2>${escape(labels[room.type])}${room.name !== room.type ? `：${escape(room.name)}` : ''}</h2>
    ${room.cleared || room.type === 'START' ? '<p>次の部屋</p>'
      : ['COMBAT', 'ELITE_COMBAT', 'BOSS'].includes(room.type) ? button('戦闘開始', 'fight')
      : room.type === 'REWARD' ? button('報酬を受け取る', 'reward')
      : room.type === 'REST' ? button('休憩する', 'rest')
      : r.choices.map((c, i) => button(c, 'event', String(i))).join('')}</article></section>`;
}

function battle() {
  const b = state.battle!;
  return `<section><h1>戦闘 <small>ラウンド ${b.round}</small></h1><div class="units">${b.units.map(u => `<article class="${u.enemy ? 'enemy' : 'player'}"><h2>${escape(u.name)}</h2><p>HP ${u.hp} / ${u.maxHp}・破損 ${u.broken} 面</p><meter min="0" max="${u.maxHp}" value="${u.hp}"></meter></article>`).join('')}</div>
    <canvas width="${b.width}" height="${b.height}" aria-label="ダイストレイ。ドラッグで投げる方向と強さを指定" role="img"></canvas>
    <div class="controls">${b.status === 'ONGOING' ? button(b.rolling ? '転がり中…' : '投げる', 'throw', '450:180', b.rolling)
      : `<strong>${b.status === 'PLAYER1_WON' ? '戦闘勝利' : b.status === 'DRAW' ? '引き分け' : '戦闘敗北'}</strong>${button('探索へ戻る', 'continue')}`}
      <details><summary>投げ方</summary><p>トレイをドラッグすると方向と強さを指定できます。「投げる」はキーボードでも操作できます。</p></details></div>
    <ol class="log" aria-live="polite">${b.log.map(l => `<li>${escape(l)}</li>`).join('')}</ol></section>`;
}

function draw() {
  const canvas = app.querySelector('canvas'); if (!canvas) return;
  const ctx = canvas.getContext('2d')!; const b = state.battle!;
  ctx.clearRect(0, 0, b.width, b.height);
  ctx.strokeStyle = '#426258'; ctx.setLineDash([6, 8]); ctx.beginPath(); ctx.moveTo(b.width / 2, 0); ctx.lineTo(b.width / 2, b.height); ctx.stroke(); ctx.setLineDash([]);
  for (const d of b.dice) {
    ctx.fillStyle = d.enemy ? '#f8b19d' : '#b4eee0'; ctx.beginPath(); ctx.roundRect(d.x - d.radius, d.y - d.radius, d.radius * 2, d.radius * 2, 6); ctx.fill();
    ctx.fillStyle = '#16251f'; ctx.font = 'bold 10px sans-serif'; ctx.textAlign = 'center'; ctx.fillText(`${d.face.label}${d.face.value || ''}`, d.x, d.y + 4);
  }
}

function animate() {
  let previous = performance.now(), accumulator = 0;
  function step(now: number) {
    accumulator += Math.min(now - previous, 100); previous = now;
    while (accumulator >= 1000 / 60 && state.battle!.rolling) {
      state = JSON.parse(game.tick()); accumulator -= 1000 / 60;
    }
    draw();
    if (state.battle!.rolling) frame = requestAnimationFrame(step); else render();
  }
  frame = requestAnimationFrame(step);
}

async function persist() {
  const data = game.save();
  if (data === lastSaved) return;
  await storage.save(data); lastSaved = data;
}

async function dispatch(action: string, arg = '') {
  if (busy || (saveFailed && action !== 'retry-save')) return;
  if (action === 'tab') { tab = arg; errorMessage = ''; render(); return; }
  if (action === 'start') {
    const seed = app.querySelector<HTMLInputElement>('#seed')!.value;
    if (!/^-?\d+$/.test(seed) || !Number.isSafeInteger(Number(seed))) { errorMessage = 'シードは安全な範囲の整数を指定してください'; render(); return; }
    arg = seed;
  }
  if (['replace', 'add'].includes(action)) {
    const skill = app.querySelector<HTMLSelectElement>('#skill')!.value;
    const value = app.querySelector<HTMLInputElement>('#value')!.value;
    arg = `${action}:${arg || '0'}:${skill}:${value}`; action = 'craft';
  }
  busy = true; errorMessage = '';
  try {
    if (action !== 'retry-save') state = JSON.parse(game.command(action, arg));
    try { await persist(); saveFailed = false; }
    catch (e) { saveFailed = true; throw e; }
  } catch (e) { errorMessage = e instanceof Error ? e.message : String(e); }
  finally { busy = false; render(); }
}

try {
  const saved = await storage.load();
  game = new Game(saved); lastSaved = saved;
  state = JSON.parse(game.snapshot()); render();
} catch (e) {
  // Never overwrite an unreadable save with defaults.
  app.innerHTML = `<main><h1>セーブを読み込めませんでした</h1><p role="alert">${escape(String(e))}</p><p>アプリを終了し、セーブを確認してから再起動してください。</p></main>`;
}
