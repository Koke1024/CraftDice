import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
const root = fileURLToPath(new URL('../../', import.meta.url));
const tasks = process.argv[2] === 'test'
  ? ['jsNodeTest', 'jsProductionLibraryDistribution'] : ['jsProductionLibraryDistribution'];
// cmd.exe handles Gradle's Windows batch wrapper. Arguments are fixed, never user input.
const windows = process.platform === 'win32';
const result = spawnSync(windows ? 'cmd.exe' : './gradlew', windows
  ? ['/d', '/s', '/c', 'gradlew.bat -p tauriApp/game-core ' + tasks.join(' ') + ' --no-daemon']
  : ['-p', 'tauriApp/game-core', ...tasks, '--no-daemon'], { cwd: root, stdio: 'inherit' });
if (result.error) throw result.error;
process.exit(result.status ?? 1);
