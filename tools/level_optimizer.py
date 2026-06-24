import os
import json
import shutil
import tkinter as tk
from tkinter import filedialog

# Default values for comparison
LEVEL_DEFAULTS = {
    "name": "Unnamed Level",
    "bgColor": "1a1a2e",
    "groundColor": "16213e",
    "difficulty": "normal",
    "musicFile": "",
    "bgImage": "",
    "youtubeLink": ""
}

OBJECT_DEFAULTS = {
    "size": 100.0,
    "blockType": "",
    "rotation": 0.0,
    "flipped": False,
    "triggerBgColor": "",
    "triggerGroundColor": "",
    "fadeDuration": 1.0,
    "pulseBgColor": "",
    "pulseGroundColor": "",
    "fadeInTime": 0.1,
    "holdTime": 0.2,
    "fadeOutTime": 0.5
}

def optimize_object(obj):
    # Mandatory fields
    optimized = {
        "type": obj.get("type", ""),
        "x": obj.get("x", 0.0),
        "y": obj.get("y", 0.0)
    }

    # Optional fields - only include if they differ from default
    for key, default_val in OBJECT_DEFAULTS.items():
        if key in obj:
            val = obj[key]
            # Handle float comparison
            if isinstance(default_val, float):
                if abs(val - default_val) > 0.0001:
                    optimized[key] = val
            elif val != default_val:
                optimized[key] = val

    return optimized

def is_duplicate(obj1, obj2):
    return obj1 == obj2

def optimize_level(level_path, backup_path):
    print(f"Optimizing: {os.path.basename(level_path)}")

    with open(level_path, 'r', encoding='utf-8') as f:
        try:
            data = json.load(f)
        except json.JSONDecodeError:
            print(f"Error: Could not decode {level_path}")
            return

    # Backup if not already backed up
    backup_file = os.path.join(backup_path, os.path.basename(level_path))
    if not os.path.exists(backup_file):
        shutil.copy2(level_path, backup_file)

    # Optimize Level Metadata
    optimized_data = {}
    for key, default_val in LEVEL_DEFAULTS.items():
        if key in data:
            val = data[key]
            if val != default_val:
                optimized_data[key] = val

    # Optimize Objects
    raw_objects = data.get("objects", [])
    optimized_objects = []
    seen_objects = []

    for obj in raw_objects:
        opt_obj = optimize_object(obj)

        is_dup = False
        for seen in seen_objects:
            if is_duplicate(opt_obj, seen):
                is_dup = True
                break

        if not is_dup:
            optimized_objects.append(opt_obj)
            seen_objects.append(opt_obj)

    optimized_data["objects"] = optimized_objects

    # Save back as optimized JSON
    with open(level_path, 'w', encoding='utf-8') as f:
        json.dump(optimized_data, f, indent=2)

def main():
    root = tk.Tk()
    root.withdraw()

    print("Please select the levels directory...")
    levels_dir = filedialog.askdirectory(title="Select Levels Directory", initialdir=os.getcwd())

    if not levels_dir:
        print("No directory selected. Exiting.")
        return

    backup_dir = os.path.join(levels_dir, "backups_original")
    if not os.path.exists(backup_dir):
        os.makedirs(backup_dir)
        print(f"Created backup directory at: {backup_dir}")

    for filename in os.listdir(levels_dir):
        if filename.endswith(".json"):
            level_path = os.path.join(levels_dir, filename)
            optimize_level(level_path, backup_dir)

    print("\nOptimization and Migration to minimalist JSON complete!")
    print(f"Note: Levels are saved as .json but the game is now ready to load binary .ubj versions if you choose to convert them.")
    print(f"Original files were backed up to: {backup_dir}")

if __name__ == "__main__":
    main()
