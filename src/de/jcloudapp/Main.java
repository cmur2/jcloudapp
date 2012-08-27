/*
 * JCloudApp - Easy access to CloudApp (tm) - cross-platform
 *
 * Copyright 2011 Christian Nicolai <chrnicolai@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * See the NOTICE file distributed along with this work for further
 * information.
 */

package de.jcloudapp;

import java.awt.AWTException;
import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import com.cloudapp.rest.CloudApi;
import com.cloudapp.rest.CloudApiException;
import com.cloudapp.rest.CloudAppInputStream;

/**
 * @author Ch. Nicolai
 */
public class Main {

    private static final SimpleDateFormat df = (SimpleDateFormat) SimpleDateFormat.getInstance();
    
    static {
        df.applyPattern("yyyyMMdd-HHmmss");
    }
    
    public static void main(String[] args) throws Exception {
        Map<String, String> settings = getSettings(args);
        
        System.out.println(settings);
        
        Main m = new Main(settings.get(":username"), settings.get(":password"));
        m.run();
    }
    
    @SuppressWarnings("unchecked")
    private static Map<String, String> getSettings(String[] args) {
        if(args.length == 2) {
            HashMap<String, String> m = new HashMap<String, String>();
            m.put(":username", args[0]);
            m.put(":password", args[1]);
            return m;
        }
        
        File storage = new File(System.getProperty("user.home") + File.separatorChar + ".cloudapp-cli");
        if(storage.exists() && storage.isFile()) {
            Yaml yaml = new Yaml();
            try {
                Map<String, String> m =
                    (Map<String, String>) yaml.load(new FileInputStream(storage));
                return m;
            } catch(IOException ex) {
                showErrorDialog("Loading settings from .cloudapp-cli failed: "+ex);
                System.exit(1);
            }
        }
        
        // TODO: show input dialog
        
        return null;
    }
    
    private CloudApi client;
    private TrayIcon icon;
    private boolean working = false;
    
    private Image imNormal;
    private Image imWorking;
    
    public Main(String user, String pwd) {
        client = new CloudApi(user, pwd);
    }
    
    public void run() {
        try {
            client.getItems(1, 1, null, false);
        } catch(CloudApiException ex) {
            showErrorDialog("Login incorrect!");
            exit();
        }
        
        if(!SystemTray.isSupported()) {
            showErrorDialog("SystemTray is unsupported!");
            exit();
        }
        
        try {
            // borrowed from https://github.com/cmur2/gloudapp
            imNormal = ImageIO.read(Main.class.getResourceAsStream("gloudapp.png"));
            imWorking = ImageIO.read(Main.class.getResourceAsStream("gloudapp_working.png"));
        } catch(IOException ex) {
            showErrorDialog("Could not load image!\n"+ex);
            exit();
        }
        
        icon = new TrayIcon(imNormal);
        icon.setImageAutoSize(true);
        icon.setToolTip("JCloudApp");
        icon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!working) { working = true; doScreenshot(); working = false; }
            }
        });
        
        MenuItem screen = new MenuItem("Take Screenshot");
        screen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!working) { working = true; doScreenshot(); working = false; }
            }
        });
        
        MenuItem uploadClip = new MenuItem("Upload from Clipboard");
        uploadClip.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!working) { working = true; doUploadClipboard(); working = false; }
            }
        });
        
        MenuItem upload = new MenuItem("Upload File...");
        upload.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!working) { working = true; doUploadFile(); working = false; }
            }
        });
        
        MenuItem about = new MenuItem("About");
        about.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { doAbout(); }
        });
        
        MenuItem quit = new MenuItem("Quit");
        quit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) { doQuit(); }
        });
        
        PopupMenu popupMenu = new PopupMenu();
        popupMenu.add(screen);
        popupMenu.add(uploadClip);
        popupMenu.add(upload);
        popupMenu.add(about);
        popupMenu.addSeparator();
        popupMenu.add(quit);
        icon.setPopupMenu(popupMenu);

        try {
            SystemTray.getSystemTray().add(icon);
        } catch(AWTException ex) {
            showErrorDialog("No SystemTray found!\n"+ex);
            exit();
        }
    }
    
    public void doScreenshot() {
        System.out.println("Taking screenshot...");
        BufferedImage bi = takeScreenshot();
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            baos.close();
            byte[] image = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(image);
            String filename = String.format("Screenshot %s.png", df.format(new Date()));
            
            setImageWorking();
            JSONObject drop = client.uploadFile(new CloudAppInputStream(bais, "image/png", filename, image.length));
            String url = getDropUrl(drop);
            System.out.println("Upload complete, URL:\n"+url);
            setClipboard(url);
            setImageNormal();
            icon.displayMessage("Upload finished", String.format("Item: %s", filename), TrayIcon.MessageType.INFO);
        } catch(IOException ex) {
            System.out.println(ex);
        } catch(CloudApiException ex) {
            icon.displayMessage("Upload failed", ex.toString(), TrayIcon.MessageType.ERROR);
        }
    }
    
    @SuppressWarnings("unchecked")
    public void doUploadClipboard() {
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable t = cb.getContents(null);
        
        if(t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            try {
                List<File> data = (List<File>) 
                        t.getTransferData(DataFlavor.javaFileListFlavor);
                uploadFilesFromClipboard(data);
                return;
            } catch(UnsupportedFlavorException ex) {
                return;
            } catch(IOException ex) {
                return;
                
            }
        }
        else if(t.isDataFlavorSupported(DataFlavor.imageFlavor)) {
            try {
                Image data = (Image) t.getTransferData(DataFlavor.imageFlavor);
                BufferedImage bi = (BufferedImage) data;
                uploadImageFromClipboard(bi);
            } catch(UnsupportedFlavorException ex) {
                return;
            } catch(IOException ex) {
                return;
            }
        }
        else if(t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                String data = (String) t.getTransferData(DataFlavor.stringFlavor);
                uploadStringFromClipboard(data);
            } catch(UnsupportedFlavorException ex) {
                return;
            } catch(IOException ex) {
                return;
            }
        }
    }
    
    public void doUploadFile() {
        FileDialog dlg = new FileDialog((Dialog)null, "Upload File...");
        dlg.setVisible(true);
        if(dlg.getDirectory() == null || dlg.getFile() == null) {
            return;
        }
        File f = new File(dlg.getDirectory()+File.separator+dlg.getFile());
        if(f.exists()) {
            setImageWorking();
            JSONObject drop = upload(f);
            if(drop == null) { return; }
            String url = getDropUrl(drop);
            System.out.println("Upload complete, URL:\n"+url);
            setClipboard(url);
            setImageNormal();
            icon.displayMessage("Upload finished", String.format("Item: %s", f.getName()), TrayIcon.MessageType.INFO);
        }
    }
    
    public void doAbout() {
        String msg = "JCloudApp (C) 2011 Christian Nicolai\n\n"
        + "Easy uploading of screenshots and files to CloudApp (tm) - cross-plattform.";
        JOptionPane.showMessageDialog(null, msg, "About", JOptionPane.INFORMATION_MESSAGE);
    }
    
    public void doQuit() {
        exit();
    }
    
    private BufferedImage takeScreenshot() {
        try {
            Robot robot = new Robot();
            Rectangle captureSize = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            return robot.createScreenCapture(captureSize);
        } catch(AWTException ex) {
            System.out.println(ex);
        }
        return null;
    }
    
    private void uploadFilesFromClipboard(List<File> data) {
        if(data != null && data.size() > 0) {
            setImageWorking();
            ArrayList<String> urls = new ArrayList<String>();
            for(File f : data) {
                JSONObject drop = upload(f);
                // cancel all uploads on error
                if(drop == null) { return; }
                String url = getDropUrl(drop);
                System.out.println("Upload complete, URL:\n"+url);
                urls.add(url);
            }
            int n = urls.size();
            String text = urls.remove(0);
            for(String s : urls) { text += " "+s; }
            setClipboard(text);
            String msg;
            if(n == 1) {
                msg = String.format("Item: %s", data.get(0).getName());
            } else {
                msg = String.format("%d Items: %s", n, data.get(0).getName());
                int nchars = msg.length();
                for(int i = 1; i < data.size(); i++) {
                    if(nchars + data.get(i).getName().length() > 140) {
                        msg += ", ...";
                        break;
                    } else {
                        msg += ", "+data.get(i).getName();
                        nchars += data.get(i).getName().length();
                    }
                }
            }
            setImageNormal();
            icon.displayMessage("Upload finished", msg, TrayIcon.MessageType.INFO);
        }
    }

    private void uploadImageFromClipboard(BufferedImage bi) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            baos.close();
            byte[] image = baos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(image);
            String filename = String.format("Screenshot %s.png", df.format(new Date()));
            
            setImageWorking();
            JSONObject drop = client.uploadFile(new CloudAppInputStream(bais, "image/png", filename, image.length));
            String url = getDropUrl(drop);
            System.out.println("Upload complete, URL:\n"+url);
            setClipboard(url);
            setImageNormal();
            icon.displayMessage("Upload finished", String.format("Item: %s", filename), TrayIcon.MessageType.INFO);
        } catch(IOException ex) {
            System.out.println(ex);
        } catch(CloudApiException ex) {
            icon.displayMessage("Upload failed", ex.toString(), TrayIcon.MessageType.ERROR);
        }
    }
    
    private void uploadStringFromClipboard(String s) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(s.getBytes("utf-8"));
            String filename = String.format("Snippet %s.txt", df.format(new Date()));
            
            setImageWorking();
            JSONObject drop = client.uploadFile(new CloudAppInputStream(bais, "text/plain", filename, bais.available()));
            String url = getDropUrl(drop);
            System.out.println("Upload complete, URL:\n"+url);
            setClipboard(url);
            setImageNormal();
            icon.displayMessage("Upload finished", String.format("Item: %s", filename), TrayIcon.MessageType.INFO);
        } catch(IOException ex) {
            System.out.println(ex);
        } catch(CloudApiException ex) {
            icon.displayMessage("Upload failed", ex.toString(), TrayIcon.MessageType.ERROR);
        }
    }
    
    private JSONObject upload(File file) {
        try {
            return client.uploadFile(file);
        } catch(CloudApiException ex) {
            icon.displayMessage("Upload failed", ex.toString(), TrayIcon.MessageType.ERROR);
        }
        return null;
    }
    
    private void setClipboard(String s) {
        Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
        cb.setContents(new StringSelection(s), null);
    }
    
    private void setImageNormal() {
        icon.setImage(imNormal);
    }
    
    private void setImageWorking() {
        icon.setImage(imWorking);
    }
    
    private void exit() {
        System.exit(0);
    }
    
    // statics
    
    private static void showErrorDialog(String msg) {
        JOptionPane.showMessageDialog(null, msg, "JCloudApp - Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private static String getDropUrl(JSONObject drop) {
        try {
            return drop.getString("url");
        } catch(JSONException ex) {
            System.out.println(ex);
        }
        return null;
    }
    
    private static void debug(JSONObject o) {
        try {
            System.out.println(o.toString(2));
        } catch(JSONException ex) {
            System.out.println(ex);
        }
    }
}
