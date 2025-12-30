package com.springwater.easybot.bridge.extension;

import com.google.gson.JsonObject;
import com.springwater.easybot.bridge.BridgeClient;
import com.springwater.easybot.bridge.api.IBridgeExtension;
import com.springwater.easybot.bridge.api.rpc.BridgeRpc;
import com.springwater.easybot.bridge.api.rpc.IBridgeRpcManager;
import com.springwater.easybot.bridge.api.rpc.IRpcListener;
import com.springwater.easybot.bridge.api.rpc.RpcContext;
import lombok.Getter;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class BridgeRpcManager implements IBridgeRpcManager {
    @SuppressWarnings("ClassCanBeRecord") // 需要兼容 Java 8
    private static class RpcMethodHandler {
        @Getter
        private final IRpcListener listenerInstance;
        @Getter
        private final Method method;
        @Getter
        private final BridgeRpc annotation;

        public RpcMethodHandler(IRpcListener listenerInstance, Method method, BridgeRpc annotation) {
            this.listenerInstance = listenerInstance;
            this.method = method;
            this.annotation = annotation;
        }
    }

    private final Map<String, Map<String, RpcMethodHandler>> rpcRegistry = new ConcurrentHashMap<>();

    private final Map<IBridgeExtension, List<IRpcListener>> extensionListenersMap = new ConcurrentHashMap<>();

    @Override
    public void registerRpc(IBridgeExtension instance, IRpcListener listener) {
        if (instance == null) return;

        try {
            String extensionId = instance.getIdentifier();
            int registeredCount = 0;
            for (Method method : listener.getClass().getMethods()) {
                if (method.isAnnotationPresent(BridgeRpc.class)) {
                    if (validateAndRegister(instance, extensionId, listener, method)) {
                        registeredCount++;
                    }
                }
            }

            if (registeredCount > 0) {
                extensionListenersMap.computeIfAbsent(instance, k -> new CopyOnWriteArrayList<>()).add(listener);
                BridgeClient.getLogger().info("扩展 [" + extensionId + "] 注册了 " + registeredCount + " 个 RPC 接口");
            }

        } catch (Exception e) {
            BridgeClient.getLogger().error("注册 RPC 监听器失败: " + listener.getClass().getName());
            BridgeClient.getLogger().error(e.toString());
        }
    }

    private boolean validateAndRegister(IBridgeExtension extension, String extensionId, IRpcListener listener, Method method) {
        BridgeRpc annotation = method.getAnnotation(BridgeRpc.class);
        Parameter[] params = method.getParameters();

        if (params.length != 1 || !RpcContext.class.isAssignableFrom(params[0].getType())) {
            BridgeClient.getLogger().warn("RPC 方法 " + method.getName() + " 签名不符合要求 (必须仅包含 RpcContext 参数)，已跳过。");
            return false;
        }

        String rpcMethodName = annotation.method();
        if (rpcMethodName.isEmpty()) {
            BridgeClient.getLogger().warn("RPC 方法 " + method.getName() + " 的注解 method 属性为空，已跳过。");
            return false;
        }

        rpcRegistry.computeIfAbsent(extensionId, k -> new ConcurrentHashMap<>())
                .put(rpcMethodName, new RpcMethodHandler(listener, method, annotation));

        return true;
    }

    @Override
    public void unregisterRpc(IBridgeExtension instance) {
        if (instance == null) return;
        String extensionId = instance.getIdentifier();
        if (rpcRegistry.remove(extensionId) != null) {
            BridgeClient.getLogger().info("已注销扩展 [" + extensionId + "] 的所有 RPC 接口");
        }
        extensionListenersMap.remove(instance);
    }

    @Override
    public HashMap<IBridgeExtension, List<IRpcListener>> getRpcListeners() {
        return new HashMap<>(extensionListenersMap);
    }

    public RpcContext call(String identifier, String method, RpcContext context) {
        if (identifier == null || method == null) {
            return setContextError(context, "参数错误: identifier或method是null");
        }

        Map<String, RpcMethodHandler> extensionMethods = rpcRegistry.get(identifier);
        if (extensionMethods == null) {
            return setContextError(context, "扩展不存在: " + identifier);
        }

        RpcMethodHandler handler = extensionMethods.get(method);
        if (handler == null) {
            return setContextError(context, "扩展" + identifier + "中不存在方法: " + method);
        }

        try {
            handler.getMethod().invoke(handler.getListenerInstance(), context);
            return context;
        } catch (Throwable e) {
            BridgeClient.getLogger().error("执行 RPC [" + identifier + ":" + method + "] 时发生异常");
            BridgeClient.getLogger().error(e.toString());
            return setContextError(context, "内部错误: " + e.getLocalizedMessage());
        }
    }

    private RpcContext setContextError(RpcContext context, String errorMessage) {
        context.getError().addProperty("error", true);
        context.getError().addProperty("error_message", errorMessage);
        return context;
    }
}