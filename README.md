# Rygent Agent - Windows Platform

Rygent Agent is a high-performance system monitoring and secure tunnel management agent designed specifically for Windows platforms. It runs locally to gather system metrics, handle authentication, and orchestrate secure data tunnels back to your central management dashboard.

---

## Key Features

- **Graphical User Interface:** Built-in Tkinter-based control panel (agent_gui.py) for easy monitoring status checks and settings configuration.
- **System Metrics Engine:** Continuous lightweight gathering of CPU, Memory, Disk, and Network performance statistics on Windows.
- **Secure Tunneling:** Robust integration with remote secure tunnels (tunnel_manager.py) to map local ports to the web panel securely.
- **Standalone Compilation:** Includes pre-configured PyInstaller specs and batch build scripts (build_exe.bat) to compile the agent into a single portable Windows executable (.exe).
- **Local Dashboard:** Lightweight embedded web interface with customizable CSS/JS assets (static/templates) for local system health diagnosis.

---

## Installation & Setup

### Prerequisites

- Windows 10 or 11
- Python 3.10 or higher
- Administrative privileges (required for certain system-level metric API calls)

### Installation Steps

1. Install Python from the official website and ensure it is added to your system PATH.
2. Clone the repository and checkout the Windows branch:
   ```text
   git clone https://github.com/irgifebry/Rygent.git
   cd Rygent/Windows
   ```
3. Install required Python packages:
   ```text
   pip install -r requirements.txt
   ```

### Running the Agent

You can launch the agent in one of two ways:

- **Via Python:**
  ```text
  python main.py
  ```
- **Via Batch Script:** Double-click the `run_agent.bat` file in your explorer window.

---

## Compiling to Standalone Executable (.exe)

To bundle the agent and its assets into a single binary for deployment without needing Python installed on the target machine:

1. Open Command Prompt as Administrator.
2. Navigate to the Windows directory.
3. Run the compiler script:
   ```text
   build_exe.bat
   ```
4. The compiled executable will be located in the newly created `dist/` directory.
