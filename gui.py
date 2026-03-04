import gi
gi.require_version('Gtk', '3.0')
try:
    gi.require_version('WebKit2', '4.1')
except ValueError:
    gi.require_version('WebKit2', '4.0')
from gi.repository import Gtk, WebKit2, Gio, GLib
import sys
import threading
import time
import subprocess

class SystemMonitorGUI(Gtk.Window):
    def __init__(self):
        Gtk.Window.__init__(self, title="GiDash Monitor")
        self.set_default_size(1000, 700)
        self.set_position(Gtk.WindowPosition.CENTER)

        # Create a WebView
        self.webview = WebKit2.WebView()
        
        # Add a scrolled window to contain the WebView
        self.scrolled_window = Gtk.ScrolledWindow()
        self.scrolled_window.add(self.webview)
        self.add(self.scrolled_window)

        # Show a loading message initially
        self.set_placeholder()

        # Start checking for the server in a background thread
        threading.Thread(target=self.wait_for_server, daemon=True).start()
        
        # Close the app when the window is closed
        self.connect("destroy", Gtk.main_quit)

    def set_placeholder(self):
        # Create a simple box with a label while loading
        self.loading_box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=10)
        self.loading_box.set_valign(Gtk.Align.CENTER)
        self.loading_box.set_halign(Gtk.Align.CENTER)
        
        label = Gtk.Label(label="Connecting to local monitor service...")
        spinner = Gtk.Spinner()
        spinner.start()
        
        self.loading_box.pack_start(label, True, True, 0)
        self.loading_box.pack_start(spinner, True, True, 0)
        
        self.scrolled_window.hide()
        self.add(self.loading_box)
        self.show_all()

    def wait_for_server(self):
        url = "http://localhost:5000"
        max_retries = 10
        retries = 0
        
        while retries < max_retries:
            try:
                # Try simple connection check
                import socket
                with socket.create_connection(("localhost", 5000), timeout=1):
                    # Server is up!
                    GLib.idle_add(self.load_dashboard, url)
                    return
            except:
                retries += 1
                time.sleep(1)
        
        GLib.idle_add(self.show_error, "Could not connect to the background service. Please make sure system-monitor service is running.")

    def load_dashboard(self, url):
        self.remove(self.loading_box)
        self.scrolled_window.show()
        self.webview.load_uri(url)
        self.show_all()

    def show_error(self, message):
        self.remove(self.loading_box)
        error_label = Gtk.Label(label=message)
        self.add(error_label)
        self.show_all()

def main():
    win = SystemMonitorGUI()
    win.show_all()
    Gtk.main()

if __name__ == "__main__":
    main()
