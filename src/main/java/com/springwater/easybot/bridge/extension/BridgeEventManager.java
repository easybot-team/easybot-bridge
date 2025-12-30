package com.springwater.easybot.bridge.extension;

import com.springwater.easybot.bridge.BridgeClient;
import com.springwater.easybot.bridge.api.BridgeEventHandler;
import com.springwater.easybot.bridge.api.BridgeHandlerPriority;
import com.springwater.easybot.bridge.api.IBridgeExtension;
import com.springwater.easybot.bridge.api.IBridgeExtensionApi;
import com.springwater.easybot.bridge.api.IBridgeListener;
import com.springwater.easybot.bridge.api.events.BridgeEvent;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BridgeEventManager implements IBridgeExtensionApi {

    private final ExecutorService asyncExecutor;

    @SuppressWarnings("ClassCanBeRecord") // 需要兼容 Java 8
    private static class RegisteredHandler implements Comparable<RegisteredHandler> {
        @Getter
        private final IBridgeExtension extension;
        @Getter
        private final IBridgeListener listener;
        @Getter
        private final Method method;
        private final BridgeHandlerPriority priority;

        public RegisteredHandler(IBridgeExtension extension, IBridgeListener listener, Method method, BridgeHandlerPriority priority) {
            this.extension = extension;
            this.listener = listener;
            this.method = method;
            this.priority = priority;
        }

        @Override
        public int compareTo(RegisteredHandler other) {
            return Integer.compare(other.priority.ordinal(), this.priority.ordinal());
        }
    }

    private final Map<Class<? extends BridgeEvent>, List<RegisteredHandler>> handlersMap = new ConcurrentHashMap<>();

    public BridgeEventManager() {
        this.asyncExecutor = new ThreadPoolExecutor(
                0, Integer.MAX_VALUE,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<>(),
                new ThreadFactory() {
                    private final AtomicInteger count = new AtomicInteger(1);
                    @Override
                    public Thread newThread(@NotNull Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("BridgeEvent-Async-" + count.getAndIncrement());
                        thread.setDaemon(true);
                        return thread;
                    }
                }
        );
    }

    @Override
    public void register(IBridgeExtension instance, IBridgeListener listener) {
        if (instance == null || listener == null) {
            BridgeClient.getLogger().warn("注册失败: 扩展实例或监听器为空");
            return;
        }

        List<Method> methods = Arrays.stream(listener.getClass().getMethods())
                .filter(x -> x.isAnnotationPresent(BridgeEventHandler.class))
                .collect(Collectors.toList());

        if (methods.isEmpty()) {
            BridgeClient.getLogger().warn("注册警告: 事件处理器中没有任何处理函数 " + listener.getClass().getName());
            return;
        }

        String id = listener.getClass().getName() + "#" + Integer.toHexString(listener.hashCode());
        BridgeClient.getLogger().info("注册事件处理器: " + id + " (来源: " + instance.getClass().getSimpleName() + ")");

        for (Method method : methods) {
            Parameter[] pars = method.getParameters();
            if (pars.length != 1) {
                BridgeClient.getLogger().warn("事件处理器 " + id + " 的处理函数 " + method.getName() + " 参数数量不正确 (必须为1个)，已跳过。");
                continue;
            }

            Class<?> paramType = pars[0].getType();
            if (BridgeEvent.class.isAssignableFrom(paramType)) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends BridgeEvent> eventClass = (Class<? extends BridgeEvent>) paramType;
                    BridgeEventHandler annotation = method.getAnnotation(BridgeEventHandler.class);
                    RegisteredHandler newHandler = new RegisteredHandler(instance, listener, method, annotation.priority());
                    handlersMap.compute(eventClass, (k, v) -> {
                        List<RegisteredHandler> list = (v == null) ? new CopyOnWriteArrayList<>() : v;
                        list.add(newHandler);
                        ArrayList<RegisteredHandler> sorted = new ArrayList<>(list);
                        Collections.sort(sorted);
                        return new CopyOnWriteArrayList<>(sorted);
                    });

                } catch (ClassCastException e) {
                    BridgeClient.getLogger().warn("类型转换错误: " + method.getName());
                }
            } else {
                BridgeClient.getLogger().warn("事件处理器 " + id + " 的处理函数 " + method.getName() + " 监听了一个非 BridgeEvent 类型: " + paramType.getName());
            }
        }
    }

    @Override
    public void unregister(IBridgeListener listener) {
        if (listener == null) return;
        handlersMap.values().forEach(list -> {
            list.removeIf(handler -> handler.getListener().equals(listener));
        });
        BridgeClient.getLogger().info("已注销事件处理器: " + listener.getClass().getName());
    }

    @Override
    public Stream<IBridgeExtension> getExtensions() {
        return handlersMap.values().stream()
                .flatMap(List::stream)
                .map(RegisteredHandler::getExtension)
                .distinct();
    }

    public void push(BridgeEvent event) {
        if (event == null) return;
        List<RegisteredHandler> handlersToExecute = new ArrayList<>();
        for (Map.Entry<Class<? extends BridgeEvent>, List<RegisteredHandler>> entry : handlersMap.entrySet()) {
            if (entry.getKey().isAssignableFrom(event.getClass())) {
                handlersToExecute.addAll(entry.getValue());
            }
        }

        if (handlersToExecute.isEmpty()) {
            return;
        }

        handlersToExecute.sort(Comparator.naturalOrder());

        for (RegisteredHandler handler : handlersToExecute) {
            try {
                if (event.isCancelled() && !event.isContinuePropagation()) {
                    break;
                }

                handler.getMethod().invoke(handler.getListener(), event);

            } catch (Throwable e) {
                BridgeClient.getLogger().error("处理事件 " + event.getClass().getSimpleName() + " 时发生异常: " +
                        handler.getListener().getClass().getName() + "#" + handler.getMethod().getName());
                if (e instanceof InvocationTargetException && e.getCause() != null) {
                    BridgeClient.getLogger().error(e.getCause().toString());
                } else {
                    BridgeClient.getLogger().error(e.toString());
                }
            }
        }
    }

    public CompletableFuture<Void> pushAsync(BridgeEvent event) {
        if (event == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.runAsync(() -> {
            try {
                this.push(event);
            } catch (Exception e) {
                BridgeClient.getLogger().error("异步事件分发严重错误: " + e.getMessage());
                throw e;
            }
        }, asyncExecutor);
    }

    public void shutdown() {
        BridgeClient.getLogger().info("正在关闭事件管理器...");
        handlersMap.clear();
        try {
            asyncExecutor.shutdown();
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}