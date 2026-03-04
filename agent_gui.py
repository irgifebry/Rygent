import os
import sys

# Force X11 for Tkinter and print debug info
if os.path.exists('/.flatpak-info'):
    if not os.environ.get('DISPLAY'):
        os.environ['DISPLAY'] = ':0'
    # Unset Wayland which can confuse Tcl/Tk initialization in sandboxes
    os.environ.pop('WAYLAND_DISPLAY', None)
    print(f"[DEBUG] Flatpak startup - DISPLAY: {os.environ.get('DISPLAY')}, PATH: {os.environ.get('PATH')}")

import tkinter as tk
from tkinter import messagebox, font
import threading
import socket
import io
import time
import json

# Ensure we can import main and monitor
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from main import run_server
from monitor import monitor
from PIL import Image, ImageDraw

def create_image():
    try:
        icon_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "icon.png")
        if os.path.exists(icon_path):
            img = Image.open(icon_path).resize((64, 64))
            # Ensure the image is valid and has an alpha channel if needed
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
        
        # Border/Shadow effect
        self.draw_rect(0, 0, width, height, self.r, fill="#1E222D") # Shadow/Margin
        self.btn_rect = self.draw_rect(2, 2, width-2, height-2, self.r, fill=color)
        
        # Use parent's font family if available
        font_family = getattr(parent.master, 'font_family', "Sans") if hasattr(parent, 'master') else "Sans"
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
        self.root.title("Rygent Agent")
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
        self.use_public_qr = False
        self.tunnel_url = None
        
        # Improve font rendering
        self.setup_fonts()
        self.center_window()
        self.draw_ui()
        self.animate_skeleton()
        self.update_stats()
        self.check_tunnel_status()

    def setup_fonts(self):
        # Set scaling for high-dpi displays (crucial for Zorin/4K)
        try:
            self.root.tk.call('tk', 'scaling', 1.5)
        except:
            pass

        # Force load Inter font from bundled files
        fonts_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "static", "fonts")
        inter_loaded = False
        
        if os.path.exists(fonts_dir):
            try:
                # Load Inter Bold font file directly
                inter_bold_path = os.path.join(fonts_dir, "Inter-Bold.ttf")
                inter_regular_path = os.path.join(fonts_dir, "Inter-Regular.ttf")
                
                if os.path.exists(inter_bold_path):
                    # Load font into Tkinter
                    from tkinter import font as tkfont
                    tkfont.Font(family="Inter", size=10, weight="bold")
                    # Try to load the actual font file (Linux)
                    try:
                        import subprocess
                        subprocess.run(['fc-cache', '-f', fonts_dir], capture_output=True)
                        inter_loaded = True
                        print(f"[UI] Loaded Inter fonts from {fonts_dir}")
                    except:
                        pass
                
                if os.path.exists(inter_regular_path) and not inter_loaded:
                    inter_loaded = True
            except Exception as e:
                print(f"[UI] Error loading Inter fonts: {e}")

        # Robust font discovery
        available = [f.lower() for f in font.families()]

        # Priority list - Inter first
        if inter_loaded:
            self.font_family = "Inter"
        elif "inter" in available:
            self.font_family = "Inter"
        elif "dejavu sans" in available:
            self.font_family = "DejaVu Sans"
        else:
            self.font_family = "Sans" # Standard alias on Linux

        print(f"[UI] Using font family: {self.font_family}")

    def center_window(self):
        self.root.update_idletasks()
        w, h = 400, 650
        x = (self.root.winfo_screenwidth() // 2) - (w // 2)
        y = (self.root.winfo_screenheight() // 2) - (h // 2)
        self.root.geometry(f'{w}x{h}+{x}+{y}')

    def generate_qr(self):
        import qrcode
        from config import Config
        from PIL import Image, ImageDraw

        hostname = socket.gethostname()
        pairing_data = None
        token = Config.AUTH_TOKEN.strip()

        # Construction logic optimized for Android "Online" detection
        if self.tunnel_url:
            # e.g. https://xyz.trycloudflare.com or https://custom.domain.com
            clean_url = self.tunnel_url.replace("https://", "").replace("http://", "").split("/")[0].split(":")[0]
            
            # We always use HTTPS for tunnels
            if "trycloudflare" in self.tunnel_url:
                # Explicitly signal SSL and port for quick tunnels
                pairing_data = f"rygent://{clean_url}?token={token}&name={hostname}&ssl=1&port=443"
            else:
                # For custom named tunnels, we assume standard HTTPS (443) unless specified
                pairing_data = f"rygent://{clean_url}?token={token}&name={hostname}&ssl=1"
            
            print(f"[QR] Remote Tunnel ready: {pairing_data}")

        if pairing_data:
            qr = qrcode.QRCode(version=1, box_size=4, border=2)
            qr.add_data(pairing_data)
            qr.make(fit=True)
            img = qr.make_image(fill_color="white", back_color=self.surface_color)
            img = img.get_image().resize((180, 180))
            return pil_to_tkphoto(img, self.root)
        else:
            # Show placeholder if tunnel is not ready
            img = Image.new('RGB', (180, 180), self.surface_color)
            dc = ImageDraw.Draw(img)
            try:
                # Fallback to simple text if font fails
                dc.text((25, 80), "WAITING FOR\nREMOTE TUNNEL...", fill=self.accent_color, align="center")
            except:
                pass
            return pil_to_tkphoto(img, self.root)

    def draw_ui(self):
        # Header
        header = tk.Frame(self.root, bg=self.bg_color, pady=15)
        header.pack(fill='x')
        
        tk.Label(header, text="RYGENT", font=(self.font_family, 24, "bold"), fg=self.accent_color, bg=self.bg_color).pack()

        # QR Zone
        qr_card = tk.Frame(self.root, bg=self.surface_color, padx=20, pady=20, highlightthickness=1, highlightbackground="#2D333F")
        qr_card.pack(fill='x', padx=40, pady=5)
        
        tk.Label(qr_card, text="SCAN TO PAIR", font=(self.font_family, 9, "bold"), fg=self.accent_color, bg=self.surface_color).pack(pady=(0, 10))
        self.qr_label = tk.Label(qr_card, bg=self.surface_color)
        self.qr_label.pack()
        self.refresh_qr()

        # Metrics Card
        metrics_card = tk.Frame(self.root, bg=self.bg_color, padx=40, pady=5)
        metrics_card.pack(fill='x')

        self.cpu_val = self.create_metric(metrics_card, "System Load", "0%", self.accent_color)
        self.ram_val = self.create_metric(metrics_card, "Memory Usage", "0%", self.success_green)
        
        # Local IP hidden, showing only remote status
        self.remote_val = tk.Label(metrics_card, text="Remote: Connecting...", font=(self.font_family, 8, "bold"), fg=self.accent_color, bg=self.bg_color)
        self.remote_val.pack(pady=(5, 5))

        # Control Zone
        ctrl_frame = tk.Frame(self.root, bg=self.bg_color, pady=10)
        ctrl_frame.pack(fill='x')

        StyledButton(ctrl_frame, "MINIMIZE TO TRAY", self.minimize_to_tray, self.accent_color, width=280).pack(pady=5)
        StyledButton(ctrl_frame, "SHUTDOWN AGENT", self.exit_app, self.error_red, width=280).pack(pady=5)

        tk.Label(self.root, text="Agent Version 1.0.0", font=(self.font_family, 7), fg="#2D333F", bg=self.bg_color).pack(side=tk.BOTTOM, pady=10)

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
            info = monitor.get_system_info()
            self.is_loaded = True
            self.cpu_val.config(text=f"{info['cpu']['usage']}%")
            self.ram_val.config(text=f"{info['memory']['percent']}%")
        except: pass
        self.root.after(2000, self.update_stats)

    def check_tunnel_status(self):
        # Check tunnel status
        self.tunnel_url = None
        try:
            import json
            persistent_path = os.path.expanduser("~/.rygent/tunnel.json")
            if os.path.exists(persistent_path):
                with open(persistent_path, "r") as f:
                    data = json.load(f)
                    self.tunnel_url = data.get("url")
                    if self.tunnel_url:
                        short_url = self.tunnel_url.replace("https://", "")
                        self.remote_val.config(text=f"Remote: {short_url}", fg=self.success_green)
                    else:
                        self.remote_val.config(text="Remote: Starting...", fg=self.accent_color)
            else:
                self.remote_val.config(text="Remote: Disabled", fg=self.text_gray)
        except: pass
        self.root.after(5000, self.check_tunnel_status)

    def refresh_qr(self):
        # Try to read tunnel URL if available
        persistent_path = os.path.expanduser("~/.rygent/tunnel.json")
        if os.path.exists(persistent_path):
            try:
                with open(persistent_path, "r") as f:
                    self.tunnel_url = json.load(f).get("url")
            except:
                pass

        # Strictly generate QR code from tunnel or show WAITING placeholder
        try:
            self.qr_img = self.generate_qr()
            self.qr_label.config(image=self.qr_img)
        except Exception as e:
            print(f"[QR] Error refreshing QR: {e}")
            from PIL import Image, ImageDraw
            img = Image.new('RGB', (180, 180), self.surface_color)
            dc = ImageDraw.Draw(img)
            dc.text((25, 80), "WAITING FOR\nREMOTE TUNNEL...", fill=self.accent_color)
            self.qr_img = pil_to_tkphoto(img, self.root)
            self.qr_label.config(image=self.qr_img)

        # Re-check periodically (to update QR if tunnel becomes available)
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

    def save_remote_config(self):
        token = self.token_entry.get().strip()
        host = self.host_entry.get().strip()
        
        try:
            env_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), ".env")
            lines = []
            if os.path.exists(env_path):
                with open(env_path, "r") as f:
                    lines = f.readlines()
            
            new_lines = []
            found_token = False
            found_host = False
            
            for line in lines:
                if line.startswith("TUNNEL_TOKEN="):
                    new_lines.append(f"TUNNEL_TOKEN={token}\n")
                    found_token = True
                elif line.startswith("REMOTE_HOSTNAME="):
                    new_lines.append(f"REMOTE_HOSTNAME={host}\n")
                    found_host = True
                else:
                    new_lines.append(line)
            
            if not found_token: new_lines.append(f"TUNNEL_TOKEN={token}\n")
            if not found_host: new_lines.append(f"REMOTE_HOSTNAME={host}\n")
            
            with open(env_path, "w") as f:
                f.writelines(new_lines)
            
            messagebox.showinfo("Rygent", "Configuration saved! Please restart the agent for changes to take effect.")
        except Exception as e:
            messagebox.showerror("Error", f"Failed to save config: {e}")

    def exit_app(self):
        if messagebox.askokcancel("Rygent Agent", "Shutdown Agent completely?\n\nThis will stop the Cloudflare tunnel and disconnect all remote devices.\n\nTip: Use 'Minimize to Tray' to keep the tunnel running."):
            # Force kill the cloudflared process
            try:
                from tunnel_manager import tunnel_manager
                tunnel_manager.force_stop_tunnel()
            except: pass
            
            os._exit(0)

    def on_close(self):
        """When user clicks X button, minimize to tray instead of exiting."""
        self.minimize_to_tray()

if __name__ == "__main__":
    root = tk.Tk(className="agent_gui")
    app = AgentGUI(root)
    
    # Override window close (X button) to minimize instead of exit
    root.protocol("WM_DELETE_WINDOW", app.on_close)
    
    threading.Thread(target=run_server, daemon=True).start()
    root.mainloop()
