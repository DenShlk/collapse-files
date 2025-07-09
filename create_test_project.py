#!/usr/bin/env python3
"""
Script to create comprehensive test project structure for Collapse Files Plugin integration tests.
Based on the test plan requirements.
"""

import os
import shutil
from pathlib import Path

def create_test_project():
    """Create the comprehensive test project structure."""
    
    # Base directory for the test project
    test_project_dir = Path("src/integrationTest/testData/comprehensive-test-project")
    
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
    
    # 5. Create open-file-scenarios/ directory with files that will be opened during tests
    open_file_dir = test_project_dir / "open-file-scenarios"
    open_file_dir.mkdir(exist_ok=True)
    
    (open_file_dir / "will-open1.txt").write_text("This file will be opened in Test 5")
    (open_file_dir / "will-open2.txt").write_text("This file will also be opened in Test 5")
    
    print(f"Created {open_file_dir} with files for open file tests")
    
    # 6. Create nested-structure/ directory for complex nesting tests
    nested_dir = test_project_dir / "nested-structure"
    nested_dir.mkdir(exist_ok=True)
    
    level1_dir = nested_dir / "level1"
    level1_dir.mkdir(exist_ok=True)
    
    level2_dir = level1_dir / "level2"
    level2_dir.mkdir(exist_ok=True)
    
    (level2_dir / "deep-file.txt").write_text("This is a deeply nested file")
    
    print(f"Created {nested_dir} with nested structure")
    
    # 7. Create a simple build.gradle.kts file to make it a valid Gradle project
    build_gradle = test_project_dir / "build.gradle.kts"
    build_gradle.write_text('''
plugins {
    kotlin("jvm") version "1.9.0"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}
''')
    
    # 8. Create settings.gradle.kts
    settings_gradle = test_project_dir / "settings.gradle.kts"
    settings_gradle.write_text('rootProject.name = "comprehensive-test-project"')
    
    print(f"Created Gradle configuration files")
    
    # 9. Create a README.md for the test project
    readme = test_project_dir / "README.md"
    readme.write_text('''# Comprehensive Test Project

This project is used for integration testing of the Collapse Files Plugin.

## Structure

- `many-folders/` - 15 folders for testing folder collapsing
- `many-files/` - 15 files for testing file collapsing  
- `mixed-scenario/` - Mixed files and folders
- `below-threshold/` - 9 items (below default threshold)
- `open-file-scenarios/` - Files that will be opened during tests
- `nested-structure/` - Complex nesting for path preservation tests

This structure is designed to test all scenarios outlined in the integration test plan.
''')
    
    print(f"Test project structure created successfully!")
    print(f"Total directories created: {sum(1 for _ in test_project_dir.rglob('*') if _.is_dir())}")
    print(f"Total files created: {sum(1 for _ in test_project_dir.rglob('*') if _.is_file())}")

if __name__ == "__main__":
    create_test_project()

