package com.springwater.easybot.bridge.extension;

import com.springwater.easybot.bridge.BridgeClient;
import com.springwater.easybot.bridge.api.BridgeEventHandler;
import com.springwater.easybot.bridge.api.BridgeHandlerPriority;
import com.springwater.easybot.bridge.api.IBridgeExtension;
import com.springwater.easybot.bridge.api.IBridgeExtensionApi;
import com.springwater.easybot.bridge.api.IBridgeListener;
import com.springwater.easybot.bridge.api.events.BridgeEvent;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class BridgeEventManager implements IBridgeExtensionApi {

    /**
     * 异步事件执行线程池
     * 使用 CachedThreadPool 策略，自动回收空闲线程
     */
    private final ExecutorService asyncExecutor;

    /**
     * 内部封装类，保存 扩展实例、监听器实例、方法和优先级
     * 实现了 Comparable 以便自动排序 (优先级高的在前)
     */
    private record RegisteredHandler(
            IBridgeExtension extension,
            IBridgeListener listener,
            Method method,
            BridgeHandlerPriority priority
    ) implements Comparable<RegisteredHandler> {
        @Override
        public int compareTo(RegisteredHandler other) {
            // 优先级枚举 ordinal 越大优先级越高，因此使用降序排列
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
                .toList();

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
            // CopyOnWriteArrayList 的 removeIf 是线程安全的
            list.removeIf(handler -> handler.listener().equals(listener));
        });
        BridgeClient.getLogger().info("已注销事件处理器: " + listener.getClass().getName());
    }

    @Override
    public Stream<IBridgeExtension> getExtensions() {
        return handlersMap.values().stream()
                .flatMap(List::stream)
                .map(RegisteredHandler::extension)
                .distinct();
    }

    /**
     * 同步推送事件 (在当前线程执行)
     *
     * @param event 事件实例
     */
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
                // 检查事件取消状态
                if (event.isCancelled() && !event.isContinuePropagation()) {
                    break; // 如果事件已取消且不再继续传播，则停止后续处理
                }

                handler.method().invoke(handler.listener(), event);

            } catch (Throwable e) {
                // 捕获所有异常，防止单个监听器崩溃影响后续监听器
                BridgeClient.getLogger().error("处理事件 " + event.getClass().getSimpleName() + " 时发生异常: " +
                        handler.listener().getClass().getName() + "#" + handler.method().getName());
                // 如果是反射调用异常，打印其 Cause 会更有帮助
                if (e instanceof java.lang.reflect.InvocationTargetException && e.getCause() != null) {
                    BridgeClient.getLogger().error(e.getCause().toString());
                } else {
                    BridgeClient.getLogger().error(e.toString());
                }
            }
        }
    }

    /**
     * 异步推送事件 (在独立线程池中执行)
     *
     * @param event 事件实例
     * @return CompletableFuture 任务句柄
     */
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

    /**
     * 关闭事件管理器及其线程池
     */
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