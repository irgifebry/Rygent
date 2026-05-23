import os
import sys
import tkinter as tk
from tkinter import messagebox, font
import threading
import socket
import io
import time
import json
import platform
import subprocess
import winreg

import ctypes

# Premium Polish: High-DPI Awareness (Windows 8+)
try:
    ctypes.windll.shcore.SetProcessDpiAwareness(1)
except:
    try:
        ctypes.windll.user32.SetProcessDPIAware()
    except:
        pass

# Premium Polish: Single Instance Lock
def get_instance_lock():
    try:
        # Use a unique port for the agent lock
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind(('127.0.0.1', 47123))
        return s
    except socket.error:
        return None

_instance_lock = get_instance_lock()
if not _instance_lock:
    # Silent exit if already running
    sys.exit(0)

# Ensure we can import main and monitor
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from main import run_server
from monitor import monitor
from PIL import Image, ImageDraw

def create_image():
    try:
        if getattr(sys, 'frozen', False):
            base_path = sys._MEIPASS
        else:
            base_path = os.path.dirname(os.path.abspath(__file__))
        icon_path = os.path.join(base_path, "icon.png")
        if os.path.exists(icon_path):
            img = Image.open(icon_path).resize((64, 64))
            return img.convert('RGBA')
    except Exception as e:
        print(f"[Icon] Error: {e}")
    
    # Fallback premium icon
    image = Image.new('RGBA', (64, 64), (15, 18, 25, 255))
    dc = ImageDraw.Draw(image)
    dc.ellipse((8, 8, 56, 56), outline=(59, 130, 246), width=4)
    dc.ellipse((20, 20, 44, 44), fill=(59, 130, 246))
    return image

def pil_to_tkphoto(pil_image, root):
    buf = io.BytesIO()
    rgb_image = pil_image.convert('RGB')
    rgb_image.save(buf, format='PPM')
    ppm_data = buf.getvalue()
    buf.close()
    return tk.PhotoImage(data=ppm_data)

class StyledButton(tk.Canvas):
    def __init__(self, parent, text, command, color="#3B82F6", width=200, height=45, font_size=10):
        super().__init__(parent, width=width, height=height, bg=parent['bg'], highlightthickness=0, cursor="hand2")
        self.command = command
        self.color = color
        self.r = 20
        self.draw_rect(0, 0, width, height, self.r, fill="#1E222D")
        self.btn_rect = self.draw_rect(2, 2, width-2, height-2, self.r, fill=color)
        font_family = getattr(parent.master, 'font_family', "Segoe UI") if hasattr(parent, 'master') else "Segoe UI"
        self.btn_text = self.create_text(width/2, height/2, text=text, fill="white", font=(font_family, font_size, "bold"))
        self.bind("<Button-1>", lambda e: self.command())
        self.bind("<Enter>", self.on_enter)
        self.bind("<Leave>", self.on_leave)

    def draw_rect(self, x1, y1, x2, y2, r, **kwargs):
        points = [x1+r, y1, x1+r, y1, x2-r, y1, x2-r, y1, x2, y1, x2, y1+r, x2, y1+r, x2, y2-r, x2, y2-r, x2, y2, x2-r, y2, x2-r, y2, x1+r, y2, x1+r, y2, x1, y2, x1, y2-r, x1, y2-r, x1, y1+r, x1, y1+r, x1, y1]
        return self.create_polygon(points, smooth=True, **kwargs)

    def on_enter(self, e):
        self.itemconfig(self.btn_rect, fill="#4F46E5" if self.color == "#3B82F6" else "#F87171")

    def on_leave(self, e):
        self.itemconfig(self.btn_rect, fill=self.color)

class AgentGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("Rygent Agent (Windows)")
        self.root.geometry("400x650")
        self.root.configure(bg="#000000")
        self.root.resizable(False, False)
        
        self.bg_color = "#000000"
        self.surface_color = "#161616"
        self.accent_color = "#3B82F6"
        self.text_white = "#FAFAFA"
        self.text_gray = "#94A3B8"
        self.error_red = "#EF4444"
        self.success_green = "#10B981"
        
        self.skeleton_index = 0
        self.is_loaded = False
        self.tunnel_url = None
        
        self.setup_fonts()
        self.center_window()
        self.draw_ui()
        self.animate_skeleton()
        self.update_stats()
        self.check_tunnel_status()

    def setup_fonts(self):
        try:
            fonts_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "static", "fonts")
            if os.path.exists(fonts_dir):
                # font loading on Windows is harder but let's assume standard UI font for now
                pass
        except: pass
        self.font_family = "Segoe UI" if "segoe ui" in [f.lower() for f in font.families()] else "Arial"

    def center_window(self):
        self.root.update_idletasks()
        w, h = 400, 650
        x = (self.root.winfo_screenwidth() // 2) - (w // 2)
        y = (self.root.winfo_screenheight() // 2) - (h // 2)
        self.root.geometry(f'{w}x{h}+{x}+{y}')

    def generate_qr(self):
        import qrcode
        from config import Config
        hostname = socket.gethostname()
        pairing_data = None
        token = Config.AUTH_TOKEN.strip()
        if self.tunnel_url:
            clean_url = self.tunnel_url.replace("https://", "").replace("http://", "").split("/")[0].split(":")[0]
            pairing_data = f"rygent://{clean_url}?token={token}&name={hostname}&os=windows&ssl=1&port=443"
        if pairing_data:
            qr = qrcode.QRCode(version=1, box_size=4, border=2)
            qr.add_data(pairing_data)
            qr.make(fit=True)
            img = qr.make_image(fill_color="white", back_color=self.surface_color)
            img = img.get_image().resize((180, 180))
            return pil_to_tkphoto(img, self.root)
        else:
            img = Image.new('RGB', (180, 180), self.surface_color)
            dc = ImageDraw.Draw(img)
            try: dc.text((25, 80), "WAITING FOR\nREMOTE TUNNEL...", fill=self.accent_color, align="center")
            except: pass
            return pil_to_tkphoto(img, self.root)

    def draw_ui(self):
        header = tk.Frame(self.root, bg=self.bg_color, pady=15)
        header.pack(fill='x')
        tk.Label(header, text="RYGENT", font=(self.font_family, 24, "bold"), fg=self.accent_color, bg=self.bg_color).pack()
        qr_card = tk.Frame(self.root, bg=self.surface_color, padx=20, pady=20, highlightthickness=1, highlightbackground="#2D333F")
        qr_card.pack(fill='x', padx=40, pady=5)
        tk.Label(qr_card, text="SCAN TO PAIR (WINDOWS)", font=(self.font_family, 9, "bold"), fg=self.accent_color, bg=self.surface_color).pack(pady=(0, 10))
        self.qr_label = tk.Label(qr_card, bg=self.surface_color)
        self.qr_label.pack()
        self.refresh_qr()
        metrics_card = tk.Frame(self.root, bg=self.bg_color, padx=40, pady=5)
        metrics_card.pack(fill='x')
        self.cpu_val = self.create_metric(metrics_card, "System Load", "0%", self.accent_color)
        self.ram_val = self.create_metric(metrics_card, "Memory Usage", "0%", self.success_green)
        self.remote_val = tk.Label(metrics_card, text="Remote: Connecting...", font=(self.font_family, 8, "bold"), fg=self.accent_color, bg=self.bg_color)
        self.remote_val.pack(pady=(5, 5))
        ctrl_frame = tk.Frame(self.root, bg=self.bg_color, pady=10)
        ctrl_frame.pack(fill='x')
        
        self.startup_var = tk.BooleanVar(value=self.check_startup_status())
        startup_cb = tk.Checkbutton(ctrl_frame, text="RUN AT STARTUP", variable=self.startup_var, 
                                    command=self.toggle_startup, font=(self.font_family, 8, "bold"),
                                    fg=self.text_gray, bg=self.bg_color, activebackground=self.bg_color,
                                    selectcolor=self.surface_color, activeforeground=self.accent_color)
        startup_cb.pack(pady=5)

        StyledButton(ctrl_frame, "MINIMIZE TO TRAY", self.minimize_to_tray, self.accent_color, width=280).pack(pady=5)
        StyledButton(ctrl_frame, "SHUTDOWN AGENT", self.exit_app, self.error_red, width=280).pack(pady=5)
        tk.Label(self.root, text="Agent Version 1.1.0 (Win)", font=(self.font_family, 7), fg="#2D333F", bg=self.bg_color).pack(side=tk.BOTTOM, pady=10)

    def create_metric(self, parent, label, value, color):
        row = tk.Frame(parent, bg=self.bg_color, pady=4)
        row.pack(fill='x')
        tk.Label(row, text=label, font=(self.font_family, 10), fg=self.text_gray, bg=self.bg_color).pack(side=tk.LEFT)
        val_lbl = tk.Label(row, text=value, font=(self.font_family, 11, "bold"), fg=color, bg=self.bg_color)
        val_lbl.pack(side=tk.RIGHT)
        return val_lbl

    def animate_skeleton(self):
        if hasattr(self, 'is_loaded') and self.is_loaded: return
        frames = ["⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"]
        frame = frames[self.skeleton_index]
        if hasattr(self, 'cpu_val') and hasattr(self, 'ram_val'):
            self.cpu_val.config(text=f"{frame} Loading")
            self.ram_val.config(text=f"{frame} Loading")
        self.skeleton_index = (self.skeleton_index + 1) % len(frames)
        self.root.after(100, self.animate_skeleton)

    def update_stats(self):
        try:
            data = monitor.get_system_info()
            self.is_loaded = True
            self.cpu_val.config(text=f"{data['cpu']['usage']}%")
            self.ram_val.config(text=f"{data['memory']['percent']}%")
            if monitor.is_admin() and not hasattr(self, 'admin_badge'):
                self.admin_badge = tk.Label(self.root, text="ADMIN PRIVILEGES", font=(self.font_family, 6, "bold"), fg=self.success_green, bg=self.bg_color)
                self.admin_badge.place(x=10, y=10)
        except: pass
        self.root.after(1000, self.update_stats)

    def toggle_startup(self):
        try:
            key_path = r"Software\Microsoft\Windows\CurrentVersion\Run"
            app_name = "RygentAgent"
            if getattr(sys, 'frozen', False):
                cmd = f'"{sys.executable}"'
            else:
                cmd = f'"{sys.executable}" "{os.path.abspath(sys.argv[0])}"'
            
            with winreg.OpenKey(winreg.HKEY_CURRENT_USER, key_path, 0, winreg.KEY_ALL_ACCESS) as key:
                if self.startup_var.get():
                    winreg.SetValueEx(key, app_name, 0, winreg.REG_SZ, cmd)
                else:
                    try: winreg.DeleteValue(key, app_name)
                    except: pass
        except Exception as e:
            print("Startup toggle failed:", e)

    def check_startup_status(self):
        try:
            key_path = r"Software\Microsoft\Windows\CurrentVersion\Run"
            with winreg.OpenKey(winreg.HKEY_CURRENT_USER, key_path, 0, winreg.KEY_READ) as key:
                winreg.QueryValueEx(key, "RygentAgent")
                return True
        except: return False

    def check_tunnel_status(self):
        self.tunnel_url = None
        try:
            from tunnel_manager import TUNNEL_JSON
            if os.path.exists(TUNNEL_JSON):
                with open(TUNNEL_JSON, "r") as f:
                    self.tunnel_url = json.load(f).get("url")
                    if self.tunnel_url:
                        short_url = self.tunnel_url.replace("https://", "")
                        self.remote_val.config(text=f"Remote: {short_url}", fg=self.success_green)
                    else: self.remote_val.config(text="Remote: Starting...", fg=self.accent_color)
            else: self.remote_val.config(text="Remote: Disabled", fg=self.text_gray)
        except: pass
        self.root.after(5000, self.check_tunnel_status)

    def refresh_qr(self):
        try:
            from tunnel_manager import TUNNEL_JSON
            if os.path.exists(TUNNEL_JSON):
                with open(TUNNEL_JSON, "r") as f:
                    self.tunnel_url = json.load(f).get("url")
        except: pass
        
        try:
            self.qr_img = self.generate_qr()
            self.qr_label.config(image=self.qr_img)
        except: pass
        self.root.after(10000, self.refresh_qr)

    def minimize_to_tray(self):
        try:
            import pystray
            self.root.withdraw()
            menu = (pystray.MenuItem('Show Rygent Agent', self.show_window), pystray.MenuItem('Exit Agent', lambda i, v: os._exit(0)))
            self.tray_icon = pystray.Icon("rygent", create_image(), "Rygent Agent", menu)
            threading.Thread(target=self.tray_icon.run, daemon=True).start()
        except: self.root.iconify()

    def show_window(self, icon, item):
        icon.stop()
        self.root.after(0, self.root.deiconify)

    def exit_app(self):
        if messagebox.askokcancel("Rygent Agent", "Shutdown Agent completely?"):
            os._exit(0)

    def on_close(self):
        self.minimize_to_tray()

if __name__ == "__main__":
    root = tk.Tk(className="agent_gui")
    app = AgentGUI(root)
    root.protocol("WM_DELETE_WINDOW", app.on_close)
    threading.Thread(target=run_server, daemon=True).start()
    root.mainloop()
