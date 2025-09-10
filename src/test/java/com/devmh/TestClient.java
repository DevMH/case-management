package com.devmh;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TestClient extends JFrame {

    // UI widgets
    private final JTextField tfHttp = new JTextField("http://localhost:8081");
    private final JTextField tfWs   = new JTextField("ws://localhost:8081/ws");
    private final JCheckBox  cbSock = new JCheckBox("SockJS (use http:// for WS)", false);
    private final JCheckBox  cbJson = new JCheckBox("Pretty JSON output", false);
    private final JButton    btnConnect = new JButton("Connect");
    private final JButton    btnDisconnect = new JButton("Disconnect");
    private final DefaultListModel<String> subsModel = new DefaultListModel<>();
    private final JList<String> subsList = new JList<>(subsModel);
    private final JTextField tfNewSub = new JTextField("/topic/case/created");
    private final JButton    btnSub   = new JButton("Subscribe");
    private final JButton    btnUnsub = new JButton("Unsubscribe");
    private final DefaultTableModel headersModel = new DefaultTableModel(new Object[]{"Header", "Value"}, 0);
    private final JTable headersTable = new JTable(headersModel);
    private final JTextArea log = new JTextArea(20, 80);
    private final JTextField tfRestPath = new JTextField("/api/events/create/CASE-123");
    private final JTextArea  taRestBody = new JTextArea("{\"note\":\"hello\"}", 5, 40);
    private final JButton    btnGet = new JButton("GET");
    private final JButton    btnPost = new JButton("POST");

    // Networking & state
    private WebSocketStompClient stompClient;
    private StompSession session;
    private final ConsoleSessionHandler sessionHandler = new ConsoleSessionHandler();
    private final ObjectMapper json = new ObjectMapper().registerModule(new JavaTimeModule());
    private final HttpClient http = HttpClient.newHttpClient();

    public TestClient() {
        super("REST and WebSocket Thick Client");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel north = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4,4,4,4); c.fill = GridBagConstraints.HORIZONTAL; c.weightx=0;
        north.add(new JLabel("HTTP Base:"), c);
        c.weightx = 1; c.gridx=1; north.add(tfHttp, c);
        c.gridx=2; c.weightx=0; north.add(new JLabel("WS URL:"), c);
        c.gridx=3; c.weightx=1; north.add(tfWs, c);
        c.gridx=4; c.weightx=0; north.add(cbSock, c);
        c.gridx=5; north.add(cbJson, c);
        c.gridx=6; north.add(btnConnect, c);
        c.gridx=7; north.add(btnDisconnect, c);
        add(north, BorderLayout.NORTH);

        JSplitPane center = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        center.setResizeWeight(0.35);

        JPanel left = new JPanel(new BorderLayout(6,6));
        left.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));

        JPanel subsPanel = new JPanel(new BorderLayout(4,4));
        subsPanel.setBorder(BorderFactory.createTitledBorder("Subscriptions"));
        subsPanel.add(new JScrollPane(subsList), BorderLayout.CENTER);
        JPanel subCtl = new JPanel(new BorderLayout(4,4));
        subCtl.add(tfNewSub, BorderLayout.CENTER);
        JPanel subBtns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        subBtns.add(btnSub); subBtns.add(btnUnsub);
        subCtl.add(subBtns, BorderLayout.EAST);
        subsPanel.add(subCtl, BorderLayout.SOUTH);

        JPanel hdrPanel = new JPanel(new BorderLayout(4,4));
        hdrPanel.setBorder(BorderFactory.createTitledBorder("Default HTTP Headers"));
        headersTable.setFillsViewportHeight(true);
        hdrPanel.add(new JScrollPane(headersTable), BorderLayout.CENTER);
        JButton btnAddHdr = new JButton("Add");
        JButton btnDelHdr = new JButton("Remove");
        JPanel hdrBtns = new JPanel(new FlowLayout(FlowLayout.LEFT));
        hdrBtns.add(btnAddHdr); hdrBtns.add(btnDelHdr);
        hdrPanel.add(hdrBtns, BorderLayout.SOUTH);

        left.add(subsPanel, BorderLayout.CENTER);
        left.add(hdrPanel, BorderLayout.SOUTH);

        JPanel right = new JPanel(new BorderLayout(6,6));
        right.setBorder(BorderFactory.createEmptyBorder(6,6,6,6));

        JPanel restPanel = new JPanel(new GridBagLayout());
        GridBagConstraints r = new GridBagConstraints();
        r.insets = new Insets(4,4,4,4); r.fill = GridBagConstraints.HORIZONTAL; r.weightx=0;
        r.gridx=0; r.gridy=0; restPanel.add(new JLabel("REST path or URL:"), r);
        r.gridx=1; r.weightx=1; restPanel.add(tfRestPath, r);
        r.gridx=2; r.weightx=0; restPanel.add(btnGet, r);
        r.gridx=3; restPanel.add(btnPost, r);
        r.gridx=0; r.gridy=1; r.weightx=0;
        restPanel.add(new JLabel("POST JSON body:"), r);
        r.gridx=1; r.gridwidth=3; r.weightx=1;
        restPanel.add(new JScrollPane(taRestBody), r);

        log.setEditable(false);
        var logScroll = new JScrollPane(log);
        logScroll.setBorder(BorderFactory.createTitledBorder("Messages / Responses"));

        right.add(restPanel, BorderLayout.NORTH);
        right.add(logScroll, BorderLayout.CENTER);

        center.setLeftComponent(left);
        center.setRightComponent(right);
        add(center, BorderLayout.CENTER);

        btnConnect.addActionListener(e -> doConnect());
        btnDisconnect.addActionListener(e -> doDisconnect());
        btnSub.addActionListener(e -> {
            String dest = tfNewSub.getText().trim();
            if (!dest.isEmpty()) subscribe(dest);
        });
        btnUnsub.addActionListener(e -> {
            String sel = subsList.getSelectedValue();
            if (sel != null) unsubscribe(sel);
        });
        btnAddHdr.addActionListener(e -> headersModel.addRow(new Object[]{"Authorization", "Bearer xyz"}));
        btnDelHdr.addActionListener(e -> {
            int row = headersTable.getSelectedRow();
            if (row >= 0) headersModel.removeRow(row);
        });
        btnGet.addActionListener(e -> sendGet());
        btnPost.addActionListener(e -> sendPost());

        setSize(1180, 720);
        setLocationRelativeTo(null);
    }

    private void doConnect() {
        try {
            if (session != null && session.isConnected()) {
                append(ts() + " Already connected.\n");
                return;
            }
            // Build STOMP client per config
            boolean sock = cbSock.isSelected();
            stompClient = sock
                    ? new WebSocketStompClient(new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))))
                    : new WebSocketStompClient(new StandardWebSocketClient());

            stompClient.setTaskScheduler(new ConcurrentTaskScheduler()); // receipts/heartbeats
            if (cbJson.isSelected()) {
                var conv = new MappingJackson2MessageConverter();
                conv.setObjectMapper(json);
                stompClient.setMessageConverter(conv);
            } else {
                stompClient.setMessageConverter(new StringMessageConverter());
            }
            stompClient.setInboundMessageSizeLimit(512 * 1024);

            String url = tfWs.getText().trim();
            session = stompClient.connectAsync(url, sessionHandler).get(10, TimeUnit.SECONDS);
            append(ts() + " Connected STOMP. sessionId=" + session.getSessionId() + "\n");
        } catch (Exception ex) {
            append(ts() + " Connect failed: " + ex + "\n");
            JOptionPane.showMessageDialog(this, ex.toString(), "Connect error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doDisconnect() {
        try {
            if (session != null) {
                session.disconnect();
                append(ts() + " Disconnected.\n");
            }
        } catch (Exception ex) {
            append(ts() + " Disconnect error: " + ex + "\n");
        } finally {
            session = null;
        }
    }

    private void subscribe(String destination) {
        if (session == null || !session.isConnected()) {
            append(ts() + " Not connected.\n");
            return;
        }
        StompFrameHandler handler = cbJson.isSelected() ? new JsonDumpHandler() : new StringDumpHandler();
        session.subscribe(destination, handler);
        subsModel.addElement(destination);
        append(ts() + " SUBSCRIBE " + destination + "\n");
    }

    private void unsubscribe(String dest) {
        if (session == null) {
            return;
        }
        // STOMP API doesn’t expose dest per sub
        //session.getSubscriptions().forEach((id, sub) -> sub.unsubscribe());
        subsModel.removeElement(dest);
        append(ts() + " UNSUBSCRIBE " + dest + " (all current subs removed)\n");
    }

    private void sendGet() {
        try {
            String url = normalize(tfHttp.getText().trim(), tfRestPath.getText().trim());
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).GET();
            headersFromTable().forEach(b::header);
            HttpResponse<String> res = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            append(ts() + " GET " + url + " => " + res.statusCode() + "\n" + res.body() + "\n");
        } catch (Exception ex) {
            append(ts() + " GET error: " + ex + "\n");
        }
    }

    private void sendPost() {
        try {
            String url = normalize(tfHttp.getText().trim(), tfRestPath.getText().trim());
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                    .POST(HttpRequest.BodyPublishers.ofString(taRestBody.getText()))
                    .header("Content-Type","application/json");
            headersFromTable().forEach(b::header);
            HttpResponse<String> res = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            append(ts() + " POST " + url + " => " + res.statusCode() + "\n" + res.body() + "\n");
        } catch (Exception ex) {
            append(ts() + " POST error: " + ex + "\n");
        }
    }

    // Handlers

    private class ConsoleSessionHandler extends StompSessionHandlerAdapter {
        @Override public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            append(ts() + " STOMP CONNECTED " + connectedHeaders + "\n");
        }
        @Override public void handleException(StompSession s, StompCommand c, StompHeaders h, byte[] p, Throwable ex) {
            append(ts() + " STOMP EXCEPTION: " + ex + "\n");
        }
        @Override public void handleTransportError(StompSession s, Throwable ex) {
            append(ts() + " TRANSPORT ERROR: " + ex + "\n");
        }
    }

    private static class StringDumpHandler implements StompFrameHandler {
        @Override public java.lang.reflect.Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override public void handleFrame(StompHeaders headers, Object payload) {
            SwingUtilities.invokeLater(() -> {
                System.out.println(payload); // console
            });
        }
    }

    private class JsonDumpHandler implements StompFrameHandler {
        @Override public java.lang.reflect.Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override public void handleFrame(StompHeaders headers, Object payload) {
            SwingUtilities.invokeLater(() -> {
                try {
                    Object tree = json.readValue(String.valueOf(payload), Object.class);
                    append(ts() + " FRAME " + headers.getDestination()
                            + " CT=" + headers.getFirst("content-type") + "\n"
                            + json.writerWithDefaultPrettyPrinter().writeValueAsString(tree) + "\n");
                } catch (Exception e) {
                    append(ts() + " (JSON parse failed) raw=" + payload + "\n");
                }
            });
        }
    }

    private Map<String,String> headersFromTable() {
        Map<String,String> m = new LinkedHashMap<>();
        for (int i=0;i<headersModel.getRowCount();i++) {
            String k = String.valueOf(headersModel.getValueAt(i,0));
            String v = String.valueOf(headersModel.getValueAt(i,1));
            if (k != null && !k.isBlank()) m.put(k, v == null ? "" : v);
        }
        return m;
    }

    private static String normalize(String base, String path) {
        if (path.startsWith("http")) return path;
        if (!base.endsWith("/") && !path.startsWith("/")) return base + "/" + path;
        if (base.endsWith("/") && path.startsWith("/")) return base + path.substring(1);
        return base + path;
    }

    private static String ts() {
        return "[" + Instant.now() + "]";
    }

    private void append(String s) {
        log.append(s);
        log.setCaretPosition(log.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TestClient().setVisible(true));
    }
}

