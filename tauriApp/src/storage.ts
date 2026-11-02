import { invoke, isTauri } from '@tauri-apps/api/core';
const key = 'craftdice.tauri.preview.v1';
export async function load(): Promise<string> {
  return isTauri() ? invoke<string>('load_progress') : localStorage.getItem(key) ?? '';
}
export async function save(data: string): Promise<void> {
  if (isTauri()) await invoke('save_progress', { data });
  else localStorage.setItem(key, data);
}
