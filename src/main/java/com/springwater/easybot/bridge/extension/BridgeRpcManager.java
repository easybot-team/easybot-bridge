package com.springwater.easybot.bridge.extension;

import com.google.gson.JsonObject;
import com.springwater.easybot.bridge.BridgeClient;
import com.springwater.easybot.bridge.api.IBridgeExtension;
import com.springwater.easybot.bridge.api.rpc.BridgeRpc;
import com.springwater.easybot.bridge.api.rpc.IBridgeRpcManager;
import com.springwater.easybot.bridge.api.rpc.IRpcListener;
import com.springwater.easybot.bridge.api.rpc.RpcContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class BridgeRpcManager implements IBridgeRpcManager {
    private record RpcMethodHandler(IRpcListener listenerInstance, Method method, BridgeRpc annotation) {
    }

    // Identifier -> (RpcMethodName -> Handler)
    private final Map<String, Map<String, RpcMethodHandler>> rpcRegistry = new ConcurrentHashMap<>();

    // Extension -> List<IRpcListener>，用于 getRpcListeners 和 unregister
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

        // 校验条件：有且只有一个参数，且参数类型为 RpcContext
        if (params.length != 1 || !RpcContext.class.isAssignableFrom(params[0].getType())) {
            BridgeClient.getLogger().warn("RPC 方法 " + method.getName() + " 签名不符合要求 (必须仅包含 RpcContext 参数)，已跳过。");
            return false;
        }

        String rpcMethodName = annotation.method();
        if (rpcMethodName.isEmpty()) {
            BridgeClient.getLogger().warn("RPC 方法 " + method.getName() + " 的注解 method 属性为空，已跳过。");
            return false;
        }

        // 注册到 registry
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
        // 返回副本以保护内部数据
        return new HashMap<>(extensionListenersMap);
    }

    /**
     * 执行 RPC 调用
     *
     * @param identifier 扩展的标识符 (对应 register 时的 ID)
     * @param method     RPC 方法名 (对应 @BridgeRpc 的 method 属性)
     * @param context    上下文
     * @return 处理后的上下文
     */
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
            // 反射调用
            handler.method.invoke(handler.listenerInstance, context);
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