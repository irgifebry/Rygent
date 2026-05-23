# -*- mode: python ; coding: utf-8 -*-

block_cipher = None

# List of data files to include
added_files = [
    ('static', 'static'),
    ('templates', 'templates'),
    ('icon.png', '.'),
]

a = Analysis(
    ['agent_gui.py'],
    pathex=[],
    binaries=[],
    datas=added_files,
    hiddenimports=[
        'flask',
        'flask_cors',
        'psutil',
        'dotenv',
        'PIL',
        'qrcode',
        'pystray',
        'jinja2.ext', # Often needed for Flask in PyInstaller
    ],
    hookspath=[],
    hooksconfig={},
    runtime_hooks=[],
    excludes=[],
    win_no_prefer_redirects=False,
    win_private_assemblies=False,
    cipher=block_cipher,
    noarchive=False,
)
pyz = PYZ(a.pyz, a.cipher, b64_cipher=block_cipher)

exe = EXE(
    pyz,
    a.scripts,
    a.binaries,
    a.zipfiles,
    a.datas,
    [],
    name='RygentAgent',
    debug=False,
    bootloader_ignore_signals=False,
    strip=False,
    upx=True,
    upx_exclude=[],
    runtime_tmpdir=None,
    console=False, # Set to False to hide the terminal window
    disable_windowed_traceback=False,
    argv_emulation=False,
    target_arch=None,
    codesign_identity=None,
    entitlements_file=None,
    icon=['icon.png'],
)
