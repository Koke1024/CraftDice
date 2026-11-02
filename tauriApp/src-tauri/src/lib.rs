use std::{fs, io::Write, path::PathBuf};
use tauri::Manager;

fn save_path(app: &tauri::AppHandle) -> Result<PathBuf, String> {
    app.path()
        .app_data_dir()
        .map(|p| p.join("progress.json"))
        .map_err(|e| e.to_string())
}

#[tauri::command]
fn load_progress(app: tauri::AppHandle) -> Result<String, String> {
    match fs::read_to_string(save_path(&app)?) {
        Ok(data) => Ok(data),
        Err(e) if e.kind() == std::io::ErrorKind::NotFound => Ok(String::new()),
        Err(e) => Err(format!("セーブの読み込みに失敗しました: {e}")),
    }
}

fn write_progress(path: &std::path::Path, data: &str) -> Result<(), String> {
    if data.len() > 1024 * 1024 {
        return Err("セーブが大きすぎます".into());
    }
    serde_json::from_str::<serde_json::Value>(data).map_err(|e| e.to_string())?;
    let parent = path.parent().ok_or("保存先が不正です")?;
    fs::create_dir_all(parent).map_err(|e| e.to_string())?;
    // Same-directory atomic replacement preserves the last valid save on failure.
    let mut file = tempfile::NamedTempFile::new_in(parent).map_err(|e| e.to_string())?;
    file.write_all(data.as_bytes()).map_err(|e| e.to_string())?;
    file.as_file().sync_all().map_err(|e| e.to_string())?;
    file.persist(path).map_err(|e| e.to_string())?;
    Ok(())
}

#[tauri::command]
fn save_progress(app: tauri::AppHandle, data: String) -> Result<(), String> {
    write_progress(&save_path(&app)?, &data)
}

pub fn run() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![load_progress, save_progress])
        .run(tauri::generate_context!())
        .expect("CraftDice could not start");
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn save_replaces_existing_file_and_rejects_invalid_json() {
        let dir = tempfile::tempdir().unwrap();
        let path = dir.path().join("nested/progress.json");
        write_progress(&path, r#"{"version":1,"balance":2}"#).unwrap();
        write_progress(&path, r#"{"version":1,"balance":3}"#).unwrap();
        assert!(write_progress(&path, "broken").is_err());
        assert_eq!(
            fs::read_to_string(path).unwrap(),
            r#"{"version":1,"balance":3}"#
        );
    }
}
