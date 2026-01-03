package com.springwater.easybot.bridge;

import com.google.gson.*;
import com.springwater.easybot.bridge.adapter.OpCodeAdapter;
import com.springwater.easybot.bridge.api.IBridgeExtension;
import com.springwater.easybot.bridge.api.events.lifecycle.BridgeConnectedAsyncEvent;
import com.springwater.easybot.bridge.api.events.lifecycle.BridgeDisconnectedAsyncEvent;
import com.springwater.easybot.bridge.api.events.lifecycle.BridgeOnlineAsyncEvent;
import com.springwater.easybot.bridge.api.events.raw.BridgeSendPacketEvent;
import com.springwater.easybot.bridge.api.events.raw.RawExecPacketEvent;
import com.springwater.easybot.bridge.api.events.raw.RawPacketEvent;
import com.springwater.easybot.bridge.api.rpc.BridgeRpc;
import com.springwater.easybot.bridge.api.rpc.IRpcListener;
import com.springwater.easybot.bridge.api.rpc.RpcContext;
import com.springwater.easybot.bridge.extension.BridgeEventManager;
import com.springwater.easybot.bridge.extension.BridgeRpcManager;
import com.springwater.easybot.bridge.logger.DefaultLoggerAdapter;
import com.springwater.easybot.bridge.logger.ILogger;
import com.springwater.easybot.bridge.message.Segment;
import com.springwater.easybot.bridge.message.SegmentType;
import com.springwater.easybot.bridge.model.PlayerInfo;
import com.springwater.easybot.bridge.model.ServerInfo;
import com.springwater.easybot.bridge.packet.*;
import com.springwater.easybot.bridge.utils.GsonUtils;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.springwater.easybot.bridge.message.Segment.getSegmentClass;

public class BridgeClient implements WebSocketListener {

    @Getter
    private static BridgeClient instance;

    @Getter
    private static final BridgeEventManager eventManager = new BridgeEventManager();

    @Getter
    private static final BridgeRpcManager rpcManager = new BridgeRpcManager();

    @Setter
    @Getter
    private static ILogger logger = new DefaultLoggerAdapter();

    @Getter
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(OpCode.class, new OpCodeAdapter()).create();

    private final WebSocketClient client;
    private final ExecutorService executor;
    private final ExecutorService rpcExecutor = Executors.newFixedThreadPool(16);
    private final BridgeBehavior behavior;
    private final Object connectionLock = new Object();
    private final ConcurrentHashMap<String, CompletableFuture<String>> callbackTasks = new ConcurrentHashMap<>();

    private final ScheduledExecutorService timeoutScheduler = Executors.newScheduledThreadPool(1, r -> {
        Thread t = new Thread(r, "BridgeClient-timeoutScheduler");
        t.setDaemon(true);
        return t;
    });
    @SuppressWarnings("FieldCanBeLocal")
    private final long CallBackTimeout = 5;

    private Session session;

    @Setter
    @Getter
    private IdentifySuccessPacket identifySuccessPacket;

    @Setter
    @Getter
    private String token;

    @Setter
    private HelloPacket helloPacket;

    private String uri;

    // 状态标志位
    private volatile boolean isConnected = false;
    // 新增：正在连接中状态，防止并发重连
    private volatile boolean isConnecting = false;
    private volatile boolean isShutdown = false;

    private ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "BridgeClient-heartbeatScheduler");
        t.setDaemon(true);
        return t;
    });

    @Getter
    private boolean ready;
    @Getter
    private int heartbeatInterval = 120;

    public BridgeClient(String uri, BridgeBehavior behavior) {
        this.uri = uri;
        this.behavior = behavior;
        this.client = new WebSocketClient();
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "BridgeClient-Worker");
            t.setDaemon(true);
            return t;
        });
        instance = this;
        connect();
    }

    private String perSend(Object packet) {
        JsonObject object = GsonUtils.merge(gson, new JsonObject(), packet);
        BridgeSendPacketEvent event = new BridgeSendPacketEvent(this);
        event.setPacket(object);
        event.setHasCallbackId(packet.getClass().isInstance(PacketWithCallBackId.class));
        eventManager.push(event);
        return gson.toJson(event.getPacket());
    }

    public <T> CompletableFuture<T> sendAndWaitForCallbackAsync(PacketWithCallBackId packet, Class<T> responseType) {
        if (isShutdown) {
            CompletableFuture<T> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalStateException("BridgeClient is shutting down"));
            return failed;
        }

        String callbackId = UUID.randomUUID().toString();
        packet.setCallBackId(callbackId);

        CompletableFuture<String> future = new CompletableFuture<>();
        callbackTasks.put(callbackId, future);

        send(packet);

        ScheduledFuture<?> timeoutFuture = timeoutScheduler.schedule(() -> {
            CompletableFuture<String> removedFuture = callbackTasks.remove(callbackId);
            if (removedFuture != null) {
                removedFuture.completeExceptionally(new TimeoutException("等待EasyBot返回结果超时!"));
            }
        }, CallBackTimeout, TimeUnit.SECONDS);

        return future.thenApply(result -> {
            timeoutFuture.cancel(false);
            return gson.fromJson(result, responseType);
        }).exceptionally(ex -> {
            timeoutFuture.cancel(false);
            throw new CompletionException("Error waiting for callback", ex);
        });
    }

    /* -------------------- WebSocketListener 实现 -------------------- */

    @Override
    public void onWebSocketConnect(Session session) {
        if (isShutdown) {
            session.close();
            return;
        }
        logger.info("已连接到服务器: " + session.getUpgradeRequest().getRequestURI());
        this.session = session;
        synchronized (connectionLock) {
            isConnected = true;
            isConnecting = false; // 连接成功，复位正在连接状态
        }

        BridgeConnectedAsyncEvent event = new BridgeConnectedAsyncEvent(this);
        event.setUrl(session.getUpgradeRequest().getRequestURI().toString());
        eventManager.pushAsync(event);
    }

    @Override
    public void onWebSocketText(String message) {
        if (isShutdown) return;

        if (ClientProfile.isDebugMode()) {
            logger.info("收到消息: " + message);
        }
        Gson gson = getGson();
        Packet packet = gson.fromJson(message, Packet.class);
        JsonObject body = gson.fromJson(message, JsonObject.class);
        if (packet == null || packet.getOpCode() == null) {
            logger.warn("解析到空 packet 或 opCode，原始消息: " + message);
            return;
        }

        RawPacketEvent event = new RawPacketEvent(this);
        event.setPacket(packet);
        event.setBody(body);
        eventManager.push(event);
        if (event.isSkipInternalProcessing()) {
            return;
        }

        switch (packet.getOpCode()) {
            case Hello: {
                HelloPacket helloPacket = gson.fromJson(message, HelloPacket.class);
                setHelloPacket(helloPacket);
                logger.info("已连接到主程序!");
                logger.info(">>>主程序连接信息<<<");
                logger.info("系统: " + helloPacket.getSystemName());
                logger.info("运行时版本: " + helloPacket.getDotnetVersion());
                logger.info("主程序版本: " + helloPacket.getVersion());
                logger.info("连接信息: " + helloPacket.getSessionId() + " (心跳:" + helloPacket.getInterval() + "s)");
                logger.info(">>>服务器信息<<<");
                logger.info("令牌: " + getToken());
                logger.info("服务器: " + ClientProfile.getServerDescription());
                logger.info("插件版本: " + ClientProfile.getPluginVersion());
                logger.info("支持命令: " + ClientProfile.isCommandSupported());
                logger.info("变量支持:" + ClientProfile.isPapiSupported());
                logger.info(">>>准备上传<<<");
                logger.info("上报身份中...");

                heartbeatInterval = Math.max(10, helloPacket.getInterval() - 10);
                sendIdentifyPacket();
                break;
            }
            case IdentifySuccess: {
                IdentifySuccessPacket identifySuccessPacket = gson.fromJson(message, IdentifySuccessPacket.class);
                setIdentifySuccessPacket(identifySuccessPacket);
                logger.info("身份验证成功! 服务器名: " + identifySuccessPacket.getServerName());
                logger.info("已连接到主程序!");
                startUpdateSyncSettings();
                startHeartbeat();
                ready = true;

                BridgeOnlineAsyncEvent online = new BridgeOnlineAsyncEvent(this);
                online.setIdentify(identifySuccessPacket);
                online.setHostInfo(helloPacket);
                eventManager.pushAsync(online);
                break;
            }
            case Packet: {
                handlePacket(message);
                break;
            }
            case CallBack: {
                PacketWithCallBackId packetWithCallBackId = gson.fromJson(message, PacketWithCallBackId.class);
                if (packetWithCallBackId.getCallBackId() != null) {
                    CompletableFuture<String> future = callbackTasks.remove(packetWithCallBackId.getCallBackId());
                    if (future != null) {
                        future.complete(message);
                    }
                }
                break;
            }
            case HeartBeat: {
                break;
            }
            default: {
                logger.info("收到未知 OpCode: " + packet.getOpCode());
            }
        }
    }

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        logger.info("连接关闭: " + reason + " (code: " + statusCode + ")");
        resetConnectionStates(); // 统一清理状态

        try {
            if (heartbeatScheduler != null) {
                heartbeatScheduler.shutdownNow();
            }
        } catch (Exception ignored) {
        }

        if (isShutdown) {
            return;
        }

        BridgeDisconnectedAsyncEvent event = new BridgeDisconnectedAsyncEvent(this);
        event.setReason(reason);
        // 如果 session 还没建立就失败了，这里可能 NPE，加个判断
        if (session != null && session.getUpgradeRequest() != null) {
            event.setHost(session.getUpgradeRequest().getRequestURI().toString());
        } else {
            event.setHost(uri);
        }
        eventManager.pushAsync(event);

        reconnect();
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        if (isShutdown) return;

        logger.error("连接遇到错误: " + cause.getMessage()); // 简化日志输出
        resetConnectionStates();

        try {
            if (heartbeatScheduler != null) {
                heartbeatScheduler.shutdownNow();
            }
        } catch (Exception ignored) {
        }
        reconnect();
    }

    private void resetConnectionStates() {
        synchronized (connectionLock) {
            isConnected = false;
            isConnecting = false;
            ready = false;
        }
    }

    /* -------------------- 业务方法 -------------------- */

    private void send(Object packet) {
        if (isShutdown) return;
        try {
            String body = perSend(packet);
            Session s = this.session;
            if (s != null && s.isOpen()) {
                s.getRemote().sendStringByFuture(body);
            } else {
                // 如果是心跳包，且连接不可用，不打印warn，防止刷屏
                if (!(packet instanceof HeartbeatPacket)) {
                    logger.warn("尝试发送消息但 session 不可用，消息被丢弃");
                }
            }
        } catch (Exception e) {
            if (!isShutdown) {
                logger.error("发送消息失败: " + e.getMessage());
            }
        }
    }

    private void startHeartbeat() {
        if (isShutdown) return;
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdownNow();
        }
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            try {
                if (isShutdown) {
                    throw new RuntimeException("Shutting down");
                }
                if (session != null && session.isOpen()) {
                    send(new HeartbeatPacket());
                }
            } catch (Throwable ignored) {
            }
        }, 0, getHeartbeatInterval(), TimeUnit.SECONDS);
    }

    private void handlePacket(String message) {
        if (isShutdown) return;

        Gson gson = getGson();
        PacketWithCallBackId packet = gson.fromJson(message, PacketWithCallBackId.class);
        JsonObject callBack = new JsonObject();
        callBack.addProperty("op", OpCode.CallBack.getValue());
        callBack.addProperty("callback_id", packet.getCallBackId());
        callBack.addProperty("exec_op", packet.getOperation());

        JsonObject body = gson.fromJson(message, JsonObject.class);
        RawExecPacketEvent rawPacketEvent = new RawExecPacketEvent(this);
        rawPacketEvent.setPacket(packet);
        rawPacketEvent.setBody(body);
        rawPacketEvent.setCallBack(callBack);
        eventManager.push(rawPacketEvent);

        if (!rawPacketEvent.isSkipInternalProcessing()) {
            try {
                switch (packet.getOperation()) {
                    case "GET_SERVER_INFO": {
                        ServerInfo info = behavior.getInfo();
                        GsonUtils.merge(gson, callBack, info);
                        break;
                    }
                    case "UN_BIND_NOTIFY": {
                        PlayerUnBindNotifyPacket unBindNotifyPacket = gson.fromJson(message, PlayerUnBindNotifyPacket.class);
                        behavior.KickPlayer(unBindNotifyPacket.getPlayerName(), unBindNotifyPacket.getKickMessage());
                        break;
                    }
                    case "BIND_SUCCESS_NOTIFY": {
                        BindSuccessNotifyPacket bindSuccessNotifyPacket = gson.fromJson(message, BindSuccessNotifyPacket.class);
                        behavior.BindSuccessBroadcast(bindSuccessNotifyPacket.getPlayerName(), bindSuccessNotifyPacket.getAccountId(), bindSuccessNotifyPacket.getAccountName());
                        break;
                    }
                    case "PLACEHOLDER_API_QUERY": {
                        PlaceholderApiQueryPacket placeholderApiQueryPacket = gson.fromJson(message, PlaceholderApiQueryPacket.class);
                        PlaceholderApiQueryResultPacket papiQueryResultPacket = new PlaceholderApiQueryResultPacket();
                        try {
                            String papiQueryResult = behavior.papiQuery(placeholderApiQueryPacket.getPlayerName(), placeholderApiQueryPacket.getText());
                            papiQueryResultPacket.setSuccess(true);
                            papiQueryResultPacket.setText(papiQueryResult);
                        } catch (Exception ex) {
                            papiQueryResultPacket.setSuccess(false);
                            papiQueryResultPacket.setText(ex.getLocalizedMessage());
                            logger.error("执行Papi查询命令失败: " + ex);
                        }
                        GsonUtils.merge(gson, callBack, papiQueryResultPacket);
                        break;
                    }
                    case "RUN_COMMAND": {
                        RunCommandPacket runCommandPacket = gson.fromJson(message, RunCommandPacket.class);
                        RunCommandResultPacket runCommandResultPacket = new RunCommandResultPacket();
                        try {
                            String runCommandResult = behavior.runCommand(runCommandPacket.getPlayerName(), runCommandPacket.getCommand(), runCommandPacket.isEnablePapi());
                            runCommandResultPacket.setSuccess(true);
                            runCommandResultPacket.setText(runCommandResult);
                        } catch (Exception ex) {
                            runCommandResultPacket.setSuccess(false);
                            runCommandResultPacket.setText(ex.getLocalizedMessage());
                            logger.error("执行命令失败: " + ex);
                        }
                        GsonUtils.merge(gson, callBack, runCommandResultPacket);
                        break;
                    }
                    case "SEND_TO_CHAT": {
                        SendToChatOldPacket sendToChatPacket = gson.fromJson(message, SendToChatOldPacket.class);
                        JsonObject sendToChatPacketRaw = gson.fromJson(message, JsonObject.class);
                        JsonElement extra = sendToChatPacketRaw.get("extra");
                        if (extra == null || extra.isJsonNull()) {
                            behavior.SyncToChat(sendToChatPacket.getText());
                            break;
                        }
                        SendToChatPacket sendToChatPacketNew = gson.fromJson(message, SendToChatPacket.class);
                        List<Segment> segments = StreamSupport.stream(sendToChatPacketNew.getExtra().getAsJsonArray().spliterator(), false).map(JsonElement::getAsJsonObject).map(extraObject -> {
                            SegmentType extraType = SegmentType.getSegmentType(extraObject.get("type").getAsInt());
                            if (extraType == null) return null;
                            Class<? extends Segment> segmentClass = getSegmentClass(extraType);
                            return segmentClass != null ? gson.fromJson(extraObject, segmentClass) : null;
                        }).filter(Objects::nonNull).collect(Collectors.toList());
                        behavior.SyncToChatExtra(segments, sendToChatPacket.getText());
                        break;
                    }
                    case "SYNC_SETTINGS_UPDATED": {
                        UpdateSyncSettingsPacket updateSyncSettingsPacket = gson.fromJson(message, UpdateSyncSettingsPacket.class);
                        ClientProfile.setSyncMessageMoney(updateSyncSettingsPacket.getSyncMoney());
                        ClientProfile.setSyncMessageMode(updateSyncSettingsPacket.getSyncMode());
                        break;
                    }
                    case "PLAYER_LIST": {
                        PlayerListPacket playerListPacket = new PlayerListPacket();
                        playerListPacket.setList(behavior.getPlayerList());
                        GsonUtils.merge(gson, callBack, playerListPacket);
                        break;
                    }
                    case "RPC_CALL":
                        rpcExecutor.execute(() -> {
                            if (isShutdown) return;
                            RpcCallPacket rpcCallPacket = gson.fromJson(message, RpcCallPacket.class);
                            RpcContext context = new RpcContext(this, rpcCallPacket.getBody());
                            try {
                                context = rpcManager.call(rpcCallPacket.getIdentifier(), rpcCallPacket.getMethod(), context);
                                context.getError().addProperty("error", false);
                            } catch (Exception ex) {
                                logger.error("调用RPC方法失败: " + ex.getLocalizedMessage());
                                context.getError().addProperty("error", true);
                                context.getError().addProperty("error_message", ex.getLocalizedMessage());
                            }

                            if (context.getError().get("error").getAsBoolean()) {
                                callBack.addProperty("error", true);
                                callBack.addProperty("error_message", context.getError().get("error_message").getAsString());
                            } else {
                                callBack.addProperty("error", false);
                                callBack.addProperty("error_message", "");
                            }
                            callBack.add("result", context.getResult());
                            send(callBack);
                        });
                        break;
                    case "GET_EXTENSIONS":
                        JsonObject extensions = new JsonObject();
                        Set<IBridgeExtension> listeners = new HashSet<>();
                        HashMap<IBridgeExtension, List<IRpcListener>> extensionListenersMap = rpcManager.getRpcListeners();
                        listeners.addAll(eventManager.getExtensions().collect(Collectors.toList()));
                        listeners.addAll(extensionListenersMap.keySet());

                        for (IBridgeExtension extensionInstance : listeners) {
                            JsonObject extension = new JsonObject();
                            extension.addProperty("name", extensionInstance.getName());
                            extension.addProperty("description", extensionInstance.getDescription());
                            extension.addProperty("author", extensionInstance.getAuthor());
                            extension.addProperty("version", extensionInstance.getVersion());
                            extension.add("required_plugins", gson.toJsonTree(extensionInstance.requiredPlugins()));
                            extension.addProperty("identifier", extensionInstance.getIdentifier());
                            if (extensionListenersMap.containsKey(extensionInstance)) {
                                JsonObject rpc = new JsonObject();
                                List<IRpcListener> listenerList = extensionListenersMap.get(extensionInstance);
                                for (IRpcListener listener : listenerList) {
                                    for (Method rpcFun : Arrays.stream(listener.getClass().getMethods()).filter(method -> method.isAnnotationPresent(BridgeRpc.class)).collect(Collectors.toList())) {
                                        JsonObject rpcMethod = new JsonObject();
                                        BridgeRpc rpcAnnotation = rpcFun.getAnnotation(BridgeRpc.class);
                                        rpcMethod.addProperty("identifier", extensionInstance.getIdentifier());
                                        rpcMethod.addProperty("method", rpcAnnotation.method());
                                        rpcMethod.addProperty("description", rpcAnnotation.description());
                                        rpcMethod.addProperty("displayName", rpcAnnotation.displayName());
                                        rpcMethod.addProperty("fullMethodClassName", rpcFun.getDeclaringClass().getName() + "." + rpcFun.getName());
                                        rpc.add(rpcAnnotation.method(), rpcMethod);
                                    }
                                }
                                extension.add("rpc", rpc);
                            }
                            extensions.add(extensionInstance.getIdentifier(), extension);
                        }
                        callBack.add("extensions", extensions);
                        break;
                    default: {
                        logger.info("收到未知操作: " + packet.getOperation() + " 请确保你的插件是最新版本????");
                        break;
                    }
                }
            } catch (Exception e) {
                logger.error("处理 packet 时发生异常: " + e);
            }
        }

        if (!Objects.equals(packet.getOperation(), "RPC_CALL")) send(callBack);
    }

    private void sendIdentifyPacket() {
        if (isShutdown) return;
        IdentifyPacket packet = new IdentifyPacket(getToken());
        packet.setPluginVersion(ClientProfile.getPluginVersion());
        packet.setServerDescription(ClientProfile.getServerDescription());
        send(packet);
    }

    @SuppressWarnings("unused")
    public PlayerLoginResultPacket login(String playerName, String playerUuid) throws ExecutionException, InterruptedException {
        OnPlayerJoinPacket packet = new OnPlayerJoinPacket();
        PlayerInfo playerInfo = new PlayerInfo();
        playerInfo.setPlayerName(playerName);
        playerInfo.setPlayerUuid(playerUuid);
        packet.setPlayerInfo(playerInfo);
        return sendAndWaitForCallbackAsync(packet, PlayerLoginResultPacket.class).get();
    }

    @SuppressWarnings("unused")
    public void reportPlayer(String playerName, String playerUuid, String playerIp) {
        ReportPlayerPacket packet = new ReportPlayerPacket();
        packet.setPlayerName(playerName);
        packet.setPlayerUuid(playerUuid);
        packet.setPlayerIp(playerIp);
        packet.setCallBackId("");
        send(packet);
    }

    @SuppressWarnings("unused")
    public void serverState(String players) {
        ServerStatePacket packet = new ServerStatePacket();
        packet.setToken(getToken());
        packet.setPlayers(players);
        packet.setCallBackId("");
        send(packet);
    }

    @SuppressWarnings("unused")
    public void dataRecord(RecordTypeEnum type, String data, String name) {
        DataRecordPacket packet = new DataRecordPacket();
        packet.setType(type);
        packet.setData(data);
        packet.setName(name);
        packet.setToken(getToken());
        packet.setCallBackId("");
        send(packet);
    }

    @SuppressWarnings("unused")
    public StartBindResultPacket startBind(String playerName) throws ExecutionException, InterruptedException {
        StartBindPacket packet = new StartBindPacket();
        packet.setPlayerName(playerName);
        return sendAndWaitForCallbackAsync(packet, StartBindResultPacket.class).get();
    }

    @SuppressWarnings("unused")
    public GetSocialAccountResultPacket getSocialAccount(String playerName) throws ExecutionException, InterruptedException {
        GetSocialAccountPacket packet = new GetSocialAccountPacket();
        packet.setPlayerName(playerName);
        return sendAndWaitForCallbackAsync(packet, GetSocialAccountResultPacket.class).get();
    }

    @SuppressWarnings("unused")
    public GetNewVersionResultPacket getNewVersion() throws ExecutionException, InterruptedException {
        return sendAndWaitForCallbackAsync(new GetNewVersionPacket(), GetNewVersionResultPacket.class).get();
    }

    @SuppressWarnings("unused")
    public GetBindInfoResultPacket getBindInfo(String playerName) throws ExecutionException, InterruptedException {
        GetBindInfoPacket packet = new GetBindInfoPacket();
        packet.setPlayerName(playerName);
        return sendAndWaitForCallbackAsync(packet, GetBindInfoResultPacket.class).get();
    }

    @SuppressWarnings("unused")
    public void syncMessage(PlayerInfoWithRaw playerInfo, String message, boolean useCommand) {
        SyncMessagePacket packet = new SyncMessagePacket();
        packet.setPlayer(playerInfo);
        packet.setMessage(message);
        packet.setUseCommand(useCommand);
        packet.setCallBackId("");
        send(packet);
    }

    @SuppressWarnings("unused")
    public void syncDeathMessage(PlayerInfoWithRaw playerInfo, String killMessage, String killer) {
        SyncDeathMessagePacket packet = new SyncDeathMessagePacket();
        packet.setPlayer(playerInfo);
        packet.setRaw(killMessage);
        packet.setKiller(killer);
        packet.setCallBackId("");
        send(packet);
    }

    @SuppressWarnings("unused")
    public void syncEnterExit(PlayerInfoWithRaw playerInfo, boolean isEnter) {
        SyncEnterExitMessagePacket packet = new SyncEnterExitMessagePacket();
        packet.setPlayer(playerInfo);
        packet.setEnter(isEnter);
        packet.setCallBackId("");
        send(packet);
    }

    @SuppressWarnings("unused")
    public GetInstalledPluginResultPacket getInstalledPlugin() {
        if (!identifySuccessPacket.isSupportGetPluginList()) throw new RuntimeException("您的EasyBot版本过旧,请升级到dev11及以上版本!");
        GetInstalledPluginPacket packet = new GetInstalledPluginPacket();
        packet.setCallBackId("");
        packet.setOperation("INSTALLED_PLUGIN");
        return sendAndWaitForCallbackAsync(packet, GetInstalledPluginResultPacket.class).join();
    }

    @SuppressWarnings("unused")
    public JsonObject rpcCall(String identifier, String method, JsonObject body) {
        RpcCallPacket packet = new RpcCallPacket();
        packet.setIdentifier(identifier);
        packet.setMethod(method);
        packet.setBody(body);
        packet.setOperation("RPC_CALL");
        JsonObject result = sendAndWaitForCallbackAsync(packet, JsonObject.class).join();
        if (result.get("error").getAsBoolean()) {
            throw new RuntimeException("执行" + identifier + " 方法 " + method + "方法时发生错误: " + result.get("error_message").getAsString());
        }
        return result.get("result").getAsJsonObject();
    }

    @SuppressWarnings("unused")
    public <T> T rpcCall(String identifier, String method, JsonObject body, Class<T> responseType) {
        JsonObject resp = rpcCall(identifier, method, body);
        return getGson().fromJson(resp, responseType);
    }

    /* -------------------- 连接管理 -------------------- */

    private void connect() {
        if (isShutdown) return;
        synchronized (connectionLock) {
            if (isConnected || isConnecting) {
                return;
            }
            isConnecting = true;
        }
        executor.submit(() -> {
            if (isShutdown) {
                synchronized (connectionLock) {
                    isConnecting = false;
                }
                return;
            }

            try {
                if (!client.isStarted()) {
                    client.start();
                }
                logger.info("正在连接到服务器: " + uri);
                URI echoUri = new URI(uri);
                ClientUpgradeRequest request = new ClientUpgradeRequest();
                client.connect(this, echoUri, request);
                // 注意：这里不要设置 isConnected=true，必须等 onWebSocketConnect 回调
            } catch (Exception e) {
                if (!isShutdown) {
                    logger.error("连接发起失败: " + e.getMessage());
                    synchronized (connectionLock) {
                        isConnecting = false; // 失败了，清除正在连接状态
                    }
                    reconnect();
                }
            }
        });
    }

    public void stop() {
        try {
            client.stop();
        } catch (Exception e) {
            logger.error("停止失败: " + e.getMessage());
        }
    }

    public void reconnect() {
        if (isShutdown) return;

        executor.submit(() -> {
            try {
                if (isShutdown) return;

                TimeUnit.SECONDS.sleep(5);
                // 此时如果其他线程已经触发了连接，或者已经连接上，就不再执行 connect
                synchronized (connectionLock) {
                    if (isShutdown || isConnected || isConnecting) {
                        return;
                    }
                }
                logger.warn("正在尝试重连服务器");
                connect();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @SuppressWarnings("unused")
    public void resetUrl(String newUrl) {
        try {
            logger.info("重置URL: " + newUrl);
            if (session != null) {
                session.close();
            }
            this.uri = newUrl;
        } catch (Exception e) {
            logger.error("重置URL失败: " + e.getMessage());
        }
    }

    public void startUpdateSyncSettings() {
        NeedSyncSettingsPacket packet = new NeedSyncSettingsPacket();
        packet.setCallBackId("");
        send(packet);
    }

    public void close() {
        isShutdown = true;
        synchronized (connectionLock) {
            isConnected = false;
            isConnecting = false;
        }
        ready = false;

        logger.info("BridgeClient 正在关闭...");
        try {
            if (heartbeatScheduler != null) {
                heartbeatScheduler.shutdownNow();
            }
        } catch (Exception ignored) {
        }

        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } catch (Exception e) {
            logger.error("关闭 Session 失败: " + e.getMessage());
        }

        try {
            if (client.isStarted()) {
                client.stop();
            }
        } catch (Exception e) {
            logger.error("停止 WebSocketClient 失败: " + e.getMessage());
        }
        try {
            timeoutScheduler.shutdownNow();
            executor.shutdownNow();
            rpcExecutor.shutdownNow();
            callbackTasks.values().forEach(f -> f.completeExceptionally(new CancellationException("Client closed")));
            callbackTasks.clear();
        } catch (Exception e) {
            logger.error("关闭线程池失败: " + e.getMessage());
        }

        logger.info("BridgeClient 关闭完成。");
    }
}