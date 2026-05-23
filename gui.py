import gi
gi.require_version('Gtk', '3.0')
try:
    gi.require_version('WebKit2', '4.1')
except ValueError:
    gi.require_version('WebKit2', '4.0')
from gi.repository import Gtk, WebKit2, GLib
import threading
import time

class SystemMonitorGUI(Gtk.Window):
    def __init__(self):
        Gtk.Window.__init__(self, title="GiDash Monitor")
        self.set_default_size(1000, 700)
        self.set_position(Gtk.WindowPosition.CENTER)

        self.stack = Gtk.Stack()
        self.add(self.stack)

        loading_box = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=10)
        loading_box.set_valign(Gtk.Align.CENTER)
        loading_box.set_halign(Gtk.Align.CENTER)
        label = Gtk.Label(label="Connecting to local monitor service...")
        spinner = Gtk.Spinner()
        spinner.start()
        loading_box.pack_start(label, True, True, 0)
        loading_box.pack_start(spinner, True, True, 0)
        self.stack.add_named(loading_box, "loading")

        scrolled_window = Gtk.ScrolledWindow()
        self.webview = WebKit2.WebView()
        scrolled_window.add(self.webview)
        self.stack.add_named(scrolled_window, "browser")

        self.stack.set_visible_child_name("loading")
        self.connect("destroy", Gtk.main_quit)

        threading.Thread(target=self.wait_for_server, daemon=True).start()

    def wait_for_server(self):
        import socket as _socket
        url = "http://localhost:5000"
        max_retries = 10
        for _ in range(max_retries):
            try:
                with _socket.create_connection(("localhost", 5000), timeout=1):
                    GLib.idle_add(self.load_dashboard, url)
                    return
            except Exception:
                time.sleep(1)
        GLib.idle_add(self.show_error, "Could not connect to the background service.")

    def load_dashboard(self, url):
        self.webview.load_uri(url)
        self.stack.set_visible_child_name("browser")

    def show_error(self, message):
        error_label = Gtk.Label(label=message)
        self.stack.add_named(error_label, "error")
        self.stack.set_visible_child_name("error")
        self.show_all()

def main():
    win = SystemMonitorGUI()
    win.show_all()
    Gtk.main()

if __name__ == "__main__":
    main()
