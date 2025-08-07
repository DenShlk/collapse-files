# Collapse Files Plugin

## Functionality

Plugin will collapse sequences of unused (not opened / focused) folders and files in project view into single entries.

This should help people with big projects to avoid a lot of scrolling.

Example folder structure:

```text
scripts/
├── opened1/
│   ├── a.txt
│   └── b.js
├── closed1/
├── closed2/
├── … 
├── closed30/
├── opened2/
│   ├── c.txt
│   └── d.json
├── unused1.txt
├── unused2.js
├── …
└── unused15.py
```

Normally you would need to scroll through closed folders and unused files to get from a.txt to c.txt.

With this plugin you should see something like:

```text
scripts/
├── opened1/
│   ├── a.txt
│   └── b.js
├── <30 collapsed folders>
├── opened2/
│   ├── c.txt
│   └── d.json
└── <15 collapsed files>
```
This allows you to navigate the project a lot quicker.
You can still view all the folders and files by clicking on their respective collapsed entries.

## Settings
Open Settings | Tools | Collapse Files to configure

## Todo
- [x] Logo
- [x] Settings
  - [x] Thresholds for files and folders
  - [x] Label style: compact vs full
