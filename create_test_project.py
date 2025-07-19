#!/usr/bin/env python3
"""
Script to create test project structure for Collapse Files Plugin integration tests.
Based on the test plan requirements.
"""

import os
import shutil
from pathlib import Path

def create_test_project():
    """Create the test project structure."""
    
    # Base directory for the test project
    test_project_dir = Path("src/integrationTest/testData/test-project")
    
    # Remove existing test project if it exists
    if test_project_dir.exists():
        shutil.rmtree(test_project_dir)
    
    # Create base directory
    test_project_dir.mkdir(parents=True, exist_ok=True)
    
    print(f"Creating test project structure in: {test_project_dir}")
    
    # 1. Create many-folders/ directory with 15+ unused folders
    many_folders_dir = test_project_dir / "many-folders"
    many_folders_dir.mkdir(exist_ok=True)
    
    for i in range(1, 16):  # folder01 to folder15
        folder_name = f"folder{i:02d}"
        folder_path = many_folders_dir / folder_name
        folder_path.mkdir(exist_ok=True)
        # Add a dummy file to make folders non-empty
        (folder_path / "dummy.txt").write_text(f"Content of {folder_name}")
    
    print(f"Created {many_folders_dir} with 15 folders")
    
    # 2. Create many-files/ directory with 15+ unused files
    many_files_dir = test_project_dir / "many-files"
    many_files_dir.mkdir(exist_ok=True)
    
    for i in range(1, 16):  # file01.txt to file15.txt
        file_name = f"file{i:02d}.txt"
        file_path = many_files_dir / file_name
        file_path.write_text(f"Content of {file_name}")
    
    print(f"Created {many_files_dir} with 15 files")
    
    # 3. Create mixed-scenario/ directory with mixed files and folders
    mixed_dir = test_project_dir / "mixed-scenario"
    mixed_dir.mkdir(exist_ok=True)
    
    # Create 10 folders
    for i in range(1, 11):
        folder_name = f"folder{i:02d}"
        folder_path = mixed_dir / folder_name
        folder_path.mkdir(exist_ok=True)
        (folder_path / "dummy.txt").write_text(f"Content of {folder_name}")
    
    # Create 10 files
    for i in range(1, 11):
        file_name = f"file{i:02d}.txt"
        file_path = mixed_dir / file_name
        file_path.write_text(f"Content of {file_name}")
    
    print(f"Created {mixed_dir} with 10 folders and 10 files")
    
    # 4. Create below-threshold/ directory with 9 items (below default threshold)
    below_threshold_dir = test_project_dir / "below-threshold"
    below_threshold_dir.mkdir(exist_ok=True)
    
    for i in range(1, 10):  # item01 to item09 (9 items total)
        item_name = f"item{i:02d}"
        item_path = below_threshold_dir / item_name
        item_path.mkdir(exist_ok=True)
        (item_path / "dummy.txt").write_text(f"Content of {item_name}")
    
    print(f"Created {below_threshold_dir} with 9 items")
    
    # 6. Create nested-structure/ directory for complex nesting tests
    nested_dir = test_project_dir / "nested-structure"
    nested_dir.mkdir(exist_ok=True)
    
    level1_dir = nested_dir / "level1"
    level1_dir.mkdir(exist_ok=True)
    
    level2_dir = level1_dir / "level2"
    level2_dir.mkdir(exist_ok=True)
    
    (level2_dir / "deep-file.txt").write_text("This is a deeply nested file")
    
    print(f"Created {nested_dir} with nested structure")

    # 7. Create new-file-scenario
    new_file_scenario_dir = test_project_dir / "new-file-scenario"
    new_file_scenario_dir.mkdir(exist_ok=True)
    for i in range(11):
        file_path = new_file_scenario_dir / f"

    print(f"Total directories created: {sum(1 for _ in test_project_dir.rglob('*') if _.is_dir())}")
    print(f"Total files created: {sum(1 for _ in test_project_dir.rglob('*') if _.is_file())}")

if __name__ == "__main__":
    create_test_project()

