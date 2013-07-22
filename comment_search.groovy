/**************************************************************************
  Copyright (C) 2013  Tsutomu Uchino

  This program is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

// Provides way to search in comments and 
// to trigger searching through HTTP connection.

// server settings
def port_ui = 54398;
def port_help = 54378;

def mode = "ui"; // or "help"


import javax.swing.SwingUtilities;


/** Shows specific entry by entry index.
 *  @param n entry index
 */
def jump_to_entry(n) {
    if (n != null) {
        def goto_entry = {
            try { editor.gotoEntry(n); } 
            catch (Exception e) {}
        }
        SwingUtilities.invokeLater(new Thread(goto_entry));
    }
}


/** Search in source of entries.
 *  @param str search text
 *  @param entries list
 */
def source_search(str, entries) {
    for (int i = 0; i < entries.size(); i++) {
        def it = entries[i];
        def source = it.getSrcText();
        //println(source);
        //println(source.indexOf(str));
        if (source && source.indexOf(str) >= 0)
            return it.entryNum();
    }
    return null;
}

/** Search in target of entries.
 *  @param str search text
 *  @param entries list
 */
def target_search(str, entries) {
    for (int i = 0; i < entries.size(); i++) {
        def it = entries[i];
        def target = it.getSourceTranslation();
        if (target && target.indexOf(str) >= 0)
            return it.entryNum();
    }
    return null;
}


/** Search in comment of entries.
 *  @param str search text
 *  @param entries List<SourceTextEntry> to search
 */
def comment_search(str, entries) {
    //println("Comment Search: " + str + " in " + entries.size);
    for (int i = 0; i < entries.size(); i++) {
        def it = entries[i];
        def comment = it.getComment();
        if (comment && comment.indexOf(str) >= 0) {
            return it.entryNum();
        }
    }
    return null;
}


/** Search in entries from specific file.
 *  @param str search text
 *  @param file_name part of file path
 */
def file_search(str, file_name) {
    //println("File Search: " + str + " in " + file_name);
    if (!project.isProjectLoaded())
        return;
    def entries = project.getAllEntries();
    if (entries == null)
        return; // no entry?
    
    def _file_name = file_name + ".po";
    def file = null;
    def files = project.getProjectFiles();
    for (int i = 0; i < files.size(); i++) {
        // filterFileFormatName is human readable, so unuseful
        def _file = files[i];
        if (_file.filePath.indexOf(_file_name) >= 0 ) {
            file = _file;
            break;
        }
    }
    return file;
}


/** Search 
 *  @param keyword passed by search_from_http
 *  @search_type category to search
 */ 
def keyword_search(keyword, search_type) {
    // text/shared/01/online_update.xhp%23hd_id315256.help.text
    // text/shared/01.po online_update.xhp%23hd_id315256.help.text
    // find file
    //println("keyword_search, search_type: " + search_type);
    
    def file_name = "";
    def term = "";
    def i = keyword.lastIndexOf("/");
    if (0 < i) {
        file_name = keyword.substring(0, i);
        term = keyword.substring(i + 1);
    } else
        return; // illegal search keywords
    def file = file_search(term, file_name);
    //println((file == null ? "file not found: " : "file found: ") + file_name);
    //println(term);
    def entries = file == null ? project.getAllEntries() : file.entries;
    if (search_type == "locations")
        return comment_search(term, entries);
    else if (search_type == "source")
        return source_search(term, entries);
    else if (search_type == "target")
        return target_search(term, entries);
    return null;
}


def start_keyword_search(keyword, search_type) {
    def n = keyword_search(keyword, search_type);
    if (n != null)
        jump_to_entry(n);
}


/** Start searching for keyword through HTTP.
 *  @param keyword passed part of the request
 *  @search_type category to search in
 */
def search_from_http(keyword, search_type) {
    if (!project.isProjectLoaded()) return;
    if (!keyword.isEmpty()) {
        def searcher = {
            start_keyword_search(keyword, search_type);
        }
        new Thread(searcher).start();
    }
}


// messages
def found = "<html><body>Searching...</body></html>";
def _found = found.getBytes();
def illegal = "<html><body>Illegal command.</body></html>";
def _illegal = illegal.getBytes();

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpHandler;


// http handler
def h = {
    def path = it.getRequestURI().getPath();
    if (path == "/favicon.ico")
        return it.sendResponseHeaders(403, 0);
    
    def out = it.getResponseBody();
    //println(path);
    // parse first path as command
    if (path.startsWith("/locations/")) {
        search_from_http(path.substring(11), "locations");
        
        it.sendResponseHeaders(200, _found.length);
        out.write(_found);
    } else if (path.startsWith("/source/")) {
        search_from_http(path.substring(8), "source");
        
        it.sendResponseHeaders(200, _found.length);
        out.write(_found);
    } else if (path.startsWith("/target/")) {
        search_from_http(path.substring(8), "target");
        
        it.sendResponseHeaders(200, _found.length);
        out.write(_found);
    } else {
        // illegal command
        it.sendResponseHeaders(200, _illegal.length);
        out.write(_illegal);
    }
    out.close();
    it.close();
} as HttpHandler;

// new server
def running = false;
def server = null;

def start_server = {
    if (!running) {
        try {
            server = HttpServer.create(
                new InetSocketAddress((mode == "ui" ? port_ui : port_help)), 0);
            server.createContext("/", h);
            server.start();
            running = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} as Runnable;

def stop_server = {
    if (server != null) {
        running = false;
        server.stop(1);
        server = null;
    }
} as Runnable;


// add dockable window
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.WindowListener;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JFrame;
//import org.omegat.gui.main.DockableScrollPane;

def title = "Comment Search";

//def panel = new JPanel();
def panel = new JFrame(title);
panel.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
// DO_NOTHING_ON_CLOSE
panel.setSize(300, 80);


def top_box = new Box(BoxLayout.Y_AXIS);
panel.add(top_box);

// search
def search_box = new Box(BoxLayout.X_AXIS);
top_box.add(search_box);

def field = new JTextField();
field.setPreferredSize(new Dimension(200, 25));
search_box.add(field);

def search_action = {
    def search_text = field.getText();
    if (!search_text.isEmpty()) {
        // Search in the hole project
        def n = comment_search(search_text, project.getAllEntries());
        if (n != null)
            jump_to_entry(n);
    }
} as ActionListener;

def search_btn = new JButton("Search");
search_box.add(search_btn)
search_btn.setPreferredSize(new Dimension(90, 25));
search_btn.addActionListener(search_action);


// server
def server_box = new Box(BoxLayout.X_AXIS);
top_box.add(server_box);

def label = new JLabel("Server: ");
server_box.add(label);
server_box.setSize(new Dimension(150, 25));

// mode changer
def radio_action = {
    mode = it.getActionCommand();
} as ActionListener;

def mode_group = new ButtonGroup();

def ui_radio = new JRadioButton("UI", true);
ui_radio.setActionCommand("ui");
ui_radio.addActionListener(radio_action);
server_box.add(ui_radio);
def help_radio = new JRadioButton("Help");
help_radio.setActionCommand("help");
help_radio.addActionListener(radio_action);
server_box.add(help_radio)
mode_group.add(ui_radio);
mode_group.add(help_radio);


def server_btn = new JButton("Start");
server_box.add(server_btn);
server_btn.setPreferredSize(new Dimension(90, 25));

def server_action = {
    // ToDo button label do not reflect the server state if failed to start.
    if (running) {
        // close
        stop_server.run();
        def set_btn_label = {
            server_btn.setLabel("Start");
            ui_radio.setEnabled(true);
            help_radio.setEnabled(true);
        }
        SwingUtilities.invokeLater(new Thread(set_btn_label));
    } else {
        //server_thread.start();
        new Thread(start_server).start();
        def set_btn_label = {
            server_btn.setLabel("Stop");
            ui_radio.setEnabled(false);
            help_radio.setEnabled(false);
        }
        SwingUtilities.invokeLater(new Thread(set_btn_label));
    }
} as ActionListener;

server_btn.addActionListener(server_action);


// Non default docking window crash UI layout settings.
//mainWindow.addDockable(new DockableScrollPane("COMMENTSEARCH", title, panel, true));

def window_action = [
    windowOpened: {}, 
    windowClosing: {stop_server.run();}, 
    windowClosed: {}, 
    windowIconified: {}, 
    windowDeiconified: {}, 
    windowActivated: {}, 
    windowDeactivated: {}
] as WindowListener;

//panel.pack();
panel.addWindowListener(window_action);
panel.setVisible(true);
panel.show();
