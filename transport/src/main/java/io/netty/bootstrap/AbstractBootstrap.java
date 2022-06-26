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
 * AbstractBootstrap��һ�������࣬��ʹ����Channel��ú����ס�
 * {@link AbstractBootstrap} is a helper class that makes it easy to bootstrap a {@link Channel}.
 *
 * ��֧�ַ��������ṩһ���򵥵ķ�ʽ������bootstrap
 * It support method-chaining to provide an easy way to configure the {@link AbstractBootstrap}.
 *
 * <p>When not used in a {@link ServerBootstrap} context, the {@link #bind()} methods are useful for connectionless
 * transports such as datagram (UDP).</p>
 *
 * ������ServerBootstrap��������ʹ��ʱ��bind()�������������ӷǳ����ã����ݱ�(datagram��UDP)�ȴ��䡣
 */

//todo B extends AbstractBootstrap<B, C> ��ʾ, B��AbstractBootstrap��һ������
//todo C extends Channel ��ʾ, C��Channel ��һ������
//todo ��ServerBootStrap�У� ��һ���������� ServerBootstrap extends AbstractBootstrap<ServerBootstrap, ServerChannel> {������}
//todo B === ServerBootstrap
public abstract class AbstractBootstrap<B extends AbstractBootstrap<B, C>, C extends Channel> implements Cloneable {

    private static final Map.Entry<ChannelOption<?>, Object>[] EMPTY_OPTION_ARRAY = new Map.Entry[0];
    private static final Map.Entry<AttributeKey<?>, Object>[] EMPTY_ATTRIBUTE_ARRAY = new Map.Entry[0];

    volatile EventLoopGroup group;

    private volatile ChannelFactory<? extends C> channelFactory;
    private volatile SocketAddress localAddress;

    // The order in which ChannelOptions are applied is important they may depend on each other for validation purposes.
    //todo ChannelOptions��Ӧ��˳��ǳ���Ҫ�����ǿ����໥�����Խ�����֤��
    private final Map<ChannelOption<?>, Object> options = new LinkedHashMap<ChannelOption<?>, Object>();
    // attributeKey ��������ʲô
    private final Map<AttributeKey<?>, Object> attrs = new ConcurrentHashMap<AttributeKey<?>, Object>();

    private volatile ChannelHandler handler;

    AbstractBootstrap() {
        // Disallow extending from a different package.
        //������Ӳ�ͬ�İ���չ
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
    //todo ����¼�ѭ���飬�����ڴ������еĽ���������chanel
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
     * ����Ҫ��ʵ������Channel��
     */
     //todo ����ֵ������B����Ϊ����ʽ���ĵ��ã������������� B == ServerBootStrap
     //todo ���ܵ�������C�����������C��Channel���͵�Class����
     //todo ʹ��channel()�ᴴ��һ��ServerBootStrap���󣬵���������ǵ�Bû���޲εĹ��췽����ֻ��ʹ��channelFactory()
    public B channel(Class<? extends C> channelClass) {
        //todo ���Ǵ��ݽ����� NioServerSocketChannel���󣬸�ֵ����RegflectiveChannelFactory
        return channelFactory(new ReflectiveChannelFactory<C>(
                ObjectUtil.checkNotNull(channelClass, "channelClass")
        ));
    }

    /**
     *  Use {@link #channelFactory(io.netty.channel.ChannelFactory)} instead.
     */
    //todo ��ǰ�����Ѿ��������ˣ�������Ȼ�ǽ�����
    //todo Ψһ�Ĺ������ǰѸղ�ӵ��NioserverSocketChannel.class�� RefletiveChannelFactory��ʼ����
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
    //todo ����������һ������˵�Channel���Ұ�һ���˿ں�
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
     * ����һ��channel���Ұ�
     */
    public ChannelFuture bind(SocketAddress localAddress) {
        validate();
        //todo ִ��doBind()������������ķ�����ʾnetty˽�еķ���������ȥ
        return doBind(ObjectUtil.checkNotNull(localAddress, "localAddress"));
    }

    /**
     * todo �ر���Ҫ!!!
     * @param localAddress
     * @return
     */
    private ChannelFuture doBind(final SocketAddress localAddress) {
        //��ʼ����ע��һ�� Channel ������Ϊע�����첽�Ĺ��̣����Է���һ�� ChannelFuture ����
        //todo ��ʼ����ע��Channel���󣬴�Future���۵ı�ʾ�첽!!!�������صľ���һ��ChannelFuture
        final ChannelFuture regFuture = initAndRegister();

        final Channel channel = regFuture.channel();
        if (regFuture.cause() != null) {
            return regFuture;
        }

        //���ܿ϶�register��ɣ���Ϊregister�Ƕ���nio event loop����ִ��ȥ�ˡ�
        if (regFuture.isDone()) {
            // At this point we know that the registration was complete and successful.
            ChannelPromise promise = channel.newPromise();
            // TODO  �����󶨶˿� doBind0
            doBind0(regFuture, channel, localAddress, promise);
            return promise;
        } else {
            // Registration future is almost always fulfilled already, but just in case it's not.
            // future��ע�Ἰ�������Ѿ�ʵ�֣����Է���һ��û����ɡ� // futureû�����
            final PendingRegistrationPromise promise = new PendingRegistrationPromise(channel);
            //����register�����֪ͨ��ִ��bind
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

    //todo ��ʼ����ע��
    final ChannelFuture initAndRegister() {
        Channel channel = null;
        try {
            // ͨ����������һ�� channel
            //todo ���channelFactory�Ƿ��乤��ReflectiveChannelFactory�Է������˵�����Դ���NioServerSocketChannel����
            //todo �������������Selector��һ��ʵ�֣�����SelectorProvider.providor()����
            //todo ʵ����NioServerSocketChannel��ͨ�������ߵ����޲εĹ��죬����ȥ׷�������޲ι���ȥ
            channel = channelFactory.newChannel();

            //todo ��ʼ��Channel���ü��ָ�ֵ�Լ����handler�����
            init(channel);
        } catch (Throwable t) {
            //channel ��������
            if (channel != null) {
                // channel can be null if newChannel crashed (eg SocketException("too many open files"))
                channel.unsafe().closeForcibly();

                // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
                return new DefaultChannelPromise(channel, GlobalEventExecutor.INSTANCE).setFailure(t);
            }
            // as the Channel is not registered yet we need to force the usage of the GlobalEventExecutor
            return new DefaultChannelPromise(new FailedChannel(), GlobalEventExecutor.INSTANCE).setFailure(t);
        }

        //todo  ע�� group  == BOSS EventLoopGroup , -- > ��ʱ��Ϊ, ������ȷ����ͨ�����䴴��������NioServerSocketChannelע��� BossGroup
        //todo  Ŀ����,��ͨ�����NioServerSocketChannel�е�ServerSocketChannel ȥ accept�ͻ��˵�����, ����������ͨ��Acceptor �Ӹ� WorkerGroup
        //todo   config()--> ServerBootstrapConfig
        //todo   group()--> NioEventLoopGroup -- workerGroup
        //todo   �����û����ȥ  ���� EventLoopGroup. �� Debug ������� MultithreadEventLoopGroup�� , ��Ϊ��������� NioEventLoopGroup �� MultithreadEventLoopGroup�������
        //todo  !!! ���Ե�һ���ص�, group�� MultithreadEventLoopGroup��  ����֪���������ά������ BossGroup, ����channelע���bossgroup��
        ChannelFuture regFuture = config().group().register(channel);
        if (regFuture.cause() != null) {//todo �ǿձ�ʾע��ʧ����
            if (channel.isRegistered()) {
                channel.close();
            } else {
                channel.unsafe().closeForcibly();
            }
        }
        //todo ��������ڴ��Ľ����û��ʧ��, �ͻ�������漸�����
        // ���������������ҳ�ŵû��ʧ�ܣ������������֮һ��
        // If we are here and the promise is not failed, it's one of the following cases:
        // 1) ������ǳ��Դ��¼�ѭ��ע�ᣬע���Ѿ�����һ�������
        // 1) If we attempted registration from the event loop, the registration has been completed at this point.
        //    ���磬���ڳ��� bind ()�� connect ()�ǰ�ȫ�ģ���Ϊͨ���Ѿ�ע�ᡣ
        //todo ������� ��ͼ���¼�ѭ����ע��ͨ��, ��Ϊ�������ͨ����ע�������,���� bind() �� connet()�ǰ�ȫ��
        //    i.e. It's safe to attempt bind() or connect() now because the channel has been registered.
        // 2) ������ǳ��Դ���һ���߳�ע�ᣬע�������ѳɹ�����ӵ��¼�ѭ������������У��Ա��Ժ�ִ�С�
        // 2) If we attempted registration from the other thread, the registration request has been successfully
        //    added to the event loop's task queue for later execution.
        //    ���磬���ڳ��� bind ()�� connect ()�ǰ�ȫ��
        //    i.e. It's safe to attempt bind() or connect() now:
        //         ��Ϊ bind ()�� connect ()����ִ�мƻ���ע������֮��ִ��
        //         because bind() or connect() will be executed *after* the scheduled registration task is executed
        //         ��Ϊ register ()�� bind ()�� connect ()���󶨵�ͬһ���̡߳�
        //         because register(), bind(), and connect() are all bound to the same thread.

        return regFuture;
    }

    //todo ���Ǹ����󷽷�, �����Ƿ����,����Ȼ���Ǹ�ȥ��ServerBootStrap��ʵ��
    abstract void init(Channel channel) throws Exception;

    private static void doBind0(
            final ChannelFuture regFuture,
            final Channel channel,
            final SocketAddress localAddress,
            final ChannelPromise promise) {

        // This method is invoked before channelRegistered() is triggered.
        // Give user handlers a chance to set up the pipeline in its channelRegistered() implementation.
        //todo �˷����ڴ���channelRegistered()֮ǰ���ã����û�һ��������channelRegistered()������pipeline
        //todo ���� eventLoop�������߼��������Runable����һ��task����ʲô�������? �󶨶˿�
        //todo ����exeute()
        //todo ��ʵ�������ڿ���
        channel.eventLoop().execute(new Runnable() {
            @Override
            public void run() {
                //todo ���channel�ɹ�ע�ᵽ��ѡ�����ϣ��Ͱ󶨶˿�
                if (regFuture.isSuccess()) {
                    //todo channel�󶨶˿ڲ��������һ��listenner
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
    //todo ����һ�� AbstractBootstrapConfig����, ���������Ի�ȡ��ǰ�� bootstrap �� config
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

    //todo ����һ�� Map
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

    //todo ѭ����ֵ
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
