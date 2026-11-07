import os
import json
from datetime import datetime

BASE_URL = "https://raw.githubusercontent.com/Hance-PDv/Music-Flow/main/Audio/"
AUDIO_DIR = "Audio"

songs = []

if os.path.exists(AUDIO_DIR):
    for i, filename in enumerate(sorted(os.listdir(AUDIO_DIR))):
        if filename.lower().endswith(".mp3"):
            clean_name = filename.replace("(MP3_320K)", "").replace(".mp3", "").strip()
            
            if " - " in clean_name:
                parts = clean_name.split(" - ", 1)
                artist = parts[0].strip()
                title = parts[1].strip()
            else:
                artist = "Unknown Artist"
                title = clean_name

            song_entry = {
                "id": f"song_{i+1:03d}",
                "title": title,
                "artist": artist,
                "audio": BASE_URL + filename
            }
            songs.append(song_entry)

output_data = {
    "version": 1,
    "updated_at": datetime.now().strftime("%Y-%m-%d"),
    "songs": songs
}

with open("songs.json", "w", encoding="utf-8") as f:
    json.dump(output_data, f, indent=2, ensure_ascii=False)

print(f"Successfully generated songs.json with {len(songs)} songs!")
