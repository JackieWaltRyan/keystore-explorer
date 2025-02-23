/*
 * Copyright 2004 - 2013 Wayne Grant
 *           2013 - 2025 Kai Kramer
 *
 * This file is part of KeyStore Explorer.
 *
 * KeyStore Explorer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * KeyStore Explorer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with KeyStore Explorer.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.kse.gui.dialogs.extensions;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.security.cert.X509Extension;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.kse.crypto.CryptoException;
import org.kse.crypto.x509.X509CertUtil;
import org.kse.crypto.x509.X509Ext;
import org.kse.crypto.x509.X509ExtensionSet;
import org.kse.gui.CursorUtil;
import org.kse.gui.components.JEscDialog;
import org.kse.gui.JKseTable;
import org.kse.gui.KseFrame;
import org.kse.gui.LnfUtil;
import org.kse.gui.PlatformUtil;
import org.kse.gui.dialogs.DViewAsn1Dump;
import org.kse.gui.dialogs.DViewCertificate;
import org.kse.gui.dialogs.DViewCrl;
import org.kse.gui.error.DError;
import org.kse.utilities.asn1.Asn1Exception;
import org.kse.utilities.oid.ObjectIdComparator;

/**
 * Displays the details of X.509 Extensions.
 */
public class DViewExtensions extends JEscDialog implements HyperlinkListener {
    private static final long serialVersionUID = 1L;

    private static ResourceBundle res = ResourceBundle.getBundle("org/kse/gui/dialogs/extensions/resources");

    private JPanel jpExtensions;
    private JPanel jpExtensionsTable;
    private JScrollPane jspExtensionsTable;
    private JKseTable jtExtensions;
    private JPanel jpExtensionValue;
    private JLabel jlExtensionValue;
    private JPanel jpExtensionValueTextArea;
    private JScrollPane jspExtensionValue;
    private JEditorPane jepExtensionValue;
    private JPanel jpExtensionActionPanel;
    private JButton jbAsn1;
    private JPanel jpOK;
    private JButton jbOK;

    private X509Extension extensions;
    private KseFrame kseFrame;

    /**
     * Creates a new DViewExtensions dialog.
     *
     * @param parent     Parent frame
     * @param title      The dialog title
     * @param extensions Extensions to display
     */
    public DViewExtensions(JFrame parent, String title, X509Extension extensions) {
        super(parent, title, Dialog.ModalityType.DOCUMENT_MODAL);
        this.extensions = extensions;
        initComponents();
    }

    /**
     * Creates new DViewExtensions dialog.
     *
     * @param parent     Parent dialog
     * @param title      The dialog title
     * @param extensions Extensions to display
     */
    public DViewExtensions(JDialog parent, String title, X509Extension extensions) {
        super(parent, title, Dialog.ModalityType.DOCUMENT_MODAL);
        this.extensions = extensions;
        initComponents();
    }

    /**
     * Creates new DViewExtensions dialog. 
     * @param parent Parent dialog
     * @param title The dialog title
     * @param extensions Extensions to display
     * @param kseFrame Reference to main class with currently opened keystores and their contents
     */
    public DViewExtensions(JDialog parent, String title, X509Extension extensions, KseFrame kseFrame) {
        super(parent, title, Dialog.ModalityType.DOCUMENT_MODAL);
        this.extensions = extensions;
        this.kseFrame = kseFrame;
        initComponents();
    }

    private void initComponents() {
        ExtensionsTableModel extensionsTableModel = new ExtensionsTableModel();
        jtExtensions = new JKseTable(extensionsTableModel);

        TableRowSorter<ExtensionsTableModel> sorter = new TableRowSorter<>(extensionsTableModel);
        sorter.setComparator(2, new ObjectIdComparator());
        jtExtensions.setRowSorter(sorter);

        jtExtensions.setShowGrid(false);
        jtExtensions.setRowMargin(0);
        jtExtensions.getColumnModel().setColumnMargin(0);
        jtExtensions.getTableHeader().setReorderingAllowed(false);
        jtExtensions.setAutoResizeMode(JKseTable.AUTO_RESIZE_ALL_COLUMNS);
        jtExtensions.setRowHeight(Math.max(18, jtExtensions.getRowHeight()));

        for (int i = 0; i < jtExtensions.getColumnCount(); i++) {
            TableColumn column = jtExtensions.getColumnModel().getColumn(i);
            column.setHeaderRenderer(new ExtensionsTableHeadRend(jtExtensions.getTableHeader().getDefaultRenderer()));
            column.setCellRenderer(new ExtensionsTableCellRend());
        }

        TableColumn criticalCol = jtExtensions.getColumnModel().getColumn(0);
        criticalCol.setResizable(false);
        criticalCol.setMinWidth(28);
        criticalCol.setMaxWidth(28);
        criticalCol.setPreferredWidth(28);

        ListSelectionModel selectionModel = jtExtensions.getSelectionModel();
        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionModel.addListSelectionListener(evt -> {
            if (!evt.getValueIsAdjusting()) {
                try {
                    CursorUtil.setCursorBusy(DViewExtensions.this);
                    updateExtensionValue();
                } finally {
                    CursorUtil.setCursorFree(DViewExtensions.this);
                }
            }
        });

        jspExtensionsTable = PlatformUtil.createScrollPane(jtExtensions,
                                                           ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                           ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        jspExtensionsTable.getViewport().setBackground(jtExtensions.getBackground());

        jpExtensionsTable = new JPanel(new BorderLayout(5, 5));
        jpExtensionsTable.setPreferredSize(new Dimension(500, 200));
        jpExtensionsTable.add(jspExtensionsTable, BorderLayout.CENTER);

        jpExtensionValue = new JPanel(new BorderLayout(5, 5));

        jlExtensionValue = new JLabel(res.getString("DViewExtensions.jlExtensionValue.text"));

        jpExtensionValue.add(jlExtensionValue, BorderLayout.NORTH);

        jepExtensionValue = new JEditorPane();
        jepExtensionValue.setFont(new Font(Font.MONOSPACED, Font.PLAIN, LnfUtil.getDefaultFontSize()));
        jepExtensionValue.setEditable(false);
        jepExtensionValue.setToolTipText(res.getString("DViewExtensions.jtaExtensionValue.tooltip"));
        // JGoodies - keep uneditable color same as editable
        jepExtensionValue.putClientProperty("JTextArea.infoBackground", Boolean.TRUE);

        // for displaying URLs in extensions as clickable links
        jepExtensionValue.setContentType("text/html");
        jepExtensionValue.addHyperlinkListener(this);
        // use default font and foreground color from the component
        jepExtensionValue.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        jspExtensionValue = PlatformUtil.createScrollPane(jepExtensionValue,
                                                          ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                          ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        jpExtensionValueTextArea = new JPanel(new BorderLayout(5, 5));
        jpExtensionValueTextArea.setPreferredSize(new Dimension(500, 200));
        jpExtensionValueTextArea.add(jspExtensionValue, BorderLayout.CENTER);

        jpExtensionValue.add(jpExtensionValueTextArea, BorderLayout.CENTER);

        jbAsn1 = new JButton(res.getString("DViewExtensions.jbAsn1.text"));

        PlatformUtil.setMnemonic(jbAsn1, res.getString("DViewExtensions.jbAsn1.mnemonic").charAt(0));
        jbAsn1.setToolTipText(res.getString("DViewExtensions.jbAsn1.tooltip"));
        jbAsn1.addActionListener(evt -> {
            try {
                CursorUtil.setCursorBusy(DViewExtensions.this);
                asn1DumpPressed();
            } finally {
                CursorUtil.setCursorFree(DViewExtensions.this);
            }
        });

        JButton jbSaveTemplate = new JButton(res.getString("DAddExtensions.jbSaveTemplate.text"));
        jbSaveTemplate.setMnemonic(res.getString("DAddExtensions.jbSaveTemplate.mnemonic").charAt(0));
        jbSaveTemplate.setToolTipText(res.getString("DAddExtensions.jbSaveTemplate.tooltip"));

        jbSaveTemplate.addActionListener(evt -> {
            try {
                CursorUtil.setCursorBusy(DViewExtensions.this);
                DAddExtensions.saveTemplatePressed(new X509ExtensionSet(extensions), DViewExtensions.this);
            } finally {
                CursorUtil.setCursorFree(DViewExtensions.this);
            }
        });

        jpExtensionActionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        jpExtensionActionPanel.add(jbSaveTemplate);
        jpExtensionActionPanel.add(jbAsn1);

        jpExtensionValue.add(jpExtensionActionPanel, BorderLayout.SOUTH);

        jpExtensions = new JPanel(new GridLayout(2, 1, 5, 5));
        jpExtensions.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5),
                                                  new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 5, 5))));

        jpExtensions.add(jpExtensionsTable);
        jpExtensions.add(jpExtensionValue);

        jbOK = new JButton(res.getString("DViewExtensions.jbOK.text"));
        jbOK.addActionListener(evt -> okPressed());

        jpOK = PlatformUtil.createDialogButtonPanel(jbOK);

        extensionsTableModel.load(extensions);

        if (extensionsTableModel.getRowCount() > 0) {
            jtExtensions.changeSelection(0, 0, false, false);
        }

        getContentPane().add(jpExtensions, BorderLayout.CENTER);
        getContentPane().add(jpOK, BorderLayout.SOUTH);

        setResizable(false);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {
                closeDialog();
            }
        });

        getRootPane().setDefaultButton(jbOK);

        pack();

        SwingUtilities.invokeLater(() -> jbOK.requestFocus());
    }

    private void updateExtensionValue() {
        int selectedRow = jtExtensions.getSelectedRow();

        if (selectedRow == -1) {
            jepExtensionValue.setText("");
            jbAsn1.setEnabled(false);
        } else {
            String oid = ((ASN1ObjectIdentifier) jtExtensions.getValueAt(selectedRow, 2)).getId();
            byte[] value = extensions.getExtensionValue(oid);
            boolean criticality = (Boolean) jtExtensions.getValueAt(selectedRow, 0);

            X509Ext ext = new X509Ext(oid, value, criticality);

            try {
                jepExtensionValue.setText("<html><body>" + ext.getStringValue()
                                                              .replace(X509Ext.INDENT.getIndentChar().toString(),
                                                                       "&nbsp;").replace(X509Ext.NEWLINE, "<br/>") +
                                          "</body></html>");
            } catch (Exception e) {
                jepExtensionValue.setText("");
                DError.displayError(this, e);
            }
            jepExtensionValue.setCaretPosition(0);

            jbAsn1.setEnabled(true);
        }
    }

    private void asn1DumpPressed() {
        int selectedRow = jtExtensions.getSelectedRow();

        if (selectedRow == -1) {
            return;
        }

        String oid = ((ASN1ObjectIdentifier) jtExtensions.getValueAt(selectedRow, 2)).getId();
        byte[] value = extensions.getExtensionValue(oid);
        boolean criticality = (Boolean) jtExtensions.getValueAt(selectedRow, 0);

        X509Ext extension = new X509Ext(oid, value, criticality);

        try {
            DViewAsn1Dump dViewAsn1Dump = new DViewAsn1Dump(this, extension);
            dViewAsn1Dump.setLocationRelativeTo(this);
            dViewAsn1Dump.setVisible(true);
        } catch (Asn1Exception | IOException e) {
            DError.displayError(this, e);
        }
    }

    private void okPressed() {
        closeDialog();
    }

    private void closeDialog() {
        setVisible(false);
        dispose();
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            try {
                URL url = e.getURL();
                if (url != null) {
                    String path = url.getPath();
                    if (path.endsWith(".cer") || path.endsWith(".crt") || path.endsWith(".pem")
                            || path.endsWith(".der")) {
                        downloadCert(url);
                    } else if (url.getPath().endsWith(".crl")) {
                        downloadCrl(url);
                    } else {
                        Desktop.getDesktop().browse(url.toURI());
                    }
                }
            } catch (Exception ex) {
                DError.displayError(this, ex);
            }
        }
    }

    private boolean isRedirect(int status) {
		// normally, 3xx is redirect
    	if (status != HttpURLConnection.HTTP_OK) {
			if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
					|| status == HttpURLConnection.HTTP_SEE_OTHER)
				return true;
		}
    	return false;
    }

    private void downloadCrl(URL url) throws IOException, CryptoException {
    	HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
		int status = urlConn.getResponseCode();
		if (isRedirect(status)) {
			String newUrl = urlConn.getHeaderField("Location");
			url = new URL(newUrl);
			urlConn = (HttpURLConnection) url.openConnection();
		}
        try (InputStream is = urlConn.getInputStream()) {
            X509CRL crl = X509CertUtil.loadCRL(IOUtils.toByteArray(is));
            if (crl != null) {
                DViewCrl dViewCrl = new DViewCrl(this,
                                                 MessageFormat.format(res.getString("DViewExtensions.ViewCrl.Title"),
                                                                      url.toString()), ModalityType.DOCUMENT_MODAL,
                                                 crl);
                dViewCrl.setLocationRelativeTo(this);
                dViewCrl.setVisible(true);
            }
        }
    }

    private void downloadCert(URL url) throws IOException, CryptoException {
    	HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
		int status = urlConn.getResponseCode();
		if (isRedirect(status)) {
			String newUrl = urlConn.getHeaderField("Location");
			url = new URL(newUrl);
			urlConn = (HttpURLConnection) url.openConnection();
		}
        try (InputStream is = urlConn.getInputStream()) {
            X509Certificate[] certs = X509CertUtil.loadCertificates(IOUtils.toByteArray(is));
            if (certs != null && certs.length > 0) {
                int importExport = kseFrame == null ? DViewCertificate.NONE : DViewCertificate.IMPORT_EXPORT;
                DViewCertificate dViewCertificate = new DViewCertificate(this,
                        MessageFormat.format(res.getString("DViewExtensions.ViewCert.Title"), url.toString()), certs,
                        kseFrame, importExport);
                dViewCertificate.setLocationRelativeTo(this);
                dViewCertificate.setVisible(true);
            }
        }
    }
}
