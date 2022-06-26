/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.netty.bootstrap;

import io.netty.channel.*;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.GlobalEventExecutor;
import io.netty.util.internal.ObjectUtil;
import io.netty.util.internal.SocketUtils;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AbstractBootstrap是一个助手类，它使引导Channel变得很容易。
 * {@link AbstractBootstrap} is a helper class that makes it easy to bootstrap a {@link Channel}.
 *
 * 它支持方法链，提供一个简单的方式来配置bootstrap
 * It support method-chaining to provide an easy way to configure the {@link AbstractBootstrap}.
 *
 * <p>When not used in a {@link ServerBootstrap} context, the {@link #bind()} methods are useful for connectionless
 * transports such as datagram (UDP).</p>
 *
 * 当不在ServerBootstrap上下文中使用时，bind()方法对于无连接非常有用，数据报(datagram，UDP)等传输。
 */

//todo B extends AbstractBootstrap<B, C> 表示, B是AbstractBootstrap的一个子类
//todo C extends Channel 表示, C是Channel 的一个子类
//todo 在ServerBootStrap中， 第一个参数就是 ServerBootstrap extends AbstractBootstrap<ServerBootstrap, ServerChannel> {。。。}
//todo B === ServerBootstrap
public abstract class AbstractBootstrap<B extends AbstractBootstrap<B, C>, C extends Channel> implements Cloneable {

    private static final Map.Entry<ChannelOption<?>, Object>[] EMPTY_OPTION_ARRAY = new Map.Entry[0];
    private static final Map.Entry<AttributeKey<?>, Object>[] EMPTY_ATTRIBUTE_ARRAY = new Map.Entry[0];

    volatile EventLoopGroup group;

    private volatile ChannelFactory<? extends C> channelFactory;
    private volatile SocketAddress localAddress;

    // The order in which ChannelOptions are applied is important they may depend on each other for validation purposes.
    //todo ChannelOptions的应用顺序非常重要，它们可能相互依赖以进行验证。
    private final Map<ChannelOption<?>, Object> options = new LinkedHashMap<ChannelOption<?>, Object>();
    // attributeKey 的作用是什么
    private final Map<AttributeKey<?>, Object> attrs = new ConcurrentHashMap<AttributeKey<?>, Object>();

    private volatile ChannelHandler handler;

    AbstractBootstrap() {
        // Disallow extending from a different package.
        //不允许从不同的包扩展
    }

    AbstractBootstrap(AbstractBootstrap<B, C> bootstrap) {
        group = bootstrap.group;
        channelFactory = bootstrap.channelFactory;
        handler = bootstrap.handler;
        localAddress = bootstrap.localAddress;
        synchronized (bootstrap.options) {
            options.putAll(bootstrap.options);
        }
        attrs.putAll(bootstrap.attrs);
    }

    /**
     * The {@link EventLoopGroup} which is used to handle all the events for the to-be-created
     * {@link Channel}
     */
    //todo 这个事件循环组，被用于处理所有的将被创建的chanel
    public B group(EventLoopGroup group) {
        ObjectUtil.checkNotNull(group, "group");
        if (this.group != null) {
            throw new IllegalStateException("group set already");
        }
        this.group = group;
        return self();
    }

    private B self() {
        return (B) this;
    }

    /**
     * The {@link Class} which is used to create {@link Channel} instances from.
     * You either use this or {@link #channelFactory(io.netty.channel.ChannelFactory)} if your
     * {@link Channel} implementation has no no-args constructor.
     *
     * 设置要被实例化的Channel类
     */
     //todo 返回值类型是B，因为是链式风格的调用，上面分析服务端 B == ServerBootStrap
     //todo 接受的类型是C，上面分析，C是Channel类型的Class对象
     //todo 使用channel()会创建一个ServerBootStrap对象，但是如果我们的B没有无参的构造方法，只能使用channelFactory()
    public B channel(Class<? extends C> channelClass) {
        //todo 我们传递进来的 NioServerSocketChannel对象，赋值给了RegflectiveChannelFactory
        return channelFactory(new ReflectiveChannelFactory<C>(
                ObjectUtil.checkNotNull(channelClass, "channelClass")
        ));
    }

    /**
     *  Use {@link #channelFactory(io.netty.channel.ChannelFactory)} instead.
     */
    //todo 当前方法已经废弃掉了，但是仍然是进来了
    //todo 唯一的工作就是把刚才拥有NioserverSocketChannel.class的 RefletiveChannelFactory初始化了
    @Deprecated
    public B channelFactory(ChannelFactory<? extends C> channelFactory) {
        ObjectUtil.checkNotNull(channelFactory, "channelFactory");
        if (this.channelFactory != null) {
            throw new IllegalStateException("channelFactory set already");
        }
        this.channelFactory = channelFactory;
        return self();
    }


    /**
     * The {@link SocketAddress} which is used to bind the local "end" to.
     */
    public B localAddress(SocketAddress localAddress) {
        this.localAddress = localAddress;
        return self();
    }

    /**
     * @see #localAddress(SocketAddress)
     */
    public B localAddress(int inetPort) {
        return localAddress(new InetSocketAddress(inetPort));
    }

    /**
     * @see #localAddress(SocketAddress)
     */
    public B localAddress(String inetHost, int inetPort) {
        return localAddress(SocketUtils.socketAddress(inetHost, inetPort));
    }

    /**
     * @see #localAddress(SocketAddress)
     */
    public B localAddress(InetAddress inetHost, int inetPort) {
        return localAddress(new InetSocketAddress(inetHost, inetPort));
    }

    /**
     * Allow to specify a {@link ChannelOption} which is used for the {@link Channel} instances once they got created.
     * Use a value of {@code null} to remove a previous set {@link ChannelOption}.
     */
    public <T> B option(ChannelOption<T> option, T value) {
        ObjectUtil.checkNotNull(option, "option");
        synchronized (options) {
            if (value == null) {
                options.remove(option);
            } else {
                options.put(option, value);
            }
        }
        return self();
    }

    /**
     * Allow to specify an initial attribute of the newly created {@link Channel}.  If the {@code value} is
     * {@code null}, the attribute of the specified {@code key} is removed.
     */
    public <T> B attr(AttributeKey<T> key, T value) {
        ObjectUtil.checkNotNull(key, "key");
        if (value == null) {
            attrs.remove(key);
        } else {
            attrs.put(key, value);
        }
        return self();
    }

    /**
     * Validate all the parameters. Sub-classes may override this, but should
     * call the super method in that case.
     */
    public B validate() {
        if (group == null) {
            throw new IllegalStateException("group not set");
        }
        if (channelFactory == null) {
            throw new IllegalStateException("channel or channelFactory not set");
        }
        return self();
    }

    /**
     * Returns a deep clone of this bootstrap which has the identical configuration.  This method is useful when making
     * multiple {@link Channel}s with similar settings.  Please note that this method does not clone the
     * {@link EventLoopGroup} deeply but shallowly, making the group a shared resource.
     */
    @Override
    @SuppressWarnings("CloneDoesntDeclareCloneNotSupportedException")
    public abstract B clone();

    /**
     * Create a new {@link Channel} and register it with an {@link EventLoop}.
     */
    public ChannelFuture register() {
        validate();
        return initAndRegister();
    }

    /**
     * Create a new {@link Channel} and bind it.
     */
    public ChannelFuture bind() {
        validate();
        SocketAddress localAddress = this.localAddress;
        if (localAddress == null) {
            throw new IllegalStateException("localAddress not set");
        }
        return doBind(localAddress);
    }

    /**
     * Create a new {@link Channel} and bind it.
     */
    //todo 启动，创建一个服务端的Channel并且绑定一个端口号
    public ChannelFuture bind(int inetPort) {
        return bind(new InetSocketAddress(inetPort));
    }

    /**
     * Create a new {@link Channel} and bind it.
     */
    public ChannelFuture bind(String inetHost, int inetPort) {
        return bind(SocketUtils.socketAddress(inetHost, inetPort));
    }

    /**
     * Create a new {@link Channel} and bind it.
     */
    public ChannelFuture bind(InetAddress inetHost, int inetPort) {
        return bind(new InetSocketAddress(inetHost, inetPort));
    }

    /**
     * Create a new {@link Channel} and bind it.
     * 创建一个channel并且绑定
     */
    public ChannelFuture bind(SocketAddress localAddress) {
        validate();
        //todo 执行doBind()，以这个命名的方法表示netty私有的方法，跟进去
        return doBind(ObjectUtil.checkNotNull(localAddress, "localAddress"));
    }

    /**
     * todo 特别重要!!!
     * @param localAddress
     * @return
     */
    private ChannelFuture doBind(final SocketAddress localAddress) {
        //初始化并注册一个 Channel 对象，因为注册是异步的过程，所以返回一个 ChannelFuture 对象。
        //todo 初始化并注册Channel对象，带Future字眼的表示异步!!!它本身返回的就是一个ChannelFuture
        final ChannelFuture regFuture = initAndRegister();

        final Channel channel = regFuture.channel();
        if (regFuture.cause() != null) {
            return regFuture;
        }

        //不能肯定register完成，因为register是丢到nio event loop里面执行去了。
        if (regFuture.isDone()) {
            // At this point we know that the registration was complete and successful.
            ChannelPromise promise = channel.newPromise();
            // TODO  继续绑定端口 doBind0
            doBind0(regFuture, channel, localAddress, promise);
            return promise;
        } else {
            // Registration future is almost always fulfilled already, but just in case it's not.
            // future的注册几乎总是已经实现，但以防万一还没有完成。 // future没有完成
            final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
            //等着register完成来通知再执行bind
            regFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    Throwable cause = future.cause();
                    if (cause != null) {
                        // Registration on the EventLoop failed so fail the ChannelPromise directly to not cause an
                        // IllegalStateException once we try to access the EventLoop of the Channel.
                        promise.setFailure(cause);
                    } else {
                        // Registration was successful, so set the correct executor to use.
                        // See https://github.com/netty/netty/issues/2586
                        promise.registered();

                        doBind0(regFuture, channel, localAddress, promise);
                    }
                }
            });
            return promise;
        }
    }

    //todo 初始化和注册
    final ChannelFuture initAndRegister() {
        Channel channel = null;
        try {
            // 通过工厂创建一个 channel
            //todo 这个channelFactory是反射工厂ReflectiveChannelFactory对服务端来说，可以创建NioServerSocketChannel对象
            //todo 而这个对象又是Selector的一种实现，就是SelectorProvider.providor()方法
            //todo 实例化NioServerSocketChannel，通过反射走的是无参的构造，我们去追踪它的无参构造去
            channel = channelFactory.newChannel();

            //todo 初始化Channel，好几轮赋值以及添加handler等组件
            init(channel);
        } catch (Throwable t) {
            //channel 创建报错
            if (channel != null) {
                // channel can be null if newChannel crashed (eg SocketException("too many open files"))
                channel.unsafe().closeForcibly();

                // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
                return new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE).setFailure(t);
            }
            // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
            return new DefaultChannelPromise(new FailedChannel(), GlobalEventExecutor.INSTANCE).setFailure(t);
        }

        //todo  注册 group  == BOSS EventLoopGroup , -- > 暂时以为, 他是想确保把通过反射创建出来的NioServerSocketChannel注册进 BossGroup
        //todo  目的是,让通过这个NioServerSocketChannel中的ServerSocketChannel 去 accept客户端的连接, 进而把连接通过Acceptor 扔给 WorkerGroup
        //todo   config()--> ServerBootstrapConfig
        //todo   group()--> NioEventLoopGroup -- workerGroup
        //todo   我们用户点进去  进入 EventLoopGroup. 而 Debug 进入的是 MultithreadEventLoopGroup类 , 因为我这里的是 NioEventLoopGroup 是 MultithreadEventLoopGroup类的子类
        //todo  !!! 忽略的一个重点, group是 MultithreadEventLoopGroup类  我们知道这个类中维护的是 BossGroup, 即将channel注册进bossgroup中
        ChannelFuture regFuture = config().group().register(channel);
        if (regFuture.cause() != null) {//todo 非空表示注册失败了
            if (channel.isRegistered()) {
                channel.close();
            } else {
                channel.unsafe().closeForcibly();
            }
        }
        //todo 如果我们期待的结果并没有失败, 就会出现下面几种情况
        // 如果我们在这里，并且承诺没有失败，这是以下情况之一：
        // If we are here and the promise is not failed, it's one of the following cases:
        // 1) 如果我们尝试从事件循环注册，注册已经在这一点上完成
        // 1) If we attempted registration from the event loop, the registration has been completed at this point.
        //    例如，现在尝试 bind ()或 connect ()是安全的，因为通道已经注册。
        //todo 如果我们 企图往事件循环中注册通道, 因为现在这个通道晶注册完毕了,所以 bind() 和 connet()是安全的
        //    i.e. It's safe to attempt bind() or connect() now because the channel has been registered.
        // 2) 如果我们尝试从另一个线程注册，注册请求已成功地添加到事件循环的任务队列中，以便稍后执行。
        // 2) If we attempted registration from the other thread, the registration request has been successfully
        //    added to the event loop's task queue for later execution.
        //    例如，现在尝试 bind ()或 connect ()是安全的
        //    i.e. It's safe to attempt bind() or connect() now:
        //         因为 bind ()或 connect ()将在执行计划的注册任务之后执行
        //         because bind() or connect() will be executed *after* the scheduled registration task is executed
        //         因为 register ()、 bind ()和 connect ()都绑定到同一个线程。
        //         because register(), bind(), and connect() are all bound to the same thread.

        return regFuture;
    }

    //todo 这是个抽象方法, 现在是服务端,很显然我们该去看ServerBootStrap的实现
    abstract void init(Channel channel) throws Exception;

    private static void doBind0(
            final ChannelFuture regFuture,
            final Channel channel,
            final SocketAddress localAddress,
            final ChannelPromise promise) {

        // This method is invoked before channelRegistered() is triggered.
        // Give user handlers a chance to set up the pipeline in its channelRegistered() implementation.
        //todo 此方法在触发channelRegistered()之前调用，给用户一个机会在channelRegistered()中设置pipeline
        //todo 这是 eventLoop启动的逻辑，下面的Runable就是一个task任务，什么任务的呢? 绑定端口
        //todo 进入exeute()
        //todo 这实际上是在开启
        channel.eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                //todo 如果channel成功注册到了选择器上，就绑定端口
                if (regFuture.isSuccess()) {
                    //todo channel绑定端口并且添加了一个listenner
                    channel.bind(localAddress, promise).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
                } else {
                    promise.setFailure(regFuture.cause());
                }
            }
        });
    }

    /**
     * the {@link ChannelHandler} to use for serving the requests.
     */
    public B handler(ChannelHandler handler) {
        this.handler = ObjectUtil.checkNotNull(handler, "handler");
        return self();
    }

    /**
     * Returns the configured {@link EventLoopGroup} or {@code null} if non is configured yet.
     *
     * @deprecated Use {@link #config()} instead.
     */
    @Deprecated
    public final EventLoopGroup group() {
        return group;
    }

    /**
     * Returns the {@link AbstractBootstrapConfig} object that can be used to obtain the current config
     * of the bootstrap.
     */
    //todo 返回一个 AbstractBootstrapConfig对象, 这个对象可以获取当前的 bootstrap 的 config
    public abstract AbstractBootstrapConfig<B, C> config();

    final Map.Entry<ChannelOption<?>, Object>[] newOptionsArray() {
        return newOptionsArray(options);
    }

    static Map.Entry<ChannelOption<?>, Object>[] newOptionsArray(Map<ChannelOption<?>, Object> options) {
        synchronized (options) {
            return new LinkedHashMap<ChannelOption<?>, Object>(options).entrySet().toArray(EMPTY_OPTION_ARRAY);
        }
    }

    final Map.Entry<AttributeKey<?>, Object>[] newAttributesArray() {
        return newAttributesArray(attrs0());
    }

    static Map.Entry<AttributeKey<?>, Object>[] newAttributesArray(Map<AttributeKey<?>, Object> attributes) {
        return attributes.entrySet().toArray(EMPTY_ATTRIBUTE_ARRAY);
    }

    //todo 返回一个 Map
    final Map<ChannelOption<?>, Object> options0() {
        // TODO:   new LinkedHashMap<ChannelOption<?>, Object>();
        return options;
    }

    final Map<AttributeKey<?>, Object> attrs0() {
        return attrs;
    }

    final SocketAddress localAddress() {
        return localAddress;
    }

    @SuppressWarnings("deprecation")
    final ChannelFactory<? extends C> channelFactory() {
        return channelFactory;
    }

    final ChannelHandler handler() {
        return handler;
    }

    final Map<ChannelOption<?>, Object> options() {
        synchronized (options) {
            return copiedMap(options);
        }
    }

    final Map<AttributeKey<?>, Object> attrs() {
        return copiedMap(attrs);
    }

    static <K, V> Map<K, V> copiedMap(Map<K, V> map) {
        if (map.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<K, V>(map));
    }

    static void setAttributes(Channel channel, Map.Entry<AttributeKey<?>, Object>[] attrs) {
        for (Map.Entry<AttributeKey<?>, Object> e: attrs) {
            @SuppressWarnings("unchecked")
            AttributeKey<Object> key = (AttributeKey<Object>) e.getKey();
            channel.attr(key).set(e.getValue());
        }
    }

    //todo 循环赋值
    static void setChannelOptions(
            Channel channel, Map.Entry<ChannelOption<?>, Object>[] options, InternalLogger logger) {
        for (Map.Entry<ChannelOption<?>, Object> e: options) {
            setChannelOption(channel, e.getKey(), e.getValue(), logger);
        }
    }

    @SuppressWarnings("unchecked")
    private static void setChannelOption(
            Channel channel, ChannelOption<?> option, Object value, InternalLogger logger) {
        try {
            if (!channel.config().setOption((ChannelOption<Object>) option, value)) {
                logger.warn("Unknown channel option '{}' for channel '{}'", option, channel);
            }
        } catch (Throwable t) {
            logger.warn(
                    "Failed to set channel option '{}' with value '{}' for channel '{}'", option, value, channel, t);
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder()
            .append(StringUtil.simpleClassName(this))
            .append('(').append(config()).append(')');
        return buf.toString();
    }

    static final class PendingRegistrationPromise extends DefaultChannelPromise {

        // Is set to the correct EventExecutor once the registration was successful. Otherwise it will
        // stay null and so the GlobalEventExecutor.INSTANCE will be used for notifications.
        private volatile boolean registered;

        PendingRegistrationPromise(Channel channel) {
            super(channel);
        }

        void registered() {
            registered = true;
        }

        @Override
        protected EventExecutor executor() {
            if (registered) {
                // If the registration was a success executor is set.
                //
                // See https://github.com/netty/netty/issues/2586
                return super.executor();
            }
            // The registration failed so we can only use the GlobalEventExecutor as last resort to notify.
            return GlobalEventExecutor.INSTANCE;
        }
    }
}
