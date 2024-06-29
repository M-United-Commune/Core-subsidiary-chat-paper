package org.muc.muc_chat;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public final class MUC_Chat extends JavaPlugin implements Listener, WebSocket.Listener {

    //    ws通讯
// 创建HttpClient实例
    String context_uri = "ws://10980xe.mc5173.cn:10124/api/java/player";
    String server_name = "MUC_Chat";
    WebSocket webSocket;

    @Override
    public void onEnable() {
        // Plugin startup logic
//        监听玩家聊天
        getServer().getPluginManager().registerEvents(this, this);

        this.getLogger().info("开始加载MUC-paper端-Player插件");
//        加载配置文件
        if (getConfig().contains("uri")) {
            context_uri = getConfig().getString("uri");
            server_name = getConfig().getString("server_name");
        } else {
            getConfig().set("uri", context_uri);
            getConfig().set("server_name", server_name);
            saveConfig();
        }
//        创建WebSocket实例,并且实现
        HttpClient client = HttpClient.newHttpClient();
        var uri = URI.create("ws://"+context_uri + "/ws?server_name=" + server_name);
        webSocket = client.newWebSocketBuilder()
                .buildAsync(uri, this)
                .join();
    }


    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        String player_name = player.getName();
//        http://127.0.0.1:2024/api/java/player/broadcast?message={% mock 'cname' %}
        webSocket.sendText("[" + player_name + "]:" + message, true);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String player_name = player.getName();
        post_player_event("player_join", player_name);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String player_name = player.getName();
        post_player_event("player_leave", player_name);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        getServer().broadcastMessage(data.toString());
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

    //    发起请求
    void post_player_event(String event_msg, String player_name) {
        HttpClient.Builder builder = HttpClient.newBuilder();
        HttpClient client = builder.build();
        var uri = URI.create("http://"+context_uri + "/" + event_msg + "?name=" + player_name + "&server=" + server_name);
        var request = java.net.http.HttpRequest.newBuilder()
                .uri(uri)
                .POST(java.net.http.HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            getLogger().info(e.getMessage());
        }

    }

    @Override
    public void onDisable() {
    }
}
